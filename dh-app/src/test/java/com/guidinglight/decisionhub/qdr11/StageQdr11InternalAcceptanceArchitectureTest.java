package com.guidinglight.decisionhub.qdr11;

import static org.assertj.core.api.Assertions.assertThat;

import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceService;
import com.guidinglight.decisionhub.usecase.qdr.report.DecisionEvidenceReplayInternalReport;
import com.guidinglight.decisionhub.usecase.qdr.report.DecisionEvidenceReplayReportService;
import com.guidinglight.decisionhub.usecase.qdr.report.DecisionFeedbackInternalAcceptanceService;
import com.guidinglight.decisionhub.usecase.qdr.report.InternalAcceptanceStatus;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Stage-QDR-11 单一 evidence authority、唯一 facade 与无外部副作用架构守卫。 */
class StageQdr11InternalAcceptanceArchitectureTest {

  @Test
  void evaluatorPublicSurfaceConsumesOnlyConsolidatedEvidenceAuthority() {
    final List<Method> publicGenerateMethods =
        Arrays.stream(DecisionEvidenceReplayReportService.class.getDeclaredMethods())
            .filter(method -> method.getName().equals("generate"))
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .toList();

    assertThat(publicGenerateMethods).hasSize(1);
    assertThat(publicGenerateMethods.getFirst().getParameterTypes()[0])
        .isEqualTo(DecisionFeedbackEvidenceAggregate.class);
    assertThat(
            Arrays.stream(publicGenerateMethods.getFirst().getParameterTypes())
                .anyMatch(DecisionEvidenceAggregate.class::equals))
        .isFalse();
    assertThat(publicGenerateMethods.getFirst().getReturnType())
        .isEqualTo(DecisionEvidenceReplayInternalReport.class);
  }

  @Test
  void facadeHasOnePublicProductionConstructorAndNoExecutionDependency() {
    assertThat(
            Arrays.stream(DecisionFeedbackInternalAcceptanceService.class.getDeclaredConstructors())
                .filter(constructor -> Modifier.isPublic(constructor.getModifiers())))
        .singleElement()
        .satisfies(
            constructor ->
                assertThat(constructor.getParameterTypes())
                    .containsExactly(
                        DecisionFeedbackEvidenceService.class,
                        DecisionEvidenceReplayReportService.class));
    assertThat(InternalAcceptanceStatus.ACCEPTED.authorizesProvider()).isFalse();
    assertThat(InternalAcceptanceStatus.ACCEPTED.authorizesNqIntegration()).isFalse();
    assertThat(InternalAcceptanceStatus.ACCEPTED.allowsTradingOrExecution()).isFalse();
    assertThat(InternalAcceptanceStatus.ACCEPTED.enablesPaperOrLive()).isFalse();
  }

  @Test
  void reportAndFacadeSourceHaveNoApiPersistenceExternalIoLearningOrRuntimeDependency()
      throws IOException {
    final Path reportRoot =
        repositoryRoot()
            .resolve("dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/report");
    final String source;
    try (var paths = Files.walk(reportRoot)) {
      source =
          paths.filter(path -> path.toString().endsWith(".java"))
              .map(StageQdr11InternalAcceptanceArchitectureTest::read)
              .reduce("", String::concat);
    }
    final List<Pattern> forbidden =
        List.of(
            Pattern.compile("org\\.springframework"),
            Pattern.compile("com\\.guidinglight\\.decisionhub\\.infra"),
            Pattern.compile("\\bController\\b"),
            Pattern.compile("\\bRepository\\b"),
            Pattern.compile("\\bJdbc"),
            Pattern.compile("java\\.net"),
            Pattern.compile("WebClient"),
            Pattern.compile("RestTemplate"),
            Pattern.compile("ExperienceStore"),
            Pattern.compile("PheromoneStore"),
            Pattern.compile("FailureCaseStore"),
            Pattern.compile("import\\s+.*LangGraph"),
            Pattern.compile("Instant\\.now"),
            Pattern.compile("randomUUID"),
            Pattern.compile("\\.insert\\("),
            Pattern.compile("\\.save\\("));

    forbidden.forEach(pattern -> assertThat(pattern.matcher(source).find()).as(pattern.toString()).isFalse());
  }

  private static Path repositoryRoot() {
    final Path cwd = Path.of("").toAbsolutePath();
    return Files.isDirectory(cwd.resolve("dh-usecase")) ? cwd : cwd.getParent();
  }

  private static String read(final Path path) {
    try {
      return Files.readString(path);
    } catch (final IOException error) {
      throw new IllegalStateException("qdr11 architecture source scan failed", error);
    }
  }
}

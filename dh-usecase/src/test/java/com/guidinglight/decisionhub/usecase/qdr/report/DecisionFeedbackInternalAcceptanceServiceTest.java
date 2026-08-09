package com.guidinglight.decisionhub.usecase.qdr.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceQuery;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceService;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayObservabilityReport;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessEvaluationResult;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionReportView;
import com.guidinglight.decisionhub.usecase.qdr.replay.deterministic.DeterministicReplayResult;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Stage-QDR-11 internal facade 的单一 authority、单次读取与无 fallback 回归。 */
class DecisionFeedbackInternalAcceptanceServiceTest {

  @Test
  void publicSurfacePreservesFrozenDependenciesAndInputs() {
    assertEquals(
        1,
        Arrays.stream(DecisionFeedbackInternalAcceptanceService.class.getConstructors()).count());
    assertTrue(
        Arrays.equals(
            new Class<?>[] {
              DecisionFeedbackEvidenceService.class, DecisionEvidenceReplayReportService.class
            },
            DecisionFeedbackInternalAcceptanceService.class
                .getConstructors()[0]
                .getParameterTypes()));
    final Method evaluate =
        Arrays.stream(DecisionFeedbackInternalAcceptanceService.class.getDeclaredMethods())
            .filter(method -> method.getName().equals("evaluate"))
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .findFirst()
            .orElseThrow();
    assertTrue(
        Arrays.equals(
            new Class<?>[] {
              DecisionFeedbackEvidenceQuery.class,
              DeterministicReplayResult.class,
              RegressionReportView.class,
              ProviderReadinessEvaluationResult.class,
              ModelGatewayObservabilityReport.class
            },
            evaluate.getParameterTypes()));
    assertEquals(DecisionEvidenceReplayInternalReport.class, evaluate.getReturnType());
  }

  @Test
  void sourceHasOneAggregateReadOneEvaluationAndNoLegacyFallback() throws IOException {
    final String source = Files.readString(sourcePath());

    assertEquals(1, occurrences(source, "evidenceService.aggregate("));
    assertEquals(1, occurrences(source, "reportService.generate("));
    assertFalse(source.contains("DecisionEvidenceAggregate"));
    assertFalse(source.contains("fallback"));
    assertFalse(source.contains("retry"));
    assertTrue(source.contains("INTERNAL_ACCEPTANCE_FAILURE"));
  }

  private static int occurrences(final String source, final String token) {
    return source.split(java.util.regex.Pattern.quote(token), -1).length - 1;
  }

  private static Path sourcePath() {
    final Path cwd = Path.of("").toAbsolutePath();
    final Path module = Files.isDirectory(cwd.resolve("src/main/java")) ? cwd : cwd.resolve("dh-usecase");
    return module.resolve(
        "src/main/java/com/guidinglight/decisionhub/usecase/qdr/report/DecisionFeedbackInternalAcceptanceService.java");
  }
}

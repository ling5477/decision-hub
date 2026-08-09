package com.guidinglight.decisionhub.qdr10;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceAggregateService;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEnvironmentProvenanceQueryPort;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceFinding;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceQuery;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceService;
import com.guidinglight.decisionhub.usecase.qdr.evidence.EvidenceCompleteness;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceReadService;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Stage-QDR-10 read-only internal evidence consolidation 架构守卫。 */
class StageQdr10EvidenceArchitectureTest {

    private static final Path REPOSITORY_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path PRODUCTION_PACKAGE = REPOSITORY_ROOT.resolve(
            "dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence");

    @Test
    void consolidatedTypesRemainFrameworkIndependentAndInternal() {
        final var classes = new ClassFileImporter().importPackages(
                "com.guidinglight.decisionhub.usecase.qdr.evidence");

        noClasses()
                .that()
                .resideInAPackage("..usecase.qdr.evidence")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..infra..",
                        "..api..",
                        "org.springframework..",
                        "java.net..",
                        "java.net.http..",
                        "..agent..",
                        "..langgraph..",
                        "..trading..",
                        "..execution..")
                .check(classes);
    }

    @Test
    void productionConstructorOnlyConsumesExistingReadServices() {
        assertThat(DecisionFeedbackEvidenceService.class.getConstructors())
                .singleElement()
                .extracting(java.lang.reflect.Constructor::getParameterTypes)
                .isEqualTo(new Class<?>[] {
                    DecisionEvidenceAggregateService.class,
                    DecisionEnvironmentProvenanceQueryPort.class,
                    HistoricalFeedbackEvidenceReadService.class
                });
    }

    @Test
    void aggregateSurfaceContainsOnlySafeReadModels() {
        assertThat(DecisionFeedbackEvidenceQuery.class).matches(Class::isRecord);
        assertThat(DecisionFeedbackEvidenceAggregate.class).matches(Class::isRecord);
        assertThat(DecisionFeedbackEvidenceFinding.class).matches(Class::isRecord);
        assertThat(EvidenceCompleteness.values())
                .containsExactly(
                        EvidenceCompleteness.COMPLETE_WITHIN_BOUNDS,
                        EvidenceCompleteness.PARTIAL_WITHIN_BOUNDS,
                        EvidenceCompleteness.INCONSISTENT,
                        EvidenceCompleteness.NOT_FOUND);

        final Set<String> forbiddenComponents = Set.of(
                "rawprompt",
                "rawproviderresponse",
                "credential",
                "apikey",
                "apisecret",
                "signature",
                "nonce",
                "payload",
                "orderid",
                "tradingauthorization");
        Stream.of(DecisionFeedbackEvidenceQuery.class, DecisionFeedbackEvidenceAggregate.class)
                .flatMap(type -> Stream.of(type.getRecordComponents()))
                .map(component -> component.getName().toLowerCase(Locale.ROOT))
                .forEach(component -> assertThat(forbiddenComponents).doesNotContain(component));
    }

    @Test
    void productionSourcesAddNoControllerRepositoryClientOrLearningMutation() throws IOException {
        final String source = String.join("\n", List.of(
                "DecisionFeedbackEvidenceQuery.java",
                "DecisionFeedbackEvidenceAggregate.java",
                "EvidenceCompleteness.java",
                "DecisionFeedbackEvidenceFinding.java",
                "DecisionFeedbackEvidenceService.java",
                "BoundedEvidencePolicy.java",
                "DecisionEnvironmentProvenance.java",
                "DecisionEnvironmentProvenanceQuery.java",
                "DecisionEnvironmentProvenanceQueryPort.java").stream()
                .map(PRODUCTION_PACKAGE::resolve)
                .map(StageQdr10EvidenceArchitectureTest::read)
                .toList());

        List.of(
                        "@RestController",
                        "@Controller",
                        "JdbcTemplate",
                        "EntityManager",
                        "WebClient",
                        "RestTemplate",
                        "java.net.http",
                        "ExperienceFeedbackService",
                        "ExperienceStore",
                        "PheromoneStore",
                        "FailureCaseStore")
                .forEach(forbidden -> assertThat(source).doesNotContain(forbidden));
    }

    private static String read(final Path path) {
        try {
            return Files.readString(path);
        } catch (final IOException error) {
            throw new IllegalStateException(path.toString(), error);
        }
    }
}

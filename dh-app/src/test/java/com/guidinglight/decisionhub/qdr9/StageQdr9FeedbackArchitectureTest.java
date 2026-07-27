package com.guidinglight.decisionhub.qdr9;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackAttributionRepository;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackAttributionPersistenceService;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceTransactionBoundary;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackReferenceValidationPort;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Stage-QDR-9 B2 persistence/schema 边界守卫。 */
class StageQdr9FeedbackArchitectureTest {

  private static final Path REPOSITORY_ROOT =
      Path.of("..").toAbsolutePath().normalize();

  @Test
  void persistenceContractsRemainUsecaseOwnedAndFrameworkIndependent() {
    final var classes =
        new ClassFileImporter()
            .importPackages(
                "com.guidinglight.decisionhub.usecase.qdr.feedback");

    noClasses()
        .that()
        .resideInAPackage(
            "com.guidinglight.decisionhub.usecase.qdr.feedback")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..infra..",
            "..api..",
            "org.springframework..",
            "java.net..",
            "java.net.http..")
        .check(classes);
  }

  @Test
  void b2KeepsPortsUsecaseOwnedAndLimitsImplementationToTransactionalPersistence() {
    assertThat(FeedbackAttributionRepository.class).isInterface();
    assertThat(FeedbackReferenceValidationPort.class).isInterface();
    assertThat(FeedbackPersistenceTransactionBoundary.class).isInterface();
    assertThat(FeedbackPersistenceRecords.OutcomeObservationRecord.class)
        .matches(Class::isRecord);
    assertThat(FeedbackPersistenceRecords.FeedbackAttributionRecord.class)
        .matches(Class::isRecord);
    assertThat(FeedbackPersistenceRecords.AttributionContributionRecord.class)
        .matches(Class::isRecord);
    assertThat(FeedbackPersistenceRecords.AttributionReferenceRecord.class)
        .matches(Class::isRecord);
    assertThat(FeedbackPersistenceRecords.FeedbackPersistenceAggregate.class)
        .matches(Class::isRecord);
    assertThat(FeedbackAttributionPersistenceService.class)
        .matches(type -> !type.getPackageName().contains(".infra."));

    assertThat(
            REPOSITORY_ROOT.resolve(
                "dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/"
                    + "JdbcFeedbackAttributionRepository.java"))
        .exists();
    assertThat(
            REPOSITORY_ROOT.resolve(
                "dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/"
                    + "JdbcFeedbackReferenceValidationAdapter.java"))
        .exists();
    assertThat(
            REPOSITORY_ROOT.resolve(
                "dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/"
                    + "JdbcFeedbackPersistenceTransactionBoundary.java"))
        .exists();

    forbiddenB4Files().forEach(path -> assertThat(path).as("B4 file must remain absent").doesNotExist());
  }

  @Test
  void qdr9AggregateHasNoSensitivePayloadOrTradingComponents() {
    final Set<String> forbiddenComponents =
        Set.of(
            "rawPrompt",
            "rawProviderResponse",
            "credential",
            "authorization",
            "apiKey",
            "apiSecret",
            "password",
            "payloadJson",
            "canonicalValue",
            "orderId",
            "orderType",
            "orderAction",
            "marketOrder",
            "riskMutation",
            "ledger",
            "paper",
            "live");

    Stream.of(
            FeedbackPersistenceRecords.OutcomeObservationRecord.class,
            FeedbackPersistenceRecords.FeedbackAttributionRecord.class,
            FeedbackPersistenceRecords.AttributionContributionRecord.class,
            FeedbackPersistenceRecords.AttributionReferenceRecord.class)
        .flatMap(type -> Stream.of(type.getRecordComponents()))
        .map(component -> component.getName().toLowerCase(java.util.Locale.ROOT))
        .forEach(
            component ->
                assertThat(forbiddenComponents)
                    .noneMatch(
                        forbidden ->
                            component.equals(
                                forbidden.toLowerCase(java.util.Locale.ROOT))));
  }

  @Test
  void migrationDoesNotMutateHistoricalFeedbackOrAddRuntimeSurface() throws IOException {
    final Path migration =
        REPOSITORY_ROOT.resolve(
            "dh-app/src/main/resources/db/migration/"
                + "V15__qdr9_structured_feedback_persistence.sql");
    final String body = Files.readString(migration).toLowerCase(java.util.Locale.ROOT);

    assertThat(body).doesNotContain(" jsonb");
    assertThat(body).doesNotContain("alter table dh_nq_feedback_events");
    assertThat(body).doesNotContain("insert into dh_nq_feedback_events");
    assertThat(body).doesNotContain("update dh_nq_feedback_events");
    assertThat(body).doesNotContain("delete from dh_nq_feedback_events");
    assertThat(body).doesNotContain("create controller");
    assertThat(body).doesNotContain("openapi");
    assertThat(body).doesNotContain("http://");
    assertThat(body).doesNotContain("https://");
  }

  @Test
  void b3ReadModelRemainsInternalReadOnlyAndKeysetBounded() throws IOException {
    final Path adapter =
        REPOSITORY_ROOT.resolve(
            "dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/"
                + "JdbcHistoricalFeedbackEvidenceQueryAdapter.java");
    final String source = Files.readString(adapter).toLowerCase(java.util.Locale.ROOT);
    assertThat(adapter).exists();
    assertThat(source).contains("order by a.observed_at desc,a.attribution_id desc");
    assertThat(source).doesNotContain(" offset ").doesNotContain("insert into").doesNotContain("delete from");
    assertThat(source).contains("tenant_id=? and a.environment=?").contains("setreadonly(true)");
    assertThat(
            REPOSITORY_ROOT.resolve(
                "dh-api/src/main/java/com/guidinglight/decisionhub/api/qdr/feedback"))
        .doesNotExist();
  }

  @Test
  void signedEnvironmentAuthorityRemainsExplicitAndNoProducerRuntimeIsAdded() throws IOException {
    final String environment =
        source("dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/"
            + "FeedbackEnvironment.java");
    final String scope =
        source("dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/"
            + "FeedbackExecutionScope.java");
    final String authenticator =
        source("dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/"
            + "HmacNqDryRunAuthenticator.java");
    final String controller =
        source("dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/"
            + "DecisionDryRunController.java");
    final String auditRecord =
        source("dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/"
            + "DecisionPersistenceRecords.java");

    assertThat(environment).contains("DEV", "TEST").doesNotContain("LIVE", "PROD", "PAPER");
    assertThat(scope)
        .contains("FeedbackExecutionScope(String tenantId, FeedbackEnvironment environment)")
        .doesNotContain("ThreadLocal", "FeedbackEnvironment.DEV", "getActiveProfiles");
    assertThat(authenticator)
        .contains("FeedbackEnvironment")
        .doesNotContain("FeedbackExecutionScope", "ThreadLocal", "FeedbackEnvironment.DEV");
    assertThat(controller)
        .contains("new FeedbackExecutionScope", ".withExecutionScope(")
        .doesNotContain("ThreadLocal", "FeedbackEnvironment.DEV", "getActiveProfiles");
    assertThat(auditRecord)
        .contains("FeedbackEnvironment environment", "payload.put(\"environment\", environment.name())");

    try (Stream<Path> migrations =
        Files.list(REPOSITORY_ROOT.resolve("dh-app/src/main/resources/db/migration"))) {
      assertThat(migrations.map(path -> path.getFileName().toString()))
          .noneMatch(name -> name.startsWith("V16__"));
    }
    List.of(
            "dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/"
                + "QdrReferenceLivenessRegistry.java",
            "dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/"
                + "JdbcQdrReferenceLivenessRegistry.java",
            "dh-app/src/main/java/com/guidinglight/decisionhub/qdr9/"
                + "FeedbackRetentionScheduler.java",
            "dh-api/src/main/java/com/guidinglight/decisionhub/api/qdr/replay/"
                + "QdrReplayController.java",
            "dh-api/src/main/java/com/guidinglight/decisionhub/api/qdr/replay/"
                + "QdrEvaluationController.java")
        .forEach(path -> assertThat(REPOSITORY_ROOT.resolve(path)).doesNotExist());
  }

  private static String source(final String relativePath) throws IOException {
    return Files.readString(REPOSITORY_ROOT.resolve(relativePath));
  }

  private static List<Path> forbiddenB4Files() {
    return List.of(
        REPOSITORY_ROOT.resolve(
            "dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/"
                + "FeedbackRetentionService.java"),
        REPOSITORY_ROOT.resolve(
            "dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/"
                + "JdbcFeedbackRetentionAdapter.java"));
  }
}

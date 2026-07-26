package com.guidinglight.decisionhub.qdr9;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackAttributionRepository;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackAttributionPersistenceService;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceTransactionBoundary;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackReferenceValidationPort;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackRetentionPort;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackRetentionService;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
  void b2AndB4KeepPortsUsecaseOwnedAndInternal() {
    assertThat(FeedbackAttributionRepository.class).isInterface();
    assertThat(FeedbackReferenceValidationPort.class).isInterface();
    assertThat(FeedbackPersistenceTransactionBoundary.class).isInterface();
    assertThat(FeedbackRetentionPort.class).isInterface();
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
    assertThat(FeedbackRetentionService.class)
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

    assertThat(
            REPOSITORY_ROOT.resolve(
                "dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/"
                    + "JdbcFeedbackRetentionAdapter.java"))
        .exists();
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
  void b4RetentionRemainsInternalBoundedAndWithoutSchedulerOrForwardMigration()
      throws IOException {
    final Path adapter =
        REPOSITORY_ROOT.resolve(
            "dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/feedback/"
                + "JdbcFeedbackRetentionAdapter.java");
    final Path service =
        REPOSITORY_ROOT.resolve(
            "dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/"
                + "FeedbackRetentionService.java");
    final Path wiring =
        REPOSITORY_ROOT.resolve(
            "dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java");
    final String retentionSource =
        (Files.readString(adapter) + Files.readString(service)).toLowerCase(java.util.Locale.ROOT);
    final String wiringSource = Files.readString(wiring).toLowerCase(java.util.Locale.ROOT);

    assertThat(retentionSource)
        .contains("for update skip locked")
        .contains("set local statement_timeout")
        .doesNotContain("@scheduled")
        .doesNotContain("requestmapping")
        .doesNotContain("restcontroller")
        .doesNotContain("automatic learning")
        .doesNotContain("httpclient")
        .doesNotContain("webclient")
        .doesNotContain("langgraph");
    assertThat(wiringSource).contains("feedbackretentionservice");
    assertThat(
            REPOSITORY_ROOT.resolve(
                "dh-app/src/main/resources/db/migration/V16__qdr9_feedback_retention.sql"))
        .doesNotExist();
    assertThat(
            REPOSITORY_ROOT.resolve(
                "dh-api/src/main/java/com/guidinglight/decisionhub/api/qdr/feedback"))
        .doesNotExist();
  }
}

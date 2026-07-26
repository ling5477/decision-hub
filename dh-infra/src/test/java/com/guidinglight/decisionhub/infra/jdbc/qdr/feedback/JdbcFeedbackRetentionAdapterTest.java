package com.guidinglight.decisionhub.infra.jdbc.qdr.feedback;

import static org.assertj.core.api.Assertions.assertThat;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceErrorCode;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackRetentionCommand;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackRetentionResult;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackRetentionService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Real PostgreSQL 17 retention safety, transaction, isolation, and concurrency regression coverage. */
@Testcontainers(disabledWithoutDocker = true)
class JdbcFeedbackRetentionAdapterTest {

  private static final Instant NOW = Instant.parse("2026-07-26T12:00:00Z");

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"));

  private JdbcTemplate jdbc;
  private FeedbackRetentionService service;

  @BeforeEach
  void resetSchema() {
    jdbc = new JdbcTemplate(dataSource());
    jdbc.execute("drop schema public cascade");
    jdbc.execute("create schema public");
    executeV15();
    createReferenceSourceTables();
    service = service(jdbc);
  }

  @AfterAll
  static void stopContainer() {
    POSTGRES.stop();
  }

  @Test
  void disabledCutoffBoundaryAndReconnectRemainSafe() {
    insertAggregate(1, "tenant-a", "DEV", NOW.minus(Duration.ofDays(366)), "EVIDENCE", "ACTIVE", "evidence:old");
    insertAggregate(2, "tenant-a", "DEV", NOW.minus(Duration.ofDays(365)), "EVIDENCE", "ACTIVE", "evidence:boundary");
    insertAggregate(3, "tenant-a", "DEV", NOW.minus(Duration.ofDays(364)), "EVIDENCE", "ACTIVE", "evidence:new");

    final FeedbackRetentionResult disabled =
        service.cleanup(FeedbackRetentionCommand.defaults("tenant-a", FeedbackEnvironment.DEV));
    assertThat(disabled.status()).isEqualTo(FeedbackRetentionResult.Status.DISABLED);
    assertThat(count("qdr_feedback_outcome_observation")).isEqualTo(3);

    final FeedbackRetentionResult cleanup = service(dataSource()).cleanup(command("tenant-a", FeedbackEnvironment.DEV, 100));

    assertThat(cleanup.deletedAggregateCount()).isEqualTo(1);
    assertThat(cleanup.hasMoreCandidates()).isFalse();
    assertThat(count("qdr_feedback_outcome_observation")).isEqualTo(2);
    assertThat(count("qdr_feedback_attribution")).isEqualTo(2);
    assertThat(count("qdr_feedback_attribution_contribution")).isEqualTo(2);
    assertThat(count("qdr_feedback_attribution_reference")).isEqualTo(2);
    assertThat(count("dh_decision_audit_event")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select event_json->>'environment' from dh_decision_audit_event", String.class))
        .isEqualTo("DEV");
    assertThat(
            jdbc.queryForObject(
                "select event_json->>'reasonCode' from dh_decision_audit_event", String.class))
        .isEqualTo("RETENTION_AGE_EXPIRED");
  }

  @Test
  void activeAuditReplayAndEvaluationReferencesBlockDeletion() {
    insertAudit("audit-1", "tenant-a", "decision-1", "trace-1");
    final UUID replay = UUID.nameUUIDFromBytes("replay-1".getBytes(StandardCharsets.UTF_8));
    final UUID evaluation = UUID.nameUUIDFromBytes("evaluation-1".getBytes(StandardCharsets.UTF_8));
    jdbc.update("insert into qdr_replay_case (id,tenant_id,case_id) values (?,?,?)", replay, "tenant-a", "replay-1");
    jdbc.update("insert into qdr_evaluation_case (id,tenant_id,evaluation_id) values (?,?,?)", evaluation, "tenant-a", "evaluation-1");
    insertAggregate(1, "tenant-a", "DEV", NOW.minus(Duration.ofDays(366)), "AUDIT", "ACTIVE", "audit:audit-1");
    insertAggregate(2, "tenant-a", "DEV", NOW.minus(Duration.ofDays(367)), "REPLAY", "ACTIVE", "replay-case:" + replay);
    insertAggregate(3, "tenant-a", "DEV", NOW.minus(Duration.ofDays(368)), "EVALUATION", "ACTIVE", "evaluation:" + evaluation);

    final FeedbackRetentionResult result = service.cleanup(command("tenant-a", FeedbackEnvironment.DEV, 100));

    assertThat(result.status()).isEqualTo(FeedbackRetentionResult.Status.COMPLETED);
    assertThat(result.blockedByReferenceCount()).isEqualTo(3);
    assertThat(result.deletedAggregateCount()).isZero();
    assertThat(count("qdr_feedback_outcome_observation")).isEqualTo(3);
  }

  @Test
  void releasedReferenceMustStillResolveAndMissingOrMalformedReferenceFailsClosed() {
    insertAudit("audit-released", "tenant-a", "decision-1", "trace-1");
    insertAggregate(1, "tenant-a", "DEV", NOW.minus(Duration.ofDays(366)), "AUDIT", "RELEASED", "audit:audit-released");
    insertAggregate(2, "tenant-a", "DEV", NOW.minus(Duration.ofDays(367)), "AUDIT", "ACTIVE", "audit:missing");

    final FeedbackRetentionResult result = service.cleanup(command("tenant-a", FeedbackEnvironment.DEV, 100));

    assertThat(result.status()).isEqualTo(FeedbackRetentionResult.Status.FAILED);
    assertThat(result.failureCode()).isEqualTo(FeedbackPersistenceErrorCode.REFERENCE_STATUS_UNKNOWN);
    assertThat(count("qdr_feedback_outcome_observation")).isEqualTo(2);
    deleteAggregateDirectly(hash(2), "observation-2");
    final FeedbackRetentionResult released = service.cleanup(command("tenant-a", FeedbackEnvironment.DEV, 100));
    assertThat(released.deletedAggregateCount()).isEqualTo(1);
    assertThat(count("qdr_feedback_outcome_observation")).isZero();
  }

  @Test
  void invalidMalformedAndTenantMismatchedReferencesFailClosed() {
    insertAudit("audit-invalid", "tenant-a", "decision-1", "trace-1");
    insertAggregate(
        1,
        "tenant-a",
        "DEV",
        NOW.minus(Duration.ofDays(366)),
        "AUDIT",
        "INVALID",
        "audit:audit-invalid");

    final FeedbackRetentionResult invalid = service.cleanup(command("tenant-a", FeedbackEnvironment.DEV, 100));

    assertThat(invalid.status()).isEqualTo(FeedbackRetentionResult.Status.FAILED);
    assertThat(invalid.failureCode()).isEqualTo(FeedbackPersistenceErrorCode.RETENTION_BLOCKED);
    assertThat(count("qdr_feedback_outcome_observation")).isEqualTo(1);

    resetSchema();
    jdbc.execute(
        "alter table qdr_feedback_attribution_reference drop constraint chk_qdr_feedback_reference_scheme");
    insertAggregate(
        1,
        "tenant-a",
        "DEV",
        NOW.minus(Duration.ofDays(366)),
        "AUDIT",
        "RELEASED",
        "audit:malformed");
    jdbc.update(
        "update qdr_feedback_attribution_reference set reference_value='not-an-audit-reference'");

    final FeedbackRetentionResult malformed = service.cleanup(command("tenant-a", FeedbackEnvironment.DEV, 100));

    assertThat(malformed.status()).isEqualTo(FeedbackRetentionResult.Status.FAILED);
    assertThat(malformed.failureCode())
        .isEqualTo(FeedbackPersistenceErrorCode.REFERENCE_STATUS_UNKNOWN);
    assertThat(count("qdr_feedback_outcome_observation")).isEqualTo(1);

    resetSchema();
    insertAudit("audit-other-tenant", "tenant-b", "decision-1", "trace-1");
    insertAggregate(
        1,
        "tenant-a",
        "DEV",
        NOW.minus(Duration.ofDays(366)),
        "AUDIT",
        "RELEASED",
        "audit:audit-other-tenant");

    final FeedbackRetentionResult scopeMismatch =
        service.cleanup(command("tenant-a", FeedbackEnvironment.DEV, 100));

    assertThat(scopeMismatch.status()).isEqualTo(FeedbackRetentionResult.Status.FAILED);
    assertThat(scopeMismatch.failureCode())
        .isEqualTo(FeedbackPersistenceErrorCode.REFERENCE_STATUS_UNKNOWN);
    assertThat(count("qdr_feedback_outcome_observation")).isEqualTo(1);
  }

  @Test
  void boundedBatchesAreExplicitAndTenantEnvironmentIsolated() {
    insertEligibleBatch(251, 1, "tenant-a", "DEV");
    insertAggregate(999, "tenant-b", "DEV", NOW.minus(Duration.ofDays(366)), "EVIDENCE", "ACTIVE", "evidence:tenant-b");
    insertAggregate(1000, "tenant-a", "TEST", NOW.minus(Duration.ofDays(366)), "EVIDENCE", "ACTIVE", "evidence:test");

    final FeedbackRetentionResult first = service.cleanup(command("tenant-a", FeedbackEnvironment.DEV, 100));
    final FeedbackRetentionResult second = service.cleanup(command("tenant-a", FeedbackEnvironment.DEV, 100));
    final FeedbackRetentionResult third = service.cleanup(command("tenant-a", FeedbackEnvironment.DEV, 100));

    assertThat(first.deletedAggregateCount()).isEqualTo(100);
    assertThat(first.hasMoreCandidates()).isTrue();
    assertThat(second.deletedAggregateCount()).isEqualTo(100);
    assertThat(second.hasMoreCandidates()).isTrue();
    assertThat(third.deletedAggregateCount()).isEqualTo(51);
    assertThat(third.hasMoreCandidates()).isFalse();
    assertThat(countByScope("tenant-a", "DEV")).isZero();
    assertThat(countByScope("tenant-b", "DEV")).isEqualTo(1);
    assertThat(countByScope("tenant-a", "TEST")).isEqualTo(1);
  }

  @Test
  void concurrentWorkersDoNotDoubleDeleteAndLeaveNoOrphans() throws Exception {
    insertEligibleBatch(20, 1, "tenant-a", "DEV");
    final CountDownLatch started = new CountDownLatch(2);
    final CountDownLatch release = new CountDownLatch(1);
    try (ExecutorService workers = Executors.newFixedThreadPool(2)) {
      final List<Future<FeedbackRetentionResult>> results =
          List.of(
              workers.submit(() -> cleanupAfterBarrier(started, release)),
              workers.submit(() -> cleanupAfterBarrier(started, release)));
      assertThat(started.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      release.countDown();
      final int deleted = results.get(0).get().deletedAggregateCount() + results.get(1).get().deletedAggregateCount();
      assertThat(deleted).isEqualTo(20);
    }
    assertThat(count("qdr_feedback_outcome_observation")).isZero();
    assertThat(count("qdr_feedback_attribution")).isZero();
    assertThat(count("qdr_feedback_attribution_contribution")).isZero();
    assertThat(count("qdr_feedback_attribution_reference")).isZero();
  }

  @Test
  void integrityFailuresAndEveryDeleteFailureRollbackTheWholeAggregate() {
    insertAggregate(1, "tenant-a", "DEV", NOW.minus(Duration.ofDays(366)), "EVIDENCE", "ACTIVE", "evidence:missing-contribution");
    jdbc.update("delete from qdr_feedback_attribution_contribution where attribution_id=?", hash(1));
    final FeedbackRetentionResult incomplete = service.cleanup(command("tenant-a", FeedbackEnvironment.DEV, 100));
    assertThat(incomplete.blockedByIntegrityCount()).isEqualTo(1);
    assertThat(incomplete.deletedAggregateCount()).isZero();

    resetSchema();
    insertAggregate(1, "tenant-a", "DEV", NOW.minus(Duration.ofDays(366)), "EVIDENCE", "ACTIVE", "evidence:sort-gap");
    jdbc.update("update qdr_feedback_attribution_contribution set sort_order=1 where attribution_id=?", hash(1));
    final FeedbackRetentionResult sortGap = service.cleanup(command("tenant-a", FeedbackEnvironment.DEV, 100));
    assertThat(sortGap.blockedByIntegrityCount()).isEqualTo(1);
    assertThat(sortGap.deletedAggregateCount()).isZero();

    resetSchema();
    insertAggregate(1, "tenant-a", "DEV", NOW.minus(Duration.ofDays(366)), "EVIDENCE", "ACTIVE", "evidence:missing-reference");
    jdbc.update("delete from qdr_feedback_attribution_reference where attribution_id=?", hash(1));
    final FeedbackRetentionResult missingReference =
        service.cleanup(command("tenant-a", FeedbackEnvironment.DEV, 100));
    assertThat(missingReference.blockedByIntegrityCount()).isEqualTo(1);
    assertThat(missingReference.deletedAggregateCount()).isZero();

    for (String table :
        List.of(
            "qdr_feedback_attribution_reference",
            "qdr_feedback_attribution_contribution",
            "qdr_feedback_attribution",
            "qdr_feedback_outcome_observation")) {
      resetSchema();
      insertAggregate(1, "tenant-a", "DEV", NOW.minus(Duration.ofDays(366)), "EVIDENCE", "ACTIVE", "evidence:rollback");
      jdbc.execute(
          "create function qdr9_retention_fail() returns trigger language plpgsql as $$ begin raise exception 'forced retention delete failure'; end; $$");
      jdbc.execute(
          "create trigger qdr9_retention_fail_trigger before delete on "
              + table
              + " for each row execute function qdr9_retention_fail()");
      final FeedbackRetentionResult failed = service.cleanup(command("tenant-a", FeedbackEnvironment.DEV, 100));
      assertThat(failed.status()).isEqualTo(FeedbackRetentionResult.Status.FAILED);
      assertThat(count("qdr_feedback_outcome_observation")).isEqualTo(1);
      assertThat(count("qdr_feedback_attribution")).isEqualTo(1);
      assertThat(count("qdr_feedback_attribution_contribution")).isEqualTo(1);
      assertThat(count("qdr_feedback_attribution_reference")).isEqualTo(1);
      assertThat(count("dh_decision_audit_event")).isZero();
    }

    resetSchema();
    insertAggregate(1, "tenant-a", "DEV", NOW.minus(Duration.ofDays(366)), "EVIDENCE", "ACTIVE", "evidence:audit-rollback");
    jdbc.execute(
        "create function qdr9_retention_audit_fail() returns trigger language plpgsql as $$ begin raise exception 'forced audit failure'; end; $$");
    jdbc.execute(
        "create trigger qdr9_retention_audit_fail_trigger before insert on dh_decision_audit_event for each row execute function qdr9_retention_audit_fail()");
    final FeedbackRetentionResult auditFailure =
        service.cleanup(command("tenant-a", FeedbackEnvironment.DEV, 100));
    assertThat(auditFailure.status()).isEqualTo(FeedbackRetentionResult.Status.FAILED);
    assertAggregateRemainsIntact();
  }

  @Test
  void statementTimeoutRollsBackWithoutAnAutomaticRetry() {
    insertAggregate(1, "tenant-a", "DEV", NOW.minus(Duration.ofDays(366)), "EVIDENCE", "ACTIVE", "evidence:timeout");
    jdbc.execute(
        "create function qdr9_retention_timeout() returns trigger language plpgsql as $$ begin perform pg_sleep(6); return old; end; $$");
    jdbc.execute(
        "create trigger qdr9_retention_timeout_trigger before delete on qdr_feedback_attribution_reference for each row execute function qdr9_retention_timeout()");

    final FeedbackRetentionResult timeout = service.cleanup(command("tenant-a", FeedbackEnvironment.DEV, 100));

    assertThat(timeout.status()).isEqualTo(FeedbackRetentionResult.Status.FAILED);
    assertThat(timeout.failureCode()).isEqualTo(FeedbackPersistenceErrorCode.RETENTION_TIMEOUT);
    assertAggregateRemainsIntact();
  }

  private FeedbackRetentionResult cleanupAfterBarrier(
      final CountDownLatch started, final CountDownLatch release) throws Exception {
    started.countDown();
    assertThat(release.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    return service(dataSource()).cleanup(command("tenant-a", FeedbackEnvironment.DEV, 10));
  }

  private FeedbackRetentionService service(final JdbcTemplate template) {
    return new FeedbackRetentionService(
        new JdbcFeedbackRetentionAdapter(template, new DataSourceTransactionManager(template.getDataSource())),
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private FeedbackRetentionService service(final javax.sql.DataSource dataSource) {
    return service(new JdbcTemplate(dataSource));
  }

  private static FeedbackRetentionCommand command(
      final String tenant, final FeedbackEnvironment environment, final int batchSize) {
    return new FeedbackRetentionCommand(tenant, environment, true, Duration.ofDays(365), batchSize);
  }

  private void insertEligibleBatch(
      final int count, final int first, final String tenant, final String environment) {
    final List<Object[]> observations = new ArrayList<>();
    final List<Object[]> attributions = new ArrayList<>();
    final List<Object[]> contributions = new ArrayList<>();
    final List<Object[]> references = new ArrayList<>();
    for (int index = first; index < first + count; index++) {
      final AggregateValues values = values(index, tenant, environment, NOW.minus(Duration.ofDays(366)));
      observations.add(values.observationValues());
      attributions.add(values.attributionValues());
      contributions.add(values.contributionValues());
      references.add(values.referenceValues("EVIDENCE", "ACTIVE", "evidence:" + index));
    }
    jdbc.batchUpdate(OBSERVATION_INSERT, observations);
    jdbc.batchUpdate(ATTRIBUTION_INSERT, attributions);
    jdbc.batchUpdate(CONTRIBUTION_INSERT, contributions);
    jdbc.batchUpdate(REFERENCE_INSERT, references);
  }

  private void insertAggregate(
      final int index,
      final String tenant,
      final String environment,
      final Instant observedAt,
      final String referenceType,
      final String referenceStatus,
      final String referenceValue) {
    final AggregateValues values = values(index, tenant, environment, observedAt);
    jdbc.update(OBSERVATION_INSERT, values.observationValues());
    jdbc.update(ATTRIBUTION_INSERT, values.attributionValues());
    jdbc.update(CONTRIBUTION_INSERT, values.contributionValues());
    jdbc.update(REFERENCE_INSERT, values.referenceValues(referenceType, referenceStatus, referenceValue));
  }

  private AggregateValues values(
      final int index, final String tenant, final String environment, final Instant observedAt) {
    return new AggregateValues(
        UUID.nameUUIDFromBytes(("observation-" + index + tenant + environment).getBytes(StandardCharsets.UTF_8)),
        UUID.nameUUIDFromBytes(("attribution-" + index + tenant + environment).getBytes(StandardCharsets.UTF_8)),
        UUID.nameUUIDFromBytes(("contribution-" + index + tenant + environment).getBytes(StandardCharsets.UTF_8)),
        UUID.nameUUIDFromBytes(("reference-" + index + tenant + environment).getBytes(StandardCharsets.UTF_8)),
        tenant,
        environment,
        "decision-1",
        "trace-1",
        "observation-" + index,
        hash(index),
        hash(index),
        observedAt,
        hash(index));
  }

  private void insertAudit(
      final String id, final String tenant, final String decisionId, final String traceId) {
    jdbc.update(
        "insert into dh_decision_audit_event (id,decision_id,tenant_id,trace_id) values (?,?,?,?)",
        id,
        decisionId,
        tenant,
        traceId);
  }

  private int count(final String table) {
    return jdbc.queryForObject("select count(*) from " + table, Integer.class);
  }

  private int countByScope(final String tenant, final String environment) {
    return jdbc.queryForObject(
        "select count(*) from qdr_feedback_outcome_observation where tenant_id=? and environment=?",
        Integer.class,
        tenant,
        environment);
  }

  private void assertAggregateRemainsIntact() {
    assertThat(count("qdr_feedback_outcome_observation")).isEqualTo(1);
    assertThat(count("qdr_feedback_attribution")).isEqualTo(1);
    assertThat(count("qdr_feedback_attribution_contribution")).isEqualTo(1);
    assertThat(count("qdr_feedback_attribution_reference")).isEqualTo(1);
    assertThat(count("dh_decision_audit_event")).isZero();
  }

  private void deleteAggregateDirectly(final String attributionId, final String observationId) {
    jdbc.update("delete from qdr_feedback_attribution_reference where attribution_id=?", attributionId);
    jdbc.update("delete from qdr_feedback_attribution_contribution where attribution_id=?", attributionId);
    jdbc.update("delete from qdr_feedback_attribution where attribution_id=?", attributionId);
    jdbc.update("delete from qdr_feedback_outcome_observation where observation_id=?", observationId);
  }

  private void executeV15() {
    try {
      final Path path =
          Path.of(
              "..", "dh-app", "src", "main", "resources", "db", "migration",
              "V15__qdr9_structured_feedback_persistence.sql");
      ScriptUtils.executeSqlScript(
          jdbc.getDataSource().getConnection(),
          new ByteArrayResource(Files.readString(path).getBytes(StandardCharsets.UTF_8)));
    } catch (final Exception error) {
      throw new IllegalStateException("failed to execute V15 test migration", error);
    }
  }

  private void createReferenceSourceTables() {
    jdbc.execute(
        "create table dh_decision_audit_event (id varchar(192) primary key,decision_id varchar(128) not null,tenant_id varchar(128) not null,trace_id varchar(128) not null,event_type varchar(64),event_status varchar(32),event_json jsonb,error_code varchar(128),created_at timestamptz)");
    jdbc.execute("create table qdr_replay_case (id uuid primary key,tenant_id varchar(128) not null,case_id varchar(128) not null)");
    jdbc.execute("create table qdr_canonical_replay_snapshot (id uuid primary key,tenant_id varchar(128) not null)");
    jdbc.execute("create table qdr_evaluation_case (id uuid primary key,tenant_id varchar(128) not null,evaluation_id varchar(128) not null)");
  }

  private static DriverManagerDataSource dataSource() {
    final DriverManagerDataSource source = new DriverManagerDataSource();
    source.setDriverClassName("org.postgresql.Driver");
    source.setUrl(POSTGRES.getJdbcUrl());
    source.setUsername(POSTGRES.getUsername());
    source.setPassword(POSTGRES.getPassword());
    return source;
  }

  private static String hash(final int value) {
    return String.format("%064x", value);
  }

  private record AggregateValues(
      UUID observationId,
      UUID attributionId,
      UUID contributionId,
      UUID referenceId,
      String tenant,
      String environment,
      String decisionId,
      String traceId,
      String observationKey,
      String idempotencyKey,
      String canonicalHash,
      Instant observedAt,
      String attributionHash) {

    private Object[] observationValues() {
      return new Object[] {
        observationId, tenant, environment, decisionId, traceId, observationKey, idempotencyKey,
        "DRY_RUN_RESULT", "SUCCEEDED", Timestamp.from(observedAt), Timestamp.from(NOW), canonicalHash
      };
    }

    private Object[] attributionValues() {
      return new Object[] {
        attributionId, tenant, environment, observationKey, decisionId, traceId, Timestamp.from(observedAt),
        attributionHash, "policy-1", "v1", "ATTRIBUTED", new java.math.BigDecimal("0.5"), canonicalHash,
        "SUCCESS"
      };
    }

    private Object[] contributionValues() {
      return new Object[] {
        contributionId, tenant, environment, attributionHash, "EVIDENCE_QUALITY", new java.math.BigDecimal("0.5"),
        new java.math.BigDecimal("0.5"), "POSITIVE", new java.math.BigDecimal("0.5"), "RETENTION_TEST",
        "evidence:test", 0
      };
    }

    private Object[] referenceValues(
        final String referenceType, final String referenceStatus, final String referenceValue) {
      return new Object[] {
        referenceId, tenant, environment, attributionHash, referenceType, referenceValue, referenceStatus
      };
    }
  }

  private static final String OBSERVATION_INSERT =
      "insert into qdr_feedback_outcome_observation"
          + " (id,tenant_id,environment,decision_id,trace_id,observation_id,idempotency_key,outcome_source,outcome_status,observed_at,evaluation_time,canonical_hash)"
          + " values (?,?,?,?,?,?,?,?,?,?,?,?)";
  private static final String ATTRIBUTION_INSERT =
      "insert into qdr_feedback_attribution"
          + " (id,tenant_id,environment,observation_id,decision_id,trace_id,observed_at,attribution_id,policy_id,policy_version,attribution_status,confidence,canonical_hash,error_code)"
          + " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
  private static final String CONTRIBUTION_INSERT =
      "insert into qdr_feedback_attribution_contribution"
          + " (id,tenant_id,environment,attribution_id,dimension,measurement,contribution,impact,confidence,reason_code,evidence_ref,sort_order)"
          + " values (?,?,?,?,?,?,?,?,?,?,?,?)";
  private static final String REFERENCE_INSERT =
      "insert into qdr_feedback_attribution_reference"
          + " (id,tenant_id,environment,attribution_id,reference_type,reference_value,reference_status)"
          + " values (?,?,?,?,?,?,?)";
}

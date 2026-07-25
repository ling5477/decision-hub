package com.guidinglight.decisionhub.infra.jdbc.qdr.feedback;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.ObservedDecisionOutcome;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeSource;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceErrorCode;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.AttributionReferenceRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.OutcomeObservationRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceStatus;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** V15 reference validator 的真实 PostgreSQL tenant/environment 边界回归。 */
@Testcontainers(disabledWithoutDocker = true)
class JdbcFeedbackReferenceValidationAdapterTest {

  private static final String TENANT_A = "tenant-a";
  private static final String TENANT_B = "tenant-b";
  private static final String ATTRIBUTION_ID = "a".repeat(64);
  private static final String CANONICAL_HASH = "b".repeat(64);

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("decision_hub")
          .withUsername("decision_hub")
          .withPassword("decision_hub");

  private JdbcTemplate jdbc;
  private JdbcFeedbackReferenceValidationAdapter validator;

  @BeforeEach
  void resetSchema() {
    jdbc =
        new JdbcTemplate(
            new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    jdbc.execute("drop table if exists qdr_evaluation_case");
    jdbc.execute("drop table if exists qdr_canonical_replay_snapshot");
    jdbc.execute("drop table if exists qdr_replay_case");
    jdbc.execute("drop table if exists dh_decision_audit_event");
    jdbc.execute(
        "create table dh_decision_audit_event (id varchar(192) primary key, decision_id varchar(128) not null, tenant_id varchar(128) not null, trace_id varchar(128) not null)");
    jdbc.execute(
        "create table qdr_replay_case (id uuid primary key, tenant_id varchar(128) not null, case_id varchar(128) not null)");
    jdbc.execute(
        "create table qdr_canonical_replay_snapshot (id uuid primary key, tenant_id varchar(128) not null)");
    jdbc.execute(
        "create table qdr_evaluation_case (id uuid primary key, tenant_id varchar(128) not null)");
    validator = new JdbcFeedbackReferenceValidationAdapter(jdbc);
  }

  @Test
  void confirmsAuditReplayAndEvaluationTargetsOnlyWithinTheObservationTenant() {
    final UUID replayId = UUID.randomUUID();
    final UUID evaluationId = UUID.randomUUID();
    jdbc.update(
        "insert into dh_decision_audit_event (id, decision_id, tenant_id, trace_id) values (?,?,?,?)",
        "event-1",
        "decision-a",
        TENANT_A,
        "trace-a");
    jdbc.update(
        "insert into qdr_replay_case (id, tenant_id, case_id) values (?,?,?)",
        replayId,
        TENANT_A,
        "replay-1");
    jdbc.update("insert into qdr_evaluation_case (id, tenant_id) values (?,?)", evaluationId, TENANT_A);

    final OutcomeObservationRecord observation = observation(TENANT_A, FeedbackEnvironment.DEV);

    assertThatCode(
            () ->
                validator.confirm(
                    reference(TENANT_A, FeedbackEnvironment.DEV, ReferenceType.AUDIT, "audit:event-1"),
                    observation))
        .doesNotThrowAnyException();
    assertThatCode(
            () ->
                validator.confirm(
                    reference(
                        TENANT_A,
                        FeedbackEnvironment.DEV,
                        ReferenceType.REPLAY,
                        "replay-case:" + replayId),
                    observation))
        .doesNotThrowAnyException();
    assertThatCode(
            () ->
                validator.confirm(
                    reference(
                        TENANT_A,
                        FeedbackEnvironment.DEV,
                        ReferenceType.EVALUATION,
                        "evaluation:" + evaluationId),
                    observation))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsTargetsOutsideTheObservationTenantAndReferenceScopeMismatches() {
    final UUID replayId = UUID.randomUUID();
    jdbc.update(
        "insert into qdr_replay_case (id, tenant_id, case_id) values (?,?,?)",
        replayId,
        TENANT_B,
        "replay-2");
    final OutcomeObservationRecord observation = observation(TENANT_A, FeedbackEnvironment.DEV);

    assertError(
        () ->
            validator.confirm(
                reference(
                    TENANT_A,
                    FeedbackEnvironment.DEV,
                    ReferenceType.REPLAY,
                    "replay-case:" + replayId),
                observation),
        FeedbackPersistenceErrorCode.REFERENCE_INVALID);
    assertError(
        () ->
            validator.confirm(
                reference(TENANT_B, FeedbackEnvironment.DEV, ReferenceType.EVIDENCE, "evidence:known"),
                observation),
        FeedbackPersistenceErrorCode.TENANT_SCOPE_MISMATCH);
    assertError(
        () ->
            validator.confirm(
                reference(TENANT_A, FeedbackEnvironment.TEST, ReferenceType.EVIDENCE, "evidence:known"),
                observation),
        FeedbackPersistenceErrorCode.ENVIRONMENT_SCOPE_MISMATCH);
  }

  private static OutcomeObservationRecord observation(
      final String tenantId, final FeedbackEnvironment environment) {
    return new OutcomeObservationRecord(
        UUID.randomUUID(),
        tenantId,
        environment,
        "decision-a",
        "trace-a",
        "observation-a",
        ATTRIBUTION_ID,
        OutcomeSource.DRY_RUN_RESULT,
        ObservedDecisionOutcome.SUCCEEDED,
        Instant.parse("2026-07-25T10:00:00Z"),
        Instant.parse("2026-07-25T10:00:01Z"),
        CANONICAL_HASH);
  }

  private static AttributionReferenceRecord reference(
      final String tenantId,
      final FeedbackEnvironment environment,
      final ReferenceType type,
      final String value) {
    return new AttributionReferenceRecord(
        UUID.randomUUID(), tenantId, environment, ATTRIBUTION_ID, type, value, ReferenceStatus.ACTIVE);
  }

  private static void assertError(
      final org.assertj.core.api.ThrowableAssert.ThrowingCallable call,
      final FeedbackPersistenceErrorCode expected) {
    assertThatThrownBy(call)
        .isInstanceOf(FeedbackPersistenceException.class)
        .extracting(error -> ((FeedbackPersistenceException) error).errorCode())
        .isEqualTo(expected);
  }
}

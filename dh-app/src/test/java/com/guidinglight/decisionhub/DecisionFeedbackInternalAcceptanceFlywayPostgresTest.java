package com.guidinglight.decisionhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guidinglight.decisionhub.domain.decision.DecisionEvidence;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackExecutionScope;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;
import com.guidinglight.decisionhub.infra.jdbc.qdr.evidence.JdbcDecisionEnvironmentProvenanceQueryAdapter;
import com.guidinglight.decisionhub.infra.jdbc.qdr.feedback.JdbcHistoricalFeedbackEvidenceQueryAdapter;
import com.guidinglight.decisionhub.usecase.qdr.evidence.BoundedEvidencePolicy;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceAggregateService;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidencePolicy;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceRef;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceQuery;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceService;
import com.guidinglight.decisionhub.usecase.qdr.evidence.EvidenceCompleteness;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceReadService;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayObservabilityReport;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessAcceptanceStatus;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessDecision;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessEvaluationResult;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessReportSection;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessSignal;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessStatus;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.RedactionStatus;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionReportView;
import com.guidinglight.decisionhub.usecase.qdr.replay.deterministic.DeterministicReplayResult;
import com.guidinglight.decisionhub.usecase.qdr.replay.deterministic.ReplayReproducibilityStatus;
import com.guidinglight.decisionhub.usecase.qdr.report.DecisionEvidenceReplayInternalReport;
import com.guidinglight.decisionhub.usecase.qdr.report.DecisionEvidenceReplayReportService;
import com.guidinglight.decisionhub.usecase.qdr.report.DecisionFeedbackInternalAcceptanceService;
import com.guidinglight.decisionhub.usecase.qdr.report.InternalAcceptanceStatus;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Stage-QDR-11 persisted consolidated evidence -> internal acceptance PostgreSQL 验收。 */
@Testcontainers(disabledWithoutDocker = true)
class DecisionFeedbackInternalAcceptanceFlywayPostgresTest {

  private static final Instant TO = Instant.parse("2026-08-09T00:00:00Z");
  private static final Instant FROM = TO.minusSeconds(90L * 24 * 60 * 60);
  private static final UUID DECISION_REQUEST_ROW_ID =
      UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final UUID DECISION_RUN_ID =
      UUID.fromString("40000000-0000-0000-0000-000000000001");
  private static final List<String> OBSERVED_TABLES =
      List.of(
          "qdr_feedback_outcome_observation",
          "qdr_feedback_attribution",
          "qdr_feedback_attribution_contribution",
          "qdr_feedback_attribution_reference",
          "dh_decision_request",
          "dh_decision_context_snapshot",
          "dh_decision_trace_step",
          "dh_decision_provider_call_log",
          "dh_decision_output",
          "dh_decision_audit_event",
          "decision_request",
          "decision_run",
          "dh_qdr7_idempotency_guard",
          "dh_experience_entries",
          "dh_pheromone_edges");

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("decision_hub")
          .withUsername("decision_hub")
          .withPassword("decision_hub");

  private DataSource dataSource;
  private JdbcTemplate jdbc;

  @BeforeEach
  void migrateFreshDatabase() {
    dataSource = dataSource();
    final Flyway flyway =
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .cleanDisabled(false)
            .load();
    flyway.clean();
    flyway.migrate();
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("15");
    jdbc = new JdbcTemplate(dataSource);
    insertDecisionRoots();
    insertDecisionProvenance("test");
  }

  @Test
  void persistedCompleteEvidenceProducesBoundedAcceptedReportWithZeroWrites() {
    insertFeedback(1, TO.minusSeconds(1));
    final DecisionFeedbackEvidenceQuery query = query("tenant-a", FeedbackEnvironment.TEST, 100);
    final Map<String, Long> before = tableCounts();

    final DecisionEvidenceReplayInternalReport report =
        evaluate(query, completeDecision(query));

    assertThat(report.acceptanceStatus()).isEqualTo(InternalAcceptanceStatus.ACCEPTED);
    assertThat(report.evidenceCompleteness())
        .isEqualTo(EvidenceCompleteness.COMPLETE_WITHIN_BOUNDS);
    assertThat(report.executionScope()).isEqualTo(query.executionScope());
    assertThat(report.correlation()).isEqualTo(query.decisionCorrelation());
    assertThat(report.feedbackEvidenceCount()).isEqualTo(1);
    assertThat(report.boundedPolicy().fromObservedAt()).isEqualTo(FROM);
    assertThat(report.boundedPolicy().toObservedAt()).isEqualTo(TO);
    assertThat(report.boundedPolicy().maxFeedbackItems()).isEqualTo(100);
    assertThat(report.boundedPolicy().policyId()).isEqualTo(BoundedEvidencePolicy.POLICY_ID);
    assertThat(report.boundedPolicy().overflowBehavior())
        .isEqualTo(BoundedEvidencePolicy.OverflowBehavior.FAIL_CLOSED);
    assertThat(report.evidenceAggregateRef())
        .contains("tenant-a", "TEST", "trace-a", "request-a", "decision-a");
    assertThat(tableCounts()).isEqualTo(before);
  }

  @Test
  void persistedPartialAndNotFoundEvidenceFailClosedWithoutWrites() {
    final DecisionFeedbackEvidenceQuery query = query("tenant-a", FeedbackEnvironment.TEST, 100);
    final Map<String, Long> before = tableCounts();

    final DecisionEvidenceReplayInternalReport partial =
        evaluate(query, completeDecision(query));
    final DecisionEvidenceReplayInternalReport notFound =
        evaluate(
            query,
            query.decisionEvidenceQuery().policy().evaluate(
                query.decisionEvidenceQuery(), List.of()));

    assertThat(partial.evidenceCompleteness())
        .isEqualTo(EvidenceCompleteness.PARTIAL_WITHIN_BOUNDS);
    assertThat(partial.acceptanceStatus()).isEqualTo(InternalAcceptanceStatus.INCOMPLETE);
    assertThat(notFound.evidenceCompleteness()).isEqualTo(EvidenceCompleteness.NOT_FOUND);
    assertThat(notFound.acceptanceStatus()).isEqualTo(InternalAcceptanceStatus.INCOMPLETE);
    assertThat(tableCounts()).isEqualTo(before);
  }

  @Test
  void overflowAndEnvironmentMismatchAreInvalidAndProduceZeroWrites() {
    for (int index = 1; index <= 101; index++) {
      insertFeedback(index, FROM.plusSeconds(index));
    }
    final DecisionFeedbackEvidenceQuery overflowQuery =
        query("tenant-a", FeedbackEnvironment.TEST, 100);
    final Map<String, Long> beforeOverflow = tableCounts();

    final DecisionEvidenceReplayInternalReport overflow =
        evaluate(overflowQuery, completeDecision(overflowQuery));

    assertThat(overflow.evidenceCompleteness()).isEqualTo(EvidenceCompleteness.INCONSISTENT);
    assertThat(overflow.evidenceOverflowDetected()).isTrue();
    assertThat(overflow.acceptanceStatus()).isEqualTo(InternalAcceptanceStatus.INVALID);
    assertThat(tableCounts()).isEqualTo(beforeOverflow);

    jdbc.update("delete from dh_qdr7_idempotency_guard");
    insertDecisionProvenance("dev");
    final DecisionFeedbackEvidenceQuery environmentQuery =
        query("tenant-a", FeedbackEnvironment.TEST, 100);
    final Map<String, Long> beforeMismatch = tableCounts();

    final DecisionEvidenceReplayInternalReport mismatch =
        evaluate(environmentQuery, completeDecision(environmentQuery));

    assertThat(mismatch.evidenceCompleteness()).isEqualTo(EvidenceCompleteness.INCONSISTENT);
    assertThat(mismatch.acceptanceStatus()).isEqualTo(InternalAcceptanceStatus.INVALID);
    assertThat(tableCounts()).isEqualTo(beforeMismatch);
  }

  @Test
  void crossTenantScopeCannotConsumePersistedDecisionEvidence() {
    final DecisionFeedbackEvidenceQuery query =
        query("tenant-b", FeedbackEnvironment.TEST, 100);
    final Map<String, Long> before = tableCounts();

    final DecisionEvidenceReplayInternalReport report =
        evaluate(query, completeDecision(query));

    assertThat(report.evidenceCompleteness()).isEqualTo(EvidenceCompleteness.INCONSISTENT);
    assertThat(report.acceptanceStatus()).isEqualTo(InternalAcceptanceStatus.INVALID);
    assertThat(report.acceptanceStatus().authorizesProvider()).isFalse();
    assertThat(report.acceptanceStatus().authorizesNqIntegration()).isFalse();
    assertThat(report.acceptanceStatus().allowsTradingOrExecution()).isFalse();
    assertThat(report.acceptanceStatus().enablesPaperOrLive()).isFalse();
    assertThat(tableCounts()).isEqualTo(before);
  }

  private DecisionEvidenceReplayInternalReport evaluate(
      final DecisionFeedbackEvidenceQuery query,
      final DecisionEvidenceAggregate decisionEvidence) {
    final DecisionEvidenceAggregateService decisionService =
        mock(DecisionEvidenceAggregateService.class);
    when(decisionService.aggregate(any(), eq(DecisionEvidencePolicy.CORE_DECISION)))
        .thenReturn(decisionEvidence);
    final var adapter =
        new JdbcHistoricalFeedbackEvidenceQueryAdapter(
            jdbc, new DataSourceTransactionManager(dataSource));
    final DecisionFeedbackEvidenceService evidenceService =
        new DecisionFeedbackEvidenceService(
            decisionService,
            new JdbcDecisionEnvironmentProvenanceQueryAdapter(jdbc),
            new HistoricalFeedbackEvidenceReadService(adapter));
    final StructuredInputs inputs = structuredInputs(query);
    return new DecisionFeedbackInternalAcceptanceService(
            evidenceService, new DecisionEvidenceReplayReportService())
        .evaluate(
            query,
            inputs.replay(),
            inputs.regression(),
            inputs.readiness(),
            inputs.observability());
  }

  private static StructuredInputs structuredInputs(final DecisionFeedbackEvidenceQuery query) {
    final DeterministicReplayResult replay = mock(DeterministicReplayResult.class);
    when(replay.tenantId()).thenReturn(query.executionScope().tenantId());
    when(replay.traceId()).thenReturn(query.traceId());
    when(replay.requestId()).thenReturn(query.requestId());
    when(replay.decisionId()).thenReturn(query.decisionId());
    when(replay.decisionRunId()).thenReturn(DECISION_RUN_ID);
    when(replay.status()).thenReturn(ReplayReproducibilityStatus.REPRODUCIBLE);
    when(replay.differences()).thenReturn(List.of());

    final RegressionReportView regression = mock(RegressionReportView.class);
    when(regression.tenantId()).thenReturn(query.executionScope().tenantId());
    when(regression.traceId()).thenReturn(query.traceId());
    when(regression.sourceRequestId()).thenReturn(query.requestId());
    when(regression.sourceDecisionId()).thenReturn(query.decisionId());
    when(regression.verdict()).thenReturn(RegressionVerdict.Status.PASS);
    when(regression.severity()).thenReturn(RegressionSeverity.INFO);
    when(regression.findings()).thenReturn(List.of());

    final ProviderReadinessSignal signal = ProviderReadinessSignal.ready("readiness-ref");
    final ProviderReadinessEvaluationResult readiness =
        mock(ProviderReadinessEvaluationResult.class);
    when(readiness.traceId()).thenReturn(query.traceId());
    when(readiness.sourceRequestId()).thenReturn(query.requestId());
    when(readiness.providerRef()).thenReturn("mock-provider");
    when(readiness.decision()).thenReturn(ProviderReadinessDecision.READY);
    when(readiness.readinessSignal()).thenReturn(signal);
    when(readiness.findings()).thenReturn(List.of());

    final ProviderReadinessReportSection readinessSection =
        mock(ProviderReadinessReportSection.class);
    when(readinessSection.readinessDecision()).thenReturn(ProviderReadinessDecision.READY);
    when(readinessSection.readinessStatus()).thenReturn(ProviderReadinessStatus.READY);
    final ModelGatewayObservabilityReport observability =
        mock(ModelGatewayObservabilityReport.class);
    when(observability.tenantId()).thenReturn(query.executionScope().tenantId());
    when(observability.traceId()).thenReturn(query.traceId());
    when(observability.sourceRequestId()).thenReturn(query.requestId());
    when(observability.providerRef()).thenReturn("mock-provider");
    when(observability.modelGatewayVersionRef()).thenReturn("gateway-v1");
    when(observability.providerSummaryHash()).thenReturn("a".repeat(64));
    when(observability.sourceRef()).thenReturn("source-ref");
    when(observability.providerReadiness()).thenReturn(readinessSection);
    when(observability.acceptanceStatus())
        .thenReturn(ProviderReadinessAcceptanceStatus.PASS);
    return new StructuredInputs(replay, regression, readiness, observability);
  }

  private static DecisionFeedbackEvidenceQuery query(
      final String tenant, final FeedbackEnvironment environment, final int maxItems) {
    return new DecisionFeedbackEvidenceQuery(
        new FeedbackExecutionScope(tenant, environment),
        "trace-a",
        "request-a",
        "decision-a",
        DECISION_RUN_ID.toString(),
        FROM,
        TO,
        maxItems);
  }

  private static DecisionEvidenceAggregate completeDecision(
      final DecisionFeedbackEvidenceQuery query) {
    final List<DecisionEvidenceRef> refs =
        DecisionEvidencePolicy.CORE_DECISION.mandatoryEvidenceTypes().stream()
            .map(type -> ref(query, type, refId(query, type)))
            .toList();
    return DecisionEvidenceAggregate.evaluate(query.decisionEvidenceQuery(), refs);
  }

  private static DecisionEvidenceRef ref(
      final DecisionFeedbackEvidenceQuery query,
      final DecisionEvidencePolicy.EvidenceType type,
      final String refId) {
    return new DecisionEvidenceRef(
        new DecisionEvidence(refId, type.name(), "SAFE_" + type.name()),
        query.decisionCorrelation(),
        type,
        refId,
        null,
        "POSTGRES_" + type.name(),
        true,
        RedactionStatus.SUMMARY_ONLY);
  }

  private void insertFeedback(final int index, final Instant observedAt) {
    final String attributionId = hash(index);
    final String observationId = "observation-" + index;
    jdbc.update(
        "insert into qdr_feedback_outcome_observation"
            + " (id,tenant_id,environment,decision_id,trace_id,observation_id,idempotency_key,"
            + "outcome_source,outcome_status,observed_at,evaluation_time,canonical_hash)"
            + " values (?,?,?,?,?,?,?,?,?,?,?,?)",
        UUID.randomUUID(),
        "tenant-a",
        "TEST",
        "decision-a",
        "trace-a",
        observationId,
        hash(index + 2000),
        "STRUCTURED_TEST_FIXTURE",
        "SUCCEEDED",
        Timestamp.from(observedAt),
        Timestamp.from(observedAt.plusSeconds(1)),
        hash(index + 4000));
    jdbc.update(
        "insert into qdr_feedback_attribution"
            + " (id,tenant_id,environment,observation_id,decision_id,trace_id,observed_at,"
            + "attribution_id,policy_id,policy_version,attribution_status,confidence,canonical_hash,error_code)"
            + " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        UUID.randomUUID(),
        "tenant-a",
        "TEST",
        observationId,
        "decision-a",
        "trace-a",
        Timestamp.from(observedAt),
        attributionId,
        "policy-a",
        "v1",
        "ATTRIBUTED",
        BigDecimal.ONE,
        hash(index + 4000),
        "NONE");
    jdbc.update(
        "insert into qdr_feedback_attribution_contribution"
            + " (id,tenant_id,environment,attribution_id,dimension,measurement,contribution,impact,"
            + "confidence,reason_code,evidence_ref,sort_order) values (?,?,?,?,?,?,?,?,?,?,?,?)",
        UUID.randomUUID(),
        "tenant-a",
        "TEST",
        attributionId,
        "EVIDENCE_QUALITY",
        BigDecimal.ONE,
        BigDecimal.ONE,
        "POSITIVE",
        BigDecimal.ONE,
        "TEST",
        "evidence:item-" + index,
        0);
    jdbc.update(
        "insert into qdr_feedback_attribution_reference"
            + " (id,tenant_id,environment,attribution_id,reference_type,reference_value,reference_status)"
            + " values (?,?,?,?,?,?,?)",
        UUID.randomUUID(),
        "tenant-a",
        "TEST",
        attributionId,
        "EVIDENCE",
        "evidence:item-" + index,
        "ACTIVE");
  }

  private void insertDecisionRoots() {
    jdbc.update(
        "insert into dh_decision_request"
            + " (decision_id,request_id,trace_id,tenant_id,source,decision_type,subject_json,"
            + "context_ref,requested_at,schema_version,created_at)"
            + " values (?,?,?,?,?,'READ_ONLY_RECOMMENDATION','{}'::jsonb,?,?,?,?)",
        "decision-a",
        "request-a",
        "trace-a",
        "tenant-a",
        "NQ_DRYRUN",
        "context-a",
        Timestamp.from(TO.minusSeconds(10)),
        "v1",
        Timestamp.from(TO.minusSeconds(10)));
    jdbc.update(
        "insert into dh_decision_output"
            + " (decision_id,tenant_id,trace_id,request_id,decision_type,action,risk_level,"
            + "policy_status,confidence,output_json,created_at)"
            + " values (?,?,?,?,'READ_ONLY_RECOMMENDATION','OBSERVE','LOW','ALLOWED',1,"
            + "'{}'::jsonb,?)",
        "decision-a",
        "tenant-a",
        "trace-a",
        "request-a",
        Timestamp.from(TO.minusSeconds(9)));
    jdbc.update(
        "insert into decision_request"
            + " (id,request_key,request_type,source_system,source_ref_id,tenant_id,trace_id,"
            + "request_id,input_payload_json,context_payload_json,status,created_at,updated_at)"
            + " values (?,?,?,'NQ_DRYRUN',?,?,?,?,'{}'::jsonb,'{}'::jsonb,'ACCEPTED',?,?)",
        DECISION_REQUEST_ROW_ID,
        "request-a",
        "QUANT_DECISION_REVIEW",
        "source-ref-a",
        "tenant-a",
        "trace-a",
        "request-a",
        Timestamp.from(TO.minusSeconds(8)),
        Timestamp.from(TO.minusSeconds(8)));
    jdbc.update(
        "insert into decision_run"
            + " (id,decision_request_id,run_no,status,orchestrator_key,started_at,finished_at,"
            + "latency_ms,created_at) values (?, ?, 1, 'SUCCEEDED', 'MOCK', ?, ?, 1, ?)",
        DECISION_RUN_ID,
        DECISION_REQUEST_ROW_ID,
        Timestamp.from(TO.minusSeconds(7)),
        Timestamp.from(TO.minusSeconds(6)),
        Timestamp.from(TO.minusSeconds(7)));
  }

  private void insertDecisionProvenance(final String environment) {
    jdbc.update(
        "insert into dh_qdr7_idempotency_guard"
            + " (guard_id,environment,endpoint,source,tenant_id,request_id,request_hash,"
            + "hash_version,state,state_version,result_type,result_id,result_checksum,created_at,"
            + "updated_at,completed_at,expires_at,retention_until)"
            + " values (?,?,'/api/ai/decision-dry-runs','NQ_DRYRUN','tenant-a','request-a',?,"
            + "'QDR7-DRYRUN-CJSON-1','COMPLETED',2,'DH_DECISION_OUTPUT','decision-a',?,?,?,?,?,?)",
        UUID.randomUUID(),
        environment,
        hash(environment.hashCode()),
        hash(environment.hashCode() + 1000),
        Timestamp.from(TO.minusSeconds(5)),
        Timestamp.from(TO.minusSeconds(5)),
        Timestamp.from(TO.minusSeconds(4)),
        Timestamp.from(TO.plusSeconds(3600)),
        Timestamp.from(TO.plusSeconds(7200)));
  }

  private Map<String, Long> tableCounts() {
    final Map<String, Long> counts = new LinkedHashMap<>();
    OBSERVED_TABLES.forEach(
        table -> counts.put(table, jdbc.queryForObject("select count(*) from " + table, Long.class)));
    return counts;
  }

  private static String refId(
      final DecisionFeedbackEvidenceQuery query,
      final DecisionEvidencePolicy.EvidenceType type) {
    return switch (type) {
      case REQUEST -> "v5-request:" + query.requestId();
      case RUN -> "v6-run:" + query.decisionRunId();
      default -> "safe-" + type.name().toLowerCase(Locale.ROOT);
    };
  }

  private static String hash(final int value) {
    return String.format("%064x", value);
  }

  private static DataSource dataSource() {
    final DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setUrl(POSTGRES.getJdbcUrl());
    dataSource.setUsername(POSTGRES.getUsername());
    dataSource.setPassword(POSTGRES.getPassword());
    return dataSource;
  }

  private record StructuredInputs(
      DeterministicReplayResult replay,
      RegressionReportView regression,
      ProviderReadinessEvaluationResult readiness,
      ModelGatewayObservabilityReport observability) {}
}

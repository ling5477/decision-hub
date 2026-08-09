package com.guidinglight.decisionhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guidinglight.decisionhub.domain.decision.DecisionEvidence;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackExecutionScope;
import com.guidinglight.decisionhub.infra.jdbc.qdr.evidence.JdbcDecisionEnvironmentProvenanceQueryAdapter;
import com.guidinglight.decisionhub.infra.jdbc.qdr.feedback.JdbcHistoricalFeedbackEvidenceQueryAdapter;
import com.guidinglight.decisionhub.usecase.qdr.evidence.BoundedEvidencePolicy;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceAggregateService;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidencePolicy;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceRef;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceFinding;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceQuery;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceService;
import com.guidinglight.decisionhub.usecase.qdr.evidence.EvidenceCompleteness;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceReadService;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.RedactionStatus;
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

/** Stage-QDR-10 consolidated read path 的 PostgreSQL 17 + Flyway V1-V15 acceptance。 */
@Testcontainers(disabledWithoutDocker = true)
class DecisionFeedbackEvidenceConsolidationFlywayPostgresTest {

    private static final Instant TO = Instant.parse("2026-07-30T00:00:00Z");
    private static final Instant FROM = TO.minusSeconds(90L * 24 * 60 * 60);
    private static final UUID DECISION_REQUEST_ROW_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID DECISION_RUN_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final List<String> OBSERVED_TABLES = List.of(
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
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("decision_hub")
            .withUsername("decision_hub")
            .withPassword("decision_hub");

    private DataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    void migrateFreshDatabase() {
        dataSource = dataSource();
        final Flyway flyway = Flyway.configure()
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
    void readsOnlyExactTenantEnvironmentDecisionTraceAndProducesZeroWrites() {
        insert("tenant-a", "TEST", "decision-a", "trace-a", 1, TO.minusSeconds(1));
        insert("tenant-a", "TEST", "decision-a", "trace-a", 2, TO.minusSeconds(2));
        insert("tenant-b", "TEST", "decision-a", "trace-a", 3, TO.minusSeconds(3));
        insert("tenant-a", "DEV", "decision-a", "trace-a", 4, TO.minusSeconds(4));
        insert("tenant-a", "TEST", "decision-b", "trace-a", 5, TO.minusSeconds(5));
        insert("tenant-a", "TEST", "decision-a", "trace-b", 6, TO.minusSeconds(6));
        final Map<String, Long> before = tableCounts();
        final DecisionFeedbackEvidenceQuery query = query(100);

        final DecisionFeedbackEvidenceAggregate result = service(query).aggregate(query);

        assertThat(result.completeness()).isEqualTo(EvidenceCompleteness.COMPLETE_WITHIN_BOUNDS);
        assertThat(result.feedbackEvidence()).hasSize(2);
        assertThat(result.feedbackEvidence())
                .allMatch(view -> view.tenantId().equals("tenant-a"))
                .allMatch(view -> view.environment() == FeedbackEnvironment.TEST)
                .allMatch(view -> view.decisionId().equals("decision-a"))
                .allMatch(view -> view.traceId().equals("trace-a"));
        assertThat(result.decisionEvidence().evidenceRefs()).anyMatch(ref ->
                ref.evidenceType() == DecisionEvidencePolicy.EvidenceType.RUN
                        && ref.refId().equals("v6-run:" + DECISION_RUN_ID));
        assertThat(result.decisionEnvironmentProvenance().environment())
                .isEqualTo(FeedbackEnvironment.TEST);
        assertThat(result.boundedPolicy().fromObservedAt()).isEqualTo(FROM);
        assertThat(result.boundedPolicy().toObservedAt()).isEqualTo(TO);
        assertThat(result.boundedPolicy().maxFeedbackItems()).isEqualTo(100);
        assertThat(result.boundedPolicy().policyId()).isEqualTo(BoundedEvidencePolicy.POLICY_ID);
        assertThat(result.overflowDetected()).isFalse();
        assertThat(tableCounts()).isEqualTo(before);
    }

    @Test
    void provenDevDecisionWithTrustedDevCallerAndDevFeedbackIsUsableWithoutWrites() {
        jdbc.update("delete from dh_qdr7_idempotency_guard");
        insertDecisionProvenance("dev");
        insert("tenant-a", "DEV", "decision-a", "trace-a", 1, TO.minusSeconds(1));
        final Map<String, Long> before = tableCounts();
        final DecisionFeedbackEvidenceQuery query = query(100, FeedbackEnvironment.DEV);

        final DecisionFeedbackEvidenceAggregate result = service(query).aggregate(query);

        assertThat(result.completeness()).isEqualTo(EvidenceCompleteness.COMPLETE_WITHIN_BOUNDS);
        assertThat(result.isUsableWithinBounds()).isTrue();
        assertThat(result.decisionEnvironmentProvenance().environment())
                .isEqualTo(FeedbackEnvironment.DEV);
        assertThat(result.feedbackEvidence())
                .singleElement()
                .satisfies(view -> {
                    assertThat(view.tenantId()).isEqualTo("tenant-a");
                    assertThat(view.environment()).isEqualTo(FeedbackEnvironment.DEV);
                    assertThat(view.decisionId()).isEqualTo("decision-a");
                    assertThat(view.traceId()).isEqualTo("trace-a");
                });
        assertThat(tableCounts()).isEqualTo(before);
    }

    @Test
    void zeroOneAndOneHundredEvidenceRespectCutoffAndStableOrder() {
        final DecisionFeedbackEvidenceQuery query = query(100);
        final DecisionFeedbackEvidenceService service = service(query);

        assertThat(service.aggregate(query).completeness())
                .isEqualTo(EvidenceCompleteness.PARTIAL_WITHIN_BOUNDS);
        insert("tenant-a", "TEST", "decision-a", "trace-a", 1, FROM);
        insert("tenant-a", "TEST", "decision-a", "trace-a", 1001, FROM.minusSeconds(1));
        assertThat(service.aggregate(query).feedbackEvidence()).hasSize(1);
        for (int index = 2; index <= 100; index++) {
            insert(
                    "tenant-a",
                    "TEST",
                    "decision-a",
                    "trace-a",
                    index,
                    FROM.plusSeconds(index));
        }

        final DecisionFeedbackEvidenceAggregate first = service.aggregate(query);
        final DecisionFeedbackEvidenceAggregate second = service.aggregate(query);

        assertThat(first.completeness()).isEqualTo(EvidenceCompleteness.COMPLETE_WITHIN_BOUNDS);
        assertThat(first.feedbackEvidence()).hasSize(100).isEqualTo(second.feedbackEvidence());
        assertThat(first.feedbackEvidence())
                .isSortedAccordingTo(java.util.Comparator
                        .comparing(
                                com.guidinglight.decisionhub.usecase.qdr.feedback
                                        .HistoricalFeedbackEvidenceView::observedAt,
                                java.util.Comparator.reverseOrder())
                        .thenComparing(
                                com.guidinglight.decisionhub.usecase.qdr.feedback
                                        .HistoricalFeedbackEvidenceView::attributionId,
                                java.util.Comparator.reverseOrder()));
    }

    @Test
    void oneHundredAndOneEvidenceFailsClosedWithoutFollowingNextPage() {
        for (int index = 1; index <= 101; index++) {
            insert(
                    "tenant-a",
                    "TEST",
                    "decision-a",
                    "trace-a",
                    index,
                    FROM.plusSeconds(index));
        }
        final DecisionFeedbackEvidenceQuery query = query(100);

        final DecisionFeedbackEvidenceAggregate result = service(query).aggregate(query);

        assertThat(result.completeness()).isEqualTo(EvidenceCompleteness.INCONSISTENT);
        assertThat(result.isUsableWithinBounds()).isFalse();
        assertThat(result.overflowDetected()).isTrue();
        assertThat(result.feedbackEvidence()).isEmpty();
        assertThat(result.findings()).anyMatch(finding ->
                finding.code()
                        == DecisionFeedbackEvidenceFinding.Code.FEEDBACK_RESULT_LIMIT_EXCEEDED);
    }

    @Test
    void persistedDevDecisionRejectsTrustedTestCallerBeforeFeedbackRead() {
        jdbc.update("delete from dh_qdr7_idempotency_guard");
        insertDecisionProvenance("dev");
        insert("tenant-a", "TEST", "decision-a", "trace-a", 1, TO.minusSeconds(1));
        final DecisionFeedbackEvidenceQuery query = query(100);

        final DecisionFeedbackEvidenceAggregate result = service(query).aggregate(query);

        assertThat(result.completeness()).isEqualTo(EvidenceCompleteness.INCONSISTENT);
        assertThat(result.isUsableWithinBounds()).isFalse();
        assertThat(result.feedbackEvidence()).isEmpty();
        assertThat(result.findings()).anyMatch(finding ->
                finding.code() == DecisionFeedbackEvidenceFinding.Code.ENVIRONMENT_MISMATCH);
    }

    @Test
    void legacyDecisionWithoutGuardFailsClosedDespiteGloballyUniqueDecisionId() {
        jdbc.update("delete from dh_qdr7_idempotency_guard");
        final DecisionFeedbackEvidenceQuery query = query(100);

        final DecisionFeedbackEvidenceAggregate result = service(query).aggregate(query);

        assertThat(jdbc.queryForObject(
                        "select count(*) from dh_decision_request where decision_id='decision-a'",
                        Long.class))
                .isEqualTo(1L);
        assertThat(result.completeness()).isEqualTo(EvidenceCompleteness.INCONSISTENT);
        assertThat(result.findings()).anyMatch(finding -> finding.code()
                == DecisionFeedbackEvidenceFinding.Code.DECISION_ENVIRONMENT_PROVENANCE_MISSING);
    }

    @Test
    void multiplePersistedEnvironmentsForOneDecisionFailClosedAsAmbiguous() {
        insertDecisionProvenance("dev");
        final DecisionFeedbackEvidenceQuery query = query(100);

        final DecisionFeedbackEvidenceAggregate result = service(query).aggregate(query);

        assertThat(result.completeness()).isEqualTo(EvidenceCompleteness.INCONSISTENT);
        assertThat(result.findings()).anyMatch(finding -> finding.code()
                == DecisionFeedbackEvidenceFinding.Code.DECISION_ENVIRONMENT_PROVENANCE_AMBIGUOUS);
    }

    private DecisionFeedbackEvidenceService service(final DecisionFeedbackEvidenceQuery query) {
        final DecisionEvidenceAggregateService decisionService =
                mock(DecisionEvidenceAggregateService.class);
        when(decisionService.aggregate(any(), eq(DecisionEvidencePolicy.CORE_DECISION)))
                .thenReturn(completeDecision(query));
        final var adapter = new JdbcHistoricalFeedbackEvidenceQueryAdapter(
                jdbc, new DataSourceTransactionManager(dataSource));
        return new DecisionFeedbackEvidenceService(
                decisionService,
                new JdbcDecisionEnvironmentProvenanceQueryAdapter(jdbc),
                new HistoricalFeedbackEvidenceReadService(adapter));
    }

    private static DecisionFeedbackEvidenceQuery query(final int maxItems) {
        return query(maxItems, FeedbackEnvironment.TEST);
    }

    private static DecisionFeedbackEvidenceQuery query(
            final int maxItems, final FeedbackEnvironment environment) {
        return new DecisionFeedbackEvidenceQuery(
                new FeedbackExecutionScope("tenant-a", environment),
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
        final List<DecisionEvidenceRef> refs = DecisionEvidencePolicy.CORE_DECISION
                .mandatoryEvidenceTypes()
                .stream()
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

    private void insert(
            final String tenant,
            final String environment,
            final String decision,
            final String trace,
            final int index,
            final Instant observedAt) {
        final String attributionId = hash(index);
        final String observationId = "observation-" + index;
        jdbc.update(
                "insert into qdr_feedback_outcome_observation"
                        + " (id,tenant_id,environment,decision_id,trace_id,observation_id,idempotency_key,"
                        + "outcome_source,outcome_status,observed_at,evaluation_time,canonical_hash)"
                        + " values (?,?,?,?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID(),
                tenant,
                environment,
                decision,
                trace,
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
                tenant,
                environment,
                observationId,
                decision,
                trace,
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
                tenant,
                environment,
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
                tenant,
                environment,
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
        OBSERVED_TABLES.forEach(table -> counts.put(
                table, jdbc.queryForObject("select count(*) from " + table, Long.class)));
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
}

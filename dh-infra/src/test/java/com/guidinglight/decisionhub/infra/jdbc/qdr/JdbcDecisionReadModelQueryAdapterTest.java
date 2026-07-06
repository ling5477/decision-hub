package com.guidinglight.decisionhub.infra.jdbc.qdr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guidinglight.decisionhub.domain.qdr.DecisionRunStatus;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionEvidenceReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelUnavailableException;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionTraceReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.RedactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * stage-qdr-2 B2 JDBC read model adapter 测试。
 *
 * <p>使用 fake JdbcTemplate 验证 SQL 只读、tenant-bound、V5/V6 key 绑定与脱敏摘要，不连接真实 DB。
 */
final class JdbcDecisionReadModelQueryAdapterTest {

    private static final String TENANT_ID = "tenant-a";
    private static final String TRACE_ID = "trace-a";
    private static final String REQUEST_ID = "request-a";
    private static final UUID REQUEST_UUID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RUN_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SIGNAL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID DECISION_UUID =
            UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final Instant NOW = Instant.parse("2026-07-06T00:00:00Z");

    private RecordingJdbcTemplate jdbcTemplate;
    private JdbcDecisionReadModelQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new RecordingJdbcTemplate();
        adapter = new JdbcDecisionReadModelQueryAdapter(jdbcTemplate);
    }

    @Test
    void findDetailUsesTenantAndRunIdAndReturnsSafeSummaries() {
        jdbcTemplate.detailRows = List.of(detailRow("OBSERVE"));

        final var detail =
                adapter
                        .findDecisionRunDetail(
                                new DecisionRunReadQuery(TENANT_ID, RUN_UUID.toString(), "user-a", TRACE_ID))
                        .orElseThrow();

        assertThat(detail.tenantId()).isEqualTo(TENANT_ID);
        assertThat(detail.status()).isEqualTo(DecisionRunStatus.SUCCEEDED);
        assertThat(detail.quantSignalSummary()).contains("quant-signal://").doesNotContain("BUY", "SELL");
        assertThat(detail.quantDecisionSummary()).contains("action=OBSERVE").doesNotContain("PLACE_ORDER");
        assertThat(detail.outputSummary()).contains("decision-output://");
        assertThat(jdbcTemplate.firstQuery().sql())
                .contains("from decision_run dr")
                .contains("req.tenant_id = ?")
                .contains("dr.id = ?");
        assertThat(jdbcTemplate.firstQuery().args()).containsExactly(TENANT_ID, RUN_UUID);
        assertThat(jdbcTemplate.updates).isZero();
    }

    @Test
    void missingDetailReturnsEmptyWithoutCrossTenantFallback() {
        final var detail =
                adapter.findDecisionRunDetail(
                        new DecisionRunReadQuery(TENANT_ID, RUN_UUID.toString(), "user-a", TRACE_ID));

        assertThat(detail).isEmpty();
        assertThat(jdbcTemplate.firstQuery().args()).containsExactly(TENANT_ID, RUN_UUID);
    }

    @Test
    void traceUsesRequestIdFromV6RunContextToReadV5Steps() {
        jdbcTemplate.runContextRows = List.of(runContextRow());
        jdbcTemplate.traceRows =
                List.of(
                        traceRow("request-a-trace-1", "POLICY_CHECK", NOW, NOW.plusMillis(5)),
                        traceRow("request-a-trace-2", "OUTPUT_WRITE", NOW.plusMillis(6), NOW.plusMillis(10)));

        final var timeline =
                adapter.getDecisionTrace(
                        new DecisionTraceReadQuery(TENANT_ID, RUN_UUID.toString(), "user-a", TRACE_ID));

        assertThat(timeline.steps()).hasSize(2);
        assertThat(timeline.steps().get(0).stepNo()).isEqualTo(1);
        assertThat(timeline.steps().get(0).latencyMs()).isEqualTo(5L);
        assertThat(jdbcTemplate.queries().get(1).sql()).contains("from dh_decision_trace_step");
        assertThat(jdbcTemplate.queries().get(1).args()).containsExactly(TENANT_ID, REQUEST_ID, TRACE_ID);
    }

    @Test
    void evidenceReturnsRefsOnlyAndSummaryOnlyStatus() {
        jdbcTemplate.runContextRows = List.of(runContextRow());
        jdbcTemplate.contextRows =
                List.of(row("decision_id", REQUEST_ID, "evidence_refs_json", "[\"evidence://request-a\"]"));
        jdbcTemplate.providerRows = List.of(row("id", "request-a-provider-1"));
        jdbcTemplate.outputRows =
                List.of(
                        row(
                                "decision_output_id",
                                REQUEST_ID,
                                "decision_output_action",
                                "OBSERVE",
                                "decision_output_risk_level",
                                "LOW",
                                "decision_output_policy_status",
                                "ALLOWED"));
        jdbcTemplate.quantDecisionRows = List.of(row("id", DECISION_UUID));

        final var evidence =
                adapter
                        .findDecisionEvidence(
                                new DecisionEvidenceReadQuery(
                                        TENANT_ID, RUN_UUID.toString(), "user-a", TRACE_ID))
                        .orElseThrow();

        assertThat(evidence.redactionStatus()).isEqualTo(RedactionStatus.SUMMARY_ONLY);
        assertThat(evidence.contextSnapshotRef()).isEqualTo("context-snapshot://request-a");
        assertThat(evidence.providerCallLogRefs()).containsExactly("provider-call-log://request-a-provider-1");
        assertThat(evidence.decisionOutputRef()).contains("decision-output://request-a");
        assertThat(evidence.quantDecisionRef()).isEqualTo("quant-decision://" + DECISION_UUID);
        assertThat(evidence.toString()).doesNotContain("raw", "secret", "credential");
    }

    @Test
    void illegalActionFromDatabaseFailsClosed() {
        jdbcTemplate.detailRows = List.of(detailRow("BUY"));

        assertThatThrownBy(
                () ->
                        adapter.findDecisionRunDetail(
                                new DecisionRunReadQuery(
                                        TENANT_ID, RUN_UUID.toString(), "user-a", TRACE_ID)))
                .isInstanceOf(DecisionReadModelUnavailableException.class)
                .hasMessageContaining("detail rejected");
    }

    @Test
    void databaseFailurePropagatesAsReadModelUnavailable() {
        jdbcTemplate.failure = new DataAccessResourceFailureException("synthetic query failure");

        assertThatThrownBy(
                () ->
                        adapter.findDecisionRunDetail(
                                new DecisionRunReadQuery(
                                        TENANT_ID, RUN_UUID.toString(), "user-a", TRACE_ID)))
                .isInstanceOf(DecisionReadModelUnavailableException.class)
                .hasMessageContaining("detail query failed");
    }

    private static Map<String, Object> detailRow(final String quantDecisionAction) {
        return row(
                "decision_request_id",
                REQUEST_UUID,
                "decision_run_id",
                RUN_UUID,
                "tenant_id",
                TENANT_ID,
                "trace_id",
                TRACE_ID,
                "request_id",
                REQUEST_ID,
                "request_key",
                "request-key-a",
                "request_type",
                "QUANT_DECISION_REVIEW",
                "source_system",
                "NQ_DRYRUN",
                "source_ref_id",
                "snapshot-a",
                "run_no",
                1,
                "status",
                "SUCCEEDED",
                "started_at",
                NOW,
                "finished_at",
                NOW.plusMillis(10),
                "latency_ms",
                10L,
                "error_code",
                null,
                "error_message",
                null,
                "created_at",
                NOW,
                "quant_signal_id",
                SIGNAL_UUID,
                "quant_signal_type",
                "UNKNOWN_REVIEW_INPUT",
                "quant_signal_source",
                "NQ_DRYRUN",
                "quant_decision_id",
                DECISION_UUID,
                "quant_decision_action",
                quantDecisionAction,
                "quant_decision_confidence",
                new BigDecimal("0.5000"),
                "quant_decision_risk_level",
                "LOW",
                "decision_output_id",
                REQUEST_ID,
                "decision_output_action",
                "OBSERVE",
                "decision_output_risk_level",
                "LOW",
                "decision_output_policy_status",
                "ALLOWED",
                "decision_output_confidence",
                new BigDecimal("0.5000"));
    }

    private static Map<String, Object> runContextRow() {
        return row(
                "decision_run_id", RUN_UUID, "tenant_id", TENANT_ID, "trace_id", TRACE_ID, "request_id", REQUEST_ID);
    }

    private static Map<String, Object> traceRow(
            final String id, final String stepName, final Instant startedAt, final Instant endedAt) {
        return row(
                "id",
                id,
                "step_name",
                stepName,
                "step_status",
                "COMPLETED",
                "started_at",
                startedAt,
                "ended_at",
                endedAt,
                "error_code",
                null,
                "error_message",
                null,
                "created_at",
                startedAt);
    }

    private static Map<String, Object> row(final Object... pairs) {
        final Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            row.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return row;
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private final List<Query> queries = new ArrayList<>();
        private List<Map<String, Object>> detailRows = List.of();
        private List<Map<String, Object>> runContextRows = List.of();
        private List<Map<String, Object>> traceRows = List.of();
        private List<Map<String, Object>> contextRows = List.of();
        private List<Map<String, Object>> providerRows = List.of();
        private List<Map<String, Object>> outputRows = List.of();
        private List<Map<String, Object>> quantDecisionRows = List.of();
        private RuntimeException failure;
        private int updates;

        @Override
        public List<Map<String, Object>> queryForList(final String sql, final Object... args) {
            queries.add(new Query(sql, List.copyOf(Arrays.asList(args))));
            if (failure != null) {
                throw failure;
            }
            if (sql.contains("left join quant_signal")) {
                return detailRows;
            }
            if (sql.contains("from decision_run dr")) {
                return runContextRows;
            }
            if (sql.contains("from dh_decision_trace_step")) {
                return traceRows;
            }
            if (sql.contains("from dh_decision_context_snapshot")) {
                return contextRows;
            }
            if (sql.contains("from dh_decision_provider_call_log")) {
                return providerRows;
            }
            if (sql.contains("from dh_decision_output")) {
                return outputRows;
            }
            if (sql.contains("from quant_decision")) {
                return quantDecisionRows;
            }
            throw new AssertionError("unexpected SQL: " + sql);
        }

        @Override
        public int update(final String sql, final Object... args) {
            updates++;
            throw new AssertionError("read model adapter must not execute update");
        }

        Query firstQuery() {
            return queries.getFirst();
        }

        List<Query> queries() {
            return List.copyOf(queries);
        }
    }

    private record Query(String sql, List<Object> args) {
    }
}

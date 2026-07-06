package com.guidinglight.decisionhub.infra.jdbc.qdr;

import com.guidinglight.decisionhub.domain.qdr.DecisionRunStatus;
import com.guidinglight.decisionhub.domain.qdr.QuantDecisionAction;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionEvidenceReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionEvidenceView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelQueryPort;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelUnavailableException;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunDetailView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionTraceReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionTraceStepView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionTraceTimelineView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.RedactionStatus;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * stage-qdr-2 B2 QDR read model JDBC adapter。
 *
 * <p>本 adapter 只读现有 V6 decision core 表与 V5 decision audit 表；所有查询必须带 tenant_id 与
 * decision_run_id/request_id 绑定条件。不写库、不触发 replay execution、不调用外部 HTTP、不接真实
 * provider，不把 raw JSON / raw provider response / raw prompt 暴露给上层 DTO。
 */
public final class JdbcDecisionReadModelQueryAdapter implements DecisionReadModelQueryPort {

    private static final Set<String> OUTPUT_ACTIONS =
            Set.of("ABSTAIN", "OBSERVE", "NO_TRADE", "LONG_BIAS", "SHORT_BIAS");

    private static final String SELECT_DETAIL =
            "select req.id as decision_request_id, dr.id as decision_run_id,"
                    + " req.tenant_id, req.trace_id, req.request_id, req.request_key,"
                    + " req.request_type, req.source_system, req.source_ref_id,"
                    + " dr.run_no, dr.status, dr.started_at, dr.finished_at, dr.latency_ms,"
                    + " dr.error_code, dr.error_message, dr.created_at,"
                    + " qs.id as quant_signal_id, qs.signal_type as quant_signal_type,"
                    + " qs.source_system as quant_signal_source,"
                    + " qd.id as quant_decision_id, qd.action as quant_decision_action,"
                    + " qd.confidence_score as quant_decision_confidence,"
                    + " qd.risk_level as quant_decision_risk_level,"
                    + " dout.decision_id as decision_output_id,"
                    + " dout.action as decision_output_action,"
                    + " dout.risk_level as decision_output_risk_level,"
                    + " dout.policy_status as decision_output_policy_status,"
                    + " dout.confidence as decision_output_confidence"
                    + " from decision_run dr"
                    + " join decision_request req on req.id = dr.decision_request_id"
                    + " left join quant_signal qs on qs.decision_request_id = req.id"
                    + " left join quant_decision qd on qd.decision_run_id = dr.id"
                    + " left join dh_decision_output dout"
                    + " on dout.tenant_id = req.tenant_id and dout.decision_id = req.request_id"
                    + " where req.tenant_id = ? and dr.id = ?"
                    + " order by qs.created_at asc, qd.created_at asc"
                    + " limit 1";

    private static final String SELECT_RUN_CONTEXT =
            "select dr.id as decision_run_id, req.tenant_id, req.trace_id, req.request_id"
                    + " from decision_run dr"
                    + " join decision_request req on req.id = dr.decision_request_id"
                    + " where req.tenant_id = ? and dr.id = ?"
                    + " limit 1";

    private static final String SELECT_TRACE_STEPS =
            "select id, step_name, step_status, started_at, ended_at, error_code, error_message,"
                    + " created_at from dh_decision_trace_step"
                    + " where tenant_id = ? and decision_id = ? and trace_id = ?"
                    + " order by started_at asc, created_at asc, id asc";

    private static final String SELECT_CONTEXT_SNAPSHOT =
            "select decision_id, evidence_refs_json::text as evidence_refs_json"
                    + " from dh_decision_context_snapshot"
                    + " where tenant_id = ? and decision_id = ? and trace_id = ?"
                    + " limit 1";

    private static final String SELECT_PROVIDER_CALL_REFS =
            "select id from dh_decision_provider_call_log"
                    + " where tenant_id = ? and decision_id = ? and trace_id = ?"
                    + " order by created_at asc, id asc";

    private static final String SELECT_DECISION_OUTPUT_REF =
            "select decision_id as decision_output_id, action as decision_output_action,"
                    + " risk_level as decision_output_risk_level,"
                    + " policy_status as decision_output_policy_status"
                    + " from dh_decision_output"
                    + " where tenant_id = ? and decision_id = ? and trace_id = ?"
                    + " limit 1";

    private static final String SELECT_QUANT_DECISION_REF =
            "select id from quant_decision where decision_run_id = ? order by created_at asc limit 1";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建 QDR read model JDBC adapter。
     *
     * @param jdbcTemplate DH datasource 的 JDBC template。
     */
    public JdbcDecisionReadModelQueryAdapter(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    @Override
    public Optional<DecisionRunDetailView> findDecisionRunDetail(final DecisionRunReadQuery query) {
        final DecisionRunReadQuery checked = Objects.requireNonNull(query, "query");
        final UUID decisionRunId = uuid(checked.decisionRunId(), "decisionRunId");
        try {
            return jdbcTemplate.queryForList(SELECT_DETAIL, checked.tenantId(), decisionRunId).stream()
                    .findFirst()
                    .map(this::mapDetail);
        } catch (final DataAccessException error) {
            throw new DecisionReadModelUnavailableException("qdr read model detail query failed", error);
        } catch (final RuntimeException error) {
            throw rejected("qdr read model detail rejected", error);
        }
    }

    @Override
    public DecisionTraceTimelineView getDecisionTrace(final DecisionTraceReadQuery query) {
        final DecisionTraceReadQuery checked = Objects.requireNonNull(query, "query");
        final RunContext runContext = requireRunContext(checked.tenantId(), checked.decisionRunId());
        try {
            final List<Map<String, Object>> rows =
                    jdbcTemplate.queryForList(
                            SELECT_TRACE_STEPS,
                            runContext.tenantId(),
                            runContext.requestId(),
                            runContext.traceId());
            final List<DecisionTraceStepView> steps = new ArrayList<>(rows.size());
            for (int index = 0; index < rows.size(); index++) {
                steps.add(mapStep(rows.get(index), index + 1));
            }
            return new DecisionTraceTimelineView(
                    runContext.decisionRunId(), runContext.tenantId(), runContext.traceId(), runContext.requestId(), steps);
        } catch (final DataAccessException error) {
            throw new DecisionReadModelUnavailableException("qdr read model trace query failed", error);
        } catch (final RuntimeException error) {
            throw rejected("qdr read model trace rejected", error);
        }
    }

    @Override
    public Optional<DecisionEvidenceView> findDecisionEvidence(final DecisionEvidenceReadQuery query) {
        final DecisionEvidenceReadQuery checked = Objects.requireNonNull(query, "query");
        final Optional<RunContext> runContext = findRunContext(checked.tenantId(), checked.decisionRunId());
        if (runContext.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapEvidence(runContext.get()));
        } catch (final DataAccessException error) {
            throw new DecisionReadModelUnavailableException("qdr read model evidence query failed", error);
        } catch (final RuntimeException error) {
            throw rejected("qdr read model evidence rejected", error);
        }
    }

    private DecisionRunDetailView mapDetail(final Map<String, Object> row) {
        return new DecisionRunDetailView(
                text(row, "decision_request_id"),
                text(row, "decision_run_id"),
                text(row, "tenant_id"),
                text(row, "trace_id"),
                text(row, "request_id"),
                text(row, "request_key"),
                text(row, "request_type"),
                text(row, "source_system"),
                optionalText(row, "source_ref_id"),
                integer(row, "run_no"),
                enumValue(row, "status", DecisionRunStatus.class),
                instant(row, "started_at"),
                instantOrNull(row, "finished_at"),
                longOrNull(row, "latency_ms"),
                optionalText(row, "error_code"),
                optionalText(row, "error_message"),
                quantSignalSummary(row),
                quantDecisionSummary(row),
                outputSummary(row),
                instant(row, "created_at"));
    }

    private DecisionTraceStepView mapStep(final Map<String, Object> row, final int stepNo) {
        final Instant startedAt = instant(row, "started_at");
        final Instant finishedAt = instantOrNull(row, "ended_at");
        return new DecisionTraceStepView(
                text(row, "id"),
                stepNo,
                text(row, "step_name"),
                "AUDIT_TRACE_STEP",
                text(row, "step_status"),
                startedAt,
                finishedAt,
                latencyMs(startedAt, finishedAt),
                optionalText(row, "error_code"),
                optionalText(row, "error_message"),
                null,
                null,
                null,
                ref("decision-trace-step", text(row, "id")));
    }

    private DecisionEvidenceView mapEvidence(final RunContext runContext) {
        final List<Map<String, Object>> snapshots =
                jdbcTemplate.queryForList(
                        SELECT_CONTEXT_SNAPSHOT,
                        runContext.tenantId(),
                        runContext.requestId(),
                        runContext.traceId());
        final String contextRef =
                snapshots.isEmpty() ? null : ref("context-snapshot", text(snapshots.getFirst(), "decision_id"));
        final String evidenceRefsJson =
                snapshots.isEmpty() ? null : optionalText(snapshots.getFirst(), "evidence_refs_json");

        final List<String> providerRefs =
                jdbcTemplate.queryForList(
                                SELECT_PROVIDER_CALL_REFS,
                                runContext.tenantId(),
                                runContext.requestId(),
                                runContext.traceId())
                        .stream()
                        .map(row -> ref("provider-call-log", text(row, "id")))
                        .toList();

        final List<Map<String, Object>> outputs =
                jdbcTemplate.queryForList(
                        SELECT_DECISION_OUTPUT_REF,
                        runContext.tenantId(),
                        runContext.requestId(),
                        runContext.traceId());
        final String outputRef = outputs.isEmpty() ? null : decisionOutputRef(outputs.getFirst());

        final List<Map<String, Object>> quantDecisions =
                jdbcTemplate.queryForList(SELECT_QUANT_DECISION_REF, uuid(runContext.decisionRunId(), "decisionRunId"));
        final String quantDecisionRef =
                quantDecisions.isEmpty() ? null : ref("quant-decision", text(quantDecisions.getFirst(), "id"));

        return new DecisionEvidenceView(
                runContext.decisionRunId(),
                runContext.tenantId(),
                runContext.traceId(),
                contextRef,
                providerRefs,
                outputRef,
                quantDecisionRef,
                RedactionStatus.SUMMARY_ONLY,
                evidenceRefsJson);
    }

    private Optional<RunContext> findRunContext(final String tenantId, final String decisionRunId) {
        final UUID runId = uuid(decisionRunId, "decisionRunId");
        try {
            return jdbcTemplate.queryForList(SELECT_RUN_CONTEXT, tenantId, runId).stream()
                    .findFirst()
                    .map(this::mapRunContext);
        } catch (final DataAccessException error) {
            throw new DecisionReadModelUnavailableException("qdr read model run context query failed", error);
        } catch (final RuntimeException error) {
            throw rejected("qdr read model run context rejected", error);
        }
    }

    private RunContext requireRunContext(final String tenantId, final String decisionRunId) {
        return findRunContext(tenantId, decisionRunId)
                .orElseThrow(
                        () ->
                                new DecisionReadModelUnavailableException(
                                        "qdr read model run context not found", null));
    }

    private RunContext mapRunContext(final Map<String, Object> row) {
        return new RunContext(
                text(row, "decision_run_id"),
                text(row, "tenant_id"),
                text(row, "trace_id"),
                text(row, "request_id"));
    }

    private String quantSignalSummary(final Map<String, Object> row) {
        final String signalId = optionalText(row, "quant_signal_id");
        if (signalId == null) {
            return null;
        }
        return ref("quant-signal", signalId)
                + ";signalType="
                + text(row, "quant_signal_type")
                + ";source="
                + text(row, "quant_signal_source");
    }

    private String quantDecisionSummary(final Map<String, Object> row) {
        final String decisionId = optionalText(row, "quant_decision_id");
        if (decisionId == null) {
            return null;
        }
        final QuantDecisionAction action =
                enumValue(row, "quant_decision_action", QuantDecisionAction.class);
        final RiskLevel riskLevel = enumValue(row, "quant_decision_risk_level", RiskLevel.class);
        return ref("quant-decision", decisionId)
                + ";action="
                + action.name()
                + ";risk="
                + riskLevel.name()
                + ";confidence="
                + decimalText(row, "quant_decision_confidence");
    }

    private String outputSummary(final Map<String, Object> row) {
        final String outputId = optionalText(row, "decision_output_id");
        if (outputId == null) {
            return null;
        }
        return decisionOutputRef(row)
                + ";confidence="
                + decimalText(row, "decision_output_confidence");
    }

    private String decisionOutputRef(final Map<String, Object> row) {
        final String action = allowedOutputAction(text(row, "decision_output_action"));
        return ref("decision-output", text(row, "decision_output_id"))
                + ";action="
                + action
                + ";risk="
                + text(row, "decision_output_risk_level")
                + ";policy="
                + text(row, "decision_output_policy_status");
    }

    private static String allowedOutputAction(final String action) {
        final String normalized = action.toUpperCase(Locale.ROOT);
        if (!OUTPUT_ACTIONS.contains(normalized)) {
            throw new IllegalArgumentException("decision output action is not safe: " + action);
        }
        return normalized;
    }

    private static String ref(final String scheme, final String id) {
        return scheme + "://" + id;
    }

    private static Long latencyMs(final Instant startedAt, final Instant finishedAt) {
        if (finishedAt == null) {
            return null;
        }
        final long latency = Duration.between(startedAt, finishedAt).toMillis();
        if (latency < 0) {
            throw new IllegalArgumentException("trace step latencyMs must not be negative");
        }
        return latency;
    }

    private static <E extends Enum<E>> E enumValue(
            final Map<String, Object> row, final String key, final Class<E> enumType) {
        return Enum.valueOf(enumType, text(row, key));
    }

    private static String text(final Map<String, Object> row, final String key) {
        final String value = optionalText(row, key);
        if (value == null) {
            throw new IllegalArgumentException(key + " must not be null");
        }
        return value;
    }

    private static String optionalText(final Map<String, Object> row, final String key) {
        final Object value = row.get(key);
        if (value == null) {
            return null;
        }
        final String checked = value.toString().trim();
        return checked.isEmpty() ? null : checked;
    }

    private static Integer integer(final Map<String, Object> row, final String key) {
        final Object value = Objects.requireNonNull(row.get(key), key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private static Long longOrNull(final Map<String, Object> row, final String key) {
        final Object value = row.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private static String decimalText(final Map<String, Object> row, final String key) {
        final Object value = row.get(key);
        if (value == null) {
            return "n/a";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        return value.toString();
    }

    private static Instant instant(final Map<String, Object> row, final String key) {
        final Instant value = instantOrNull(row, key);
        if (value == null) {
            throw new IllegalArgumentException(key + " must not be null");
        }
        return value;
    }

    private static Instant instantOrNull(final Map<String, Object> row, final String key) {
        final Object value = row.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof OffsetDateTime dateTime) {
            return dateTime.toInstant();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toInstant(ZoneOffset.UTC);
        }
        return Instant.parse(value.toString());
    }

    private static UUID uuid(final String value, final String field) {
        try {
            return UUID.fromString(Objects.requireNonNull(value, field));
        } catch (final IllegalArgumentException error) {
            throw new IllegalArgumentException(field + " must be UUID", error);
        }
    }

    private static DecisionReadModelUnavailableException rejected(
            final String message, final RuntimeException error) {
        if (error instanceof DecisionReadModelUnavailableException unavailable) {
            return unavailable;
        }
        return new DecisionReadModelUnavailableException(message, error);
    }

    private record RunContext(String decisionRunId, String tenantId, String traceId, String requestId) {
    }
}

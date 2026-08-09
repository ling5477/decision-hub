package com.guidinglight.decisionhub.infra.jdbc.qdr.evidence;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEnvironmentProvenance;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEnvironmentProvenanceQuery;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEnvironmentProvenanceQueryPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardIdentity;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Resolves decision origin from an exact completed persistent guard and its V5/V6 result chain. */
public final class JdbcDecisionEnvironmentProvenanceQueryAdapter
        implements DecisionEnvironmentProvenanceQueryPort {

    private static final String SQL =
            "select g.guard_id,g.environment from dh_qdr7_idempotency_guard g"
                    + " join dh_decision_request legacy_request"
                    + " on legacy_request.tenant_id=g.tenant_id"
                    + " and legacy_request.decision_id=g.result_id"
                    + " and legacy_request.request_id=g.request_id"
                    + " and legacy_request.source=g.source"
                    + " join dh_decision_output legacy_output"
                    + " on legacy_output.tenant_id=g.tenant_id"
                    + " and legacy_output.decision_id=g.result_id"
                    + " and legacy_output.request_id=g.request_id"
                    + " and legacy_output.trace_id=legacy_request.trace_id"
                    + " join decision_request core_request"
                    + " on core_request.tenant_id=g.tenant_id"
                    + " and core_request.request_key=g.request_id"
                    + " and core_request.request_id=g.request_id"
                    + " and core_request.trace_id=legacy_request.trace_id"
                    + " and core_request.source_system=g.source"
                    + " join decision_run core_run on core_run.decision_request_id=core_request.id"
                    + " where g.tenant_id=? and g.request_id=? and g.result_id=?"
                    + " and legacy_request.trace_id=? and core_run.id=?"
                    + " and g.endpoint=? and g.source=? and g.state='COMPLETED'"
                    + " and g.result_type='DH_DECISION_OUTPUT'"
                    + " order by g.environment asc,g.guard_id asc fetch first 2 rows only";

    private final JdbcTemplate jdbc;

    public JdbcDecisionEnvironmentProvenanceQueryAdapter(final JdbcTemplate jdbcTemplate) {
        this.jdbc = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    @Override
    public DecisionEnvironmentProvenance find(final DecisionEnvironmentProvenanceQuery query) {
        final DecisionEnvironmentProvenanceQuery checked = Objects.requireNonNull(query, "query");
        final UUID decisionRunId;
        try {
            decisionRunId = UUID.fromString(checked.decisionRunId());
        } catch (final RuntimeException error) {
            return DecisionEnvironmentProvenance.invalid();
        }
        try {
            final List<Row> rows = jdbc.query(
                    SQL,
                    (result, ignored) ->
                            new Row(result.getObject("guard_id", UUID.class), result.getString("environment")),
                    checked.tenantId(),
                    checked.requestId(),
                    checked.decisionId(),
                    checked.traceId(),
                    decisionRunId,
                    PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
                    PersistentGuardIdentity.protectedDecisionSource());
            if (rows.isEmpty()) {
                return DecisionEnvironmentProvenance.missing();
            }
            if (rows.size() != 1) {
                return DecisionEnvironmentProvenance.ambiguous();
            }
            final Row row = rows.getFirst();
            final FeedbackEnvironment environment =
                    FeedbackEnvironment.fromGuardConfiguration(row.environment());
            if (row.guardId() == null || environment == null) {
                return DecisionEnvironmentProvenance.invalid();
            }
            return DecisionEnvironmentProvenance.proven(environment, row.guardId().toString());
        } catch (final RuntimeException error) {
            return DecisionEnvironmentProvenance.invalid();
        }
    }

    private record Row(UUID guardId, String environment) {}
}

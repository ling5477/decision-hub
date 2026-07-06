package com.guidinglight.decisionhub.infra.jdbc.qdr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.qdr.DecisionRequest;
import com.guidinglight.decisionhub.domain.qdr.DecisionRequestStatus;
import com.guidinglight.decisionhub.domain.qdr.DecisionRun;
import com.guidinglight.decisionhub.domain.qdr.DecisionRunStatus;
import com.guidinglight.decisionhub.domain.qdr.HumanApprovalStatus;
import com.guidinglight.decisionhub.domain.qdr.QuantDecision;
import com.guidinglight.decisionhub.domain.qdr.QuantDecisionAction;
import com.guidinglight.decisionhub.domain.qdr.QuantSignal;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.usecase.qdr.DecisionCorePersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.DecisionRequestRepository;
import com.guidinglight.decisionhub.usecase.qdr.DecisionRunRepository;
import com.guidinglight.decisionhub.usecase.qdr.QuantDecisionRepository;
import com.guidinglight.decisionhub.usecase.qdr.QuantSignalRepository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Decision Core 主线表的 JDBC repository 实现。
 *
 * <p>本 adapter 只访问 DH 自身 `decision_request`、`decision_run`、`quant_signal`、`quant_decision`
 * 表；不访问 NQ DB、不调用外部服务、不保存凭证或可执行订单 payload。JSONB 写入统一使用
 * `CAST(? AS jsonb)`，写入或查询失败必须 fail-closed。
 */
public final class JdbcDecisionCoreRepository
        implements DecisionRequestRepository,
        DecisionRunRepository,
        QuantSignalRepository,
        QuantDecisionRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private static final String INSERT_DECISION_REQUEST =
            "insert into decision_request"
                    + " (id, request_key, request_type, source_system, source_ref_id, tenant_id,"
                    + " trace_id, request_id, input_payload_json, context_payload_json, status,"
                    + " created_at, updated_at)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?)";

    private static final String SELECT_DECISION_REQUEST_BY_TENANT_KEY =
            "select id, request_key, request_type, source_system, source_ref_id, tenant_id,"
                    + " trace_id, request_id, input_payload_json, context_payload_json, status,"
                    + " created_at, updated_at"
                    + " from decision_request where tenant_id = ? and request_key = ?";

    private static final String INSERT_DECISION_RUN =
            "insert into decision_run"
                    + " (id, decision_request_id, run_no, status, orchestrator_key, model_provider,"
                    + " model_name, started_at, finished_at, latency_ms, error_code, error_message, created_at)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String COMPLETE_DECISION_RUN =
            "update decision_run set status = ?, finished_at = ?, latency_ms = ?, error_code = ?,"
                    + " error_message = ? where id = ?";

    private static final String SELECT_DECISION_RUN_BY_REQUEST =
            "select id, decision_request_id, run_no, status, orchestrator_key, model_provider,"
                    + " model_name, started_at, finished_at, latency_ms, error_code, error_message, created_at"
                    + " from decision_run where decision_request_id = ? order by run_no asc";

    private static final String SELECT_DECISION_RUN_BY_ID =
            "select id, decision_request_id, run_no, status, orchestrator_key, model_provider,"
                    + " model_name, started_at, finished_at, latency_ms, error_code, error_message, created_at"
                    + " from decision_run where id = ?";

    private static final String INSERT_QUANT_SIGNAL =
            "insert into quant_signal"
                    + " (id, decision_request_id, source_system, symbol, exchange, timeframe,"
                    + " signal_type, signal_payload_json, strategy_id, strategy_version, dataset_version,"
                    + " received_at, created_at)"
                    + " values (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?)";

    private static final String SELECT_QUANT_SIGNAL_BY_REQUEST =
            "select id, decision_request_id, source_system, symbol, exchange, timeframe,"
                    + " signal_type, signal_payload_json, strategy_id, strategy_version, dataset_version,"
                    + " received_at, created_at"
                    + " from quant_signal where decision_request_id = ? order by created_at asc";

    private static final String SELECT_QUANT_SIGNAL_BY_ID =
            "select id, decision_request_id, source_system, symbol, exchange, timeframe,"
                    + " signal_type, signal_payload_json, strategy_id, strategy_version, dataset_version,"
                    + " received_at, created_at"
                    + " from quant_signal where id = ?";

    private static final String INSERT_QUANT_DECISION =
            "insert into quant_decision"
                    + " (id, quant_signal_id, decision_run_id, action, confidence_score, risk_level,"
                    + " rationale, constraints_json, human_approval_status, created_at)"
                    + " values (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?)";

    private static final String SELECT_QUANT_DECISION_BY_RUN =
            "select id, quant_signal_id, decision_run_id, action, confidence_score, risk_level,"
                    + " rationale, constraints_json, human_approval_status, created_at"
                    + " from quant_decision where decision_run_id = ? order by created_at asc";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 创建 JDBC adapter。
     *
     * @param jdbcTemplate DH datasource 对应的 JDBC template。
     * @param objectMapper JSON mapper。
     */
    public JdbcDecisionCoreRepository(
            final JdbcTemplate jdbcTemplate, final ObjectMapper objectMapper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public void save(final DecisionRequest request) {
        final DecisionRequest checked = Objects.requireNonNull(request, "request");
        update(
                "save decision request",
                INSERT_DECISION_REQUEST,
                checked.id(),
                checked.requestKey(),
                checked.requestType(),
                checked.sourceSystem(),
                checked.sourceRefId(),
                checked.tenantId(),
                checked.traceId(),
                checked.requestId(),
                writeJson(checked.inputPayloadJson(), "inputPayloadJson"),
                writeJsonOrNull(checked.contextPayloadJson(), "contextPayloadJson"),
                checked.status().name(),
                timestamp(checked.createdAt()),
                timestamp(checked.updatedAt()));
    }

    @Override
    public Optional<DecisionRequest> findByTenantIdAndRequestKey(
            final String tenantId, final String requestKey) {
        try {
            return jdbcTemplate
                    .query(
                            SELECT_DECISION_REQUEST_BY_TENANT_KEY,
                            (rs, rowNum) -> mapDecisionRequest(rs),
                            tenantId,
                            requestKey)
                    .stream()
                    .findFirst();
        } catch (final DataAccessException error) {
            throw new DecisionCorePersistenceException("find decision request failed", error);
        }
    }

    @Override
    public void save(final DecisionRun run) {
        final DecisionRun checked = Objects.requireNonNull(run, "run");
        update(
                "save decision run",
                INSERT_DECISION_RUN,
                checked.id(),
                checked.decisionRequestId(),
                checked.runNo(),
                checked.status().name(),
                checked.orchestratorKey(),
                checked.modelProvider(),
                checked.modelName(),
                timestamp(checked.startedAt()),
                timestampOrNull(checked.finishedAt()),
                checked.latencyMs(),
                checked.errorCode(),
                checked.errorMessage(),
                timestamp(checked.createdAt()));
    }

    @Override
    public void complete(
            final UUID id,
            final DecisionRunStatus status,
            final Instant finishedAt,
            final Long latencyMs,
            final String errorCode,
            final String errorMessage) {
        update(
                "complete decision run",
                COMPLETE_DECISION_RUN,
                status.name(),
                timestampOrNull(finishedAt),
                latencyMs,
                errorCode,
                errorMessage,
                id);
    }

    @Override
    public List<DecisionRun> findByDecisionRequestId(final UUID decisionRequestId) {
        try {
            return jdbcTemplate.query(
                    SELECT_DECISION_RUN_BY_REQUEST, (rs, rowNum) -> mapDecisionRun(rs), decisionRequestId);
        } catch (final DataAccessException error) {
            throw new DecisionCorePersistenceException("find decision runs failed", error);
        }
    }

    @Override
    public Optional<DecisionRun> findById(final UUID id) {
        try {
            return jdbcTemplate.query(SELECT_DECISION_RUN_BY_ID, (rs, rowNum) -> mapDecisionRun(rs), id)
                    .stream()
                    .findFirst();
        } catch (final DataAccessException error) {
            throw new DecisionCorePersistenceException("find decision run failed", error);
        }
    }

    @Override
    public void save(final QuantSignal signal) {
        final QuantSignal checked = Objects.requireNonNull(signal, "signal");
        update(
                "save quant signal",
                INSERT_QUANT_SIGNAL,
                checked.id(),
                checked.decisionRequestId(),
                checked.sourceSystem(),
                checked.symbol(),
                checked.exchange(),
                checked.timeframe(),
                checked.signalType(),
                writeJson(checked.signalPayloadJson(), "signalPayloadJson"),
                checked.strategyId(),
                checked.strategyVersion(),
                checked.datasetVersion(),
                timestamp(checked.receivedAt()),
                timestamp(checked.createdAt()));
    }

    @Override
  public List<QuantSignal> findSignalsByDecisionRequestId(final UUID decisionRequestId) {
    try {
      return jdbcTemplate.query(
          SELECT_QUANT_SIGNAL_BY_REQUEST, (rs, rowNum) -> mapQuantSignal(rs), decisionRequestId);
        } catch (final DataAccessException error) {
            throw new DecisionCorePersistenceException("find quant signals failed", error);
        }
    }

    @Override
    public Optional<QuantSignal> findSignalById(final UUID id) {
        try {
            return jdbcTemplate.query(SELECT_QUANT_SIGNAL_BY_ID, (rs, rowNum) -> mapQuantSignal(rs), id)
                    .stream()
                    .findFirst();
        } catch (final DataAccessException error) {
            throw new DecisionCorePersistenceException("find quant signal failed", error);
        }
    }

    @Override
    public void save(final QuantDecision decision) {
        final QuantDecision checked = Objects.requireNonNull(decision, "decision");
        update(
                "save quant decision",
                INSERT_QUANT_DECISION,
                checked.id(),
                checked.quantSignalId(),
                checked.decisionRunId(),
                checked.action().name(),
                checked.confidenceScore(),
                checked.riskLevel().name(),
                checked.rationale(),
                writeJsonOrNull(checked.constraintsJson(), "constraintsJson"),
                checked.humanApprovalStatus().name(),
                timestamp(checked.createdAt()));
    }

    @Override
    public List<QuantDecision> findByDecisionRunId(final UUID decisionRunId) {
        try {
            return jdbcTemplate.query(
                    SELECT_QUANT_DECISION_BY_RUN, (rs, rowNum) -> mapQuantDecision(rs), decisionRunId);
        } catch (final DataAccessException error) {
            throw new DecisionCorePersistenceException("find quant decisions failed", error);
        }
    }

    private void update(final String operation, final String sql, final Object... args) {
        try {
            jdbcTemplate.update(sql, args);
        } catch (final DataAccessException error) {
            throw new DecisionCorePersistenceException(operation + " failed", error);
        }
    }

    private DecisionRequest mapDecisionRequest(final ResultSet rs) throws SQLException {
        return new DecisionRequest(
                uuid(rs, "id"),
                rs.getString("request_key"),
                rs.getString("request_type"),
                rs.getString("source_system"),
                rs.getString("source_ref_id"),
                rs.getString("tenant_id"),
                rs.getString("trace_id"),
                rs.getString("request_id"),
                readMap(rs.getObject("input_payload_json"), "input_payload_json"),
                readMapOrNull(rs.getObject("context_payload_json"), "context_payload_json"),
                DecisionRequestStatus.valueOf(rs.getString("status")),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }

    private DecisionRun mapDecisionRun(final ResultSet rs) throws SQLException {
        return new DecisionRun(
                uuid(rs, "id"),
                uuid(rs, "decision_request_id"),
                rs.getInt("run_no"),
                DecisionRunStatus.valueOf(rs.getString("status")),
                rs.getString("orchestrator_key"),
                rs.getString("model_provider"),
                rs.getString("model_name"),
                instant(rs, "started_at"),
                instantOrNull(rs, "finished_at"),
                longOrNull(rs, "latency_ms"),
                rs.getString("error_code"),
                rs.getString("error_message"),
                instant(rs, "created_at"));
    }

    private QuantSignal mapQuantSignal(final ResultSet rs) throws SQLException {
        return new QuantSignal(
                uuid(rs, "id"),
                uuid(rs, "decision_request_id"),
                rs.getString("source_system"),
                rs.getString("symbol"),
                rs.getString("exchange"),
                rs.getString("timeframe"),
                rs.getString("signal_type"),
                readMap(rs.getObject("signal_payload_json"), "signal_payload_json"),
                rs.getString("strategy_id"),
                rs.getString("strategy_version"),
                rs.getString("dataset_version"),
                instant(rs, "received_at"),
                instant(rs, "created_at"));
    }

    private QuantDecision mapQuantDecision(final ResultSet rs) throws SQLException {
        return new QuantDecision(
                uuid(rs, "id"),
                uuidOrNull(rs, "quant_signal_id"),
                uuid(rs, "decision_run_id"),
                QuantDecisionAction.valueOf(rs.getString("action")),
                rs.getBigDecimal("confidence_score"),
                RiskLevel.valueOf(rs.getString("risk_level")),
                rs.getString("rationale"),
                readMapOrNull(rs.getObject("constraints_json"), "constraints_json"),
                HumanApprovalStatus.valueOf(rs.getString("human_approval_status")),
                instant(rs, "created_at"));
    }

    private String writeJson(final Object payload, final String fieldName) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (final JsonProcessingException error) {
            throw new DecisionCorePersistenceException("failed to serialize " + fieldName, error);
        }
    }

    private String writeJsonOrNull(final Object payload, final String fieldName) {
        return payload == null ? null : writeJson(payload, fieldName);
    }

    private Map<String, Object> readMap(final Object value, final String fieldName) {
        final Map<String, Object> payload = readMapOrNull(value, fieldName);
        return payload == null ? Map.of() : payload;
    }

    private Map<String, Object> readMapOrNull(final Object value, final String fieldName) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value.toString(), MAP_TYPE);
        } catch (final JsonProcessingException error) {
            throw new DecisionCorePersistenceException("failed to parse " + fieldName, error);
        }
    }

    private static Timestamp timestamp(final Instant value) {
        return Timestamp.from(Objects.requireNonNull(value, "value"));
    }

    private static Timestamp timestampOrNull(final Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(final ResultSet rs, final String column) throws SQLException {
        return Objects.requireNonNull(rs.getTimestamp(column), column).toInstant();
    }

    private static Instant instantOrNull(final ResultSet rs, final String column) throws SQLException {
        final Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static UUID uuid(final ResultSet rs, final String column) throws SQLException {
        return (UUID) rs.getObject(column);
    }

    private static UUID uuidOrNull(final ResultSet rs, final String column) throws SQLException {
        final Object value = rs.getObject(column);
        return value == null ? null : (UUID) value;
    }

    private static Long longOrNull(final ResultSet rs, final String column) throws SQLException {
        final long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}

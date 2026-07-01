package com.guidinglight.decisionhub.infra.jdbc.decision;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionPersistenceException;
import com.guidinglight.decisionhub.usecase.decision.DecisionPersistenceRecords;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * K3 decision audit repository 的 JDBC 实现。
 *
 * <p>本实现只写 DH-owned audit tables，不查询 replay、不调用外部服务、不接 NQ、不保存 credential。JSON 写入统一使用
 * {@code CAST(? AS jsonb)}；JSON 序列化或数据库写入失败都会抛 {@link DecisionPersistenceException}。
 */
public final class JdbcDecisionAuditRepository implements DecisionAuditRepository {

  private static final String INSERT_REQUEST_SQL =
      "insert into dh_decision_request"
          + " (decision_id, request_id, trace_id, tenant_id, source, decision_type,"
          + " subject_json, context_ref, requested_at, schema_version, created_at)"
          + " values (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?)";

  private static final String INSERT_CONTEXT_SNAPSHOT_SQL =
      "insert into dh_decision_context_snapshot"
          + " (decision_id, tenant_id, trace_id, context_snapshot_json, evidence_refs_json,"
          + " created_at)"
          + " values (?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?)";

  private static final String INSERT_TRACE_STEP_SQL =
      "insert into dh_decision_trace_step"
          + " (id, decision_id, tenant_id, trace_id, step_name, step_status, started_at,"
          + " ended_at, error_code, error_message, created_at)"
          + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private static final String INSERT_PROVIDER_CALL_SQL =
      "insert into dh_decision_provider_call_log"
          + " (id, decision_id, tenant_id, trace_id, provider_name, provider_status,"
          + " latency_ms, signal_json, error_code, created_at)"
          + " values (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?)";

  private static final String INSERT_OUTPUT_SQL =
      "insert into dh_decision_output"
          + " (decision_id, tenant_id, trace_id, request_id, decision_type, action,"
          + " risk_level, policy_status, confidence, output_json, created_at)"
          + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)";

  private static final String INSERT_AUDIT_EVENT_SQL =
      "insert into dh_decision_audit_event"
          + " (id, decision_id, tenant_id, trace_id, event_type, event_status, event_json,"
          + " error_code, created_at)"
          + " values (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?)";

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  /**
   * 创建 JDBC repository。
   *
   * @param jdbcTemplate DH 自身 datasource 的 JdbcTemplate，不得指向 NQ DB。
   * @param objectMapper JSON 序列化器。
   */
  public JdbcDecisionAuditRepository(
      final JdbcTemplate jdbcTemplate, final ObjectMapper objectMapper) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  @Override
  public void saveRequest(final DecisionPersistenceRecords.RequestRecord record) {
    final DecisionPersistenceRecords.RequestRecord checked =
        Objects.requireNonNull(record, "record");
    update(
        "save decision request",
        INSERT_REQUEST_SQL,
        checked.decisionId(),
        checked.requestId(),
        checked.traceId(),
        checked.tenantId(),
        checked.source(),
        checked.decisionType().name(),
        writeJson(checked.subjectJson(), "subjectJson"),
        checked.contextRef(),
        timestamp(checked.requestedAt()),
        checked.schemaVersion(),
        timestamp(checked.createdAt()));
  }

  @Override
  public void saveContextSnapshot(
      final DecisionPersistenceRecords.ContextSnapshotRecord record) {
    final DecisionPersistenceRecords.ContextSnapshotRecord checked =
        Objects.requireNonNull(record, "record");
    update(
        "save decision context snapshot",
        INSERT_CONTEXT_SNAPSHOT_SQL,
        checked.decisionId(),
        checked.tenantId(),
        checked.traceId(),
        writeJson(checked.contextSnapshotJson(), "contextSnapshotJson"),
        writeJson(checked.evidenceRefsJson(), "evidenceRefsJson"),
        timestamp(checked.createdAt()));
  }

  @Override
  public void saveTraceStep(final DecisionPersistenceRecords.TraceStepRecord record) {
    final DecisionPersistenceRecords.TraceStepRecord checked =
        Objects.requireNonNull(record, "record");
    update(
        "save decision trace step",
        INSERT_TRACE_STEP_SQL,
        checked.id(),
        checked.decisionId(),
        checked.tenantId(),
        checked.traceId(),
        checked.stepName().name(),
        checked.stepStatus().name(),
        timestamp(checked.startedAt()),
        timestampOrNull(checked.endedAt()),
        checked.errorCode(),
        checked.errorMessage(),
        timestamp(checked.createdAt()));
  }

  @Override
  public void saveProviderCall(final DecisionPersistenceRecords.ProviderCallRecord record) {
    final DecisionPersistenceRecords.ProviderCallRecord checked =
        Objects.requireNonNull(record, "record");
    update(
        "save decision provider call",
        INSERT_PROVIDER_CALL_SQL,
        checked.id(),
        checked.decisionId(),
        checked.tenantId(),
        checked.traceId(),
        checked.providerName(),
        checked.providerStatus().name(),
        checked.latencyMs(),
        writeJson(checked.signalJson(), "signalJson"),
        checked.errorCode(),
        timestamp(checked.createdAt()));
  }

  @Override
  public void saveOutput(final DecisionPersistenceRecords.OutputRecord record) {
    final DecisionPersistenceRecords.OutputRecord checked =
        Objects.requireNonNull(record, "record");
    update(
        "save decision output",
        INSERT_OUTPUT_SQL,
        checked.decisionId(),
        checked.tenantId(),
        checked.traceId(),
        checked.requestId(),
        checked.decisionType().name(),
        checked.action().name(),
        checked.riskLevel().name(),
        checked.policyStatus().name(),
        checked.confidence(),
        writeJson(checked.outputJson(), "outputJson"),
        timestamp(checked.createdAt()));
  }

  @Override
  public void saveAuditEvent(final DecisionPersistenceRecords.AuditEventRecord record) {
    final DecisionPersistenceRecords.AuditEventRecord checked =
        Objects.requireNonNull(record, "record");
    update(
        "save decision audit event",
        INSERT_AUDIT_EVENT_SQL,
        checked.id(),
        checked.decisionId(),
        checked.tenantId(),
        checked.traceId(),
        checked.eventType().name(),
        checked.eventStatus().name(),
        writeJson(checked.eventJson(), "eventJson"),
        checked.errorCode(),
        timestamp(checked.createdAt()));
  }

  private void update(final String operation, final String sql, final Object... args) {
    try {
      jdbcTemplate.update(sql, args);
    } catch (final DataAccessException error) {
      throw new DecisionPersistenceException(operation + " failed", error);
    }
  }

  private String writeJson(final Object payload, final String fieldName) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (final JsonProcessingException error) {
      throw new DecisionPersistenceException("failed to serialize " + fieldName, error);
    }
  }

  private static Timestamp timestamp(final Instant value) {
    return Timestamp.from(Objects.requireNonNull(value, "value"));
  }

  private static Timestamp timestampOrNull(final Instant value) {
    return value == null ? null : Timestamp.from(value);
  }
}

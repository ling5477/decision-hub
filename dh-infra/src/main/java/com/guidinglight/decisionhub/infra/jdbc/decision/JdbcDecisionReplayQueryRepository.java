package com.guidinglight.decisionhub.infra.jdbc.decision;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayAuditEventView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayContextView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayOutputView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayProviderCallView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayRequestView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayTimelineView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayTraceStepView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQuery;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQueryRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * K4 replay read model 的 JDBC repository。
 *
 * <p>本实现只读取 DH-owned K3 六张 decision audit 表；所有 SQL 都带 tenant_id 和 decision_id，不做跨租户查询，
 * 不写库、不调用 provider、不重跑 orchestrator、不接 HTTP 或 NQ runtime。
 */
public final class JdbcDecisionReplayQueryRepository implements DecisionReplayQueryRepository {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

  private static final String SELECT_REQUEST_SQL =
      "select decision_id, request_id, trace_id, tenant_id, source, decision_type,"
          + " subject_json::text as subject_json, context_ref, requested_at, schema_version,"
          + " created_at from dh_decision_request where tenant_id = ? and decision_id = ?";

  private static final String SELECT_CONTEXT_SQL =
      "select decision_id, tenant_id, trace_id, context_snapshot_json::text"
          + " as context_snapshot_json, evidence_refs_json::text as evidence_refs_json,"
          + " created_at from dh_decision_context_snapshot where tenant_id = ? and decision_id = ?";

  private static final String SELECT_TRACE_SQL =
      "select id, decision_id, tenant_id, trace_id, step_name, step_status, started_at,"
          + " ended_at, error_code, error_message, created_at from dh_decision_trace_step"
          + " where tenant_id = ? and decision_id = ? order by started_at asc, created_at asc, id asc";

  private static final String SELECT_PROVIDER_SQL =
      "select id, decision_id, tenant_id, trace_id, provider_name, provider_status, latency_ms,"
          + " signal_json::text as signal_json, error_code, created_at"
          + " from dh_decision_provider_call_log where tenant_id = ? and decision_id = ?"
          + " order by created_at asc, id asc";

  private static final String SELECT_OUTPUT_SQL =
      "select decision_id, tenant_id, trace_id, request_id, decision_type, action, risk_level,"
          + " policy_status, confidence, output_json::text as output_json, created_at"
          + " from dh_decision_output where tenant_id = ? and decision_id = ?";

  private static final String SELECT_AUDIT_SQL =
      "select id, decision_id, tenant_id, trace_id, event_type, event_status,"
          + " event_json::text as event_json, error_code, created_at"
          + " from dh_decision_audit_event where tenant_id = ? and decision_id = ?"
          + " order by created_at asc, id asc";

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  /**
   * 创建 K4 JDBC replay repository。
   *
   * @param jdbcTemplate DH 自身 datasource 的 JdbcTemplate；不得指向 NQ DB。
   * @param objectMapper K4 内部 JSON parser；不得作为全局 Spring ObjectMapper bean 暴露。
   */
  public JdbcDecisionReplayQueryRepository(
      final JdbcTemplate jdbcTemplate, final ObjectMapper objectMapper) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  @Override
  public DecisionReplayView findReplay(final DecisionReplayQuery query) {
    final DecisionReplayQuery checked = Objects.requireNonNull(query, "query");
    try {
      final List<Map<String, Object>> requestRows = query(SELECT_REQUEST_SQL, checked);
      final List<Map<String, Object>> contextRows = query(SELECT_CONTEXT_SQL, checked);
      final List<Map<String, Object>> traceRows = query(SELECT_TRACE_SQL, checked);
      final List<Map<String, Object>> providerRows = query(SELECT_PROVIDER_SQL, checked);
      final List<Map<String, Object>> outputRows = query(SELECT_OUTPUT_SQL, checked);
      final List<Map<String, Object>> auditRows = query(SELECT_AUDIT_SQL, checked);

      if (allEmpty(requestRows, contextRows, traceRows, providerRows, outputRows, auditRows)) {
        return DecisionReplayView.notFound(checked.tenantId(), checked.decisionId());
      }

      final List<String> missing = missingCriticalRows(requestRows, contextRows, traceRows, outputRows, auditRows);
      if (!missing.isEmpty()) {
        return DecisionReplayView.incomplete(
            checked.tenantId(),
            checked.decisionId(),
            firstTraceId(requestRows, contextRows, traceRows, outputRows, auditRows),
            firstRequestId(requestRows, outputRows),
            missing);
      }

      final DecisionReplayRequestView request = requestView(single(requestRows, "request"));
      final DecisionReplayContextView context = contextView(single(contextRows, "context"));
      final DecisionReplayOutputView output = outputView(single(outputRows, "output"));
      final DecisionReplayTimelineView timeline =
          new DecisionReplayTimelineView(
              traceRows.stream().map(this::traceStepView).toList(),
              providerRows.stream().map(this::providerCallView).toList(),
              auditRows.stream().map(this::auditEventView).toList());
      return DecisionReplayView.found(request, context, output, timeline);
    } catch (final ReplayCorruptionException error) {
      return DecisionReplayView.corrupted(
          checked.tenantId(), checked.decisionId(), "REPLAY_DATA_CORRUPTED");
    } catch (final DataAccessException error) {
      return DecisionReplayView.blocked(
          checked.tenantId(), checked.decisionId(), "REPLAY_QUERY_FAILED");
    }
  }

  private List<Map<String, Object>> query(final String sql, final DecisionReplayQuery query) {
    return jdbcTemplate.queryForList(sql, query.tenantId(), query.decisionId());
  }

  private DecisionReplayRequestView requestView(final Map<String, Object> row) {
    return new DecisionReplayRequestView(
        text(row, "decision_id"),
        text(row, "request_id"),
        text(row, "trace_id"),
        text(row, "tenant_id"),
        text(row, "source"),
        enumValue(DecisionType.class, row, "decision_type"),
        readMap(row.get("subject_json"), "subject_json"),
        nullableText(row.get("context_ref")),
        instant(row, "requested_at"),
        text(row, "schema_version"),
        instant(row, "created_at"));
  }

  private DecisionReplayContextView contextView(final Map<String, Object> row) {
    return new DecisionReplayContextView(
        text(row, "decision_id"),
        text(row, "tenant_id"),
        text(row, "trace_id"),
        readMap(row.get("context_snapshot_json"), "context_snapshot_json"),
        readStringList(row.get("evidence_refs_json"), "evidence_refs_json"),
        instant(row, "created_at"));
  }

  private DecisionReplayTraceStepView traceStepView(final Map<String, Object> row) {
    return new DecisionReplayTraceStepView(
        text(row, "id"),
        text(row, "decision_id"),
        text(row, "tenant_id"),
        text(row, "trace_id"),
        text(row, "step_name"),
        text(row, "step_status"),
        instant(row, "started_at"),
        instantOrNull(row.get("ended_at")),
        nullableText(row.get("error_code")),
        nullableText(row.get("error_message")),
        instant(row, "created_at"));
  }

  private DecisionReplayProviderCallView providerCallView(final Map<String, Object> row) {
    return new DecisionReplayProviderCallView(
        text(row, "id"),
        text(row, "decision_id"),
        text(row, "tenant_id"),
        text(row, "trace_id"),
        text(row, "provider_name"),
        enumValue(ProviderSignalStatus.class, row, "provider_status"),
        longValue(row, "latency_ms"),
        readMap(row.get("signal_json"), "signal_json"),
        nullableText(row.get("error_code")),
        instant(row, "created_at"));
  }

  private DecisionReplayOutputView outputView(final Map<String, Object> row) {
    return new DecisionReplayOutputView(
        text(row, "decision_id"),
        text(row, "tenant_id"),
        text(row, "trace_id"),
        text(row, "request_id"),
        enumValue(DecisionType.class, row, "decision_type"),
        enumValue(DecisionAction.class, row, "action"),
        enumValue(DecisionRiskLevel.class, row, "risk_level"),
        enumValue(DecisionPolicyStatus.class, row, "policy_status"),
        decimal(row, "confidence"),
        readMap(row.get("output_json"), "output_json"),
        instant(row, "created_at"));
  }

  private DecisionReplayAuditEventView auditEventView(final Map<String, Object> row) {
    return new DecisionReplayAuditEventView(
        text(row, "id"),
        text(row, "decision_id"),
        text(row, "tenant_id"),
        text(row, "trace_id"),
        text(row, "event_type"),
        text(row, "event_status"),
        readMap(row.get("event_json"), "event_json"),
        nullableText(row.get("error_code")),
        instant(row, "created_at"));
  }

  @SafeVarargs
  private static boolean allEmpty(final List<Map<String, Object>>... rowSets) {
    for (List<Map<String, Object>> rows : rowSets) {
      if (!rows.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  private static List<String> missingCriticalRows(
      final List<Map<String, Object>> requestRows,
      final List<Map<String, Object>> contextRows,
      final List<Map<String, Object>> traceRows,
      final List<Map<String, Object>> outputRows,
      final List<Map<String, Object>> auditRows) {
    final List<String> missing = new ArrayList<>();
    if (requestRows.isEmpty()) {
      missing.add("REQUEST_MISSING");
    }
    if (contextRows.isEmpty()) {
      missing.add("CONTEXT_SNAPSHOT_MISSING");
    }
    if (traceRows.isEmpty()) {
      missing.add("TRACE_STEP_MISSING");
    }
    if (outputRows.isEmpty()) {
      missing.add("OUTPUT_MISSING");
    }
    if (auditRows.isEmpty()) {
      missing.add("AUDIT_EVENT_MISSING");
    }
    return missing;
  }

  private static Map<String, Object> single(
      final List<Map<String, Object>> rows, final String component) {
    if (rows.size() != 1) {
      throw new ReplayCorruptionException(component + " row count must be exactly one");
    }
    return rows.get(0);
  }

  @SafeVarargs
  private static String firstTraceId(final List<Map<String, Object>>... rowSets) {
    for (List<Map<String, Object>> rows : rowSets) {
      for (Map<String, Object> row : rows) {
        final String traceId = nullableText(row.get("trace_id"));
        if (traceId != null) {
          return traceId;
        }
      }
    }
    return null;
  }

  private static String firstRequestId(
      final List<Map<String, Object>> requestRows, final List<Map<String, Object>> outputRows) {
    for (Map<String, Object> row : requestRows) {
      final String requestId = nullableText(row.get("request_id"));
      if (requestId != null) {
        return requestId;
      }
    }
    for (Map<String, Object> row : outputRows) {
      final String requestId = nullableText(row.get("request_id"));
      if (requestId != null) {
        return requestId;
      }
    }
    return null;
  }

  private Map<String, Object> readMap(final Object value, final String fieldName) {
    if (value == null) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(value.toString(), MAP_TYPE);
    } catch (final JsonProcessingException error) {
      throw new ReplayCorruptionException("failed to parse " + fieldName, error);
    }
  }

  private List<String> readStringList(final Object value, final String fieldName) {
    if (value == null) {
      return List.of();
    }
    try {
      return objectMapper.readValue(value.toString(), STRING_LIST_TYPE);
    } catch (final JsonProcessingException error) {
      throw new ReplayCorruptionException("failed to parse " + fieldName, error);
    }
  }

  private static String text(final Map<String, Object> row, final String field) {
    final String value = nullableText(row.get(field));
    if (value == null) {
      throw new ReplayCorruptionException(field + " is blank");
    }
    return value;
  }

  private static String nullableText(final Object value) {
    if (value == null) {
      return null;
    }
    final String text = value.toString().trim();
    return text.isEmpty() ? null : text;
  }

  private static <T extends Enum<T>> T enumValue(
      final Class<T> enumType, final Map<String, Object> row, final String field) {
    try {
      return Enum.valueOf(enumType, text(row, field));
    } catch (final IllegalArgumentException error) {
      throw new ReplayCorruptionException(field + " has unknown value", error);
    }
  }

  private static Instant instant(final Map<String, Object> row, final String field) {
    final Instant value = instantOrNull(row.get(field));
    if (value == null) {
      throw new ReplayCorruptionException(field + " is missing");
    }
    return value;
  }

  private static Instant instantOrNull(final Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toInstant();
    }
    throw new ReplayCorruptionException("timestamp field has unsupported type");
  }

  private static long longValue(final Map<String, Object> row, final String field) {
    final Object value = row.get(field);
    if (value instanceof Number number) {
      return number.longValue();
    }
    throw new ReplayCorruptionException(field + " is not numeric");
  }

  private static BigDecimal decimal(final Map<String, Object> row, final String field) {
    final Object value = row.get(field);
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    if (value instanceof String text) {
      return new BigDecimal(text);
    }
    throw new ReplayCorruptionException(field + " is not decimal");
  }

  private static final class ReplayCorruptionException extends RuntimeException {
    private ReplayCorruptionException(final String message) {
      super(message);
    }

    private ReplayCorruptionException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}

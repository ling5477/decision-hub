package com.guidinglight.decisionhub.infra.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.feedback.FeedbackSource;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Stage2-PoC-B5：NqFeedbackEventRepository 的 JDBC 实现。
 *
 * <p>JSON 列写入采用 {@code CAST(? AS jsonb)}；TIMESTAMPTZ 列采用 {@link Timestamp#from(Instant)} 注入。 eventId
 * 唯一性由 V3 迁移 {@code ux_dh_nq_feedback_events_event_id} 唯一索引 + 本类 {@link #findEnvelopeByEventId} 双重保障。
 *
 * <p>本实现不调用任何外部服务，不发起 HTTP，不处理订单/成交/仓位。
 */
public final class JdbcNqFeedbackEventRepository implements NqFeedbackEventRepository {

  private static final String INSERT_EVENT_SQL =
      "insert into dh_nq_feedback_events"
          + " (id, tenant_id, run_id, candidate_id, trace_id, source, event_type, positive,"
          + "  status, payload_json, occurred_at, received_at)"
          + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?)";

  private static final String SELECT_BY_RUN_SQL =
      "select id, tenant_id, run_id, candidate_id, trace_id, source, event_type, positive,"
          + " payload_json, occurred_at, received_at"
          + " from dh_nq_feedback_events"
          + " where tenant_id = ? and run_id = ?"
          + " order by received_at asc";

  private static final String INSERT_ENVELOPE_SQL =
      "insert into dh_nq_feedback_events"
          + " (id, tenant_id, run_id, candidate_id, trace_id, source, event_type, positive,"
          + "  status, payload_json, occurred_at, received_at,"
          + "  event_id, schema_version, validation_status, source_job_id, request_id, correlation_id)"
          + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?)";

  private static final String SELECT_BY_EVENT_ID_SQL =
      "select event_id, event_type, occurred_at, source, source_job_id, trace_id,"
          + " request_id, correlation_id, schema_version, payload_json, received_at"
          + " from dh_nq_feedback_events"
          + " where event_id = ?";

  private static final String ENVELOPE_VALIDATION_STATUS_VALID = "VALID";
  private static final String ENVELOPE_DEFAULT_STATUS = "RECEIVED";
  /** envelope 写入时与 Stage1 NqFeedbackEvent 共表，positive 字段在 envelope 路径下不可用，默认 {@code true}。 */
  private static final boolean ENVELOPE_DEFAULT_POSITIVE = true;
  /** envelope 路径下没有 runId / candidateId 概念；保留 NOT NULL 列时落占位串。 */
  private static final String ENVELOPE_RUN_ID_PLACEHOLDER = "envelope";
  private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE =
      new TypeReference<Map<String, Object>>() {};

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  /** 构造。 */
  public JdbcNqFeedbackEventRepository(
      final JdbcTemplate jdbcTemplate, final ObjectMapper objectMapper) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  @Override
  public void append(final NqFeedbackEvent event) {
    Objects.requireNonNull(event, "event");
    jdbcTemplate.update(
        INSERT_EVENT_SQL,
        event.getEventId(),
        event.getTenantId(),
        event.getRunId(),
        event.getCandidateId(),
        event.getTraceId(),
        event.getSource().name(),
        event.getEventType(),
        event.isPositive(),
        ENVELOPE_DEFAULT_STATUS,
        writeJson(event.getPayloadJson()),
        Timestamp.from(event.getOccurredAt()),
        Timestamp.from(event.getReceivedAt()));
  }

  @Override
  public List<NqFeedbackEvent> listByRun(final String tenantId, final String runId) {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(runId, "runId");
    return jdbcTemplate.query(SELECT_BY_RUN_SQL, eventRowMapper(), tenantId, runId);
  }

  @Override
  public boolean saveEnvelope(final NqFeedbackEnvelope envelope) {
    Objects.requireNonNull(envelope, "envelope");
    // 双重保障：先按 eventId 查一次，命中则视作幂等（false）。再写入时若并发命中唯一索引，捕获后返回 false。
    if (findEnvelopeByEventId(envelope.getEventId()).isPresent()) {
      return false;
    }
    final Instant receivedAt = envelope.getReceivedAt() != null ? envelope.getReceivedAt() : Instant.now();
    try {
      jdbcTemplate.update(
          INSERT_ENVELOPE_SQL,
          envelope.getEventId(),
          envelope.getSourceSystem(),
          ENVELOPE_RUN_ID_PLACEHOLDER,
          null,
          envelope.getTraceId(),
          FeedbackSource.PAPER.name(),
          envelope.getEventType().name(),
          ENVELOPE_DEFAULT_POSITIVE,
          ENVELOPE_DEFAULT_STATUS,
          envelope.getPayloadJson(),
          Timestamp.from(envelope.getOccurredAt()),
          Timestamp.from(receivedAt),
          envelope.getEventId(),
          envelope.getSchemaVersion(),
          ENVELOPE_VALIDATION_STATUS_VALID,
          envelope.getSourceJobId(),
          envelope.getRequestId(),
          envelope.getCorrelationId());
      return true;
    } catch (DuplicateKeyException ex) {
      return false;
    }
  }

  @Override
  public Optional<NqFeedbackEnvelope> findEnvelopeByEventId(final String eventId) {
    if (eventId == null) {
      return Optional.empty();
    }
    final List<NqFeedbackEnvelope> rows =
        jdbcTemplate.query(SELECT_BY_EVENT_ID_SQL, envelopeRowMapper(), eventId);
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(rows.get(0));
  }

  private RowMapper<NqFeedbackEvent> eventRowMapper() {
    return (rs, rowNum) ->
        NqFeedbackEvent.create(
            rs.getString("tenant_id"),
            rs.getString("run_id"),
            rs.getString("candidate_id"),
            rs.getString("trace_id"),
            FeedbackSource.valueOf(rs.getString("source")),
            rs.getString("event_type"),
            rs.getBoolean("positive"),
            readJson(rs.getString("payload_json")),
            rs.getTimestamp("occurred_at").toInstant(),
            rs.getTimestamp("received_at").toInstant());
  }

  private RowMapper<NqFeedbackEnvelope> envelopeRowMapper() {
    return (rs, rowNum) ->
        NqFeedbackEnvelope.of(
            rs.getString("event_id"),
            NqFeedbackEventType.valueOf(rs.getString("event_type")),
            rs.getTimestamp("occurred_at").toInstant(),
            NqFeedbackEnvelope.SOURCE_SYSTEM_NEXUS_QUANT,
            rs.getString("source_job_id"),
            rs.getString("trace_id"),
            rs.getString("request_id"),
            rs.getString("correlation_id"),
            rs.getString("schema_version"),
            rs.getString("payload_json"),
            rs.getTimestamp("received_at") != null ? rs.getTimestamp("received_at").toInstant() : null);
  }

  private String writeJson(final Map<String, Object> payload) {
    final Map<String, Object> safe = payload == null ? Map.of() : payload;
    try {
      return objectMapper.writeValueAsString(safe);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("failed to serialize payloadJson", e);
    }
  }

  private Map<String, Object> readJson(final String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(json, PAYLOAD_TYPE);
    } catch (Exception e) {
      throw new IllegalStateException("failed to deserialize payloadJson", e);
    }
  }
}

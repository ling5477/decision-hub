package com.guidinglight.decisionhub.infra.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.feedback.FeedbackSource;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionTransactionException;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionUnitOfWork;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Stage2-PoC-B5：NqFeedbackEventRepository 的 JDBC 实现。
 *
 * <p>JSON 列写入采用 {@code CAST(? AS jsonb)}；TIMESTAMPTZ 列采用 {@link Timestamp#from(Instant)} 注入。 eventId
 * 唯一性由 V3 迁移 {@code ux_dh_nq_feedback_events_event_id} 唯一索引 + 本类 {@link #findEnvelopeByEventId} 双重保障。
 *
 * <p>本实现不调用任何外部服务，不发起 HTTP，不处理订单/成交/仓位。
 */
public final class JdbcNqFeedbackEventRepository
    implements NqFeedbackEventRepository, NqFeedbackIngestionUnitOfWork {

  private static final String INSERT_EVENT_SQL =
      "insert into dh_nq_feedback_events"
          + " (id, tenant_id, run_id, candidate_id, trace_id, source, event_type, positive,"
          + "  status, payload_json, occurred_at, received_at, correlation_id)"
          + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?)";

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
          + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?)"
          + " on conflict do nothing";

  private static final String SELECT_BY_EVENT_ID_SQL =
      "select event_id, event_type, occurred_at, source, source_job_id, trace_id,"
          + " request_id, correlation_id, schema_version, payload_json, received_at"
          + " from dh_nq_feedback_events"
          + " where event_id = ?";

  private static final String SELECT_INGESTION_PERSISTENCE_SQL =
      "with ingestion_rows as ("
          + " select * from dh_nq_feedback_events"
          + " where event_id = ? or (correlation_id = ? and event_id is null)"
          + ")"
          + " select"
          + " count(*) filter (where event_id is not null) as envelope_count,"
          + " count(*) filter (where event_id is null) as correlated_event_count,"
          + " max(event_id) filter (where event_id is not null) as envelope_event_id,"
          + " max(event_type) filter (where event_id is not null) as envelope_event_type,"
          + " max(occurred_at) filter (where event_id is not null) as envelope_occurred_at,"
          + " max(source_job_id) filter (where event_id is not null) as envelope_source_job_id,"
          + " max(trace_id) filter (where event_id is not null) as envelope_trace_id,"
          + " max(request_id) filter (where event_id is not null) as envelope_request_id,"
          + " max(correlation_id) filter (where event_id is not null)"
          + " as envelope_correlation_id,"
          + " max(schema_version) filter (where event_id is not null) as envelope_schema_version,"
          + " max(payload_json::text) filter (where event_id is not null)"
          + " as envelope_payload_json,"
          + " max(received_at) filter (where event_id is not null) as envelope_received_at,"
          + " max(tenant_id) filter (where event_id is null) as event_tenant_id,"
          + " max(run_id) filter (where event_id is null) as event_run_id,"
          + " max(trace_id) filter (where event_id is null) as event_trace_id,"
          + " max(source) filter (where event_id is null) as event_source,"
          + " max(event_type) filter (where event_id is null) as event_type,"
          + " max(correlation_id) filter (where event_id is null) as event_correlation_id"
          + " from ingestion_rows";

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
  private final TransactionTemplate transactionTemplate;
  private final ThreadLocal<EventCorrelation> activeEventCorrelation = new ThreadLocal<>();

  /** 构造。 */
  public JdbcNqFeedbackEventRepository(
      final JdbcTemplate jdbcTemplate,
      final ObjectMapper objectMapper,
      final PlatformTransactionManager transactionManager) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    final DataSource repositoryDataSource =
        Objects.requireNonNull(jdbcTemplate.getDataSource(), "jdbcTemplate.dataSource");
    if (!(Objects.requireNonNull(transactionManager, "transactionManager")
        instanceof DataSourceTransactionManager dataSourceTransactionManager)) {
      throw new IllegalArgumentException(
          "feedback ingestion requires a DataSourceTransactionManager");
    }
    if (dataSourceTransactionManager.getDataSource() != repositoryDataSource) {
      throw new IllegalArgumentException(
          "feedback ingestion transaction manager DataSource does not match repository DataSource");
    }
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
  }

  @Override
  public <T> T required(final Supplier<T> action) {
    final Supplier<T> checked = Objects.requireNonNull(action, "action");
    final boolean[] actionCompleted = {false};
    try {
      return transactionTemplate.execute(
          status -> {
            try {
              final T result = checked.get();
              actionCompleted[0] = true;
              return result;
            } catch (final RuntimeException | Error failure) {
              throw new ActionFailure(failure);
            }
          });
    } catch (final ActionFailure failure) {
      failure.rethrowCause();
      throw new AssertionError("unreachable");
    } catch (final CannotCreateTransactionException failure) {
      throw persistenceFailure("feedback ingestion transaction cannot be created", failure);
    } catch (final TransactionSystemException failure) {
      if (actionCompleted[0]) {
        throw new NqFeedbackIngestionTransactionException(
            NqFeedbackIngestionTransactionException.ErrorCode.COMMIT_OUTCOME_UNKNOWN,
            "feedback ingestion commit outcome cannot be proven",
            failure);
      }
      throw persistenceFailure("feedback ingestion transaction rollback failed", failure);
    } catch (final TransactionException failure) {
      throw persistenceFailure("feedback ingestion transaction failed", failure);
    }
  }

  @Override
  public void append(final NqFeedbackEvent event) {
    Objects.requireNonNull(event, "event");
    final EventCorrelation correlation = activeEventCorrelation.get();
    if (correlation != null && !correlation.matches(event)) {
      throw persistenceFailure("routed feedback event does not match envelope correlation", null);
    }
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
        Timestamp.from(event.getReceivedAt()),
        correlation == null ? null : correlation.envelopeEventId());
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
    final Instant receivedAt = envelope.getReceivedAt() != null ? envelope.getReceivedAt() : Instant.now();
    final int updated =
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
    if (updated == 1) {
      return true;
    }
    if (updated == 0 && findEnvelopeByEventId(envelope.getEventId()).isPresent()) {
      return false;
    }
    throw persistenceFailure("feedback envelope conflict could not be reconciled", null);
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

  @Override
  public void beginEventCorrelation(
      final NqFeedbackEnvelope envelope, final String tenantId) {
    Objects.requireNonNull(envelope, "envelope");
    Objects.requireNonNull(tenantId, "tenantId");
    if (activeEventCorrelation.get() != null) {
      throw persistenceFailure("nested feedback event correlation is not allowed", null);
    }
    activeEventCorrelation.set(
        new EventCorrelation(
            envelope.getEventId(),
            tenantId,
            envelope.getTraceId(),
            envelope.getEventType().name(),
            expectedSource(envelope.getEventType())));
  }

  @Override
  public void endEventCorrelation() {
    activeEventCorrelation.remove();
  }

  @Override
  public FeedbackIngestionPersistence findIngestionPersistence(
      final String eventId,
      final String tenantId,
      final String traceId,
      final NqFeedbackEventType eventType) {
    final List<IngestionPersistenceSnapshot> rows =
        jdbcTemplate.query(
            SELECT_INGESTION_PERSISTENCE_SQL,
            (rs, rowNum) -> {
              final int envelopeCount = rs.getInt("envelope_count");
              final int correlatedEventCount = rs.getInt("correlated_event_count");
              final Optional<NqFeedbackEnvelope> envelope =
                  envelopeCount == 1
                      ? Optional.of(
                          NqFeedbackEnvelope.of(
                              rs.getString("envelope_event_id"),
                              NqFeedbackEventType.valueOf(
                                  rs.getString("envelope_event_type")),
                              rs.getTimestamp("envelope_occurred_at").toInstant(),
                              NqFeedbackEnvelope.SOURCE_SYSTEM_NEXUS_QUANT,
                              rs.getString("envelope_source_job_id"),
                              rs.getString("envelope_trace_id"),
                              rs.getString("envelope_request_id"),
                              rs.getString("envelope_correlation_id"),
                              rs.getString("envelope_schema_version"),
                              rs.getString("envelope_payload_json"),
                              rs.getTimestamp("envelope_received_at") != null
                                  ? rs.getTimestamp("envelope_received_at").toInstant()
                                  : null))
                      : Optional.empty();
              final Optional<CorrelatedEvent> correlatedEvent =
                  correlatedEventCount == 1
                      ? Optional.of(
                          new CorrelatedEvent(
                              rs.getString("event_tenant_id"),
                              rs.getString("event_run_id"),
                              rs.getString("event_trace_id"),
                              FeedbackSource.valueOf(rs.getString("event_source")),
                              rs.getString("event_type"),
                              rs.getString("event_correlation_id")))
                      : Optional.empty();
              return new IngestionPersistenceSnapshot(
                  envelopeCount, correlatedEventCount, envelope, correlatedEvent);
            },
            eventId,
            eventId);
    if (rows.size() != 1) {
      throw persistenceFailure("feedback ingestion state query returned an invalid result", null);
    }
    final IngestionPersistenceSnapshot snapshot = rows.get(0);
    final Optional<NqFeedbackEnvelope> envelope = snapshot.envelope();
    if (snapshot.envelopeCount() > 1 || snapshot.correlatedEventCount() > 1) {
      return new FeedbackIngestionPersistence(
          FeedbackIngestionPersistenceState.AMBIGUOUS_CORRELATION, envelope);
    }
    if (snapshot.correlatedEventCount() == 0) {
      return new FeedbackIngestionPersistence(
          snapshot.envelopeCount() == 1
              ? FeedbackIngestionPersistenceState.ENVELOPE_ONLY
              : FeedbackIngestionPersistenceState.ABSENT,
          envelope);
    }
    if (snapshot.envelopeCount() == 0) {
      return new FeedbackIngestionPersistence(
          FeedbackIngestionPersistenceState.EVENT_ONLY, Optional.empty());
    }
    final CorrelatedEvent event = snapshot.correlatedEvent().orElseThrow();
    final boolean exact =
        Objects.equals(tenantId, event.tenantId())
            && Objects.equals(traceId, event.traceId())
            && Objects.equals(traceId, event.runId())
            && eventType.name().equals(event.eventType())
            && expectedSource(eventType) == event.source()
            && Objects.equals(eventId, event.correlationId());
    return new FeedbackIngestionPersistence(
        exact
            ? FeedbackIngestionPersistenceState.COMPLETE_MATCH
            : FeedbackIngestionPersistenceState.EVENT_CONFLICT,
        envelope);
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

  private static NqFeedbackIngestionTransactionException persistenceFailure(
      final String safeMessage, final Throwable cause) {
    if (cause == null) {
      return new NqFeedbackIngestionTransactionException(
          NqFeedbackIngestionTransactionException.ErrorCode.PERSISTENCE_FAILURE, safeMessage);
    }
    return new NqFeedbackIngestionTransactionException(
        NqFeedbackIngestionTransactionException.ErrorCode.PERSISTENCE_FAILURE,
        safeMessage,
        cause);
  }

  private static FeedbackSource expectedSource(final NqFeedbackEventType eventType) {
    return eventType == NqFeedbackEventType.BACKTEST_RESULT_READY
        ? FeedbackSource.BACKTEST
        : FeedbackSource.PAPER;
  }

  private record CorrelatedEvent(
      String tenantId,
      String runId,
      String traceId,
      FeedbackSource source,
      String eventType,
      String correlationId) {}

  private record IngestionPersistenceSnapshot(
      int envelopeCount,
      int correlatedEventCount,
      Optional<NqFeedbackEnvelope> envelope,
      Optional<CorrelatedEvent> correlatedEvent) {}

  private record EventCorrelation(
      String envelopeEventId,
      String tenantId,
      String traceId,
      String eventType,
      FeedbackSource source) {

    private boolean matches(final NqFeedbackEvent event) {
      return Objects.equals(tenantId, event.getTenantId())
          && Objects.equals(traceId, event.getTraceId())
          && Objects.equals(traceId, event.getRunId())
          && Objects.equals(eventType, event.getEventType())
          && source == event.getSource();
    }
  }

  private static final class ActionFailure extends RuntimeException {
    private final Throwable actionCause;

    private ActionFailure(final Throwable actionCause) {
      super("feedback ingestion action failed", actionCause);
      this.actionCause = actionCause;
    }

    private void rethrowCause() {
      if (actionCause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw (Error) actionCause;
    }
  }
}

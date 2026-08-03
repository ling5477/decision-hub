package com.guidinglight.decisionhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.feedback.FeedbackSource;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.infra.jdbc.JdbcNqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionCommand;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionErrorCode;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionOutcome;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackEventTypeRouter;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionService;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionTransactionException;
import com.guidinglight.decisionhub.usecase.agent.feedback.ValidationResult;
import com.guidinglight.decisionhub.usecase.agent.feedback.impl.DefaultNqFeedbackIngestionService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Legacy feedback envelope + routed event 的真实 PostgreSQL 17 all-or-nothing 回归。 */
@Testcontainers
class FeedbackIngestAtomicityFlywayPostgresTest {

  private static final String TENANT = "tenant-postgres-atomic";
  private static final String TRACE = "trace-postgres-atomic";
  private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("decision_hub")
          .withUsername("decision_hub")
          .withPassword("decision_hub");

  private DataSource dataSource;
  private JdbcTemplate jdbc;
  private JdbcNqFeedbackEventRepository repository;

  @BeforeEach
  void migrateFreshDatabase() {
    dataSource =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    final Flyway flyway =
        Flyway.configure().cleanDisabled(false).dataSource(dataSource).load();
    flyway.clean();
    flyway.migrate();
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("15");
    jdbc = new JdbcTemplate(dataSource);
    repository =
        new JdbcNqFeedbackEventRepository(
            jdbc, new ObjectMapper(), new DataSourceTransactionManager(dataSource));
  }

  @Test
  void routerAndAppendFailuresRollbackBothRowsThenSameKeyRetrySucceeds() {
    final IngestionCommand routerKey = command("evt-router-pg");
    final NqFeedbackIngestionService routerFailure =
        service(
            repository,
            repository,
            (envelope, tenantId) -> {
              throw new IllegalStateException("controlled router failure");
            });
    assertThatThrownBy(() -> routerFailure.ingest(routerKey))
        .isInstanceOf(IllegalStateException.class);
    assertCounts(routerKey.getEventId(), 0, 0);

    final IngestionCommand appendKey = command("evt-append-pg");
    final NqFeedbackEventRepository failingAppend = new FailingAppendRepository(repository);
    assertThatThrownBy(
            () -> service(failingAppend, repository, appendRouter(failingAppend)).ingest(appendKey))
        .isInstanceOf(DataAccessResourceFailureException.class);
    assertCounts(appendKey.getEventId(), 0, 0);

    assertThat(service(repository, repository, appendRouter(repository)).ingest(appendKey).getOutcome())
        .isEqualTo(IngestionOutcome.ACCEPTED);
    assertCounts(appendKey.getEventId(), 1, 1);
  }

  @Test
  void successfulCommitThenResponseLossRetryIsDuplicateWithoutSecondAppend() {
    final IngestionCommand command = command("evt-response-loss-pg");
    final NqFeedbackIngestionService service =
        service(repository, repository, appendRouter(repository));

    assertThat(service.ingest(command).getOutcome()).isEqualTo(IngestionOutcome.ACCEPTED);
    assertThat(service.ingest(command).getOutcome()).isEqualTo(IngestionOutcome.DUPLICATE);
    assertCounts(command.getEventId(), 1, 1);
  }

  @Test
  void strictSingleRootPrecedesJdbcDuplicateResolutionAndRejectedInputsWriteNothing() {
    final NqFeedbackIngestionService service =
        service(repository, repository, appendRouter(repository));
    final IngestionCommand existing = command("evt-strict-root-pg");
    assertThat(service.ingest(existing).getOutcome()).isEqualTo(IngestionOutcome.ACCEPTED);

    final IngestionCommand reordered =
        withPayload(
            existing, "{\"paperRunId\":\"pr-1\",\"candidateId\":\"candidate-1\"}");
    assertThat(service.ingest(reordered).getOutcome()).isEqualTo(IngestionOutcome.DUPLICATE);
    assertThat(
            service
                .ingest(withPayload(existing, existing.getPayloadJson() + " \r\n\t"))
                .getOutcome())
        .isEqualTo(IngestionOutcome.DUPLICATE);

    for (String trailingValue : List.of("{}", "42")) {
      final var rejected =
          service.ingest(
              withPayload(existing, existing.getPayloadJson() + " " + trailingValue));
      assertThat(rejected.getOutcome()).isEqualTo(IngestionOutcome.REJECTED);
      assertThat(rejected.getErrorCode()).isEqualTo(IngestionErrorCode.INVALID_SCHEMA);
    }
    assertCounts(existing.getEventId(), 1, 1);

    final IngestionCommand newTrailingRoot =
        withPayload(command("evt-new-trailing-root-pg"), "{\"candidateId\":\"candidate-1\"} {}");
    final IngestionCommand newTrailingScalar =
        withPayload(
            command("evt-new-trailing-scalar-pg"),
            "{\"candidateId\":\"candidate-1\"} true");
    assertThat(service.ingest(newTrailingRoot).getErrorCode())
        .isEqualTo(IngestionErrorCode.INVALID_SCHEMA);
    assertThat(service.ingest(newTrailingScalar).getErrorCode())
        .isEqualTo(IngestionErrorCode.INVALID_SCHEMA);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_nq_feedback_events where event_id in (?,?)",
                Integer.class,
                newTrailingRoot.getEventId(),
                newTrailingScalar.getEventId()))
        .isZero();
    assertCorrelatedCount(newTrailingRoot.getEventId(), 0);
    assertCorrelatedCount(newTrailingScalar.getEventId(), 0);
    assertThat(
            jdbc.queryForObject("select count(*) from dh_nq_feedback_events", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void incompleteEnvelopeIsNotReportedAsCommittedDuplicate() {
    final IngestionCommand command = command("evt-incomplete-pg");
    repository.required(
        () -> {
          assertThat(repository.saveEnvelope(envelope(command))).isTrue();
          return null;
        });

    assertThatThrownBy(
            () ->
                service(repository, repository, appendRouter(repository)).ingest(command))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("orphan envelope");
    assertCounts(command.getEventId(), 1, 0);
  }

  @Test
  void orphanEnvelopeIsNotCompletedByUnrelatedSameContentEventAndConflictIsNotDuplicate() {
    final IngestionCommand unrelated = command("evt-unrelated-pg");
    final IngestionCommand orphan = command("evt-orphan-pg");
    assertThat(service(repository, repository, appendRouter(repository)).ingest(unrelated).getOutcome())
        .isEqualTo(IngestionOutcome.ACCEPTED);
    repository.required(
        () -> {
          assertThat(repository.saveEnvelope(envelope(orphan))).isTrue();
          return null;
        });

    assertThatThrownBy(
            () -> service(repository, repository, appendRouter(repository)).ingest(orphan))
        .isInstanceOf(NqFeedbackIngestionTransactionException.class)
        .hasMessageContaining("orphan envelope");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_nq_feedback_events where event_id=?",
                Integer.class,
                orphan.getEventId()))
        .isOne();
    assertCorrelatedCount(orphan.getEventId(), 0);

    final IngestionCommand conflict =
        withPayload(
            unrelated,
            unrelated.getPayloadJson().replace("candidate-1", "candidate-conflict"));
    assertThatThrownBy(
            () -> service(repository, repository, appendRouter(repository)).ingest(conflict))
        .isInstanceOf(NqFeedbackIngestionTransactionException.class)
        .hasMessageContaining("conflicts with canonical envelope");
    assertCounts(unrelated.getEventId(), 1, 1);
  }

  @Test
  void eventOnlyCrossTenantEventConflictAndAmbiguousCorrelationAreFailClosed() {
    final IngestionCommand eventOnly = command("evt-event-only-pg");
    insertCorrelatedEvent(eventOnly.getEventId(), TENANT, TRACE, "PAPER", eventOnly.getRawEventType());
    assertThatThrownBy(
            () -> service(repository, repository, appendRouter(repository)).ingest(eventOnly))
        .isInstanceOf(NqFeedbackIngestionTransactionException.class)
        .hasMessageContaining("orphan routed event");
    assertCounts(eventOnly.getEventId(), 0, 1);

    final IngestionCommand crossTenant = command("evt-cross-tenant-pg");
    assertThat(service(repository, repository, appendRouter(repository)).ingest(crossTenant).getOutcome())
        .isEqualTo(IngestionOutcome.ACCEPTED);
    assertThatThrownBy(
            () ->
                service(repository, repository, appendRouter(repository))
                    .ingest(withTenant(crossTenant, "tenant-other")))
        .isInstanceOf(NqFeedbackIngestionTransactionException.class)
        .hasMessageContaining("correlation conflicts");

    final IngestionCommand conflictingEvent = command("evt-event-conflict-pg");
    repository.required(
        () -> {
          assertThat(repository.saveEnvelope(envelope(conflictingEvent))).isTrue();
          return null;
        });
    insertCorrelatedEvent(
        conflictingEvent.getEventId(), TENANT, TRACE, "BACKTEST", conflictingEvent.getRawEventType());
    assertThatThrownBy(
            () ->
                service(repository, repository, appendRouter(repository)).ingest(conflictingEvent))
        .isInstanceOf(NqFeedbackIngestionTransactionException.class)
        .hasMessageContaining("correlation conflicts");

    final IngestionCommand ambiguous = command("evt-ambiguous-pg");
    repository.required(
        () -> {
          assertThat(repository.saveEnvelope(envelope(ambiguous))).isTrue();
          return null;
        });
    insertCorrelatedEvent(ambiguous.getEventId(), TENANT, TRACE, "PAPER", ambiguous.getRawEventType());
    insertCorrelatedEvent(ambiguous.getEventId(), TENANT, TRACE, "PAPER", ambiguous.getRawEventType());
    assertThatThrownBy(
            () -> service(repository, repository, appendRouter(repository)).ingest(ambiguous))
        .isInstanceOf(NqFeedbackIngestionTransactionException.class)
        .hasMessageContaining("ambiguous");
  }

  @Test
  void concurrentSameKeyProducesOneCompleteWinnerAndNoPartialRows() throws Exception {
    final int workers = 8;
    final IngestionCommand command = command("evt-concurrent-pg");
    final NqFeedbackIngestionService service =
        service(repository, repository, appendRouter(repository));
    final ExecutorService executor = Executors.newFixedThreadPool(workers);
    final CountDownLatch ready = new CountDownLatch(workers);
    final CountDownLatch start = new CountDownLatch(1);
    final List<Future<IngestionOutcome>> futures = new ArrayList<>();
    try {
      for (int index = 0; index < workers; index++) {
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("start barrier timed out");
                  }
                  return service.ingest(command).getOutcome();
                }));
      }
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      final List<IngestionOutcome> outcomes = new ArrayList<>();
      for (Future<IngestionOutcome> future : futures) {
        outcomes.add(future.get(20, TimeUnit.SECONDS));
      }
      assertThat(outcomes.stream().filter(IngestionOutcome.ACCEPTED::equals).count()).isOne();
      assertThat(outcomes.stream().filter(IngestionOutcome.DUPLICATE::equals).count())
          .isEqualTo(workers - 1L);
      assertCounts(command.getEventId(), 1, 1);
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  void concurrentConflictingSameEventIdHasOneCompleteWinnerAndConflictsAreNotDuplicates()
      throws Exception {
    final int workers = 8;
    final IngestionCommand first = command("evt-concurrent-conflict-pg");
    final IngestionCommand second =
        withPayload(
            first, first.getPayloadJson().replace("candidate-1", "candidate-conflict"));
    final NqFeedbackIngestionService service =
        service(repository, repository, appendRouter(repository));
    final ExecutorService executor = Executors.newFixedThreadPool(workers);
    final CountDownLatch ready = new CountDownLatch(workers);
    final CountDownLatch start = new CountDownLatch(1);
    final List<Future<String>> futures = new ArrayList<>();
    try {
      for (int index = 0; index < workers; index++) {
        final IngestionCommand candidate = index % 2 == 0 ? first : second;
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("start barrier timed out");
                  }
                  try {
                    return service.ingest(candidate).getOutcome().name();
                  } catch (NqFeedbackIngestionTransactionException conflict) {
                    return "CONFLICT";
                  }
                }));
      }
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      final List<String> outcomes = new ArrayList<>();
      for (Future<String> future : futures) {
        outcomes.add(future.get(20, TimeUnit.SECONDS));
      }
      assertThat(outcomes.stream().filter("ACCEPTED"::equals).count()).isOne();
      assertThat(outcomes.stream().filter("DUPLICATE"::equals).count()).isEqualTo(3L);
      assertThat(outcomes.stream().filter("CONFLICT"::equals).count()).isEqualTo(4L);
      assertCounts(first.getEventId(), 1, 1);
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  void transactionManagerDataSourceIdentityGuardRejectsDifferentPool() {
    final DataSource other =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());

    assertThatThrownBy(
            () ->
                new JdbcNqFeedbackEventRepository(
                    jdbc, new ObjectMapper(), new DataSourceTransactionManager(other)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not match");
  }

  private NqFeedbackIngestionService service(
      final NqFeedbackEventRepository eventRepository,
      final JdbcNqFeedbackEventRepository unitOfWork,
      final NqFeedbackEventTypeRouter router) {
    return new DefaultNqFeedbackIngestionService(
        command -> ValidationResult.ok(envelope(command)),
        eventRepository,
        router,
        unitOfWork);
  }

  private static IngestionCommand command(final String eventId) {
    return IngestionCommand.of(
        TENANT,
        eventId,
        NqFeedbackEventType.PAPER_RUN_CREATED.name(),
        NOW,
        NqFeedbackEnvelope.SOURCE_SYSTEM_NEXUS_QUANT,
        "job-" + eventId,
        TRACE,
        "request-" + eventId,
        "correlation-" + eventId,
        NqFeedbackEnvelope.DEFAULT_SCHEMA_VERSION,
        "{\"candidateId\":\"candidate-1\",\"paperRunId\":\"pr-1\"}",
        NOW.plusSeconds(1));
  }

  private static IngestionCommand withPayload(
      final IngestionCommand command, final String payloadJson) {
    return IngestionCommand.of(
        command.getTenantId(),
        command.getEventId(),
        command.getRawEventType(),
        command.getOccurredAt(),
        command.getSourceSystem(),
        command.getSourceJobId(),
        command.getTraceId(),
        command.getRequestId(),
        command.getCorrelationId(),
        command.getSchemaVersion(),
        payloadJson,
        command.getReceivedAt());
  }

  private static IngestionCommand withTenant(
      final IngestionCommand command, final String tenantId) {
    return IngestionCommand.of(
        tenantId,
        command.getEventId(),
        command.getRawEventType(),
        command.getOccurredAt(),
        command.getSourceSystem(),
        command.getSourceJobId(),
        command.getTraceId(),
        command.getRequestId(),
        command.getCorrelationId(),
        command.getSchemaVersion(),
        command.getPayloadJson(),
        command.getReceivedAt());
  }

  private static NqFeedbackEnvelope envelope(final IngestionCommand command) {
    return NqFeedbackEnvelope.of(
        command.getEventId(),
        NqFeedbackEventType.PAPER_RUN_CREATED,
        command.getOccurredAt(),
        command.getSourceSystem(),
        command.getSourceJobId(),
        command.getTraceId(),
        command.getRequestId(),
        command.getCorrelationId(),
        command.getSchemaVersion(),
        command.getPayloadJson(),
        command.getReceivedAt());
  }

  private static NqFeedbackEventTypeRouter appendRouter(
      final NqFeedbackEventRepository eventRepository) {
    return (envelope, tenantId) -> eventRepository.append(event(envelope, tenantId));
  }

  private static NqFeedbackEvent event(
      final NqFeedbackEnvelope envelope, final String tenantId) {
    return NqFeedbackEvent.create(
        tenantId,
        envelope.getTraceId(),
        "candidate-1",
        envelope.getTraceId(),
        FeedbackSource.PAPER,
        envelope.getEventType().name(),
        true,
        Map.of("rawPayloadJson", envelope.getPayloadJson()),
        envelope.getOccurredAt(),
        envelope.getReceivedAt());
  }

  private void assertCounts(
      final String eventId, final int expectedEnvelopes, final int expectedEvents) {
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_nq_feedback_events where event_id=?",
                Integer.class,
                eventId))
        .isEqualTo(expectedEnvelopes);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_nq_feedback_events"
                    + " where event_id is null and tenant_id=? and run_id=?",
                Integer.class,
                TENANT,
                TRACE))
        .isEqualTo(expectedEvents);
  }

  private void insertCorrelatedEvent(
      final String envelopeEventId,
      final String tenantId,
      final String traceId,
      final String source,
      final String eventType) {
    jdbc.update(
        "insert into dh_nq_feedback_events"
            + " (id,tenant_id,run_id,candidate_id,trace_id,source,event_type,positive,status,"
            + " payload_json,occurred_at,received_at,correlation_id)"
            + " values (?,?,?,?,?,?,?,?,?,cast(? as jsonb),?,?,?)",
        java.util.UUID.randomUUID().toString(),
        tenantId,
        traceId,
        "candidate-direct",
        traceId,
        source,
        eventType,
        true,
        "RECEIVED",
        "{}",
        java.sql.Timestamp.from(NOW),
        java.sql.Timestamp.from(NOW.plusSeconds(1)),
        envelopeEventId);
  }

  private void assertCorrelatedCount(final String envelopeEventId, final int expected) {
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_nq_feedback_events"
                    + " where event_id is null and correlation_id=?",
                Integer.class,
                envelopeEventId))
        .isEqualTo(expected);
  }

  private static final class FailingAppendRepository implements NqFeedbackEventRepository {
    private final NqFeedbackEventRepository delegate;

    private FailingAppendRepository(final NqFeedbackEventRepository delegate) {
      this.delegate = delegate;
    }

    @Override
    public void append(final NqFeedbackEvent event) {
      throw new DataAccessResourceFailureException("controlled append SQL failure");
    }

    @Override
    public List<NqFeedbackEvent> listByRun(final String tenantId, final String runId) {
      return delegate.listByRun(tenantId, runId);
    }

    @Override
    public boolean saveEnvelope(final NqFeedbackEnvelope envelope) {
      return delegate.saveEnvelope(envelope);
    }

    @Override
    public Optional<NqFeedbackEnvelope> findEnvelopeByEventId(final String eventId) {
      return delegate.findEnvelopeByEventId(eventId);
    }

    @Override
    public void beginEventCorrelation(
        final NqFeedbackEnvelope envelope, final String tenantId) {
      delegate.beginEventCorrelation(envelope, tenantId);
    }

    @Override
    public void endEventCorrelation() {
      delegate.endEventCorrelation();
    }

    @Override
    public FeedbackIngestionPersistence findIngestionPersistence(
        final String eventId,
        final String tenantId,
        final String traceId,
        final NqFeedbackEventType eventType) {
      return delegate.findIngestionPersistence(eventId, tenantId, traceId, eventType);
    }
  }
}

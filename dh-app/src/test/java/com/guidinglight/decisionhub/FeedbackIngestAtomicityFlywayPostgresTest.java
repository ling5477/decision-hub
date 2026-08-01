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
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionOutcome;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackEventTypeRouter;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionService;
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
        .hasMessageContaining("incomplete or inconsistent");
    assertCounts(command.getEventId(), 1, 0);
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
  }
}

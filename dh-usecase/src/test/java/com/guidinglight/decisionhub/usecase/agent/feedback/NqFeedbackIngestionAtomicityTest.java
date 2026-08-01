package com.guidinglight.decisionhub.usecase.agent.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.feedback.FeedbackSource;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.feedback.impl.DefaultNqFeedbackContractValidator;
import com.guidinglight.decisionhub.usecase.agent.feedback.impl.DefaultNqFeedbackIngestionService;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryNqFeedbackEventRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/** Use-case owned unit-of-work 的 failure、retry、duplicate 与并发回归。 */
class NqFeedbackIngestionAtomicityTest {

  private static final String TENANT = "t-default";
  private static final String TRACE = "trace-atomic";

  @Test
  void successPersistsOneEnvelopeAndOneEvent() {
    final InMemoryNqFeedbackEventRepository repository = new InMemoryNqFeedbackEventRepository();
    final IngestionResult result = service(repository, repository, appendRouter(repository)).ingest(command("evt-ok"));

    assertEquals(IngestionOutcome.ACCEPTED, result.getOutcome());
    assertEquals(1, repository.envelopeCount());
    assertEquals(1, repository.listByRun(TENANT, TRACE).size());
  }

  @Test
  void routerFailureRollsBackEnvelopeAndAllowsCompleteRetry() {
    final InMemoryNqFeedbackEventRepository repository = new InMemoryNqFeedbackEventRepository();
    final AtomicBoolean fail = new AtomicBoolean(true);
    final NqFeedbackEventTypeRouter router =
        (envelope, tenantId) -> {
          if (fail.getAndSet(false)) {
            throw new IllegalStateException("controlled router failure");
          }
          appendEvent(repository, envelope, tenantId);
        };
    final NqFeedbackIngestionService service = service(repository, repository, router);

    assertThrows(IllegalStateException.class, () -> service.ingest(command("evt-router-fail")));
    assertEmpty(repository);

    assertEquals(
        IngestionOutcome.ACCEPTED, service.ingest(command("evt-router-fail")).getOutcome());
    assertComplete(repository);
  }

  @Test
  void appendMutationThenFailureRestoresBothStructures() {
    final InMemoryNqFeedbackEventRepository storage = new InMemoryNqFeedbackEventRepository();
    final NqFeedbackEventRepository failing = new AppendThenFailRepository(storage);
    final NqFeedbackIngestionService service = service(failing, storage, appendRouter(failing));

    assertThrows(IllegalStateException.class, () -> service.ingest(command("evt-append-fail")));
    assertEmpty(storage);
  }

  @Test
  void unsupportedOrUnknownHandlerFailureLeavesNoPartialState() {
    final InMemoryNqFeedbackEventRepository repository = new InMemoryNqFeedbackEventRepository();
    final NqFeedbackIngestionService service =
        service(
            repository,
            repository,
            (envelope, tenantId) -> {
              throw new UnsupportedOperationException("controlled unsupported event");
            });

    assertThrows(
        UnsupportedOperationException.class, () -> service.ingest(command("evt-unsupported")));
    assertEmpty(repository);
  }

  @Test
  void committedDuplicateDoesNotAppendAgainAndResponseLossRetryConverges() {
    final InMemoryNqFeedbackEventRepository repository = new InMemoryNqFeedbackEventRepository();
    final NqFeedbackIngestionService service = service(repository, repository, appendRouter(repository));

    final IngestionResult discardedResponse = service.ingest(command("evt-response-loss"));
    final IngestionResult retry = service.ingest(command("evt-response-loss"));

    assertEquals(IngestionOutcome.ACCEPTED, discardedResponse.getOutcome());
    assertEquals(IngestionOutcome.DUPLICATE, retry.getOutcome());
    assertComplete(repository);
  }

  @Test
  void incompleteEnvelopeIsFailClosedInsteadOfDuplicateSuccess() {
    final InMemoryNqFeedbackEventRepository repository = new InMemoryNqFeedbackEventRepository();
    final IngestionCommand command = command("evt-incomplete");
    final NqFeedbackEnvelope envelope = validator().validate(command).getEnvelope();
    assertTrue(repository.saveEnvelope(envelope));
    final NqFeedbackIngestionService service = service(repository, repository, appendRouter(repository));

    final NqFeedbackIngestionTransactionException failure =
        assertThrows(
            NqFeedbackIngestionTransactionException.class, () -> service.ingest(command));
    assertEquals(
        NqFeedbackIngestionTransactionException.ErrorCode.PERSISTENCE_FAILURE,
        failure.errorCode());
    assertEquals(1, repository.envelopeCount());
    assertEquals(0, repository.size());
  }

  @Test
  void concurrentSameKeyProducesExactlyOneCompleteWinnerWithoutSleep() throws Exception {
    final int workers = 8;
    final InMemoryNqFeedbackEventRepository repository = new InMemoryNqFeedbackEventRepository();
    final NqFeedbackIngestionService service = service(repository, repository, appendRouter(repository));
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
                  assertTrue(start.await(5, TimeUnit.SECONDS));
                  return service.ingest(command("evt-concurrent")).getOutcome();
                }));
      }
      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();

      final List<IngestionOutcome> outcomes = new ArrayList<>();
      for (Future<IngestionOutcome> future : futures) {
        outcomes.add(future.get(10, TimeUnit.SECONDS));
      }
      assertEquals(1L, outcomes.stream().filter(IngestionOutcome.ACCEPTED::equals).count());
      assertEquals(
          workers - 1L, outcomes.stream().filter(IngestionOutcome.DUPLICATE::equals).count());
      assertComplete(repository);
    } finally {
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  private static NqFeedbackIngestionService service(
      final NqFeedbackEventRepository repository,
      final NqFeedbackIngestionUnitOfWork unitOfWork,
      final NqFeedbackEventTypeRouter router) {
    return new DefaultNqFeedbackIngestionService(validator(), repository, router, unitOfWork);
  }

  private static NqFeedbackContractValidator validator() {
    return new DefaultNqFeedbackContractValidator(
        B2TestFixtures.repoWithRun(TRACE), new ObjectMapper());
  }

  private static IngestionCommand command(final String eventId) {
    return B2TestFixtures.legalCommand(eventId, NqFeedbackEventType.PAPER_RUN_CREATED, TRACE);
  }

  private static NqFeedbackEventTypeRouter appendRouter(
      final NqFeedbackEventRepository repository) {
    return (envelope, tenantId) -> appendEvent(repository, envelope, tenantId);
  }

  private static void appendEvent(
      final NqFeedbackEventRepository repository,
      final NqFeedbackEnvelope envelope,
      final String tenantId) {
    repository.append(
        NqFeedbackEvent.create(
            tenantId,
            envelope.getTraceId(),
            "candidate-atomic",
            envelope.getTraceId(),
            FeedbackSource.PAPER,
            envelope.getEventType().name(),
            true,
            Map.of("rawPayloadJson", envelope.getPayloadJson()),
            envelope.getOccurredAt(),
            envelope.getReceivedAt()));
  }

  private static void assertEmpty(final InMemoryNqFeedbackEventRepository repository) {
    assertEquals(0, repository.envelopeCount());
    assertEquals(0, repository.size());
  }

  private static void assertComplete(final InMemoryNqFeedbackEventRepository repository) {
    assertEquals(1, repository.envelopeCount());
    assertEquals(1, repository.listByRun(TENANT, TRACE).size());
  }

  private static final class AppendThenFailRepository implements NqFeedbackEventRepository {
    private final NqFeedbackEventRepository delegate;

    private AppendThenFailRepository(final NqFeedbackEventRepository delegate) {
      this.delegate = delegate;
    }

    @Override
    public void append(final NqFeedbackEvent event) {
      delegate.append(event);
      throw new IllegalStateException("controlled failure after append mutation");
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

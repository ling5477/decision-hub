package com.guidinglight.decisionhub.usecase.agent.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guidinglight.decisionhub.domain.feedback.FeedbackSource;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunRepository;
import com.guidinglight.decisionhub.usecase.agent.feedback.impl.DefaultNqFeedbackContractValidator;
import com.guidinglight.decisionhub.usecase.agent.feedback.impl.DefaultNqFeedbackIngestionService;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryNqFeedbackEventRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Stage2-PoC-B2：幂等测试。
 *
 * <p>覆盖 docs/current/STAGE2_POC_WORK_ORDER.md §Batch 2.5 / §Batch 2.7：
 *
 * <ol>
 *   <li>同一 eventId 重放 -> 仅入库 1 次，handler 仅调用 1 次，第二次返回 DUPLICATE。
 *   <li>不同 eventId 多次 -> 各入库 1 次，handler 每次都调用。
 * </ol>
 */
class NqFeedbackIdempotencyTest {

  private static final String TRACE = "trace-idem";

  @Test
  void replaySameEventIdSavesOnceAndDispatchesOnce() {
    final ResearchRunRepository runRepo = B2TestFixtures.repoWithRun(TRACE);
    final InMemoryNqFeedbackEventRepository feedbackRepo = new InMemoryNqFeedbackEventRepository();
    final AtomicInteger handlerCalls = new AtomicInteger(0);

    final Map<NqFeedbackEventType, NqFeedbackEventHandler> handlerMap = new HashMap<>();
    for (NqFeedbackEventType t : NqFeedbackEventType.values()) {
      handlerMap.put(t, new CountingHandler(t, handlerCalls, feedbackRepo));
    }
    final NqFeedbackEventTypeRouter router =
        (envelope, tenantId) -> handlerMap.get(envelope.getEventType()).handle(envelope, tenantId);

    final NqFeedbackContractValidator validator =
        new DefaultNqFeedbackContractValidator(runRepo, new ObjectMapper());
    final NqFeedbackIngestionService service =
        new DefaultNqFeedbackIngestionService(validator, feedbackRepo, router, feedbackRepo);

    final IngestionCommand cmd =
        B2TestFixtures.legalCommand(
            "evt-fixed", NqFeedbackEventType.PAPER_RUN_CREATED, TRACE);

    final IngestionResult first = service.ingest(cmd);
    final IngestionResult second = service.ingest(cmd);
    final IngestionResult third = service.ingest(cmd);

    assertEquals(IngestionOutcome.ACCEPTED, first.getOutcome());
    assertEquals(IngestionOutcome.DUPLICATE, second.getOutcome());
    assertEquals(IngestionOutcome.DUPLICATE, third.getOutcome());
    assertEquals("RECEIVED", first.getStatus());
    assertEquals("RECEIVED", second.getStatus(), "duplicate must echo 原 status");
    assertEquals("RECEIVED", third.getStatus());
    assertEquals(1, handlerCalls.get(), "handler must be invoked exactly once for replays");
    assertTrue(feedbackRepo.findEnvelopeByEventId("evt-fixed").isPresent());
  }

  @Test
  void differentEventIdsDispatchSeparately() {
    final ResearchRunRepository runRepo = B2TestFixtures.repoWithRun(TRACE);
    final InMemoryNqFeedbackEventRepository feedbackRepo = new InMemoryNqFeedbackEventRepository();
    final AtomicInteger handlerCalls = new AtomicInteger(0);

    final NqFeedbackEventTypeRouter router =
        (envelope, tenantId) -> {
          handlerCalls.incrementAndGet();
          appendEvent(feedbackRepo, envelope, tenantId);
        };
    final NqFeedbackContractValidator validator =
        new DefaultNqFeedbackContractValidator(runRepo, new ObjectMapper());
    final NqFeedbackIngestionService service =
        new DefaultNqFeedbackIngestionService(validator, feedbackRepo, router, feedbackRepo);

    service.ingest(B2TestFixtures.legalCommand("e-a", NqFeedbackEventType.PAPER_RUN_CREATED, TRACE));
    service.ingest(B2TestFixtures.legalCommand("e-b", NqFeedbackEventType.PAPER_RUN_STARTED, TRACE));
    service.ingest(B2TestFixtures.legalCommand("e-c", NqFeedbackEventType.PAPER_RUN_STOPPED, TRACE));

    assertEquals(3, handlerCalls.get());
    assertTrue(feedbackRepo.findEnvelopeByEventId("e-a").isPresent());
    assertTrue(feedbackRepo.findEnvelopeByEventId("e-b").isPresent());
    assertTrue(feedbackRepo.findEnvelopeByEventId("e-c").isPresent());
  }

  @Test
  void rejectedDoesNotPersistEnvelope() {
    final ResearchRunRepository runRepo = B2TestFixtures.repoWithRun(TRACE);
    final InMemoryNqFeedbackEventRepository feedbackRepo = new InMemoryNqFeedbackEventRepository();
    final AtomicInteger handlerCalls = new AtomicInteger(0);
    final NqFeedbackEventTypeRouter router = (envelope, tenantId) -> handlerCalls.incrementAndGet();
    final NqFeedbackContractValidator validator =
        new DefaultNqFeedbackContractValidator(runRepo, new ObjectMapper());
    final NqFeedbackIngestionService service =
        new DefaultNqFeedbackIngestionService(validator, feedbackRepo, router, feedbackRepo);

    final IngestionCommand bad =
        B2TestFixtures.commandWith(
            B2TestFixtures.legalCommand("e-bad", NqFeedbackEventType.PAPER_RUN_STARTED, TRACE),
            "schemaVersion",
            "0.1.0");
    final IngestionResult r = service.ingest(bad);
    assertEquals(IngestionOutcome.REJECTED, r.getOutcome());
    assertEquals(IngestionErrorCode.INVALID_SCHEMA, r.getErrorCode());
    assertEquals(0, handlerCalls.get());
    assertTrue(feedbackRepo.findEnvelopeByEventId("e-bad").isEmpty());
  }

  @Test
  void forbiddenPayloadDoesNotPersistEnvelopeOrDispatchHandler() {
    final ResearchRunRepository runRepo = B2TestFixtures.repoWithRun(TRACE);
    final InMemoryNqFeedbackEventRepository feedbackRepo = new InMemoryNqFeedbackEventRepository();
    final AtomicInteger handlerCalls = new AtomicInteger(0);
    final NqFeedbackEventTypeRouter router = (envelope, tenantId) -> handlerCalls.incrementAndGet();
    final NqFeedbackContractValidator validator =
        new DefaultNqFeedbackContractValidator(runRepo, new ObjectMapper());
    final NqFeedbackIngestionService service =
        new DefaultNqFeedbackIngestionService(validator, feedbackRepo, router, feedbackRepo);

    final IngestionCommand forbidden =
        B2TestFixtures.commandWith(
            B2TestFixtures.legalCommand(
                "e-forbidden-store", NqFeedbackEventType.PAPER_RUN_CREATED, TRACE),
            "payloadJson",
            "{\"paperRunId\":\"pr-1\",\"candidateId\":\"c-1\",\"strategyName\":\"S1\","
                + "\"requestedBy\":\"alice\",\"createdAt\":\"2026-05-25T08:00:00Z\","
                + "\"rawPayloadJson\":\"{\\\"apiSecret\\\":\\\"redacted\\\"}\"}");

    final IngestionResult r = service.ingest(forbidden);

    assertEquals(IngestionOutcome.REJECTED, r.getOutcome());
    assertEquals(IngestionErrorCode.FORBIDDEN_FIELD, r.getErrorCode());
    assertEquals(0, handlerCalls.get(), "forbidden payload must not dispatch handler");
    assertTrue(feedbackRepo.findEnvelopeByEventId("e-forbidden-store").isEmpty());
  }

  @Test
  void validationPrecedesDuplicateForMissingFieldAndUnknownEventType() {
    final InMemoryNqFeedbackEventRepository repository = new InMemoryNqFeedbackEventRepository();
    final NqFeedbackIngestionService service = service(repository);
    final IngestionCommand original =
        B2TestFixtures.legalCommand(
            "evt-validation-precedence", NqFeedbackEventType.PAPER_RUN_CREATED, TRACE);
    assertEquals(IngestionOutcome.ACCEPTED, service.ingest(original).getOutcome());

    final IngestionCommand missingRequired =
        B2TestFixtures.commandWith(original, "payloadJson", "{\"rawPayloadJson\":\"{}\"}");
    final IngestionCommand unknownType =
        B2TestFixtures.commandWith(original, "rawEventType", "NOT_A_FEEDBACK_EVENT");

    assertEquals(IngestionErrorCode.INVALID_SCHEMA, service.ingest(missingRequired).getErrorCode());
    assertEquals(IngestionErrorCode.UNKNOWN_EVENT_TYPE, service.ingest(unknownType).getErrorCode());
    assertEquals(1, repository.envelopeCount());
    assertEquals(1, repository.size());
  }

  @Test
  void canonicalObjectKeyReorderingIsDuplicateButRealPayloadDifferencesConflict()
      throws Exception {
    final InMemoryNqFeedbackEventRepository repository = new InMemoryNqFeedbackEventRepository();
    final NqFeedbackIngestionService service = service(repository);
    final IngestionCommand base =
        B2TestFixtures.legalCommand(
            "evt-canonical-conflict", NqFeedbackEventType.PAPER_RUN_CREATED, TRACE);
    final IngestionCommand withArray =
        B2TestFixtures.commandWith(base, "payloadJson", payloadWithSequence(base, 1, 2));
    assertEquals(IngestionOutcome.ACCEPTED, service.ingest(withArray).getOutcome());

    final IngestionCommand reordered =
        B2TestFixtures.commandWith(
            withArray, "payloadJson", reverseObjectKeys(withArray.getPayloadJson()));
    assertEquals(IngestionOutcome.DUPLICATE, service.ingest(reordered).getOutcome());

    final IngestionCommand arrayOrderConflict =
        B2TestFixtures.commandWith(withArray, "payloadJson", payloadWithSequence(base, 2, 1));
    final IngestionCommand valueConflict =
        B2TestFixtures.commandWith(
            withArray,
            "payloadJson",
            withArray.getPayloadJson().replace("\"strategyName\":\"S1\"", "\"strategyName\":\"S2\""));
    assertThrows(NqFeedbackIngestionTransactionException.class, () -> service.ingest(arrayOrderConflict));
    assertThrows(NqFeedbackIngestionTransactionException.class, () -> service.ingest(valueConflict));
    assertEquals(1, repository.envelopeCount());
    assertEquals(1, repository.size());
  }

  @Test
  void duplicateObjectKeysFailClosedInsteadOfCollapsingToDuplicate() {
    final InMemoryNqFeedbackEventRepository repository = new InMemoryNqFeedbackEventRepository();
    final NqFeedbackIngestionService service = service(repository);
    final IngestionCommand base =
        B2TestFixtures.legalCommand(
            "evt-duplicate-object-key", NqFeedbackEventType.PAPER_RUN_CREATED, TRACE);
    final IngestionCommand repeatedKey =
        B2TestFixtures.commandWith(
            base,
            "payloadJson",
            base.getPayloadJson()
                .replace(
                    "\"candidateId\":\"cand-1\"",
                    "\"candidateId\":\"first\",\"candidateId\":\"cand-1\""));

    final IngestionResult rejected = service.ingest(repeatedKey);
    assertEquals(IngestionOutcome.REJECTED, rejected.getOutcome());
    assertEquals(IngestionErrorCode.INVALID_SCHEMA, rejected.getErrorCode());
    assertEquals(0, repository.envelopeCount());
    assertEquals(0, repository.size());
    assertEquals(IngestionOutcome.ACCEPTED, service.ingest(base).getOutcome());
    assertEquals(1, repository.envelopeCount());
    assertEquals(1, repository.size());
  }

  @Test
  void repositoryWriteFailurePropagatesAndDoesNotDispatchHandler() {
    final ResearchRunRepository runRepo = B2TestFixtures.repoWithRun(TRACE);
    final AtomicInteger handlerCalls = new AtomicInteger();
    final FailingSaveRepository repository = new FailingSaveRepository();
    final NqFeedbackIngestionService service =
        new DefaultNqFeedbackIngestionService(
            new DefaultNqFeedbackContractValidator(runRepo, new ObjectMapper()),
            repository,
            (envelope, tenantId) -> handlerCalls.incrementAndGet(),
            repository);

    assertThrows(
        IllegalStateException.class,
        () ->
            service.ingest(
                B2TestFixtures.legalCommand(
                    "e-save-failure", NqFeedbackEventType.PAPER_RUN_CREATED, TRACE)));
    assertEquals(0, handlerCalls.get(), "failed persistence must not dispatch a handler");
  }

  @Test
  void unknownHandlerFailurePropagatesInsteadOfReturningAccepted() {
    final ResearchRunRepository runRepo = B2TestFixtures.repoWithRun(TRACE);
    final InMemoryNqFeedbackEventRepository feedbackRepo = new InMemoryNqFeedbackEventRepository();
    final NqFeedbackIngestionService service =
        new DefaultNqFeedbackIngestionService(
            new DefaultNqFeedbackContractValidator(runRepo, new ObjectMapper()),
            feedbackRepo,
            (envelope, tenantId) -> {
              throw new IllegalStateException("unexpected handler failure");
            },
            feedbackRepo);

    assertThrows(
        IllegalStateException.class,
        () ->
            service.ingest(
                B2TestFixtures.legalCommand(
                    "e-handler-failure", NqFeedbackEventType.PAPER_RUN_CREATED, TRACE)));
  }

  /** 计数 handler，断言重放时不重复调用。 */
  private static final class CountingHandler implements NqFeedbackEventHandler {
    private final NqFeedbackEventType type;
    private final AtomicInteger counter;
    private final NqFeedbackEventRepository repository;

    CountingHandler(
        final NqFeedbackEventType type,
        final AtomicInteger counter,
        final NqFeedbackEventRepository repository) {
      this.type = type;
      this.counter = counter;
      this.repository = repository;
    }

    @Override
    public NqFeedbackEventType supportedType() {
      return type;
    }

    @Override
    public void handle(final NqFeedbackEnvelope envelope, final String tenantId) {
      assertSame(type, envelope.getEventType());
      counter.incrementAndGet();
      appendEvent(repository, envelope, tenantId);
    }
  }

  private static void appendEvent(
      final NqFeedbackEventRepository repository,
      final NqFeedbackEnvelope envelope,
      final String tenantId) {
    repository.append(
        NqFeedbackEvent.create(
            tenantId,
            envelope.getTraceId(),
            "candidate-1",
            envelope.getTraceId(),
            FeedbackSource.PAPER,
            envelope.getEventType().name(),
            true,
            Map.of("rawPayloadJson", envelope.getPayloadJson()),
            envelope.getOccurredAt(),
            envelope.getReceivedAt()));
  }

  private static NqFeedbackIngestionService service(
      final InMemoryNqFeedbackEventRepository repository) {
    return new DefaultNqFeedbackIngestionService(
        new DefaultNqFeedbackContractValidator(B2TestFixtures.repoWithRun(TRACE), new ObjectMapper()),
        repository,
        (envelope, tenantId) -> appendEvent(repository, envelope, tenantId),
        repository);
  }

  private static String payloadWithSequence(
      final IngestionCommand command, final int first, final int second) throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final ObjectNode payload = (ObjectNode) mapper.readTree(command.getPayloadJson());
    final ArrayNode sequence = payload.putArray("sequence");
    sequence.add(first);
    sequence.add(second);
    return mapper.writeValueAsString(payload);
  }

  private static String reverseObjectKeys(final String payloadJson) throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final ObjectNode source = (ObjectNode) mapper.readTree(payloadJson);
    final java.util.List<String> names = new java.util.ArrayList<>();
    source.fieldNames().forEachRemaining(names::add);
    java.util.Collections.reverse(names);
    final ObjectNode reversed = mapper.createObjectNode();
    names.forEach(name -> reversed.set(name, source.get(name)));
    return mapper.writeValueAsString(reversed);
  }

  private static final class FailingSaveRepository
      implements NqFeedbackEventRepository, NqFeedbackIngestionUnitOfWork {
    @Override
    public void append(final NqFeedbackEvent event) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<NqFeedbackEvent> listByRun(final String tenantId, final String runId) {
      return List.of();
    }

    @Override
    public boolean saveEnvelope(final NqFeedbackEnvelope envelope) {
      throw new IllegalStateException("save failed");
    }

    @Override
    public Optional<NqFeedbackEnvelope> findEnvelopeByEventId(final String eventId) {
      return Optional.empty();
    }

    @Override
    public FeedbackIngestionPersistence findIngestionPersistence(
        final String eventId,
        final String tenantId,
        final String traceId,
        final NqFeedbackEventType eventType) {
      return new FeedbackIngestionPersistence(
          FeedbackIngestionPersistenceState.ABSENT, Optional.empty());
    }

    @Override
    public <T> T required(final java.util.function.Supplier<T> action) {
      return action.get();
    }
  }
}

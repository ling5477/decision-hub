package com.guidinglight.decisionhub.usecase.agent.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunRepository;
import com.guidinglight.decisionhub.usecase.agent.feedback.impl.DefaultNqFeedbackContractValidator;
import com.guidinglight.decisionhub.usecase.agent.feedback.impl.DefaultNqFeedbackIngestionService;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryNqFeedbackEventRepository;
import java.util.HashMap;
import java.util.Map;
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
    final NqFeedbackEventRepository feedbackRepo = new InMemoryNqFeedbackEventRepository();
    final AtomicInteger handlerCalls = new AtomicInteger(0);

    final Map<NqFeedbackEventType, NqFeedbackEventHandler> handlerMap = new HashMap<>();
    for (NqFeedbackEventType t : NqFeedbackEventType.values()) {
      handlerMap.put(t, new CountingHandler(t, handlerCalls));
    }
    final NqFeedbackEventTypeRouter router =
        envelope -> handlerMap.get(envelope.getEventType()).handle(envelope);

    final NqFeedbackContractValidator validator =
        new DefaultNqFeedbackContractValidator(runRepo, new ObjectMapper());
    final NqFeedbackIngestionService service =
        new DefaultNqFeedbackIngestionService(validator, feedbackRepo, router);

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
    final NqFeedbackEventRepository feedbackRepo = new InMemoryNqFeedbackEventRepository();
    final AtomicInteger handlerCalls = new AtomicInteger(0);

    final NqFeedbackEventTypeRouter router =
        envelope -> {
          handlerCalls.incrementAndGet();
        };
    final NqFeedbackContractValidator validator =
        new DefaultNqFeedbackContractValidator(runRepo, new ObjectMapper());
    final NqFeedbackIngestionService service =
        new DefaultNqFeedbackIngestionService(validator, feedbackRepo, router);

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
    final NqFeedbackEventRepository feedbackRepo = new InMemoryNqFeedbackEventRepository();
    final AtomicInteger handlerCalls = new AtomicInteger(0);
    final NqFeedbackEventTypeRouter router = envelope -> handlerCalls.incrementAndGet();
    final NqFeedbackContractValidator validator =
        new DefaultNqFeedbackContractValidator(runRepo, new ObjectMapper());
    final NqFeedbackIngestionService service =
        new DefaultNqFeedbackIngestionService(validator, feedbackRepo, router);

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

  /** 计数 handler，断言重放时不重复调用。 */
  private static final class CountingHandler implements NqFeedbackEventHandler {
    private final NqFeedbackEventType type;
    private final AtomicInteger counter;

    CountingHandler(final NqFeedbackEventType type, final AtomicInteger counter) {
      this.type = type;
      this.counter = counter;
    }

    @Override
    public NqFeedbackEventType supportedType() {
      return type;
    }

    @Override
    public void handle(final NqFeedbackEnvelope envelope) {
      assertSame(type, envelope.getEventType());
      counter.incrementAndGet();
    }
  }
}

package com.guidinglight.decisionhub.usecase.agent.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.feedback.handler.AbstractNqFeedbackEventHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.handler.BacktestResultReadyHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.handler.PaperRunAlertRaisedHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.handler.PaperRunCreatedHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.handler.PaperRunDailyReportGeneratedHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.handler.PaperRunRecoveryEventRecordedHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.handler.PaperRunStabilityCheckCompletedHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.handler.PaperRunStartedHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.handler.PaperRunStoppedHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.impl.DefaultNqFeedbackEventTypeRouter;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryNqFeedbackEventRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Stage2-PoC-B2：handler 派发测试。
 *
 * <p>覆盖：
 *
 * <ol>
 *   <li>Router 必须覆盖 8 个 eventType，缺一即抛 {@link IllegalStateException}。
 *   <li>每个 eventType 命中各自 handler 一次（不串、不漏）。
 *   <li>8 个 handler 都只追加 Stage1 feedback event，不具备 mutable learning 依赖。
 * </ol>
 */
class NqFeedbackHandlerDispatchTest {

  @Test
  void routerMustCoverAllEightEventTypes_orThrow() {
    assertThrows(
        IllegalStateException.class,
        () -> new DefaultNqFeedbackEventTypeRouter(List.of()),
        "empty handler list must throw");
  }

  @Test
  void routerDispatchesEachEventTypeToItsOwnHandler() {
    final NqFeedbackEventRepository repo = new InMemoryNqFeedbackEventRepository();
    final ObjectMapper om = new ObjectMapper();
    final List<NqFeedbackEventHandler> handlers = handlers(repo, om);

    final NqFeedbackEventTypeRouter router = new DefaultNqFeedbackEventTypeRouter(handlers);

    for (NqFeedbackEventType t : NqFeedbackEventType.values()) {
      final NqFeedbackEnvelope envelope = envelopeOf(t);
      router.route(envelope, "tenant-dispatch");
      final List<NqFeedbackEvent> appended =
          repo.listByRun("tenant-dispatch", envelope.getTraceId());
      assertEquals(1, appended.size(), t + " must append exactly one feedback event");
      assertEquals(t.name(), appended.get(0).getEventType());
    }
  }

  @Test
  void duplicateHandlerRegistrationThrows() {
    final NqFeedbackEventRepository repo = new InMemoryNqFeedbackEventRepository();
    final ObjectMapper om = new ObjectMapper();
    final List<NqFeedbackEventHandler> handlers =
        List.of(new PaperRunCreatedHandler(repo, om), new PaperRunCreatedHandler(repo, om));
    assertThrows(
        IllegalStateException.class, () -> new DefaultNqFeedbackEventTypeRouter(handlers));
  }

  @Test
  void abstractHandlerAppendsStage1EventAndPreservesRawPayload() {
    final NqFeedbackEventRepository repo = new InMemoryNqFeedbackEventRepository();
    final ObjectMapper om = new ObjectMapper();
    final AbstractNqFeedbackEventHandler handler = new PaperRunCreatedHandler(repo, om);
    final NqFeedbackEnvelope envelope = envelopeOf(NqFeedbackEventType.PAPER_RUN_CREATED);

    handler.handle(envelope, "tenant-handler");

    assertTrue(
        repo.listByRun("tenant-handler", envelope.getTraceId()).size() > 0,
        "handler must append Stage1 NqFeedbackEvent");
    // raw payload 必须被保留进入 Stage1 事件的 payloadJson
    final Map<String, Object> stage1Payload =
        repo.listByRun("tenant-handler", envelope.getTraceId()).get(0).getPayloadJson();
    assertTrue(
        stage1Payload.containsKey("rawPayloadJson"),
        "Stage1 payload map must contain rawPayloadJson");
  }

  @Test
  void allEightEventTypesCoveredByDistinctHandlerClasses() {
    final NqFeedbackEventRepository repo = new InMemoryNqFeedbackEventRepository();
    final ObjectMapper om = new ObjectMapper();
    final List<NqFeedbackEventHandler> handlers = handlers(repo, om);
    final EnumSet<NqFeedbackEventType> covered = EnumSet.noneOf(NqFeedbackEventType.class);
    for (NqFeedbackEventHandler h : handlers) {
      covered.add(h.supportedType());
    }
    assertEquals(EnumSet.allOf(NqFeedbackEventType.class), covered);
  }

  @Test
  void appendFailurePropagatesAndDoesNotReturnFalseSuccess() {
    final AbstractNqFeedbackEventHandler handler =
        new PaperRunCreatedHandler(new FailingAppendRepository(), new ObjectMapper());

    assertThrows(
        IllegalStateException.class,
        () -> handler.handle(envelopeOf(NqFeedbackEventType.PAPER_RUN_CREATED), "tenant-fail"));
  }

  private static List<NqFeedbackEventHandler> handlers(
      final NqFeedbackEventRepository repo, final ObjectMapper objectMapper) {
    return List.of(
        new PaperRunCreatedHandler(repo, objectMapper),
        new PaperRunStartedHandler(repo, objectMapper),
        new PaperRunStoppedHandler(repo, objectMapper),
        new PaperRunDailyReportGeneratedHandler(repo, objectMapper),
        new PaperRunAlertRaisedHandler(repo, objectMapper),
        new PaperRunRecoveryEventRecordedHandler(repo, objectMapper),
        new PaperRunStabilityCheckCompletedHandler(repo, objectMapper),
        new BacktestResultReadyHandler(repo, objectMapper));
  }

  private static NqFeedbackEnvelope envelopeOf(final NqFeedbackEventType type) {
    return NqFeedbackEnvelope.of(
        "evt-" + type,
        type,
        Instant.parse("2026-05-25T08:00:00Z"),
        NqFeedbackEnvelope.SOURCE_SYSTEM_NEXUS_QUANT,
        "src-job-" + type,
        "trace-" + type,
        "req-" + type,
        "corr-" + type,
        "1.0.0",
        B2TestFixtures.defaultPayloadFor(type),
        TimeProvider.now());
  }

  private static final class FailingAppendRepository implements NqFeedbackEventRepository {
    @Override
    public void append(final NqFeedbackEvent event) {
      throw new IllegalStateException("append failed");
    }

    @Override
    public List<NqFeedbackEvent> listByRun(final String tenantId, final String runId) {
      return List.of();
    }

    @Override
    public boolean saveEnvelope(final NqFeedbackEnvelope envelope) {
      return false;
    }

    @Override
    public Optional<NqFeedbackEnvelope> findEnvelopeByEventId(final String eventId) {
      return Optional.empty();
    }
  }
}

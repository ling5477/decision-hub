package com.guidinglight.decisionhub.usecase.agent.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.ExperienceFeedbackService;
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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Stage2-PoC-B2：handler 派发测试。
 *
 * <p>覆盖：
 *
 * <ol>
 *   <li>Router 必须覆盖 8 个 eventType，缺一即抛 {@link IllegalStateException}。
 *   <li>每个 eventType 命中各自 handler 一次（不串、不漏）。
 *   <li>handler 会触发 {@link ExperienceFeedbackService#apply} 与 Stage1 仓库 append。
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
    final CountingFeedbackService experience = new CountingFeedbackService();

    final List<NqFeedbackEventHandler> handlers =
        List.of(
            new PaperRunCreatedHandler(experience, repo, om),
            new PaperRunStartedHandler(experience, repo, om),
            new PaperRunStoppedHandler(experience, repo, om),
            new PaperRunDailyReportGeneratedHandler(experience, repo, om),
            new PaperRunAlertRaisedHandler(experience, repo, om),
            new PaperRunRecoveryEventRecordedHandler(experience, repo, om),
            new PaperRunStabilityCheckCompletedHandler(experience, repo, om),
            new BacktestResultReadyHandler(experience, repo, om));

    final NqFeedbackEventTypeRouter router = new DefaultNqFeedbackEventTypeRouter(handlers);

    // 期望：依次派发 8 种 eventType，experience.apply 被调用 8 次，对应 eventType 各 1 次
    final Map<NqFeedbackEventType, Integer> expectedCount = new EnumMap<>(NqFeedbackEventType.class);
    for (NqFeedbackEventType t : NqFeedbackEventType.values()) {
      router.route(envelopeOf(t), "tenant-dispatch");
      expectedCount.merge(t, 1, Integer::sum);
    }

    assertEquals(8, experience.totalCalls);
    assertEquals(expectedCount, experience.callsByEventTypeName(), "每个 eventType 命中各自 handler 一次");
  }

  @Test
  void duplicateHandlerRegistrationThrows() {
    final NqFeedbackEventRepository repo = new InMemoryNqFeedbackEventRepository();
    final ObjectMapper om = new ObjectMapper();
    final CountingFeedbackService experience = new CountingFeedbackService();
    final List<NqFeedbackEventHandler> handlers =
        List.of(
            new PaperRunCreatedHandler(experience, repo, om),
            new PaperRunCreatedHandler(experience, repo, om));
    assertThrows(
        IllegalStateException.class, () -> new DefaultNqFeedbackEventTypeRouter(handlers));
  }

  @Test
  void abstractHandlerAppendsStage1EventAndPreservesRawPayload() {
    final NqFeedbackEventRepository repo = new InMemoryNqFeedbackEventRepository();
    final ObjectMapper om = new ObjectMapper();
    final CountingFeedbackService experience = new CountingFeedbackService();
    final AbstractNqFeedbackEventHandler handler =
        new PaperRunCreatedHandler(experience, repo, om);
    final NqFeedbackEnvelope envelope = envelopeOf(NqFeedbackEventType.PAPER_RUN_CREATED);

    handler.handle(envelope, "tenant-handler");

    assertEquals(1, experience.totalCalls);
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
    final CountingFeedbackService experience = new CountingFeedbackService();
    final List<NqFeedbackEventHandler> handlers =
        List.of(
            new PaperRunCreatedHandler(experience, repo, om),
            new PaperRunStartedHandler(experience, repo, om),
            new PaperRunStoppedHandler(experience, repo, om),
            new PaperRunDailyReportGeneratedHandler(experience, repo, om),
            new PaperRunAlertRaisedHandler(experience, repo, om),
            new PaperRunRecoveryEventRecordedHandler(experience, repo, om),
            new PaperRunStabilityCheckCompletedHandler(experience, repo, om),
            new BacktestResultReadyHandler(experience, repo, om));
    final EnumSet<NqFeedbackEventType> covered = EnumSet.noneOf(NqFeedbackEventType.class);
    for (NqFeedbackEventHandler h : handlers) {
      covered.add(h.supportedType());
    }
    assertEquals(EnumSet.allOf(NqFeedbackEventType.class), covered);
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

  /** 计数 ExperienceFeedbackService 的 fake，统计 handler 是否触达 + 按 eventType 计数。 */
  private static final class CountingFeedbackService implements ExperienceFeedbackService {
    int totalCalls;
    final Map<String, Integer> byEventType = new java.util.HashMap<>();

    @Override
    public void apply(final com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent event) {
      totalCalls++;
      byEventType.merge(event.getEventType(), 1, Integer::sum);
    }

    Map<NqFeedbackEventType, Integer> callsByEventTypeName() {
      final Map<NqFeedbackEventType, Integer> out = new EnumMap<>(NqFeedbackEventType.class);
      for (Map.Entry<String, Integer> e : byEventType.entrySet()) {
        out.put(NqFeedbackEventType.valueOf(e.getKey()), e.getValue());
      }
      return out;
    }
  }
}

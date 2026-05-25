package com.guidinglight.decisionhub.usecase.agent.feedback;

import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunRepository;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryResearchRunRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Stage2-PoC-B2 测试公共工具：登记 ResearchRun、构造合法 envelope 命令、合法 payload 字符串。 */
final class B2TestFixtures {

  private B2TestFixtures() {}

  static ResearchRunRepository repoWithRun(final String traceId) {
    final ResearchRunRepository repo = new InMemoryResearchRunRepository();
    final ResearchRun run =
        ResearchRun.rehydrate(
            traceId,
            "t-default",
            traceId,
            "topic-test",
            Map.of(),
            com.guidinglight.decisionhub.domain.research.ResearchRunStatus.CREATED,
            Instant.parse("2026-05-25T08:00:00Z"),
            Instant.parse("2026-05-25T08:00:00Z"));
    repo.save(run);
    return repo;
  }

  static IngestionCommand legalCommand(
      final String eventId, final NqFeedbackEventType eventType, final String traceId) {
    return IngestionCommand.of(
        "t-default",
        eventId,
        eventType.name(),
        Instant.parse("2026-05-25T08:00:00Z"),
        NqFeedbackEnvelope.SOURCE_SYSTEM_NEXUS_QUANT,
        "src-job-" + eventId,
        traceId,
        "req-" + eventId,
        "corr-" + eventId,
        "1.0.0",
        defaultPayloadFor(eventType),
        TimeProvider.now());
  }

  static IngestionCommand commandWith(
      final IngestionCommand base, final String field, final String value) {
    return IngestionCommand.of(
        base.getTenantId(),
        "eventId".equals(field) ? value : base.getEventId(),
        "rawEventType".equals(field) ? value : base.getRawEventType(),
        base.getOccurredAt(),
        "sourceSystem".equals(field) ? value : base.getSourceSystem(),
        "sourceJobId".equals(field) ? value : base.getSourceJobId(),
        "traceId".equals(field) ? value : base.getTraceId(),
        "requestId".equals(field) ? value : base.getRequestId(),
        "correlationId".equals(field) ? value : base.getCorrelationId(),
        "schemaVersion".equals(field) ? value : base.getSchemaVersion(),
        "payloadJson".equals(field) ? value : base.getPayloadJson(),
        base.getReceivedAt());
  }

  /**
   * 为每种 eventType 提供一个最小合法 payload（命中 {@code
   * DefaultNqFeedbackContractValidator} 必填字段表）。
   */
  static String defaultPayloadFor(final NqFeedbackEventType type) {
    final Map<String, Object> p = new HashMap<>();
    p.put("rawPayloadJson", "{\"src\":\"fake\"}");
    switch (type) {
      case PAPER_RUN_CREATED -> {
        p.put("paperRunId", "pr-1");
        p.put("candidateId", "cand-1");
        p.put("strategyName", "S1");
        p.put("requestedBy", "alice");
        p.put("createdAt", "2026-05-25T08:00:00Z");
      }
      case PAPER_RUN_STARTED -> {
        p.put("paperRunId", "pr-1");
        p.put("startedAt", "2026-05-25T08:00:00Z");
        p.put("mode", "CONTINUOUS");
      }
      case PAPER_RUN_STOPPED -> {
        p.put("paperRunId", "pr-1");
        p.put("stoppedAt", "2026-05-25T08:00:00Z");
        p.put("reason", "MANUAL");
      }
      case PAPER_RUN_DAILY_REPORT_GENERATED -> {
        p.put("paperRunId", "pr-1");
        p.put("reportId", "r-1");
        p.put("reportDate", "2026-05-25");
        p.put("realizedPnl", 100.0);
      }
      case PAPER_RUN_ALERT_RAISED -> {
        p.put("paperRunId", "pr-1");
        p.put("alertId", "a-1");
        p.put("alertLevel", "WARN");
        p.put("alertCode", "RISK_THRESHOLD");
        p.put("message", "approaching limit");
        p.put("raisedAt", "2026-05-25T08:00:00Z");
      }
      case PAPER_RUN_RECOVERY_EVENT_RECORDED -> {
        p.put("paperRunId", "pr-1");
        p.put("recoveryEventId", "rec-1");
        p.put("recoveryReason", "RESTART_OK");
        p.put("recoveredAt", "2026-05-25T08:00:00Z");
      }
      case PAPER_RUN_STABILITY_CHECK_COMPLETED -> {
        p.put("paperRunId", "pr-1");
        p.put("checkId", "ck-1");
        p.put("result", "STABLE");
        p.put("summary", "ok");
        p.put("completedAt", "2026-05-25T08:00:00Z");
      }
      case BACKTEST_RESULT_READY -> {
        p.put("backtestId", "bt-1");
        p.put("requestId", "req-1");
        p.put("candidateId", "cand-1");
        p.put("periodStart", "2024-01-01");
        p.put("periodEnd", "2025-12-31");
        p.put("verdict", "PASS");
        p.put("readyAt", "2026-05-25T08:00:00Z");
      }
    }
    return toJson(p);
  }

  static String toJson(final Map<String, Object> map) {
    try {
      return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}

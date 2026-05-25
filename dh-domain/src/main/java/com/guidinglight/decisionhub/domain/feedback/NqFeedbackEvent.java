package com.guidinglight.decisionhub.domain.feedback;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import java.time.Instant;
import java.util.Map;

/**
 * Stage1：NQ 回流事件（事实面）。
 *
 * <p>对应工单 4.1：NqFeedbackEvent。来源包括 BACKTEST/RISK/PAPER/RELEASE/LIVE/REVIEW； DH 收到后用于更新
 * ExperienceEntry / PheromoneEdge。
 */
public final class NqFeedbackEvent {

  private final String eventId;
  private final String tenantId;
  private final String runId;
  private final String candidateId;
  private final String traceId;
  private final FeedbackSource source;
  /** Stage1：NQ 的事实事件名，例如 BacktestCompleted、RiskRejected。 */
  private final String eventType;
  /** 是否为正面结果，用于 ExperienceEntry / PheromoneEdge 的增强或衰减判断。 */
  private final boolean positive;
  private final Map<String, Object> payloadJson;
  private final Instant occurredAt;
  private final Instant receivedAt;

  private NqFeedbackEvent(
      final String eventId,
      final String tenantId,
      final String runId,
      final String candidateId,
      final String traceId,
      final FeedbackSource source,
      final String eventType,
      final boolean positive,
      final Map<String, Object> payloadJson,
      final Instant occurredAt,
      final Instant receivedAt) {
    this.eventId = eventId;
    this.tenantId = tenantId;
    this.runId = runId;
    this.candidateId = candidateId;
    this.traceId = traceId;
    this.source = source;
    this.eventType = eventType;
    this.positive = positive;
    this.payloadJson = payloadJson == null ? Map.of() : Map.copyOf(payloadJson);
    this.occurredAt = occurredAt;
    this.receivedAt = receivedAt;
  }

  /** 工厂方法。 */
  public static NqFeedbackEvent create(
      final String tenantId,
      final String runId,
      final String candidateId,
      final String traceId,
      final FeedbackSource source,
      final String eventType,
      final boolean positive,
      final Map<String, Object> payloadJson,
      final Instant occurredAt,
      final Instant receivedAt) {
    return new NqFeedbackEvent(
        IdGenerator.newId(),
        tenantId,
        runId,
        candidateId,
        traceId,
        source,
        eventType,
        positive,
        payloadJson,
        occurredAt,
        receivedAt);
  }

  public String getEventId() {
    return eventId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getRunId() {
    return runId;
  }

  public String getCandidateId() {
    return candidateId;
  }

  public String getTraceId() {
    return traceId;
  }

  public FeedbackSource getSource() {
    return source;
  }

  public String getEventType() {
    return eventType;
  }

  public boolean isPositive() {
    return positive;
  }

  public Map<String, Object> getPayloadJson() {
    return payloadJson;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }
}

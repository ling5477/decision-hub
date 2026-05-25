package com.guidinglight.decisionhub.domain.judge;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Stage1：面向上游消费者的结构化决策建议。
 *
 * <p>对应工单 4.1：DecisionRecommendation。NQ 或 NQ Console 接收此对象作为人工审批/正式回测入口的依据。
 * 注意：这只是建议，不是订单或交易指令。
 */
public final class DecisionRecommendation {

  private final String recommendationId;
  private final String runId;
  private final String decisionId;
  private final String traceId;
  private final List<String> selectedCandidateIds;
  private final Map<String, Object> payloadJson;
  private final Instant createdAt;

  private DecisionRecommendation(
      final String recommendationId,
      final String runId,
      final String decisionId,
      final String traceId,
      final List<String> selectedCandidateIds,
      final Map<String, Object> payloadJson,
      final Instant createdAt) {
    this.recommendationId = recommendationId;
    this.runId = runId;
    this.decisionId = decisionId;
    this.traceId = traceId;
    this.selectedCandidateIds =
        selectedCandidateIds == null ? List.of() : List.copyOf(selectedCandidateIds);
    this.payloadJson = payloadJson == null ? Map.of() : Map.copyOf(payloadJson);
    this.createdAt = createdAt;
  }

  /** 工厂方法。 */
  public static DecisionRecommendation create(
      final String runId,
      final String decisionId,
      final String traceId,
      final List<String> selectedCandidateIds,
      final Map<String, Object> payloadJson,
      final Instant now) {
    return new DecisionRecommendation(
        IdGenerator.newId(),
        runId,
        decisionId,
        traceId,
        selectedCandidateIds,
        payloadJson,
        now);
  }

  public String getRecommendationId() {
    return recommendationId;
  }

  public String getRunId() {
    return runId;
  }

  public String getDecisionId() {
    return decisionId;
  }

  public String getTraceId() {
    return traceId;
  }

  public List<String> getSelectedCandidateIds() {
    return Collections.unmodifiableList(selectedCandidateIds);
  }

  public Map<String, Object> getPayloadJson() {
    return payloadJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

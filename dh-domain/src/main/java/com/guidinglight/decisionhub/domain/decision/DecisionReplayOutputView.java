package com.guidinglight.decisionhub.domain.decision;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * K4 replay output 只读视图。
 *
 * <p>该视图复述 K1 `READ_ONLY_RECOMMENDATION` 输出字段；`action` 仍受 `DecisionAction` 枚举约束，不会引入
 * BUY / SELL / PLACE_ORDER / CANCEL_ORDER 等交易指令。
 */
public record DecisionReplayOutputView(
    String decisionId,
    String tenantId,
    String traceId,
    String requestId,
    DecisionType decisionType,
    DecisionAction action,
    DecisionRiskLevel riskLevel,
    DecisionPolicyStatus policyStatus,
    BigDecimal confidence,
    Map<String, Object> outputJson,
    Instant createdAt) {

  /** 校验 output 只读合同与追踪字段。 */
  public DecisionReplayOutputView {
    decisionId = DecisionReplaySafety.requireText(decisionId, "decisionId");
    tenantId = DecisionReplaySafety.requireText(tenantId, "tenantId");
    traceId = DecisionReplaySafety.requireText(traceId, "traceId");
    requestId = DecisionReplaySafety.requireText(requestId, "requestId");
    decisionType = Objects.requireNonNull(decisionType, "decisionType");
    if (decisionType != DecisionType.READ_ONLY_RECOMMENDATION) {
      throw new IllegalArgumentException("decisionType must be READ_ONLY_RECOMMENDATION");
    }
    action = Objects.requireNonNull(action, "action");
    riskLevel = Objects.requireNonNull(riskLevel, "riskLevel");
    policyStatus = Objects.requireNonNull(policyStatus, "policyStatus");
    confidence = Objects.requireNonNull(confidence, "confidence");
    outputJson = DecisionReplaySafety.copyMap(outputJson);
    createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }
}

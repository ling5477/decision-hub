package com.guidinglight.decisionhub.domain.decision;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * K4 replay request 只读视图。
 *
 * <p>该对象来自 K3 `dh_decision_request` 的脱敏审计数据，只用于展示请求归属、合同类型和 subject 摘要；
 * 不包含账户、价格、数量、方向、凭证或任何执行字段。
 */
public record DecisionReplayRequestView(
    String decisionId,
    String requestId,
    String traceId,
    String tenantId,
    String source,
    DecisionType decisionType,
    Map<String, Object> subjectJson,
    String contextRef,
    Instant requestedAt,
    String schemaVersion,
    Instant createdAt) {

  /** 校验 request 视图的追踪字段，保持 replay 结果可审计。 */
  public DecisionReplayRequestView {
    decisionId = DecisionReplaySafety.requireText(decisionId, "decisionId");
    requestId = DecisionReplaySafety.requireText(requestId, "requestId");
    traceId = DecisionReplaySafety.requireText(traceId, "traceId");
    tenantId = DecisionReplaySafety.requireText(tenantId, "tenantId");
    source = DecisionReplaySafety.requireText(source, "source");
    decisionType = Objects.requireNonNull(decisionType, "decisionType");
    if (decisionType != DecisionType.READ_ONLY_RECOMMENDATION) {
      throw new IllegalArgumentException("decisionType must be READ_ONLY_RECOMMENDATION");
    }
    subjectJson = DecisionReplaySafety.copyMap(subjectJson);
    requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
    schemaVersion = DecisionReplaySafety.requireText(schemaVersion, "schemaVersion");
    createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }
}

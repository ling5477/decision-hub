package com.guidinglight.decisionhub.domain.decision;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * K4 replay audit event 只读视图。
 *
 * <p>audit event 只展示 K3 已落库的安全摘要；不得包含 raw request、raw response、credential、token 或 NQ DB
 * 内容。
 */
public record DecisionReplayAuditEventView(
    String id,
    String decisionId,
    String tenantId,
    String traceId,
    String eventType,
    String eventStatus,
    Map<String, Object> eventJson,
    String errorCode,
    Instant createdAt) {

  /** 校验 audit event 归属和事件标识。 */
  public DecisionReplayAuditEventView {
    id = DecisionReplaySafety.requireText(id, "id");
    decisionId = DecisionReplaySafety.requireText(decisionId, "decisionId");
    tenantId = DecisionReplaySafety.requireText(tenantId, "tenantId");
    traceId = DecisionReplaySafety.requireText(traceId, "traceId");
    eventType = DecisionReplaySafety.requireText(eventType, "eventType");
    eventStatus = DecisionReplaySafety.requireText(eventStatus, "eventStatus");
    eventJson = DecisionReplaySafety.copyMap(eventJson);
    createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }
}

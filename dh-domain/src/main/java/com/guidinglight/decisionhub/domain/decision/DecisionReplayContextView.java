package com.guidinglight.decisionhub.domain.decision;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * K4 replay context snapshot 只读视图。
 *
 * <p>只呈现 K3 已保存的脱敏 context 摘要和 evidence 引用；不会重新构造上下文、不会读取 NQ DB，也不会触发
 * provider 或 orchestrator。
 */
public record DecisionReplayContextView(
    String decisionId,
    String tenantId,
    String traceId,
    Map<String, Object> contextSnapshotJson,
    List<String> evidenceRefsJson,
    Instant createdAt) {

  /** 校验 context snapshot 的链路归属。 */
  public DecisionReplayContextView {
    decisionId = DecisionReplaySafety.requireText(decisionId, "decisionId");
    tenantId = DecisionReplaySafety.requireText(tenantId, "tenantId");
    traceId = DecisionReplaySafety.requireText(traceId, "traceId");
    contextSnapshotJson = DecisionReplaySafety.copyMap(contextSnapshotJson);
    evidenceRefsJson = DecisionReplaySafety.copyStrings(evidenceRefsJson);
    createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }
}

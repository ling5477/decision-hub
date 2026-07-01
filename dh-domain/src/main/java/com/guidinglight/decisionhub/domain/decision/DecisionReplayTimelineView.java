package com.guidinglight.decisionhub.domain.decision;

import java.util.List;

/**
 * K4 replay timeline 只读视图。
 *
 * <p>timeline 由 trace step、provider call summary 和 audit event 三组已落库记录组成；它只用于审计查询，不会
 * 重跑任何步骤。
 */
public record DecisionReplayTimelineView(
    List<DecisionReplayTraceStepView> traceSteps,
    List<DecisionReplayProviderCallView> providerCalls,
    List<DecisionReplayAuditEventView> auditEvents) {

  /** 复制三组时间线数据，保证调用方无法修改 replay view。 */
  public DecisionReplayTimelineView {
    traceSteps = DecisionReplaySafety.copyList(traceSteps);
    providerCalls = DecisionReplaySafety.copyList(providerCalls);
    auditEvents = DecisionReplaySafety.copyList(auditEvents);
  }

  /** 创建空时间线，用于 fail-closed 状态。 */
  public static DecisionReplayTimelineView empty() {
    return new DecisionReplayTimelineView(List.of(), List.of(), List.of());
  }
}

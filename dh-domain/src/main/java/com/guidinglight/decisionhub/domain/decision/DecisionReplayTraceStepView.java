package com.guidinglight.decisionhub.domain.decision;

import java.time.Instant;
import java.util.Objects;

/**
 * K4 replay trace step 只读视图。
 *
 * <p>trace step 只描述 DH 内部只读编排步骤，不代表 provider、NQ runtime、LangGraph 或交易执行被重放。
 */
public record DecisionReplayTraceStepView(
    String id,
    String decisionId,
    String tenantId,
    String traceId,
    String stepName,
    String stepStatus,
    Instant startedAt,
    Instant endedAt,
    String errorCode,
    String errorMessage,
    Instant createdAt) {

  /** 校验 trace step 的 ID、步骤名和时间，避免不可排序的 replay timeline。 */
  public DecisionReplayTraceStepView {
    id = DecisionReplaySafety.requireText(id, "id");
    decisionId = DecisionReplaySafety.requireText(decisionId, "decisionId");
    tenantId = DecisionReplaySafety.requireText(tenantId, "tenantId");
    traceId = DecisionReplaySafety.requireText(traceId, "traceId");
    stepName = DecisionReplaySafety.requireText(stepName, "stepName");
    stepStatus = DecisionReplaySafety.requireText(stepStatus, "stepStatus");
    startedAt = Objects.requireNonNull(startedAt, "startedAt");
    createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }
}

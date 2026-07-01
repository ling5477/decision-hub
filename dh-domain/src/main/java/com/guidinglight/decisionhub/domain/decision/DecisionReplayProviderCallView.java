package com.guidinglight.decisionhub.domain.decision;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * K4 replay provider call 只读视图。
 *
 * <p>K4 只能读取 K3 保存的 deterministic mock provider summary。该视图不得保存或返回 raw provider
 * sensitive response、credential、token 或外部模型上下文。
 */
public record DecisionReplayProviderCallView(
    String id,
    String decisionId,
    String tenantId,
    String traceId,
    String providerName,
    ProviderSignalStatus providerStatus,
    long latencyMs,
    Map<String, Object> signalJson,
    String errorCode,
    Instant createdAt) {

  /** 校验 provider call summary；失败态和 mock 态都只作为审计事实呈现。 */
  public DecisionReplayProviderCallView {
    id = DecisionReplaySafety.requireText(id, "id");
    decisionId = DecisionReplaySafety.requireText(decisionId, "decisionId");
    tenantId = DecisionReplaySafety.requireText(tenantId, "tenantId");
    traceId = DecisionReplaySafety.requireText(traceId, "traceId");
    providerName = DecisionReplaySafety.requireText(providerName, "providerName");
    providerStatus = Objects.requireNonNull(providerStatus, "providerStatus");
    if (latencyMs < 0) {
      throw new IllegalArgumentException("latencyMs must not be negative");
    }
    signalJson = DecisionReplaySafety.copyMap(signalJson);
    createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }
}

package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;

/**
 * K4 replay read model usecase service。
 *
 * <p>本服务只做只读查询和 fail-closed 归一化；不会重跑 provider、orchestrator、LLM、NQ runtime 或交易执行。
 */
public interface DecisionReplayQueryService {

  /**
   * 查询单次 decision 的 replay view。
   *
   * @param tenantId 租户 ID，必填；用于跨租户隔离。
   * @param decisionId decision run ID，必填；主查询条件。
   * @param traceId 可选 traceId，一致性不匹配时返回 BLOCKED。
   * @param requestId 可选 requestId，一致性不匹配时返回 BLOCKED。
   * @return 结构化 replay 查询结果。
   */
  DecisionReplayView replay(String tenantId, String decisionId, String traceId, String requestId);
}

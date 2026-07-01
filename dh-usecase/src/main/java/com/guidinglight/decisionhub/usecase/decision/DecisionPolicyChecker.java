package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionPolicyResult;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;

/**
 * K2 read-only 安全策略检查接口。
 *
 * <p>该接口负责在任何 provider 或 risk 步骤之前阻断非只读请求、执行意图和非法输入。实现必须无外部副作用，不得授权
 * NQ、交易、真实 provider 或 LIVE 能力。
 */
public interface DecisionPolicyChecker {

  /**
   * 检查请求是否满足 K2 只读编排边界。
   *
   * @param request K1 冻结请求；可以为 null，null 必须返回非 allowed
   * @return policy 结果；只有 ALLOWED 才能继续后续只读编排
   */
  DecisionPolicyResult check(DecisionRequest request);
}

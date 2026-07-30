package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;

/**
 * GateK K2 Decision Pipeline MVP 编排入口。
 *
 * <p>该接口只返回 read-only structured output，不提交订单、不调用 NQ、不触发 agent runtime、不访问真实 provider。实现必须
 * fail-closed，所有异常都应转换为 ABSTAIN/BLOCKED 输出。
 */
public interface DecisionOrchestrator {

  /**
   * 执行一次只读推荐编排。
   *
   * @param request K1 冻结请求合同
   * @return K1 冻结输出合同；永不代表交易指令
   */
  DecisionOutput decide(DecisionRequest request);
}

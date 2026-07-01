package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionRequest;

/**
 * 构造 K2 编排上下文的边界接口。
 *
 * <p>K2 仅允许从请求内的只读 snapshot 提取引用；实现不得读取 DB、NQ、HTTP、文件系统或凭证。实现应为无副作用，
 * 调用失败由 orchestrator 统一转换为 fail-closed output。
 */
public interface DecisionContextBuilder {

  /**
   * 根据 K1 request 构造只读编排上下文。
   *
   * @param request K1 冻结的只读请求合同
   * @return 单次编排使用的只读上下文
   */
  DecisionContext build(DecisionRequest request);
}

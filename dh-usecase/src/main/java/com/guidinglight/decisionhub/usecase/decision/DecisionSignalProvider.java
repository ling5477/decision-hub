package com.guidinglight.decisionhub.usecase.decision;

/**
 * K2 provider signal 边界接口。
 *
 * <p>K2 只允许 deterministic mock provider。实现不得接入真实 AI provider、LangGraph、HTTP、NQ、交易所、MCP 写能力或凭证。
 */
public interface DecisionSignalProvider {

  /**
   * 根据只读上下文生成 provider signal。
   *
   * @param context 单次编排上下文
   * @return provider signal；失败态必须由 assembler 转换为 ABSTAIN
   */
  DecisionSignalResult signal(DecisionContext context);
}

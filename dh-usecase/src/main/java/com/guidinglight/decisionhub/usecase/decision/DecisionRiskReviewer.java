package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionRiskReview;

/**
 * K2 risk review 边界接口。
 *
 * <p>该接口只根据本轮只读上下文和 mock signal 做 deterministic 风险判定，不调用 NQ 风控、账户、交易、外部模型或数据库。
 */
public interface DecisionRiskReviewer {

  /**
   * 审查当前只读推荐是否允许进入输出组装。
   *
   * @param context 编排上下文
   * @param signal provider signal
   * @return risk 结果；HIGH/BLOCKED/UNKNOWN 必须阻断 directional bias
   */
  DecisionRiskReview review(DecisionContext context, DecisionSignalResult signal);
}

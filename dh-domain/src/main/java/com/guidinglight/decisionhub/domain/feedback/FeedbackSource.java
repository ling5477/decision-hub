package com.guidinglight.decisionhub.domain.feedback;

/**
 * NQ 事实回流来源。
 * 来源不同会带来不同的经验强化/衰减权重。
 */
public enum FeedbackSource {
  /** 正式回测。 */
  BACKTEST,
  /** 风控审查。 */
  RISK,
  /** 模拟盘 / paper trial。 */
  PAPER,
  /** 策略发布审批。 */
  RELEASE,
  /** 实盘运行。 */
  LIVE,
  /** 事后复盘 / post-mortem。 */
  REVIEW
}

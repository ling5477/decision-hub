package com.guidinglight.decisionhub.domain.qdr.feedback;

/** 已验证观察所表达的决策结果，不包含交易动作语义。 */
public enum ObservedDecisionOutcome {
  /** 观察到目标完整达成。 */
  SUCCEEDED,
  /** 观察到目标部分达成。 */
  PARTIALLY_SUCCEEDED,
  /** 观察到目标未达成。 */
  FAILED
}

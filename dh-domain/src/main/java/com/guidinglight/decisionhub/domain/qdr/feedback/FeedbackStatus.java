package com.guidinglight.decisionhub.domain.qdr.feedback;

/** 结构化归因的稳定状态。 */
public enum FeedbackStatus {
  /** 证据完整且完成确定性归因。 */
  ATTRIBUTED,
  /** 证据不足，不能声称归因成功。 */
  INCONCLUSIVE,
  /** 输入、关联或策略不允许归因。 */
  REJECTED
}

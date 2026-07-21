package com.guidinglight.decisionhub.usecase.qdr.feedback;

/** 归因 use case 的稳定 fail-closed 错误分类。 */
public enum FeedbackAttributionErrorCode {
  /** 无错误。 */
  NONE,
  /** feedback subject 非法。 */
  INVALID_FEEDBACK_SUBJECT,
  /** observation 非法或时间越界。 */
  INVALID_OBSERVATION,
  /** observation 来源被策略拒绝。 */
  OUTCOME_SOURCE_DENIED,
  /** subject 与 observation 的 tenant 不一致。 */
  TENANT_SCOPE_MISMATCH,
  /** subject 与 observation 的 environment 不一致。 */
  ENVIRONMENT_SCOPE_MISMATCH,
  /** decision 引用不一致。 */
  DECISION_REFERENCE_INVALID,
  /** trace 引用不一致。 */
  TRACE_REFERENCE_INVALID,
  /** 通用关联不一致，保留给端口适配错误。 */
  CORRELATION_MISMATCH,
  /** 未知或不受支持的封闭值。 */
  UNKNOWN_VALUE,
  /** 幂等端口不可用。 */
  IDEMPOTENCY_UNAVAILABLE,
  /** 同一冻结 key 的 canonical hash 冲突。 */
  IDEMPOTENCY_CONFLICT,
  /** 强制证据不完整。 */
  EVIDENCE_INCOMPLETE,
  /** 观察结果与结构化贡献方向矛盾。 */
  ATTRIBUTION_CONTRADICTION,
  /** 审计写入或安全引用失败。 */
  AUDIT_FAILED,
  /** 未知内部异常，已脱敏。 */
  ATTRIBUTION_FAILED
}

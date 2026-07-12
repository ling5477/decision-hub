package com.guidinglight.decisionhub.usecase.decision;

/**
 * K3 decision audit event 类型。
 *
 * <p>事件用于审计只读 recommendation 生命周期；不得被解释为交易指令或 NQ 状态变更。
 */
public enum DecisionAuditEventType {
  /** Decision run 已完成并生成结构化输出。 */
  DECISION_COMPLETED,

  /** Policy 拒绝或输入无效导致 fail-closed。 */
  POLICY_DENIED,

  /** Provider mock 路径失败、超时或不可用。 */
  PROVIDER_FAILED,

  /** Risk review 阻断方向性偏好。 */
  RISK_BLOCKED,

  /** 持久化失败触发 fail-closed。 */
  PERSISTENCE_FAILED,

  /** 编排内部异常触发 fail-closed。 */
  DECISION_FAILED,

  /** Human Approval Packet 已创建；只表示 DH 内部审查证据生成，不表示交易授权。 */
  HUMAN_APPROVAL_PACKET_CREATED,

  /** Human Approval Packet 审批决定已提交；APPROVED 不等于 BUY。 */
  HUMAN_APPROVAL_DECISION_SUBMITTED,

  /** Human Approval Packet 被人工拒绝；REJECTED 不等于 SELL。 */
  HUMAN_APPROVAL_DECISION_REJECTED,

  /** Human Approval Packet 需要继续人工审查；不得触发 replay 或 provider。 */
  HUMAN_APPROVAL_DECISION_NEEDS_REVIEW,

  /** Human Approval Packet 状态转移被状态机拒绝。 */
  HUMAN_APPROVAL_TRANSITION_DENIED,

  /** Stage-QDR-7 persistent rate admission结果；不包含quota或identity明文。 */
  QDR7_RATE_LIMIT_ADMISSION,

  /** Stage-QDR-7 exact idempotency admission结果。 */
  QDR7_IDEMPOTENCY_ADMISSION,

  /** Stage-QDR-7 idempotency已与safe result原子完成。 */
  QDR7_IDEMPOTENCY_COMPLETED,

  /** Stage-QDR-7 idempotency已冻结stable failure。 */
  QDR7_IDEMPOTENCY_FAILED
}

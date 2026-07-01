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
  DECISION_FAILED
}

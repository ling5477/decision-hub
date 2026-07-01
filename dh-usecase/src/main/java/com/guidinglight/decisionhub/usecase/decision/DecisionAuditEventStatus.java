package com.guidinglight.decisionhub.usecase.decision;

/** K3 audit event 写入结果状态。 */
public enum DecisionAuditEventStatus {
  /** 审计事件表示该阶段按只读合同完成。 */
  SUCCESS,

  /** 审计事件表示该阶段 fail-closed。 */
  FAILED
}

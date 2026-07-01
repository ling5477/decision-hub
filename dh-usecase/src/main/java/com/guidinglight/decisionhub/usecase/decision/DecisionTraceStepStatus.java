package com.guidinglight.decisionhub.usecase.decision;

/** K3 trace step 写入状态。 */
public enum DecisionTraceStepStatus {
  /** 步骤开始。 */
  STARTED,

  /** 步骤完成。 */
  COMPLETED,

  /** 步骤失败，调用方必须 fail-closed。 */
  FAILED
}

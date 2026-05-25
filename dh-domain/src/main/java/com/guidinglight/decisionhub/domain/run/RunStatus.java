package com.guidinglight.decisionhub.domain.run;

/**
 * @deprecated Stage1-CLOSE：旧 Run 状态机；新链路使用
 *     {@link com.guidinglight.decisionhub.domain.research.ResearchRunStatus}。
 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public enum RunStatus {
  DRAFT,
  QUEUED,
  RUNNING,
  PAUSED,
  FAILED,
  SUCCEEDED,
  DEFERRED,
  CANCELLED
}

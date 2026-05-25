package com.guidinglight.decisionhub.domain.agent;

/**
 * AgentTask 中单个任务节点的状态。
 */
public enum TaskNodeStatus {
  /** 待执行。 */
  PENDING,
  /** 执行中。 */
  RUNNING,
  /** 成功。 */
  SUCCEEDED,
  /** 失败。 */
  FAILED,
  /** 被跳过（依赖失败或条件不满足）。 */
  SKIPPED
}

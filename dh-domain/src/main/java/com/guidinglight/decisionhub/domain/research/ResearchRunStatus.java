package com.guidinglight.decisionhub.domain.research;

/**
 * ResearchRun 生命周期状态。
 * Stage1：CREATED -> PLANNING -> EXPLORING -> REVIEWING -> JUDGING -> WAITING_NQ -> COMPLETED。
 * 失败路径：FAILED；用户取消：CANCELLED。
 */
public enum ResearchRunStatus {
  /** 初始状态。 */
  CREATED,
  /** Leader 规划任务图。 */
  PLANNING,
  /** Scout/Analyst/Strategy 并行搜索候选。 */
  EXPLORING,
  /** Risk/Strategy Reviewer 审查候选。 */
  REVIEWING,
  /** Judge 仲裁阶段。 */
  JUDGING,
  /** 等待 NQ 正式回测/风控等反馈。 */
  WAITING_NQ,
  /** 完成。 */
  COMPLETED,
  /** 失败。 */
  FAILED,
  /** 取消。 */
  CANCELLED
}

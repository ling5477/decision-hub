package com.guidinglight.decisionhub.domain.checkpoint;

/**
 * Stage2-PoC-B1：checkpoint 类型。
 *
 * <p>区分 checkpoint 触发场景。JudgeDecision 仍是唯一最终出口；checkpoint 只是过程证据。
 */
public enum CheckpointType {
  /** 候选已冻结，可送 Judge。 */
  CANDIDATE_FROZEN,
  /** Judge 完成决策。 */
  JUDGE_DECISION,
  /** Planner 决定 pivot（重新选边）。 */
  PIVOT_TRIGGERED,
  /** Planner 决定 abort（终止后续 step）。 */
  ABORT_TRIGGERED,
  /** 已发起 DH -> NQ backtest 请求。 */
  BACKTEST_REQUESTED
}

package com.guidinglight.decisionhub.domain.judge;

/**
 * JudgeDecision 生命周期状态。
 */
public enum JudgeDecisionStatus {
  /** Judge 草稿。 */
  DRAFT,
  /** 已定稿，可发往下游或人审。 */
  FINALIZED,
  /** Judge 整体拒绝（所有候选不达标）。 */
  REJECTED
}

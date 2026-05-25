package com.guidinglight.decisionhub.domain.candidate;

/**
 * 策略候选的流转状态。
 */
public enum CandidateStatus {
  /** 已生成（来自 Strategy/Analyst Agent）。 */
  GENERATED,
  /** 经过过滤被淘汰。 */
  FILTERED,
  /** 经 Judge 选定。 */
  SELECTED,
  /** 经 Judge 明确拒绝。 */
  REJECTED,
  /** 已发送到 NQ 申请正式回测/审查。 */
  SENT_TO_NQ
}

package com.guidinglight.decisionhub.domain.feedback.payload;

/**
 * Stage2-PoC-B1：paper run 稳定性检查结论。
 *
 * <p>由 NQ 侧稳定性检查流程产出；DH 用于经验强化和后续 reflection。
 */
public enum StabilityCheckResult {
  /** 稳定。 */
  STABLE,
  /** 不稳定。 */
  UNSTABLE,
  /** 数据不足以判定，建议复查。 */
  INCONCLUSIVE
}

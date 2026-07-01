package com.guidinglight.decisionhub.domain.decision;

/**
 * Risk level used by GateK decision contracts.
 *
 * <p>{@link #HIGH} and {@link #BLOCKED} must never permit directional bias
 * output in K1.
 */
public enum DecisionRiskLevel {
  /** Risk review found no elevated issue. */
  LOW,

  /** Risk review found material but non-blocking uncertainty. */
  MEDIUM,

  /** Risk is too high for directional bias. */
  HIGH,

  /** Policy or safety gate blocks the recommendation. */
  BLOCKED,

  /** Risk was not available; callers must fail closed. */
  UNKNOWN
}

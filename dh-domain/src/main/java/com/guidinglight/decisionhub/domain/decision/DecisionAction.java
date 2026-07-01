package com.guidinglight.decisionhub.domain.decision;

/**
 * GateK read-only recommendation action vocabulary.
 *
 * <p>These values express analysis posture only. They are deliberately not
 * order sides, order commands, or exchange execution instructions.
 */
public enum DecisionAction {
  /** Default fail-closed action when evidence is absent or unsafe. */
  ABSTAIN,

  /** Observe only; do not express directional preference. */
  OBSERVE,

  /** Explicit no-trade recommendation. */
  NO_TRADE,

  /** Analytical long-side bias; not an execution instruction. */
  LONG_BIAS,

  /** Analytical short-side bias; not an execution instruction. */
  SHORT_BIAS
}

package com.guidinglight.decisionhub.domain.decision;

/**
 * GateK Decision Pipeline output category.
 *
 * <p>K1 freezes a single read-only type. It is not an execution command and
 * must not be expanded without a later reviewed contract batch.
 */
public enum DecisionType {
  /** Structured read-only recommendation, with no runtime execution authority. */
  READ_ONLY_RECOMMENDATION
}

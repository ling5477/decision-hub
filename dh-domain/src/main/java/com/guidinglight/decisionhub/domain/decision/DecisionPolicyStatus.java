package com.guidinglight.decisionhub.domain.decision;

/**
 * Policy gate status for GateK read-only recommendation contracts.
 *
 * <p>Denied, invalid, or blocked status must fail closed and must not produce
 * directional bias.
 */
public enum DecisionPolicyStatus {
  /** Policy allows a read-only recommendation. */
  ALLOWED,

  /** Policy explicitly denies the request. */
  DENIED,

  /** Human or downstream review is required before directional output. */
  REVIEW_REQUIRED,

  /** Input is invalid for the decision contract. */
  INVALID,

  /** Safety policy blocks the request. */
  BLOCKED
}

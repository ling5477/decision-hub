package com.guidinglight.decisionhub.domain.decision;

/**
 * Contract-level status for a GateK decision output.
 *
 * <p>Status describes the structured result state only. It does not represent
 * execution lifecycle or trading state.
 */
public enum DecisionStatus {
  /** Output abstained because the safe default was required. */
  ABSTAINED,

  /** Output is an observation-only recommendation. */
  OBSERVATION_ONLY,

  /** Output contains directional analytical bias only. */
  DIRECTIONAL_BIAS,

  /** Output is blocked by policy or risk. */
  BLOCKED,

  /** Input or provider state was invalid for a safe recommendation. */
  INVALID
}

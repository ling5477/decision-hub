package com.guidinglight.decisionhub.domain.decision;

/**
 * Provider signal status recorded by the GateK contract.
 *
 * <p>K1 only freezes the value vocabulary. It does not enable provider runtime
 * calls or external model access.
 */
public enum ProviderSignalStatus {
  /** Provider was intentionally not called. */
  NOT_CALLED,

  /** Provider signal came from a deterministic mock. */
  MOCKED,

  /** Provider returned a usable signal. */
  SUCCESS,

  /** Provider timed out; output must fail closed. */
  TIMEOUT,

  /** Provider failed; output must fail closed. */
  FAILED,

  /** Provider was not trusted for this decision. */
  UNTRUSTED,

  /** Provider budget was exhausted. */
  BUDGET_EXCEEDED,

  /** Provider is disabled by configuration or gate. */
  DISABLED
}

package com.guidinglight.decisionhub.domain.decision;

import java.util.List;
import java.util.Objects;

/**
 * Policy result attached to a GateK decision output.
 *
 * <p>Non-allowed states are fail-closed and must not allow directional bias.
 */
public record DecisionPolicyResult(DecisionPolicyStatus status, List<String> reasonCodes) {

  /** Normalizes reason codes to an immutable list. */
  public DecisionPolicyResult {
    status = Objects.requireNonNull(status, "status");
    reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
  }

  /** Returns true only when policy explicitly allows a read-only recommendation. */
  public boolean isAllowed() {
    return status == DecisionPolicyStatus.ALLOWED;
  }
}

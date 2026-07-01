package com.guidinglight.decisionhub.domain.decision;

import java.util.List;
import java.util.Objects;

/**
 * Provider signal summary for the GateK contract.
 *
 * <p>K1 does not enable real provider calls. This value object records the
 * status vocabulary that later mock-only batches must respect.
 */
public record ProviderDecisionSignal(ProviderSignalStatus status, List<String> reasonCodes) {

  /** Normalizes reason codes to an immutable list. */
  public ProviderDecisionSignal {
    status = Objects.requireNonNull(status, "status");
    reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
  }

  /** Returns true when provider status requires a fail-closed abstain. */
  public boolean requiresAbstain() {
    return status == ProviderSignalStatus.TIMEOUT
        || status == ProviderSignalStatus.FAILED
        || status == ProviderSignalStatus.UNTRUSTED
        || status == ProviderSignalStatus.BUDGET_EXCEEDED
        || status == ProviderSignalStatus.DISABLED;
  }
}

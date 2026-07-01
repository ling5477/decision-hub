package com.guidinglight.decisionhub.domain.decision;

import java.util.List;
import java.util.Objects;

/**
 * Risk review summary for the GateK contract.
 *
 * <p>High and blocked risk levels must prevent {@link DecisionAction#LONG_BIAS}
 * and {@link DecisionAction#SHORT_BIAS}.
 */
public record DecisionRiskReview(DecisionRiskLevel level, List<String> reasonCodes) {

  /** Normalizes reason codes to an immutable list. */
  public DecisionRiskReview {
    level = Objects.requireNonNull(level, "level");
    reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
  }

  /** Returns true when the risk level forbids directional analytical bias. */
  public boolean forbidsDirectionalBias() {
    return level == DecisionRiskLevel.HIGH || level == DecisionRiskLevel.BLOCKED;
  }
}

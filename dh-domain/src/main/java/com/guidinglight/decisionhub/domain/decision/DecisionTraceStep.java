package com.guidinglight.decisionhub.domain.decision;

import java.time.Instant;
import java.util.Objects;

/**
 * Contract shape for a future decision trace step.
 *
 * <p>K1 freezes the value object only. Persistence and replay are explicitly
 * deferred to later reviewed batches.
 */
public record DecisionTraceStep(String stepName, String outcome, Instant occurredAt) {

  /** Validates trace step identifiers. */
  public DecisionTraceStep {
    stepName = requireText(stepName, "stepName");
    outcome = requireText(outcome, "outcome");
    occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
  }

  private static String requireText(final String value, final String field) {
    final String checked = Objects.requireNonNull(value, field).trim();
    if (checked.isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return checked;
  }
}

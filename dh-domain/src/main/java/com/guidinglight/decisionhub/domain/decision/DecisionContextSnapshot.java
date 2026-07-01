package com.guidinglight.decisionhub.domain.decision;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Minimal read-only context snapshot referenced by a GateK request.
 *
 * <p>K1 stores only identifiers and evidence references. It does not persist
 * audit, trace, or replay data; those are later batches.
 */
public record DecisionContextSnapshot(
    String snapshotId, Instant capturedAt, List<String> evidenceRefs) {

  /** Normalizes evidence references to an immutable list. */
  public DecisionContextSnapshot {
    snapshotId = requireText(snapshotId, "snapshotId");
    capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
    evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
  }

  private static String requireText(final String value, final String field) {
    final String checked = Objects.requireNonNull(value, field).trim();
    if (checked.isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return checked;
  }
}

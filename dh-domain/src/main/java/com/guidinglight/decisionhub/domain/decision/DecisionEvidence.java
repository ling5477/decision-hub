package com.guidinglight.decisionhub.domain.decision;

import java.util.Objects;

/**
 * Evidence reference used by the frozen GateK contract.
 *
 * <p>The value object carries references and a short structured summary only.
 * It must not contain provider credentials or execution instructions.
 */
public record DecisionEvidence(String evidenceId, String evidenceType, String summary) {

  /** Validates required evidence identifiers. */
  public DecisionEvidence {
    evidenceId = requireText(evidenceId, "evidenceId");
    evidenceType = requireText(evidenceType, "evidenceType");
    summary = summary == null ? "" : summary;
  }

  private static String requireText(final String value, final String field) {
    final String checked = Objects.requireNonNull(value, field).trim();
    if (checked.isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return checked;
  }
}

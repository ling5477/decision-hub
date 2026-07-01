package com.guidinglight.decisionhub.domain.decision;

import java.time.Instant;
import java.util.Objects;

/**
 * Contract shape for a future decision audit event.
 *
 * <p>K1 freezes the structured event shape only. Audit tables and write
 * behavior are intentionally deferred to K3.
 */
public record DecisionAuditEvent(
    String eventId, String requestId, String traceId, String eventType, Instant occurredAt) {

  /** Validates audit event identifiers. */
  public DecisionAuditEvent {
    eventId = requireText(eventId, "eventId");
    requestId = requireText(requestId, "requestId");
    traceId = requireText(traceId, "traceId");
    eventType = requireText(eventType, "eventType");
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

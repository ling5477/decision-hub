package com.guidinglight.decisionhub.domain.decision;

import java.time.Instant;
import java.util.Objects;

/**
 * GateK Decision Pipeline read-only request contract.
 *
 * <p>This value object can only ask DH for a structured read-only
 * recommendation. It intentionally has no account, quantity, price, side,
 * venue credential, or execution fields.
 */
public final class DecisionRequest {

  /** First frozen schema version for GateK K1. */
  public static final String DEFAULT_SCHEMA_VERSION = "1.0.0";

  private final String requestId;
  private final String traceId;
  private final String tenantId;
  private final String source;
  private final DecisionType decisionType;
  private final DecisionSubject subject;
  private final String contextRef;
  private final DecisionContextSnapshot contextSnapshot;
  private final Instant requestedAt;
  private final String schemaVersion;

  private DecisionRequest(
      final String requestId,
      final String traceId,
      final String tenantId,
      final String source,
      final DecisionType decisionType,
      final DecisionSubject subject,
      final String contextRef,
      final DecisionContextSnapshot contextSnapshot,
      final Instant requestedAt,
      final String schemaVersion) {
    this.requestId = requireText(requestId, "requestId");
    this.traceId = requireText(traceId, "traceId");
    this.tenantId = requireText(tenantId, "tenantId");
    this.source = requireText(source, "source");
    this.decisionType = Objects.requireNonNull(decisionType, "decisionType");
    if (this.decisionType != DecisionType.READ_ONLY_RECOMMENDATION) {
      throw new IllegalArgumentException("decisionType must be READ_ONLY_RECOMMENDATION");
    }
    this.subject = Objects.requireNonNull(subject, "subject");
    this.contextRef = contextRef;
    this.contextSnapshot = contextSnapshot;
    this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
    this.schemaVersion = requireText(schemaVersion, "schemaVersion");
  }

  /** Creates a read-only recommendation request with the default schema version. */
  public static DecisionRequest readOnlyRecommendation(
      final String requestId,
      final String traceId,
      final String tenantId,
      final String source,
      final DecisionSubject subject,
      final String contextRef,
      final DecisionContextSnapshot contextSnapshot,
      final Instant requestedAt) {
    return new DecisionRequest(
        requestId,
        traceId,
        tenantId,
        source,
        DecisionType.READ_ONLY_RECOMMENDATION,
        subject,
        contextRef,
        contextSnapshot,
        requestedAt,
        DEFAULT_SCHEMA_VERSION);
  }

  public String getRequestId() {
    return requestId;
  }

  public String getTraceId() {
    return traceId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getSource() {
    return source;
  }

  public DecisionType getDecisionType() {
    return decisionType;
  }

  public DecisionSubject getSubject() {
    return subject;
  }

  public String getContextRef() {
    return contextRef;
  }

  public DecisionContextSnapshot getContextSnapshot() {
    return contextSnapshot;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public String getSchemaVersion() {
    return schemaVersion;
  }

  private static String requireText(final String value, final String field) {
    final String checked = Objects.requireNonNull(value, field).trim();
    if (checked.isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return checked;
  }
}

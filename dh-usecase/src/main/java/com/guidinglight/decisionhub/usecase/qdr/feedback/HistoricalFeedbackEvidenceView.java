package com.guidinglight.decisionhub.usecase.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionDimension;
import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionImpact;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackStatus;
import com.guidinglight.decisionhub.domain.qdr.feedback.ObservedDecisionOutcome;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeSource;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceStatus;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Immutable, safe projection of one complete persisted feedback evidence aggregate. */
public record HistoricalFeedbackEvidenceView(
    String tenantId,
    FeedbackEnvironment environment,
    String decisionId,
    String traceId,
    String observationId,
    String attributionId,
    Instant observedAt,
    OutcomeSource outcomeSource,
    ObservedDecisionOutcome outcomeStatus,
    Instant evaluationTime,
    String policyId,
    String policyVersion,
    FeedbackStatus attributionStatus,
    BigDecimal confidence,
    List<Contribution> contributions,
    List<Reference> references) {

  /** Freezes child lists so callers cannot mutate a page after it is assembled. */
  public HistoricalFeedbackEvidenceView {
    tenantId = Objects.requireNonNull(tenantId, "tenantId");
    environment = Objects.requireNonNull(environment, "environment");
    decisionId = Objects.requireNonNull(decisionId, "decisionId");
    traceId = Objects.requireNonNull(traceId, "traceId");
    observationId = Objects.requireNonNull(observationId, "observationId");
    attributionId = Objects.requireNonNull(attributionId, "attributionId");
    observedAt = Objects.requireNonNull(observedAt, "observedAt");
    outcomeSource = Objects.requireNonNull(outcomeSource, "outcomeSource");
    outcomeStatus = Objects.requireNonNull(outcomeStatus, "outcomeStatus");
    evaluationTime = Objects.requireNonNull(evaluationTime, "evaluationTime");
    policyId = Objects.requireNonNull(policyId, "policyId");
    policyVersion = Objects.requireNonNull(policyVersion, "policyVersion");
    attributionStatus = Objects.requireNonNull(attributionStatus, "attributionStatus");
    confidence = Objects.requireNonNull(confidence, "confidence");
    contributions = List.copyOf(Objects.requireNonNull(contributions, "contributions"));
    references = List.copyOf(Objects.requireNonNull(references, "references"));
  }

  /** Ordered, safe contribution projection without a JDBC surrogate id. */
  public record Contribution(
      AttributionDimension dimension,
      BigDecimal measurement,
      BigDecimal contribution,
      AttributionImpact impact,
      BigDecimal confidence,
      String reasonCode,
      String evidenceReference,
      int sortOrder) {}

  /** Safe reference projection without a JDBC surrogate id or raw payload. */
  public record Reference(ReferenceType type, String value, ReferenceStatus status) {}
}

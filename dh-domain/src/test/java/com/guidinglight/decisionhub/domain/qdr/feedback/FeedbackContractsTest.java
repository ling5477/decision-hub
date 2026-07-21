package com.guidinglight.decisionhub.domain.qdr.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FeedbackContractsTest {

  private static final String HASH = "a".repeat(64);
  private static final Instant EVALUATION_TIME = Instant.parse("2026-07-21T10:00:00Z");

  @Test
  void subjectBindsTenantEnvironmentDecisionAndTrace() {
    final FeedbackSubject subject = subject();

    assertEquals("tenant-a", subject.tenantId());
    assertEquals(FeedbackEnvironment.DEV, subject.environment());
    assertEquals("decision-1", subject.decisionId());
    assertEquals("trace-1", subject.traceId());
  }

  @Test
  void subjectRejectsNullAndBlankScope() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new FeedbackSubject(" ", FeedbackEnvironment.DEV, "decision-1", "trace-1"));
    assertThrows(
        NullPointerException.class,
        () -> new FeedbackSubject("tenant-a", null, "decision-1", "trace-1"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new FeedbackSubject("tenant-a", FeedbackEnvironment.TEST, "", "trace-1"));
  }

  @Test
  void environmentAndSourceRejectUnknownValues() {
    assertThrows(IllegalArgumentException.class, () -> FeedbackEnvironment.valueOf("PROD"));
    assertThrows(IllegalArgumentException.class, () -> OutcomeSource.valueOf("NQ_RUNTIME"));
  }

  @Test
  void confidenceAcceptsClosedBoundsAndRejectsInvalidNumbers() {
    assertEquals(0.0d, new FeedbackConfidence(0.0d).value());
    assertEquals(1.0d, new FeedbackConfidence(1.0d).value());
    assertThrows(IllegalArgumentException.class, () -> new FeedbackConfidence(-0.01d));
    assertThrows(IllegalArgumentException.class, () -> new FeedbackConfidence(1.01d));
    assertThrows(IllegalArgumentException.class, () -> new FeedbackConfidence(Double.NaN));
    assertThrows(
        IllegalArgumentException.class,
        () -> new FeedbackConfidence(Double.POSITIVE_INFINITY));
  }

  @Test
  void observationDefensivelyCopiesAndStablyOrdersCollections() {
    final EnumMap<AttributionDimension, BigDecimal> measurements =
        new EnumMap<>(AttributionDimension.class);
    measurements.put(AttributionDimension.RISK_DISCIPLINE, new BigDecimal("0.5"));
    measurements.put(AttributionDimension.EVIDENCE_QUALITY, new BigDecimal("0.8"));
    final EnumMap<AttributionDimension, String> evidence =
        new EnumMap<>(AttributionDimension.class);
    evidence.put(AttributionDimension.RISK_DISCIPLINE, "SAFE:risk-1");
    evidence.put(AttributionDimension.EVIDENCE_QUALITY, "SAFE:evidence-1");
    final Map<String, String> metadata = new HashMap<>();
    metadata.put("schema", "fixture-v1");

    final OutcomeObservation observation = observation(measurements, evidence, metadata);
    measurements.clear();
    evidence.clear();
    metadata.clear();

    assertEquals(
        List.of(AttributionDimension.EVIDENCE_QUALITY, AttributionDimension.RISK_DISCIPLINE),
        new ArrayList<>(observation.measurements().keySet()));
    assertEquals(2, observation.evidenceReferences().size());
    assertEquals(Map.of("schema", "fixture-v1"), observation.safeMetadata());
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            observation
                .measurements()
                .put(AttributionDimension.OUTCOME_STABILITY, BigDecimal.ONE));
  }

  @Test
  void observationRejectsOutOfRangeMetricAndSensitiveMetadata() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            observation(
                Map.of(AttributionDimension.EVIDENCE_QUALITY, new BigDecimal("1.1")),
                Map.of(AttributionDimension.EVIDENCE_QUALITY, "SAFE:evidence-1"),
                Map.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            observation(
                Map.of(AttributionDimension.EVIDENCE_QUALITY, BigDecimal.ONE),
                Map.of(AttributionDimension.EVIDENCE_QUALITY, "SAFE:evidence-1"),
                Map.of("authorization", "masked")));
  }

  @Test
  void policyDefensivelyCopiesAndRequiresBoundedWindowAndWeights() {
    final EnumSet<OutcomeSource> sources = EnumSet.of(OutcomeSource.DRY_RUN_RESULT);
    final EnumSet<AttributionDimension> dimensions =
        EnumSet.of(AttributionDimension.EVIDENCE_QUALITY);
    final EnumMap<AttributionDimension, BigDecimal> weights =
        new EnumMap<>(AttributionDimension.class);
    weights.put(AttributionDimension.EVIDENCE_QUALITY, BigDecimal.ONE);

    final FeedbackPolicy policy =
        new FeedbackPolicy(
            "policy-1",
            "v1",
            EVALUATION_TIME,
            Duration.ofDays(1),
            sources,
            dimensions,
            weights);
    sources.clear();
    dimensions.clear();
    weights.clear();

    assertEquals(Set.of(OutcomeSource.DRY_RUN_RESULT), policy.allowedSources());
    assertEquals(1, policy.requiredDimensions().size());
    assertEquals(BigDecimal.ONE, policy.dimensionWeights().get(AttributionDimension.EVIDENCE_QUALITY));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new FeedbackPolicy(
                "policy-1",
                "v1",
                EVALUATION_TIME,
                Duration.ZERO,
                EnumSet.of(OutcomeSource.DRY_RUN_RESULT),
                EnumSet.of(AttributionDimension.EVIDENCE_QUALITY),
                Map.of(AttributionDimension.EVIDENCE_QUALITY, BigDecimal.ONE)));
  }

  @Test
  void contributionRequiresImpactToMatchSign() {
    final AttributionContribution contribution = contribution(AttributionDimension.RISK_DISCIPLINE);

    assertEquals(AttributionImpact.POSITIVE, contribution.impact());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AttributionContribution(
                AttributionDimension.RISK_DISCIPLINE,
                BigDecimal.ONE,
                AttributionImpact.NEGATIVE,
                new FeedbackConfidence(1.0d),
                "MEASUREMENT_POSITIVE",
                "SAFE:risk-1"));
  }

  @Test
  void attributionResultSortsContributionsAndKeepsStableEquality() {
    final FeedbackAuditReference audit = auditReference();
    final AttributionResult left =
        new AttributionResult(
            subject(),
            "observation-1",
            FeedbackStatus.ATTRIBUTED,
            List.of(
                contribution(AttributionDimension.RISK_DISCIPLINE),
                contribution(AttributionDimension.EVIDENCE_QUALITY)),
            new FeedbackConfidence(0.8d),
            "policy-1",
            "v1",
            HASH,
            audit);
    final AttributionResult right =
        new AttributionResult(
            subject(),
            "observation-1",
            FeedbackStatus.ATTRIBUTED,
            List.of(
                contribution(AttributionDimension.EVIDENCE_QUALITY),
                contribution(AttributionDimension.RISK_DISCIPLINE)),
            new FeedbackConfidence(0.8d),
            "policy-1",
            "v1",
            HASH,
            audit);

    assertEquals(left, right);
    assertEquals(left.hashCode(), right.hashCode());
    assertEquals(AttributionDimension.EVIDENCE_QUALITY, left.contributions().getFirst().dimension());
    assertNotEquals(FeedbackStatus.INCONCLUSIVE, left.status());
  }

  @Test
  void auditReferenceRejectsSensitiveOrMismatchedHashFields() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new FeedbackAuditReference(
                "tenant-a",
                FeedbackEnvironment.DEV,
                "decision-1",
                "trace-1",
                "observation-1",
                HASH,
                "policy-1",
                "v1",
                HASH,
                "authorization:masked",
                "REPLAY:ref-1"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new FeedbackAuditReference(
                "tenant-a",
                FeedbackEnvironment.DEV,
                "decision-1",
                "trace-1",
                "observation-1",
                "not-a-hash",
                "policy-1",
                "v1",
                HASH,
                "AUDIT:ref-1",
                "REPLAY:ref-1"));
  }

  private static FeedbackSubject subject() {
    return new FeedbackSubject("tenant-a", FeedbackEnvironment.DEV, "decision-1", "trace-1");
  }

  private static OutcomeObservation observation(
      final Map<AttributionDimension, BigDecimal> measurements,
      final Map<AttributionDimension, String> evidence,
      final Map<String, String> metadata) {
    return new OutcomeObservation(
        "tenant-a",
        FeedbackEnvironment.DEV,
        "decision-1",
        "trace-1",
        "observation-1",
        OutcomeSource.DRY_RUN_RESULT,
        ObservedDecisionOutcome.SUCCEEDED,
        EVALUATION_TIME.minusSeconds(60),
        measurements,
        evidence,
        metadata);
  }

  private static AttributionContribution contribution(final AttributionDimension dimension) {
    return new AttributionContribution(
        dimension,
        new BigDecimal("0.8"),
        AttributionImpact.POSITIVE,
        new FeedbackConfidence(0.8d),
        "MEASUREMENT_POSITIVE",
        "SAFE:" + dimension.name());
  }

  private static FeedbackAuditReference auditReference() {
    return new FeedbackAuditReference(
        "tenant-a",
        FeedbackEnvironment.DEV,
        "decision-1",
        "trace-1",
        "observation-1",
        HASH,
        "policy-1",
        "v1",
        HASH,
        "AUDIT:ref-1",
        "REPLAY:ref-1");
  }

}

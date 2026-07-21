package com.guidinglight.decisionhub.usecase.qdr.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionDimension;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.ObservedDecisionOutcome;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeSource;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FeedbackCanonicalizerTest {

  private final FeedbackCanonicalizer canonicalizer = new FeedbackCanonicalizer();

  @Test
  void identicalInputProducesIdenticalCanonicalValueAndHashes() {
    final FeedbackCanonicalHash left =
        canonicalizer.canonicalize(FeedbackAttributionTestSupport.validCommand());
    final FeedbackCanonicalHash right =
        canonicalizer.canonicalize(FeedbackAttributionTestSupport.validCommand());

    assertEquals(left, right);
  }

  @Test
  void mapInsertionOrderDoesNotAffectCanonicalHash() {
    final Map<AttributionDimension, BigDecimal> measurements = new LinkedHashMap<>();
    measurements.put(AttributionDimension.RISK_DISCIPLINE, new BigDecimal("0.60"));
    measurements.put(AttributionDimension.EVIDENCE_QUALITY, new BigDecimal("0.800"));
    final Map<AttributionDimension, String> evidence = new LinkedHashMap<>();
    evidence.put(AttributionDimension.RISK_DISCIPLINE, "SAFE:risk-1");
    evidence.put(AttributionDimension.EVIDENCE_QUALITY, "SAFE:evidence-1");
    final FeedbackAttributionCommand reordered =
        FeedbackAttributionTestSupport.command(
            "tenant-a",
            FeedbackEnvironment.DEV,
            "decision-1",
            "trace-1",
            "observation-1",
            OutcomeSource.DRY_RUN_RESULT,
            ObservedDecisionOutcome.SUCCEEDED,
            measurements,
            evidence);

    assertEquals(
        canonicalizer.canonicalize(FeedbackAttributionTestSupport.validCommand()),
        canonicalizer.canonicalize(reordered));
  }

  @Test
  void changedMetricChangesCanonicalHashButNotFrozenKey() {
    final FeedbackAttributionCommand changed =
        FeedbackAttributionTestSupport.command(
            "tenant-a",
            FeedbackEnvironment.DEV,
            "decision-1",
            "trace-1",
            "observation-1",
            OutcomeSource.DRY_RUN_RESULT,
            ObservedDecisionOutcome.SUCCEEDED,
            Map.of(
                AttributionDimension.EVIDENCE_QUALITY, new BigDecimal("0.9"),
                AttributionDimension.RISK_DISCIPLINE, new BigDecimal("0.6")),
            Map.of(
                AttributionDimension.EVIDENCE_QUALITY, "SAFE:evidence-1",
                AttributionDimension.RISK_DISCIPLINE, "SAFE:risk-1"));
    final FeedbackCanonicalHash baseline =
        canonicalizer.canonicalize(FeedbackAttributionTestSupport.validCommand());
    final FeedbackCanonicalHash different = canonicalizer.canonicalize(changed);

    assertNotEquals(baseline.value(), different.value());
    assertEquals(baseline.idempotencyKey(), different.idempotencyKey());
  }

  @Test
  void tenantAndEnvironmentArePartOfIdempotencyIsolationKey() {
    final FeedbackCanonicalHash baseline =
        canonicalizer.canonicalize(FeedbackAttributionTestSupport.validCommand());
    final FeedbackAttributionCommand otherTenant =
        FeedbackAttributionTestSupport.command(
            "tenant-b",
            FeedbackEnvironment.DEV,
            "decision-1",
            "trace-1",
            "observation-1",
            OutcomeSource.DRY_RUN_RESULT,
            ObservedDecisionOutcome.SUCCEEDED,
            Map.of(
                AttributionDimension.EVIDENCE_QUALITY, new BigDecimal("0.8"),
                AttributionDimension.RISK_DISCIPLINE, new BigDecimal("0.6")),
            Map.of(
                AttributionDimension.EVIDENCE_QUALITY, "SAFE:evidence-1",
                AttributionDimension.RISK_DISCIPLINE, "SAFE:risk-1"));
    final FeedbackAttributionCommand otherEnvironment =
        FeedbackAttributionTestSupport.command(
            "tenant-a",
            FeedbackEnvironment.TEST,
            "decision-1",
            "trace-1",
            "observation-1",
            OutcomeSource.DRY_RUN_RESULT,
            ObservedDecisionOutcome.SUCCEEDED,
            Map.of(
                AttributionDimension.EVIDENCE_QUALITY, new BigDecimal("0.8"),
                AttributionDimension.RISK_DISCIPLINE, new BigDecimal("0.6")),
            Map.of(
                AttributionDimension.EVIDENCE_QUALITY, "SAFE:evidence-1",
                AttributionDimension.RISK_DISCIPLINE, "SAFE:risk-1"));

    assertNotEquals(
        baseline.idempotencyKey(), canonicalizer.canonicalize(otherTenant).idempotencyKey());
    assertNotEquals(
        baseline.idempotencyKey(), canonicalizer.canonicalize(otherEnvironment).idempotencyKey());
  }
}

package com.guidinglight.decisionhub.usecase.qdr.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionDimension;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackPolicy;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackStatus;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackSubject;
import com.guidinglight.decisionhub.domain.qdr.feedback.ObservedDecisionOutcome;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeObservation;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeSource;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultFeedbackAttributionServiceTest {

  @Test
  void attributesMultiDimensionInputDeterministicallyAndAuditsSafeReferences() {
    final FeedbackAttributionTestSupport.BoundedIdempotencyPort idempotency =
        new FeedbackAttributionTestSupport.BoundedIdempotencyPort(8);
    final FeedbackAttributionTestSupport.RecordingAuditPort audit =
        new FeedbackAttributionTestSupport.RecordingAuditPort();
    final DefaultFeedbackAttributionService service =
        FeedbackAttributionTestSupport.service(idempotency, audit);

    final FeedbackAttributionResult first =
        service.attribute(FeedbackAttributionTestSupport.validCommand());
    final FeedbackAttributionResult second =
        service.attribute(FeedbackAttributionTestSupport.validCommand());

    assertEquals(FeedbackStatus.ATTRIBUTED, first.status());
    assertEquals(FeedbackAttributionErrorCode.NONE, first.errorCode());
    assertNotNull(first.attributionResult());
    assertEquals(first, second);
    assertEquals(
        AttributionDimension.EVIDENCE_QUALITY,
        first.attributionResult().contributions().getFirst().dimension());
    assertEquals(2, first.attributionResult().contributions().size());
    assertTrue(first.attributionResult().auditReference().auditReference().startsWith("AUDIT:"));
    assertTrue(first.attributionResult().auditReference().replayReference().startsWith("REPLAY:"));
    assertEquals(1, audit.writeCount());
    assertEquals(1, idempotency.size());
  }

  @Test
  void missingMandatoryEvidenceReturnsAuditedInconclusive() {
    final FeedbackAttributionTestSupport.RecordingAuditPort audit =
        new FeedbackAttributionTestSupport.RecordingAuditPort();
    final DefaultFeedbackAttributionService service =
        FeedbackAttributionTestSupport.service(
            new FeedbackAttributionTestSupport.BoundedIdempotencyPort(8), audit);
    final FeedbackAttributionCommand command =
        FeedbackAttributionTestSupport.command(
            "tenant-a",
            FeedbackEnvironment.DEV,
            "decision-1",
            "trace-1",
            "observation-1",
            OutcomeSource.DRY_RUN_RESULT,
            ObservedDecisionOutcome.SUCCEEDED,
            Map.of(
                AttributionDimension.EVIDENCE_QUALITY, new BigDecimal("0.8"),
                AttributionDimension.RISK_DISCIPLINE, new BigDecimal("0.6")),
            Map.of(AttributionDimension.EVIDENCE_QUALITY, "SAFE:evidence-1"));

    final FeedbackAttributionResult result = service.attribute(command);

    assertEquals(FeedbackStatus.INCONCLUSIVE, result.status());
    assertEquals(FeedbackAttributionErrorCode.EVIDENCE_INCOMPLETE, result.errorCode());
    assertNotNull(result.attributionResult());
    assertEquals(1, audit.writeCount());
  }

  @Test
  void contradictoryOutcomeReturnsAuditedRejectedWithoutGuessing() {
    final FeedbackAttributionTestSupport.RecordingAuditPort audit =
        new FeedbackAttributionTestSupport.RecordingAuditPort();
    final DefaultFeedbackAttributionService service =
        FeedbackAttributionTestSupport.service(
            new FeedbackAttributionTestSupport.BoundedIdempotencyPort(8), audit);
    final FeedbackAttributionCommand command =
        FeedbackAttributionTestSupport.command(
            "tenant-a",
            FeedbackEnvironment.DEV,
            "decision-1",
            "trace-1",
            "observation-1",
            OutcomeSource.DRY_RUN_RESULT,
            ObservedDecisionOutcome.FAILED,
            Map.of(
                AttributionDimension.EVIDENCE_QUALITY, new BigDecimal("0.8"),
                AttributionDimension.RISK_DISCIPLINE, new BigDecimal("0.6")),
            Map.of(
                AttributionDimension.EVIDENCE_QUALITY, "SAFE:evidence-1",
                AttributionDimension.RISK_DISCIPLINE, "SAFE:risk-1"));

    final FeedbackAttributionResult result = service.attribute(command);

    assertEquals(FeedbackStatus.REJECTED, result.status());
    assertEquals(FeedbackAttributionErrorCode.ATTRIBUTION_CONTRADICTION, result.errorCode());
    assertNotNull(result.attributionResult());
    assertEquals(1, audit.writeCount());
  }

  @Test
  void policyDeniedSourceReturnsAuditedRejected() {
    final FeedbackAttributionCommand baseline = FeedbackAttributionTestSupport.validCommand();
    final OutcomeObservation observation =
        new OutcomeObservation(
            baseline.observation().tenantId(),
            baseline.observation().environment(),
            baseline.observation().decisionId(),
            baseline.observation().traceId(),
            baseline.observation().observationId(),
            OutcomeSource.DETERMINISTIC_REPLAY,
            baseline.observation().outcome(),
            baseline.observation().observedAt(),
            baseline.observation().measurements(),
            baseline.observation().evidenceReferences(),
            baseline.observation().safeMetadata());
    final FeedbackPolicy restricted =
        new FeedbackPolicy(
            "policy-1",
            "v1",
            FeedbackAttributionTestSupport.EVALUATION_TIME,
            Duration.ofHours(1),
            EnumSet.of(OutcomeSource.DRY_RUN_RESULT),
            baseline.policy().requiredDimensions(),
            baseline.policy().dimensionWeights());
    final FeedbackAttributionTestSupport.RecordingAuditPort audit =
        new FeedbackAttributionTestSupport.RecordingAuditPort();
    final DefaultFeedbackAttributionService service =
        FeedbackAttributionTestSupport.service(
            new FeedbackAttributionTestSupport.BoundedIdempotencyPort(8), audit);

    final FeedbackAttributionResult result =
        service.attribute(new FeedbackAttributionCommand(baseline.subject(), observation, restricted));

    assertEquals(FeedbackStatus.REJECTED, result.status());
    assertEquals(FeedbackAttributionErrorCode.OUTCOME_SOURCE_DENIED, result.errorCode());
    assertNotNull(result.attributionResult());
    assertEquals(1, audit.writeCount());
  }

  @Test
  void rejectsTenantEnvironmentDecisionAndTraceMismatchesBeforeAudit() {
    assertPreAuditMismatch("tenant-b", FeedbackEnvironment.DEV, "decision-1", "trace-1",
        FeedbackAttributionErrorCode.TENANT_SCOPE_MISMATCH);
    assertPreAuditMismatch("tenant-a", FeedbackEnvironment.TEST, "decision-1", "trace-1",
        FeedbackAttributionErrorCode.ENVIRONMENT_SCOPE_MISMATCH);
    assertPreAuditMismatch("tenant-a", FeedbackEnvironment.DEV, "decision-2", "trace-1",
        FeedbackAttributionErrorCode.DECISION_REFERENCE_INVALID);
    assertPreAuditMismatch("tenant-a", FeedbackEnvironment.DEV, "decision-1", "trace-2",
        FeedbackAttributionErrorCode.TRACE_REFERENCE_INVALID);
  }

  @Test
  void rejectsFutureAndExpiredObservationUsingExplicitPolicyTime() {
    final FeedbackAttributionCommand baseline = FeedbackAttributionTestSupport.validCommand();
    final DefaultFeedbackAttributionService service =
        FeedbackAttributionTestSupport.service(
            new FeedbackAttributionTestSupport.BoundedIdempotencyPort(8),
            new FeedbackAttributionTestSupport.RecordingAuditPort());

    assertEquals(
        FeedbackAttributionErrorCode.INVALID_OBSERVATION,
        service.attribute(withObservedAt(baseline, FeedbackAttributionTestSupport.EVALUATION_TIME.plusSeconds(1)))
            .errorCode());
    assertEquals(
        FeedbackAttributionErrorCode.INVALID_OBSERVATION,
        service.attribute(withObservedAt(baseline, FeedbackAttributionTestSupport.EVALUATION_TIME.minusSeconds(3601)))
            .errorCode());
  }

  @Test
  void auditFailureIsFailClosedAndDoesNotCacheSuccess() {
    final FeedbackAttributionTestSupport.BoundedIdempotencyPort idempotency =
        new FeedbackAttributionTestSupport.BoundedIdempotencyPort(8);
    final DefaultFeedbackAttributionService service =
        FeedbackAttributionTestSupport.service(
            idempotency, new FeedbackAttributionTestSupport.RecordingAuditPort(true));

    final FeedbackAttributionResult result =
        service.attribute(FeedbackAttributionTestSupport.validCommand());

    assertEquals(FeedbackStatus.REJECTED, result.status());
    assertEquals(FeedbackAttributionErrorCode.AUDIT_FAILED, result.errorCode());
    assertNull(result.attributionResult());
    assertEquals(0, idempotency.size());
  }

  @Test
  void idempotencyUnavailableIsFailClosedWithoutAudit() {
    final FeedbackAttributionTestSupport.RecordingAuditPort audit =
        new FeedbackAttributionTestSupport.RecordingAuditPort();
    final DefaultFeedbackAttributionService service =
        FeedbackAttributionTestSupport.service(
            new FeedbackAttributionTestSupport.BoundedIdempotencyPort(1, true), audit);

    final FeedbackAttributionResult result =
        service.attribute(FeedbackAttributionTestSupport.validCommand());

    assertEquals(FeedbackAttributionErrorCode.IDEMPOTENCY_UNAVAILABLE, result.errorCode());
    assertEquals(FeedbackStatus.REJECTED, result.status());
    assertEquals(0, audit.writeCount());
  }

  @Test
  void sameKeyDifferentCanonicalHashIsRejectedAsConflict() {
    final FeedbackAttributionTestSupport.RecordingAuditPort audit =
        new FeedbackAttributionTestSupport.RecordingAuditPort();
    final DefaultFeedbackAttributionService service =
        FeedbackAttributionTestSupport.service(
            new FeedbackAttributionTestSupport.BoundedIdempotencyPort(8), audit);
    service.attribute(FeedbackAttributionTestSupport.validCommand());
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

    final FeedbackAttributionResult result = service.attribute(changed);

    assertEquals(FeedbackAttributionErrorCode.IDEMPOTENCY_CONFLICT, result.errorCode());
    assertEquals(FeedbackStatus.REJECTED, result.status());
    assertEquals(1, audit.writeCount());
  }

  @Test
  void nullCommandAndUnexpectedPortFailureAreSanitized() {
    final FeedbackAttributionTestSupport.RecordingAuditPort audit =
        new FeedbackAttributionTestSupport.RecordingAuditPort();
    final DefaultFeedbackAttributionService nullService =
        FeedbackAttributionTestSupport.service(
            new FeedbackAttributionTestSupport.BoundedIdempotencyPort(8), audit);
    assertEquals(
        FeedbackAttributionErrorCode.INVALID_FEEDBACK_SUBJECT,
        nullService.attribute(null).errorCode());

    final DefaultFeedbackAttributionService failedService =
        new DefaultFeedbackAttributionService(
            new FeedbackCanonicalizer(),
            new FeedbackAttributionPolicyEvaluator(),
            (key, hash, first) -> {
              throw new IllegalStateException("raw internal detail");
            },
            audit);
    final FeedbackAttributionResult failed =
        failedService.attribute(FeedbackAttributionTestSupport.validCommand());
    assertEquals(FeedbackAttributionErrorCode.ATTRIBUTION_FAILED, failed.errorCode());
    assertEquals("feedback attribution failed closed", failed.safeMessage());
  }

  private static void assertPreAuditMismatch(
      final String observationTenant,
      final FeedbackEnvironment observationEnvironment,
      final String observationDecision,
      final String observationTrace,
      final FeedbackAttributionErrorCode expected) {
    final FeedbackAttributionCommand baseline = FeedbackAttributionTestSupport.validCommand();
    final OutcomeObservation observation =
        new OutcomeObservation(
            observationTenant,
            observationEnvironment,
            observationDecision,
            observationTrace,
            baseline.observation().observationId(),
            baseline.observation().source(),
            baseline.observation().outcome(),
            baseline.observation().observedAt(),
            baseline.observation().measurements(),
            baseline.observation().evidenceReferences(),
            baseline.observation().safeMetadata());
    final FeedbackAttributionTestSupport.RecordingAuditPort audit =
        new FeedbackAttributionTestSupport.RecordingAuditPort();
    final DefaultFeedbackAttributionService service =
        FeedbackAttributionTestSupport.service(
            new FeedbackAttributionTestSupport.BoundedIdempotencyPort(8), audit);

    final FeedbackAttributionResult result =
        service.attribute(
            new FeedbackAttributionCommand(baseline.subject(), observation, baseline.policy()));

    assertEquals(expected, result.errorCode());
    assertEquals(FeedbackStatus.REJECTED, result.status());
    assertEquals(0, audit.writeCount());
  }

  private static FeedbackAttributionCommand withObservedAt(
      final FeedbackAttributionCommand baseline, final java.time.Instant observedAt) {
    final OutcomeObservation observation =
        new OutcomeObservation(
            baseline.observation().tenantId(),
            baseline.observation().environment(),
            baseline.observation().decisionId(),
            baseline.observation().traceId(),
            baseline.observation().observationId(),
            baseline.observation().source(),
            baseline.observation().outcome(),
            observedAt,
            baseline.observation().measurements(),
            baseline.observation().evidenceReferences(),
            baseline.observation().safeMetadata());
    return new FeedbackAttributionCommand(baseline.subject(), observation, baseline.policy());
  }
}

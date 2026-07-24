package com.guidinglight.decisionhub.usecase.qdr.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionDimension;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackStatus;
import com.guidinglight.decisionhub.domain.qdr.feedback.ObservedDecisionOutcome;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeSource;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.AttributionReferenceRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.FeedbackAttributionRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.FeedbackPersistenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.OutcomeObservationRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.PersistenceRowIdentities;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceStatus;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FeedbackPersistenceRecordsTest {

  private static final String HASH_A = "a".repeat(64);

  @Test
  void mapsAuditedStageQdr8ResultIntoTenantEnvironmentBoundAggregate() {
    final FeedbackPersistenceAggregate aggregate = validAggregate();

    assertEquals("tenant-a", aggregate.observation().tenantId());
    assertEquals(FeedbackEnvironment.DEV, aggregate.observation().environment());
    assertEquals(
        aggregate.observation().canonicalHash(), aggregate.attribution().canonicalHash());
    assertEquals(
        aggregate.observation().observationId(), aggregate.attribution().observationId());
    assertEquals(2, aggregate.contributions().size());
    assertEquals(4, aggregate.references().size());
    assertTrue(
        aggregate.references().stream()
            .allMatch(reference -> reference.referenceStatus() == ReferenceStatus.ACTIVE));
    assertTrue(
        aggregate.references().stream()
            .anyMatch(reference -> reference.referenceValue().startsWith("audit:")));
    assertTrue(
        aggregate.references().stream()
            .anyMatch(reference -> reference.referenceValue().startsWith("replay:")));
  }

  @Test
  void contributionOrderingIsStableAndCollectionsAreImmutable() {
    final FeedbackPersistenceAggregate aggregate = validAggregate();

    assertEquals(0, aggregate.contributions().get(0).sortOrder());
    assertEquals(1, aggregate.contributions().get(1).sortOrder());
    assertEquals(
        AttributionDimension.EVIDENCE_QUALITY,
        aggregate.contributions().get(0).dimension());
    assertThrows(
        UnsupportedOperationException.class,
        () -> aggregate.contributions().add(aggregate.contributions().getFirst()));
    assertThrows(
        UnsupportedOperationException.class,
        () -> aggregate.references().clear());
  }

  @Test
  void rejectsBlankOrOversizedIdentifiersAndInvalidObservationTime() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            observation(
                " ",
                FeedbackEnvironment.DEV,
                Instant.parse("2026-07-21T09:59:00Z"),
                Instant.parse("2026-07-21T10:00:00Z")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            observation(
                "x".repeat(129),
                FeedbackEnvironment.DEV,
                Instant.parse("2026-07-21T09:59:00Z"),
                Instant.parse("2026-07-21T10:00:00Z")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            observation(
                "tenant-a",
                FeedbackEnvironment.DEV,
                Instant.parse("2026-07-21T10:00:01Z"),
                Instant.parse("2026-07-21T10:00:00Z")));
  }

  @Test
  void rejectsMissingEnvironmentAndOutOfRangeConfidence() {
    assertThrows(
        NullPointerException.class,
        () ->
            observation(
                "tenant-a",
                null,
                Instant.parse("2026-07-21T09:59:00Z"),
                Instant.parse("2026-07-21T10:00:00Z")));
    assertThrows(
        IllegalArgumentException.class,
        () -> attribution(new BigDecimal("1.00001")));
    assertThrows(
        IllegalArgumentException.class,
        () -> attribution(new BigDecimal("-0.00001")));
  }

  @Test
  void rejectsSensitiveOrTradingReferenceMaterialAndInvalidScheme() {
    assertThrows(
        IllegalArgumentException.class,
        () -> reference(ReferenceType.EVIDENCE, "Authorization:secret"));
    assertThrows(
        IllegalArgumentException.class,
        () -> reference(ReferenceType.EVIDENCE, "BUY"));
    assertThrows(
        IllegalArgumentException.class,
        () -> reference(ReferenceType.AUDIT, "replay-case:123"));
  }

  @Test
  void rejectsRowIdentityCardinalityAndDuplication() {
    final FeedbackAttributionCommand command =
        FeedbackAttributionTestSupport.validCommand();
    final FeedbackCanonicalHash canonical = new FeedbackCanonicalizer().canonicalize(command);
    final FeedbackAttributionResult result = auditedResult(command);
    final UUID duplicate = UUID.fromString("00000000-0000-0000-0000-000000000001");

    assertThrows(
        IllegalArgumentException.class,
        () ->
            FeedbackPersistenceRecords.from(
                command,
                canonical,
                result,
                new PersistenceRowIdentities(
                    duplicate,
                    duplicate,
                    List.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000003"),
                        UUID.fromString("00000000-0000-0000-0000-000000000004")),
                    referenceIds(result))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            FeedbackPersistenceRecords.from(
                command,
                canonical,
                result,
                new PersistenceRowIdentities(
                    UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    UUID.fromString("00000000-0000-0000-0000-000000000002"),
                    List.of(),
                    referenceIds(result))));
  }

  @Test
  void rejectsCanonicalOrSourceCorrelationMismatchBeforeSql() {
    final FeedbackAttributionCommand command =
        FeedbackAttributionTestSupport.validCommand();
    final FeedbackAttributionResult result = auditedResult(command);
    final FeedbackCanonicalHash mismatched =
        new FeedbackCanonicalHash("safe", "b".repeat(64), "c".repeat(64));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            FeedbackPersistenceRecords.from(
                command, mismatched, result, identities(result)));
  }

  @Test
  void exceptionUsesStableCodeAndBoundedSafeMessage() {
    final FeedbackPersistenceException error =
        new FeedbackPersistenceException(
            FeedbackPersistenceErrorCode.COMMIT_OUTCOME_UNKNOWN,
            "feedback commit outcome cannot be proven");

    assertEquals(
        FeedbackPersistenceErrorCode.COMMIT_OUTCOME_UNKNOWN, error.errorCode());
    assertFalse(error.getMessage().isBlank());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new FeedbackPersistenceException(
                FeedbackPersistenceErrorCode.PERSISTENCE_FAILURE, "x".repeat(257)));
  }

  private static FeedbackPersistenceAggregate validAggregate() {
    final FeedbackAttributionCommand command =
        FeedbackAttributionTestSupport.validCommand();
    final FeedbackCanonicalHash canonical = new FeedbackCanonicalizer().canonicalize(command);
    final FeedbackAttributionResult result = auditedResult(command);
    return FeedbackPersistenceRecords.from(
        command, canonical, result, identities(result));
  }

  private static FeedbackAttributionResult auditedResult(
      final FeedbackAttributionCommand command) {
    final DefaultFeedbackAttributionService service =
        FeedbackAttributionTestSupport.service(
            new FeedbackAttributionTestSupport.BoundedIdempotencyPort(8),
            new FeedbackAttributionTestSupport.RecordingAuditPort());
    final FeedbackAttributionResult result = service.attribute(command);
    assertEquals(FeedbackStatus.ATTRIBUTED, result.status());
    return result;
  }

  private static PersistenceRowIdentities identities(
      final FeedbackAttributionResult result) {
    return new PersistenceRowIdentities(
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
        UUID.fromString("00000000-0000-0000-0000-000000000002"),
        List.of(
            UUID.fromString("00000000-0000-0000-0000-000000000003"),
            UUID.fromString("00000000-0000-0000-0000-000000000004")),
        referenceIds(result));
  }

  private static List<UUID> referenceIds(
      final FeedbackAttributionResult result) {
    final int count = FeedbackPersistenceRecords.requiredReferenceCount(result);
    final List<UUID> values = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      values.add(
          UUID.fromString(
              "00000000-0000-0000-0000-" + String.format("%012d", index + 10)));
    }
    return List.copyOf(values);
  }

  private static OutcomeObservationRecord observation(
      final String tenantId,
      final FeedbackEnvironment environment,
      final Instant observedAt,
      final Instant evaluationTime) {
    return new OutcomeObservationRecord(
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
        tenantId,
        environment,
        "decision-1",
        "trace-1",
        "observation-1",
        HASH_A,
        OutcomeSource.DRY_RUN_RESULT,
        ObservedDecisionOutcome.SUCCEEDED,
        observedAt,
        evaluationTime,
        HASH_A);
  }

  private static FeedbackAttributionRecord attribution(
      final BigDecimal confidence) {
    return new FeedbackAttributionRecord(
        UUID.fromString("00000000-0000-0000-0000-000000000002"),
        "tenant-a",
        FeedbackEnvironment.DEV,
        "observation-1",
        "decision-1",
        "trace-1",
        Instant.parse("2026-07-21T09:59:00Z"),
        HASH_A,
        "policy-1",
        "v1",
        FeedbackStatus.ATTRIBUTED,
        confidence,
        HASH_A,
        FeedbackAttributionErrorCode.NONE);
  }

  private static AttributionReferenceRecord reference(
      final ReferenceType type, final String value) {
    return new AttributionReferenceRecord(
        UUID.fromString("00000000-0000-0000-0000-000000000010"),
        "tenant-a",
        FeedbackEnvironment.DEV,
        HASH_A,
        type,
        value,
        ReferenceStatus.ACTIVE);
  }
}

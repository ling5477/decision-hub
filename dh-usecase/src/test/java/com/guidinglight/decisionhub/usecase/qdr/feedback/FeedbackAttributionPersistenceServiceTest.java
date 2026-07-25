package com.guidinglight.decisionhub.usecase.qdr.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionDimension;
import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionImpact;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackStatus;
import com.guidinglight.decisionhub.domain.qdr.feedback.ObservedDecisionOutcome;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeSource;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackAttributionPersistenceService.PersistenceStatus;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.AttributionContributionRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.AttributionReferenceRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.FeedbackAttributionRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.FeedbackPersistenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.OutcomeObservationRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceStatus;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceType;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** B2 persistence service 的无 Spring/JDBC 依赖行为契约。 */
class FeedbackAttributionPersistenceServiceTest {

  private static final Instant OBSERVED_AT = Instant.parse("2026-07-25T09:59:00.123456789Z");
  private static final Instant EVALUATION_TIME = Instant.parse("2026-07-25T10:00:00Z");

  @Test
  void createsThenReusesTheCompleteAggregateAndClassifiesCanonicalHashConflict() {
    final RecordingRepository repository = new RecordingRepository();
    final FeedbackAttributionPersistenceService service = service(repository, directBoundary());
    final FeedbackPersistenceAggregate original = aggregate("tenant-a", FeedbackEnvironment.DEV, hash('a'), hash('b'));

    final var created = service.persist(original);
    final var reused = service.persist(original);
    final var conflict =
        service.persist(aggregate("tenant-a", FeedbackEnvironment.DEV, hash('a'), hash('c')));

    assertEquals(PersistenceStatus.CREATED, created.status());
    assertEquals(original, created.aggregate());
    assertEquals(PersistenceStatus.REUSED, reused.status());
    assertEquals(original, reused.aggregate());
    assertEquals(PersistenceStatus.IDEMPOTENCY_CONFLICT, conflict.status());
    assertNull(conflict.aggregate());
    assertEquals(1, repository.aggregateCount());
  }

  @Test
  void duplicateKeyIsReconciledAfterRollbackWithoutAnInMemoryIdempotencyShortcut() {
    final RecordingRepository repository = new RecordingRepository();
    repository.duplicateOnFirstObservationWrite = true;
    final FeedbackAttributionPersistenceService service = service(repository, directBoundary());
    final FeedbackPersistenceAggregate aggregate =
        aggregate("tenant-a", FeedbackEnvironment.DEV, hash('a'), hash('b'));

    final var result = service.persist(aggregate);

    assertEquals(PersistenceStatus.REUSED, result.status());
    assertEquals(aggregate, result.aggregate());
    assertEquals(1, repository.observationWrites.get());
    assertEquals(1, repository.aggregateCount());
  }

  @Test
  void rejectsInvalidEvidenceReferenceBeforeAnyPersistenceWrite() {
    final RecordingRepository repository = new RecordingRepository();
    final FeedbackAttributionPersistenceService service =
        new FeedbackAttributionPersistenceService(
            repository,
            (reference, observation) -> {
              if (reference.referenceType() == ReferenceType.EVIDENCE) {
                throw new FeedbackPersistenceException(
                    FeedbackPersistenceErrorCode.REFERENCE_INVALID, "evidence is unavailable");
              }
            },
            directBoundary());

    final FeedbackPersistenceException error =
        assertThrows(
            FeedbackPersistenceException.class,
            () -> service.persist(aggregate("tenant-a", FeedbackEnvironment.DEV, hash('a'), hash('b'))));
    assertEquals(FeedbackPersistenceErrorCode.REFERENCE_INVALID, error.errorCode());
    assertEquals(0, repository.aggregateCount());
    assertEquals(0, repository.observationWrites.get());
  }

  @Test
  void commitUnknownDoesNotReturnSuccessOrRetryAndOnlySupportsScopedReadOnlyReconciliation() {
    final RecordingRepository repository = new RecordingRepository();
    final AtomicInteger transactionCalls = new AtomicInteger();
    final FeedbackPersistenceTransactionBoundary commitUnknownBoundary =
        new FeedbackPersistenceTransactionBoundary() {
          @Override
          public <T> T requiredRepeatableRead(final java.util.function.Supplier<T> action) {
            if (transactionCalls.incrementAndGet() == 1) {
              action.get();
              throw new FeedbackPersistenceException(
                  FeedbackPersistenceErrorCode.COMMIT_OUTCOME_UNKNOWN,
                  "simulated commit outcome cannot be proven");
            }
            return action.get();
          }
        };
    final FeedbackAttributionPersistenceService service = service(repository, commitUnknownBoundary);
    final FeedbackPersistenceAggregate aggregate =
        aggregate("tenant-a", FeedbackEnvironment.DEV, hash('a'), hash('b'));

    final var unknown = service.persist(aggregate);
    final var reconciled = service.reconcileReadOnly(unknown.reconciliationReference());

    assertEquals(PersistenceStatus.COMMIT_OUTCOME_UNKNOWN, unknown.status());
    assertNull(unknown.aggregate());
    assertEquals(2, transactionCalls.get());
    assertEquals(1, repository.observationWrites.get());
    assertEquals(PersistenceStatus.REUSED, reconciled.status());
    assertEquals(aggregate, reconciled.aggregate());
  }

  private static FeedbackAttributionPersistenceService service(
      final RecordingRepository repository,
      final FeedbackPersistenceTransactionBoundary transactionBoundary) {
    return new FeedbackAttributionPersistenceService(repository, (reference, observation) -> {}, transactionBoundary);
  }

  private static FeedbackPersistenceTransactionBoundary directBoundary() {
    return new FeedbackPersistenceTransactionBoundary() {
      @Override
      public <T> T requiredRepeatableRead(final java.util.function.Supplier<T> action) {
        return action.get();
      }
    };
  }

  private static FeedbackPersistenceAggregate aggregate(
      final String tenantId,
      final FeedbackEnvironment environment,
      final String idempotencyKey,
      final String canonicalHash) {
    final String correlation = tenantId + environment + idempotencyKey + canonicalHash;
    final String attributionId = hash((char) ('d' + (canonicalHash.charAt(0) % 2)));
    final OutcomeObservationRecord observation =
        new OutcomeObservationRecord(
            uuid("observation-" + correlation),
            tenantId,
            environment,
            "decision-" + tenantId,
            "trace-" + tenantId,
            "observation-" + canonicalHash.charAt(0),
            idempotencyKey,
            OutcomeSource.DRY_RUN_RESULT,
            ObservedDecisionOutcome.SUCCEEDED,
            OBSERVED_AT,
            EVALUATION_TIME,
            canonicalHash);
    final FeedbackAttributionRecord attribution =
        new FeedbackAttributionRecord(
            uuid("attribution-" + correlation),
            tenantId,
            environment,
            observation.observationId(),
            observation.decisionId(),
            observation.traceId(),
            OBSERVED_AT,
            attributionId,
            "policy-1",
            "v1",
            FeedbackStatus.ATTRIBUTED,
            new BigDecimal("0.80000"),
            canonicalHash,
            FeedbackAttributionErrorCode.NONE);
    final AttributionContributionRecord contribution =
        new AttributionContributionRecord(
            uuid("contribution-" + correlation),
            tenantId,
            environment,
            attributionId,
            AttributionDimension.EVIDENCE_QUALITY,
            new BigDecimal("0.80000"),
            new BigDecimal("0.60000"),
            AttributionImpact.POSITIVE,
            new BigDecimal("0.80000"),
            "MEASUREMENT_POSITIVE",
            "safe-evidence",
            0);
    return new FeedbackPersistenceAggregate(
        observation,
        attribution,
        List.of(contribution),
        List.of(
            reference("audit-" + correlation, tenantId, environment, attributionId, ReferenceType.AUDIT, "audit:event-1"),
            reference(
                "replay-" + correlation,
                tenantId,
                environment,
                attributionId,
                ReferenceType.REPLAY,
                "replay-case:" + uuid("replay-" + correlation)),
            reference(
                "evidence-" + correlation,
                tenantId,
                environment,
                attributionId,
                ReferenceType.EVIDENCE,
                "evidence:safe-evidence")));
  }

  private static AttributionReferenceRecord reference(
      final String id,
      final String tenantId,
      final FeedbackEnvironment environment,
      final String attributionId,
      final ReferenceType type,
      final String value) {
    return new AttributionReferenceRecord(
        uuid(id), tenantId, environment, attributionId, type, value, ReferenceStatus.ACTIVE);
  }

  private static UUID uuid(final String value) {
    return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String hash(final char value) {
    return String.valueOf(value).repeat(64);
  }

  private static final class RecordingRepository implements FeedbackAttributionRepository {

    private final Map<String, FeedbackPersistenceAggregate> aggregates = new HashMap<>();
    private final AtomicInteger observationWrites = new AtomicInteger();
    private boolean duplicateOnFirstObservationWrite;
    private OutcomeObservationRecord observation;
    private FeedbackAttributionRecord attribution;
    private List<AttributionContributionRecord> contributions;

    @Override
    public Optional<FeedbackPersistenceAggregate> findByIdempotencyKey(
        final String tenantId,
        final FeedbackEnvironment environment,
        final String idempotencyKey) {
      return Optional.ofNullable(aggregates.get(key(tenantId, environment, idempotencyKey)));
    }

    @Override
    public Optional<FeedbackPersistenceAggregate> findByAttributionId(
        final String tenantId,
        final FeedbackEnvironment environment,
        final String attributionId) {
      return aggregates.values().stream()
          .filter(
              aggregate ->
                  aggregate.attribution().tenantId().equals(tenantId)
                      && aggregate.attribution().environment() == environment
                      && aggregate.attribution().attributionId().equals(attributionId))
          .findFirst();
    }

    @Override
    public void insertObservation(final OutcomeObservationRecord value) {
      observationWrites.incrementAndGet();
      observation = value;
      if (duplicateOnFirstObservationWrite) {
        duplicateOnFirstObservationWrite = false;
        aggregates.put(
            key(value.tenantId(), value.environment(), value.idempotencyKey()),
            new FeedbackPersistenceAggregate(
                value,
                attributionFor(value),
                List.of(contributionFor(value)),
                referencesFor(value)));
        throw new FeedbackPersistenceException(
            FeedbackPersistenceErrorCode.DUPLICATE_KEY, "simulated duplicate key");
      }
    }

    @Override
    public void insertAttribution(final FeedbackAttributionRecord value) {
      attribution = value;
    }

    @Override
    public void insertContributions(final List<AttributionContributionRecord> value) {
      contributions = List.copyOf(value);
    }

    @Override
    public void insertReferences(final List<AttributionReferenceRecord> references) {
      aggregates.put(
          key(observation.tenantId(), observation.environment(), observation.idempotencyKey()),
          new FeedbackPersistenceAggregate(observation, attribution, contributions, references));
    }

    private int aggregateCount() {
      return aggregates.size();
    }

    private static String key(
        final String tenantId, final FeedbackEnvironment environment, final String idempotencyKey) {
      return tenantId + "\u0000" + environment + "\u0000" + idempotencyKey;
    }

    private static FeedbackAttributionRecord attributionFor(final OutcomeObservationRecord observation) {
      return aggregate(
              observation.tenantId(),
              observation.environment(),
              observation.idempotencyKey(),
              observation.canonicalHash())
          .attribution();
    }

    private static AttributionContributionRecord contributionFor(
        final OutcomeObservationRecord observation) {
      return aggregate(
              observation.tenantId(),
              observation.environment(),
              observation.idempotencyKey(),
              observation.canonicalHash())
          .contributions()
          .getFirst();
    }

    private static List<AttributionReferenceRecord> referencesFor(
        final OutcomeObservationRecord observation) {
      return aggregate(
              observation.tenantId(),
              observation.environment(),
              observation.idempotencyKey(),
              observation.canonicalHash())
          .references();
    }
  }
}

package com.guidinglight.decisionhub.usecase.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import java.time.Instant;
import java.util.Objects;

/** Bounded, redacted result of one internal feedback retention invocation. */
public record FeedbackRetentionResult(
    String tenantId,
    FeedbackEnvironment environment,
    Instant cutoff,
    int requestedBatchSize,
    int candidatesScanned,
    int eligibleCount,
    int blockedByReferenceCount,
    int blockedByIntegrityCount,
    int deletedAggregateCount,
    boolean hasMoreCandidates,
    Status status,
    FeedbackPersistenceErrorCode failureCode) {

  /** Stable completion category; no raw SQL, references, identifiers, or exceptions are returned. */
  public enum Status {
    DISABLED,
    COMPLETED,
    FAILED
  }

  /** Validates redacted counters and the relationship between terminal status and failure code. */
  public FeedbackRetentionResult {
    tenantId = Objects.requireNonNull(tenantId, "tenantId").trim();
    environment = Objects.requireNonNull(environment, "environment");
    cutoff = Objects.requireNonNull(cutoff, "cutoff");
    status = Objects.requireNonNull(status, "status");
    validateCount(requestedBatchSize, "requestedBatchSize");
    validateCount(candidatesScanned, "candidatesScanned");
    validateCount(eligibleCount, "eligibleCount");
    validateCount(blockedByReferenceCount, "blockedByReferenceCount");
    validateCount(blockedByIntegrityCount, "blockedByIntegrityCount");
    validateCount(deletedAggregateCount, "deletedAggregateCount");
    if (requestedBatchSize > FeedbackRetentionCommand.MAX_BATCH_SIZE
        || candidatesScanned > requestedBatchSize
        || eligibleCount > candidatesScanned
        || deletedAggregateCount > eligibleCount) {
      throw new IllegalArgumentException("retention result exceeds bounded command counts");
    }
    if (status == Status.FAILED && failureCode == null) {
      throw new IllegalArgumentException("failed retention result requires a failure code");
    }
    if (status != Status.FAILED && failureCode != null) {
      throw new IllegalArgumentException("non-failed retention result must not carry a failure code");
    }
  }

  /** Disabled result proves no candidate was selected and no aggregate was deleted. */
  public static FeedbackRetentionResult disabled(
      final FeedbackRetentionCommand command, final Instant cutoff) {
    return new FeedbackRetentionResult(
        command.tenantId(),
        command.environment(),
        cutoff,
        command.batchSize(),
        0,
        0,
        0,
        0,
        0,
        false,
        Status.DISABLED,
        null);
  }

  /** Fail-closed result never claims deleted aggregates. */
  public static FeedbackRetentionResult failed(
      final FeedbackRetentionCommand command,
      final Instant cutoff,
      final int candidatesScanned,
      final int blockedByReferenceCount,
      final int blockedByIntegrityCount,
      final FeedbackPersistenceErrorCode failureCode) {
    return new FeedbackRetentionResult(
        command.tenantId(),
        command.environment(),
        cutoff,
        command.batchSize(),
        candidatesScanned,
        0,
        blockedByReferenceCount,
        blockedByIntegrityCount,
        0,
        false,
        Status.FAILED,
        Objects.requireNonNull(failureCode, "failureCode"));
  }

  private static void validateCount(final int value, final String field) {
    if (value < 0 || value > FeedbackRetentionCommand.MAX_BATCH_SIZE) {
      throw new IllegalArgumentException(field + " must be within 0..100");
    }
  }
}

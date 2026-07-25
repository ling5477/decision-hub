package com.guidinglight.decisionhub.usecase.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackStatus;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.AttributionReferenceRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.FeedbackPersistenceAggregate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * Stage-QDR-9 B2 的 aggregate persistence 编排。
 *
 * <p>该服务只接收已完成审计的结构化 aggregate，在一个 required/repeatable-read transaction 中完成
 * reference confirmation、四表写入和完整 read-back。数据库唯一约束是幂等竞争的最终裁决；本类不保存
 * JVM 内存状态、不重试写入，也不调用外部系统。
 */
public final class FeedbackAttributionPersistenceService {

  private final FeedbackAttributionRepository repository;
  private final FeedbackReferenceValidationPort referenceValidationPort;
  private final FeedbackPersistenceTransactionBoundary transactionBoundary;

  /** 创建使用 usecase-owned ports 的 fail-closed persistence service。 */
  public FeedbackAttributionPersistenceService(
      final FeedbackAttributionRepository repository,
      final FeedbackReferenceValidationPort referenceValidationPort,
      final FeedbackPersistenceTransactionBoundary transactionBoundary) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.referenceValidationPort =
        Objects.requireNonNull(referenceValidationPort, "referenceValidationPort");
    this.transactionBoundary = Objects.requireNonNull(transactionBoundary, "transactionBoundary");
  }

  /**
   * 持久化已验证 aggregate。
   *
   * <p>同 key/same hash 返回完整既有 aggregate；同 key/different hash 返回冲突。唯一键竞争产生的
   * duplicate-key 分类必须先回滚，再在新只读 transaction 中 reconciliation。
   */
  public PersistenceResult persist(final FeedbackPersistenceAggregate aggregate) {
    final FeedbackPersistenceAggregate checked = Objects.requireNonNull(aggregate, "aggregate");
    final ReconciliationReference reconciliationReference = referenceOf(checked);
    try {
      return transactionBoundary.requiredRepeatableRead(
          () -> persistWithinTransaction(checked, reconciliationReference));
    } catch (final FeedbackPersistenceException error) {
      if (error.errorCode() == FeedbackPersistenceErrorCode.DUPLICATE_KEY) {
        return reconcileAfterDuplicate(checked, reconciliationReference);
      }
      if (error.errorCode() == FeedbackPersistenceErrorCode.COMMIT_OUTCOME_UNKNOWN) {
        return PersistenceResult.commitOutcomeUnknown(reconciliationReference);
      }
      throw error;
    }
  }

  /**
   * 仅允许 caller 在 commit result unknown 后执行的 scoped read-only reconciliation。
   *
   * <p>该方法不写入、不自动 retry；找不到 row 时仍保留 {@code COMMIT_OUTCOME_UNKNOWN}。
   */
  public PersistenceResult reconcileReadOnly(final ReconciliationReference reference) {
    final ReconciliationReference checked = Objects.requireNonNull(reference, "reference");
    return transactionBoundary.requiredRepeatableRead(
        () ->
            repository
                .findByIdempotencyKey(
                    checked.tenantId(), checked.environment(), checked.idempotencyKey())
                .map(
                    existing ->
                        existing.observation().canonicalHash().equals(checked.canonicalHash())
                            ? PersistenceResult.reused(existing, checked)
                            : PersistenceResult.idempotencyConflict(checked))
                .orElseGet(() -> PersistenceResult.commitOutcomeUnknown(checked)));
  }

  private PersistenceResult persistWithinTransaction(
      final FeedbackPersistenceAggregate aggregate,
      final ReconciliationReference reconciliationReference) {
    final var existing =
        repository.findByIdempotencyKey(
            reconciliationReference.tenantId(),
            reconciliationReference.environment(),
            reconciliationReference.idempotencyKey());
    if (existing.isPresent()) {
      return reconcileExisting(existing.orElseThrow(), reconciliationReference);
    }

    validateReferences(aggregate);
    repository.insertObservation(aggregate.observation());
    repository.insertAttribution(aggregate.attribution());
    repository.insertContributions(aggregate.contributions());
    repository.insertReferences(aggregate.references());

    final FeedbackPersistenceAggregate reloaded =
        repository
            .findByIdempotencyKey(
                reconciliationReference.tenantId(),
                reconciliationReference.environment(),
                reconciliationReference.idempotencyKey())
            .orElseThrow(
                () ->
                    failure(
                        FeedbackPersistenceErrorCode.AGGREGATE_INCOMPLETE,
                        "persisted feedback aggregate is unavailable after write"));
    assertComplete(reloaded);
    if (!sameAggregate(aggregate, reloaded)) {
      throw failure(
          FeedbackPersistenceErrorCode.AGGREGATE_INCOMPLETE,
          "persisted feedback aggregate differs from requested aggregate");
    }
    return PersistenceResult.created(reloaded, reconciliationReference);
  }

  private PersistenceResult reconcileAfterDuplicate(
      final FeedbackPersistenceAggregate aggregate,
      final ReconciliationReference reconciliationReference) {
    return transactionBoundary.requiredRepeatableRead(
        () ->
            repository
                .findByIdempotencyKey(
                    reconciliationReference.tenantId(),
                    reconciliationReference.environment(),
                    reconciliationReference.idempotencyKey())
                .map(existing -> reconcileExisting(existing, reconciliationReference))
                .orElseThrow(
                    () ->
                        failure(
                            FeedbackPersistenceErrorCode.PERSISTENCE_FAILURE,
                            "duplicate feedback aggregate cannot be reconciled")));
  }

  private static PersistenceResult reconcileExisting(
      final FeedbackPersistenceAggregate existing,
      final ReconciliationReference reconciliationReference) {
    assertComplete(existing);
    if (existing.observation().canonicalHash().equals(reconciliationReference.canonicalHash())) {
      return PersistenceResult.reused(existing, reconciliationReference);
    }
    return PersistenceResult.idempotencyConflict(reconciliationReference);
  }

  private void validateReferences(final FeedbackPersistenceAggregate aggregate) {
    for (AttributionReferenceRecord reference : aggregate.references()) {
      if (reference.referenceType() == FeedbackPersistenceRecords.ReferenceType.EVIDENCE
          && aggregate.contributions().stream()
              .noneMatch(
                  contribution ->
                      reference
                          .referenceValue()
                          .equals("evidence:" + contribution.evidenceRef()))) {
        throw failure(
            FeedbackPersistenceErrorCode.REFERENCE_INVALID,
            "feedback evidence reference is not bound to a contribution");
      }
      referenceValidationPort.confirm(reference, aggregate.observation());
    }
  }

  private static void assertComplete(final FeedbackPersistenceAggregate aggregate) {
    final FeedbackPersistenceAggregate checked = Objects.requireNonNull(aggregate, "aggregate");
    if (checked.attribution().attributionStatus() == FeedbackStatus.ATTRIBUTED
        && checked.contributions().isEmpty()) {
      throw failure(
          FeedbackPersistenceErrorCode.AGGREGATE_INCOMPLETE,
          "attributed feedback aggregate has no contributions");
    }
    final List<FeedbackPersistenceRecords.ReferenceType> types =
        checked.references().stream()
            .map(AttributionReferenceRecord::referenceType)
            .toList();
    if (!types.contains(FeedbackPersistenceRecords.ReferenceType.AUDIT)
        || !types.contains(FeedbackPersistenceRecords.ReferenceType.REPLAY)) {
      throw failure(
          FeedbackPersistenceErrorCode.AGGREGATE_INCOMPLETE,
          "feedback aggregate is missing audit or replay reference");
    }
  }

  private static boolean sameAggregate(
      final FeedbackPersistenceAggregate expected, final FeedbackPersistenceAggregate actual) {
    return expected.observation().id().equals(actual.observation().id())
        && expected.observation().tenantId().equals(actual.observation().tenantId())
        && expected.observation().environment() == actual.observation().environment()
        && expected.observation().decisionId().equals(actual.observation().decisionId())
        && expected.observation().traceId().equals(actual.observation().traceId())
        && expected.observation().observationId().equals(actual.observation().observationId())
        && expected.observation().idempotencyKey().equals(actual.observation().idempotencyKey())
        && expected.observation().outcomeSource() == actual.observation().outcomeSource()
        && expected.observation().outcomeStatus() == actual.observation().outcomeStatus()
        && sameInstant(expected.observation().observedAt(), actual.observation().observedAt())
        && sameInstant(expected.observation().evaluationTime(), actual.observation().evaluationTime())
        && expected.observation().canonicalHash().equals(actual.observation().canonicalHash())
        && sameAttribution(expected.attribution(), actual.attribution())
        && expected.contributions().equals(actual.contributions())
        && expected.references().equals(actual.references());
  }

  private static boolean sameInstant(
      final java.time.Instant expected, final java.time.Instant actual) {
    return expected.truncatedTo(ChronoUnit.MICROS).equals(actual.truncatedTo(ChronoUnit.MICROS));
  }

  private static boolean sameAttribution(
      final FeedbackPersistenceRecords.FeedbackAttributionRecord expected,
      final FeedbackPersistenceRecords.FeedbackAttributionRecord actual) {
    return expected.id().equals(actual.id())
        && expected.tenantId().equals(actual.tenantId())
        && expected.environment() == actual.environment()
        && expected.observationId().equals(actual.observationId())
        && expected.decisionId().equals(actual.decisionId())
        && expected.traceId().equals(actual.traceId())
        && sameInstant(expected.observedAt(), actual.observedAt())
        && expected.attributionId().equals(actual.attributionId())
        && expected.policyId().equals(actual.policyId())
        && expected.policyVersion().equals(actual.policyVersion())
        && expected.attributionStatus() == actual.attributionStatus()
        && expected.confidence().equals(actual.confidence())
        && expected.canonicalHash().equals(actual.canonicalHash())
        && expected.errorCode() == actual.errorCode();
  }

  private static ReconciliationReference referenceOf(
      final FeedbackPersistenceAggregate aggregate) {
    return new ReconciliationReference(
        aggregate.observation().tenantId(),
        aggregate.observation().environment(),
        aggregate.observation().idempotencyKey(),
        aggregate.observation().canonicalHash());
  }

  private static FeedbackPersistenceException failure(
      final FeedbackPersistenceErrorCode code, final String message) {
    return new FeedbackPersistenceException(code, message);
  }

  /** Persistence operation 的稳定结果。 */
  public record PersistenceResult(
      PersistenceStatus status,
      FeedbackPersistenceAggregate aggregate,
      ReconciliationReference reconciliationReference) {

    /** 固定结果约束，避免 unknown/conflict 伪装为成功。 */
    public PersistenceResult {
      status = Objects.requireNonNull(status, "status");
      reconciliationReference =
          Objects.requireNonNull(reconciliationReference, "reconciliationReference");
      if ((status == PersistenceStatus.CREATED || status == PersistenceStatus.REUSED)
          != (aggregate != null)) {
        throw new IllegalArgumentException("success result aggregate presence mismatch");
      }
    }

    static PersistenceResult created(
        final FeedbackPersistenceAggregate aggregate,
        final ReconciliationReference reconciliationReference) {
      return new PersistenceResult(PersistenceStatus.CREATED, aggregate, reconciliationReference);
    }

    static PersistenceResult reused(
        final FeedbackPersistenceAggregate aggregate,
        final ReconciliationReference reconciliationReference) {
      return new PersistenceResult(PersistenceStatus.REUSED, aggregate, reconciliationReference);
    }

    static PersistenceResult idempotencyConflict(
        final ReconciliationReference reconciliationReference) {
      return new PersistenceResult(
          PersistenceStatus.IDEMPOTENCY_CONFLICT, null, reconciliationReference);
    }

    static PersistenceResult commitOutcomeUnknown(
        final ReconciliationReference reconciliationReference) {
      return new PersistenceResult(
          PersistenceStatus.COMMIT_OUTCOME_UNKNOWN, null, reconciliationReference);
    }
  }

  /** 成功、冲突与提交结果不明的封闭状态。 */
  public enum PersistenceStatus {
    CREATED,
    REUSED,
    IDEMPOTENCY_CONFLICT,
    COMMIT_OUTCOME_UNKNOWN
  }

  /** commit-unknown reconciliation 所需且仅允许暴露的 scoped identity。 */
  public record ReconciliationReference(
      String tenantId,
      FeedbackEnvironment environment,
      String idempotencyKey,
      String canonicalHash) {

    /** 拒绝无 scope 或非 hash 的 reconciliation identity。 */
    public ReconciliationReference {
      tenantId = Objects.requireNonNull(tenantId, "tenantId");
      environment = Objects.requireNonNull(environment, "environment");
      idempotencyKey = requireHash(idempotencyKey, "idempotencyKey");
      canonicalHash = requireHash(canonicalHash, "canonicalHash");
    }

    private static String requireHash(final String value, final String name) {
      final String checked = Objects.requireNonNull(value, name);
      if (!checked.matches("[0-9a-f]{64}")) {
        throw new IllegalArgumentException(name + " must be lowercase SHA-256");
      }
      return checked;
    }
  }
}

package com.guidinglight.decisionhub.usecase.decision.dryrun;

import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventStatus;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventType;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionPersistenceRecords;
import com.guidinglight.decisionhub.usecase.qdr.guard.GuardTransactionBoundary;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionResult;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyGuardPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyRecordView;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyState;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyTransitionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardException;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardIdentity;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardStoreException;
import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Stage-QDR-7 persistent idempotency orchestration wrapper。
 *
 * <p>认证/nonce/rate已由Controller完成后才进入本service。首次admission独立短事务；执行事务原子包含lease、existing
 * safe output/audit writes、guard completion/failure和guard audit。任何store/commit/result不确定都fail-closed，绝不
 * fallback到generic in-memory idempotency。
 */
public final class PersistentGuardedDecisionDryRunService
    implements DecisionDryRunService, AutoCloseable {

  private static final String RESULT_TYPE_DECISION_OUTPUT = "DH_DECISION_OUTPUT";

  private final DecisionDryRunService delegate;
  private final IdempotencyGuardPort idempotencyPort;
  private final GuardTransactionBoundary transactions;
  private final DecisionAuditRepository auditRepository;
  private final DecisionDryRunRequestFingerprint fingerprint;
  private final DecisionDryRunSafeResultProjector resultProjector;
  private final DecisionDryRunGuardProperties guardProperties;
  private final NoSideEffectDecisionContract noSideEffectContract;
  private final LimitedDryRunRuntimeService runtimeService;
  private final Clock clock;
  private final String leaseOwner;

  /** 创建persistent idempotency orchestration。 */
  public PersistentGuardedDecisionDryRunService(
      final DecisionDryRunService delegate,
      final IdempotencyGuardPort idempotencyPort,
      final GuardTransactionBoundary transactions,
      final DecisionAuditRepository auditRepository,
      final DecisionDryRunRequestFingerprint fingerprint,
      final DecisionDryRunSafeResultProjector resultProjector,
      final DecisionDryRunGuardProperties guardProperties,
      final Clock clock) {
    this(
        delegate,
        idempotencyPort,
        transactions,
        auditRepository,
        fingerprint,
        resultProjector,
        guardProperties,
        clock,
        null);
  }

  /**
   * 创建带 B3 limited runtime policy 的 persistent idempotency orchestration。
   *
   * @param delegate 既有 mock-only dry-run service。
   * @param idempotencyPort persistent idempotency port。
   * @param transactions required transaction boundary。
   * @param auditRepository audit repository。
   * @param fingerprint request fingerprint service。
   * @param resultProjector safe result projector。
   * @param guardProperties persistent guard 配置。
   * @param clock UTC clock。
   * @param runtimePolicy B3 runtime policy；必须先于 persistent admission 判定。
   */
  public PersistentGuardedDecisionDryRunService(
      final DecisionDryRunService delegate,
      final IdempotencyGuardPort idempotencyPort,
      final GuardTransactionBoundary transactions,
      final DecisionAuditRepository auditRepository,
      final DecisionDryRunRequestFingerprint fingerprint,
      final DecisionDryRunSafeResultProjector resultProjector,
      final DecisionDryRunGuardProperties guardProperties,
      final Clock clock,
      final LimitedDryRunRuntimePolicy runtimePolicy) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
    this.idempotencyPort = Objects.requireNonNull(idempotencyPort, "idempotencyPort");
    this.transactions = Objects.requireNonNull(transactions, "transactions");
    this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository");
    this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
    this.resultProjector = Objects.requireNonNull(resultProjector, "resultProjector");
    this.guardProperties = Objects.requireNonNull(guardProperties, "guardProperties");
    this.noSideEffectContract =
        runtimePolicy == null
            ? new NoSideEffectDecisionContract()
            : runtimePolicy.noSideEffectContract();
    this.clock = Objects.requireNonNull(clock, "clock");
    this.leaseOwner = "qdr7-" + UUID.randomUUID();
    this.runtimeService =
        runtimePolicy == null
            ? null
            : new LimitedDryRunRuntimeService(new PersistentExecutionDelegate(), runtimePolicy);
  }

  @Override
  public DecisionDryRunResult execute(final DecisionDryRunCommand command) {
    if (!hasVerifiedExecutionScope(command)) {
      return delegate.reject(
          command,
          403,
          DecisionDryRunErrorCode.POLICY_DENIED,
          "dry-run request lacks verified execution authority");
    }
    return runtimeService == null ? executePersistent(command) : runtimeService.execute(command);
  }

  private DecisionDryRunResult executePersistent(final DecisionDryRunCommand command) {
    if (!guardProperties.runtimeEnabled()) {
      return delegate.execute(command);
    }
    final DecisionDryRunCommand checked = Objects.requireNonNull(command, "command");
    final PersistentGuardIdentity identity = identity(checked);
    final String requestHash = fingerprint.hash(checked);
    final IdempotencyAdmissionResult admission;
    try {
      admission =
          transactions.required(
              () -> {
                final IdempotencyAdmissionResult result =
                    idempotencyPort.admit(
                        new IdempotencyAdmissionCommand(
                            identity,
                            checked.requestId(),
                            requestHash,
                            IdempotencyAdmissionCommand.HASH_VERSION,
                            guardProperties.idempotencyTtl(),
                            guardProperties.retentionPeriod()));
                if (result.status()
                    != com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionStatus
                        .STORE_UNAVAILABLE) {
                  final String errorCode =
                      result.status()
                              == com.guidinglight.decisionhub.usecase.qdr.guard
                                  .IdempotencyAdmissionStatus.ADMITTED
                          ? null
                          : result.status().name();
                  writeAudit(
                      checked,
                      DecisionAuditEventType.QDR7_IDEMPOTENCY_ADMISSION,
                      result.status().name(),
                      errorCode);
                }
                return result;
              });
    } catch (final PersistentGuardStoreException error) {
      return rejected(checked, 503, DecisionDryRunErrorCode.IDEMPOTENCY_STORE_UNAVAILABLE);
    } catch (final RuntimeException error) {
      return rejected(checked, 503, DecisionDryRunErrorCode.IDEMPOTENCY_COMMIT_UNKNOWN);
    }
    return switch (admission.status()) {
      case ADMITTED -> executeFirst(checked, requestHash, admission.record());
      case IN_PROGRESS ->
          rejected(checked, 409, DecisionDryRunErrorCode.IDEMPOTENCY_IN_PROGRESS);
      case COMPLETED -> completedDuplicate(checked, admission.record());
      case FAILED -> failedDuplicate(checked, admission.record());
      case EXPIRED -> rejected(checked, 409, DecisionDryRunErrorCode.IDEMPOTENCY_EXPIRED);
      case CONFLICT -> rejected(checked, 409, DecisionDryRunErrorCode.IDEMPOTENCY_CONFLICT);
      case STORE_UNAVAILABLE ->
          rejected(checked, 503, DecisionDryRunErrorCode.IDEMPOTENCY_STORE_UNAVAILABLE);
      case COMMIT_UNKNOWN ->
          rejected(checked, 503, DecisionDryRunErrorCode.IDEMPOTENCY_COMMIT_UNKNOWN);
      case STATE_INVALID ->
          rejected(checked, 503, DecisionDryRunErrorCode.IDEMPOTENCY_STATE_INVALID);
      case RESULT_UNAVAILABLE ->
          rejected(checked, 503, DecisionDryRunErrorCode.IDEMPOTENCY_RESULT_UNAVAILABLE);
    };
  }

  @Override
  public DecisionDryRunResult reject(
      final DecisionDryRunCommand command,
      final int status,
      final DecisionDryRunErrorCode errorCode,
      final String message) {
    return delegate.reject(command, status, errorCode, message);
  }

  private DecisionDryRunResult executeFirst(
      final DecisionDryRunCommand command,
      final String requestHash,
      final IdempotencyRecordView received) {
    try {
      return transactions.required(
          () -> {
            final UUID leaseToken = UUID.randomUUID();
            final IdempotencyRecordView inProgress =
                idempotencyPort.transition(
                    new IdempotencyTransitionCommand(
                        received.identity(),
                        received.requestId(),
                        requestHash,
                        IdempotencyState.RECEIVED,
                        received.stateVersion(),
                        null,
                        null,
                        IdempotencyState.IN_PROGRESS,
                        leaseOwner,
                        leaseToken,
                        guardProperties.leaseDuration(),
                        null,
                        null,
                        null,
                        null));
            final DecisionDryRunResult delegated = delegate.execute(command);
            if (!delegated.success()) {
              idempotencyPort.transition(
                  terminal(
                      inProgress,
                      requestHash,
                      leaseToken,
                      IdempotencyState.FAILED,
                      null,
                      null,
                      delegated.errorCode() == null
                          ? DecisionDryRunErrorCode.UNKNOWN_ERROR.name()
                          : delegated.errorCode().name()));
              writeAudit(
                  command,
                  DecisionAuditEventType.QDR7_IDEMPOTENCY_FAILED,
                  "FAILED",
                  delegated.errorCode() == null ? "UNKNOWN_ERROR" : delegated.errorCode().name());
              return delegated;
            }
            final java.util.Optional<RuntimeFailureClassification> noSideEffectViolation =
                noSideEffectContract.validate(delegated);
            if (noSideEffectViolation.isPresent()) {
              final DecisionDryRunResult rejected =
                  delegate.reject(
                      command,
                      403,
                      DecisionDryRunErrorCode.POLICY_DENIED,
                      "limited dry-run runtime rejected: "
                          + noSideEffectViolation.orElseThrow().name());
              idempotencyPort.transition(
                  terminal(
                      inProgress,
                      requestHash,
                      leaseToken,
                      IdempotencyState.FAILED,
                      null,
                      null,
                      DecisionDryRunErrorCode.POLICY_DENIED.name()));
              writeAudit(
                  command,
                  DecisionAuditEventType.QDR7_IDEMPOTENCY_FAILED,
                  "FAILED",
                  DecisionDryRunErrorCode.POLICY_DENIED.name());
              return rejected;
            }
            final DecisionDryRunSnapshot normalized =
                resultProjector.project(command.tenantId(), delegated.snapshot().decisionId());
            final String checksum = resultProjector.checksum(normalized);
            idempotencyPort.transition(
                terminal(
                    inProgress,
                    requestHash,
                    leaseToken,
                    IdempotencyState.COMPLETED,
                    normalized.decisionId(),
                    checksum,
                    null));
            writeAudit(
                command,
                DecisionAuditEventType.QDR7_IDEMPOTENCY_COMPLETED,
                "COMPLETED",
                null);
            return DecisionDryRunResult.success(normalized);
          });
    } catch (final PersistentGuardStoreException error) {
      return rejected(command, 503, DecisionDryRunErrorCode.IDEMPOTENCY_STORE_UNAVAILABLE);
    } catch (final PersistentGuardException error) {
      return rejected(command, 503, DecisionDryRunErrorCode.IDEMPOTENCY_STATE_INVALID);
    } catch (final IllegalStateException error) {
      return rejected(command, 503, DecisionDryRunErrorCode.IDEMPOTENCY_RESULT_UNAVAILABLE);
    } catch (final RuntimeException error) {
      return rejected(command, 503, DecisionDryRunErrorCode.IDEMPOTENCY_COMMIT_UNKNOWN);
    }
  }

  private DecisionDryRunResult completedDuplicate(
      final DecisionDryRunCommand command, final IdempotencyRecordView record) {
    try {
      if (!RESULT_TYPE_DECISION_OUTPUT.equals(record.resultType())) {
        return rejected(command, 503, DecisionDryRunErrorCode.IDEMPOTENCY_RESULT_UNAVAILABLE);
      }
      final DecisionDryRunSnapshot snapshot =
          transactions.required(
              () -> resultProjector.project(command.tenantId(), record.resultId()));
      if (!resultProjector.checksum(snapshot).equals(record.resultChecksum())) {
        return rejected(command, 503, DecisionDryRunErrorCode.IDEMPOTENCY_RESULT_UNAVAILABLE);
      }
      return DecisionDryRunResult.success(snapshot);
    } catch (final RuntimeException error) {
      return rejected(command, 503, DecisionDryRunErrorCode.IDEMPOTENCY_RESULT_UNAVAILABLE);
    }
  }

  private static DecisionDryRunResult failedDuplicate(
      final DecisionDryRunCommand command, final IdempotencyRecordView record) {
    final DecisionDryRunErrorCode code;
    try {
      code = DecisionDryRunErrorCode.valueOf(record.stableErrorCode());
    } catch (final RuntimeException error) {
      return rejected(command, 503, DecisionDryRunErrorCode.IDEMPOTENCY_STATE_INVALID);
    }
    return rejected(command, 409, code);
  }

  private IdempotencyTransitionCommand terminal(
      final IdempotencyRecordView inProgress,
      final String requestHash,
      final UUID leaseToken,
      final IdempotencyState target,
      final String resultId,
      final String checksum,
      final String errorCode) {
    return new IdempotencyTransitionCommand(
        inProgress.identity(),
        inProgress.requestId(),
        requestHash,
        IdempotencyState.IN_PROGRESS,
        inProgress.stateVersion(),
        inProgress.leaseOwner(),
        leaseToken,
        target,
        null,
        null,
        null,
        target == IdempotencyState.COMPLETED ? RESULT_TYPE_DECISION_OUTPUT : null,
        resultId,
        checksum,
        errorCode);
  }

  private PersistentGuardIdentity identity(final DecisionDryRunCommand command) {
    return new PersistentGuardIdentity(
        guardProperties.environment(),
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
        command.source(),
        command.tenantId());
  }

  private void writeAudit(
      final DecisionDryRunCommand command,
      final DecisionAuditEventType eventType,
      final String state,
      final String errorCode) {
    auditRepository.saveAuditEvent(
        new DecisionPersistenceRecords.AuditEventRecord(
            command.requestId() + "-qdr7-guard-" + UUID.randomUUID(),
            command.requestId(),
            command.tenantId(),
            command.traceId(),
            command.executionScope().environment(),
            eventType,
            errorCode == null
                ? DecisionAuditEventStatus.SUCCESS
                : DecisionAuditEventStatus.FAILED,
            Map.of(
                "endpoint",
                PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
                "source",
                command.source(),
                "state",
                state),
            errorCode,
            clock.instant()));
  }

  private static DecisionDryRunResult rejected(
      final DecisionDryRunCommand command,
      final int status,
      final DecisionDryRunErrorCode errorCode) {
    return DecisionDryRunResult.rejected(
        status,
        errorCode,
        "dry-run persistent guard rejected request",
        command == null ? null : command.requestId(),
        command == null ? null : command.traceId(),
        null);
  }

  private static boolean hasVerifiedExecutionScope(final DecisionDryRunCommand command) {
    return command != null
        && command.executionScope() != null
        && command.executionScope().tenantId().equals(command.tenantId())
        && command.executionScope().environment().name().equals(command.environment());
  }

  /** 关闭 B3 bounded executor；旧无 runtime-policy 构造路径无资源可关闭。 */
  @Override
  public void close() {
    if (runtimeService != null) {
      runtimeService.close();
    }
  }

  private final class PersistentExecutionDelegate implements DecisionDryRunService {

    @Override
    public DecisionDryRunResult execute(final DecisionDryRunCommand command) {
      return executePersistent(command);
    }

    @Override
    public DecisionDryRunResult reject(
        final DecisionDryRunCommand command,
        final int status,
        final DecisionDryRunErrorCode errorCode,
        final String message) {
      return delegate.reject(command, status, errorCode, message);
    }
  }
}

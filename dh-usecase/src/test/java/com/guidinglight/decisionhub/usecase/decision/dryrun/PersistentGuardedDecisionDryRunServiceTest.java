package com.guidinglight.decisionhub.usecase.decision.dryrun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.usecase.decision.InMemoryDecisionAuditRepository;
import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayContextView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayOutputView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayRequestView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayTimelineView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackExecutionScope;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionResult;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyGuardPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyRecordView;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyTransitionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardIdentity;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardStoreException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Persistent idempotency wrapper 的 store/commit 分类回归。 */
class PersistentGuardedDecisionDryRunServiceTest {

  @Test
  void completedDuplicateMissingResultMapsToResultUnavailable() {
    final DecisionDryRunResult result = completedDuplicateService(
        new DecisionDryRunSafeResultProjector(query -> null), "b".repeat(64)).execute(command());
    assertFalse(result.success());
    assertEquals(503, result.status());
    assertEquals(DecisionDryRunErrorCode.IDEMPOTENCY_RESULT_UNAVAILABLE, result.errorCode());
  }

  @Test
  void completedDuplicateChecksumMismatchMapsToResultUnavailable() {
    final DecisionDryRunSafeResultProjector projector =
        new DecisionDryRunSafeResultProjector(query -> replay());
    final DecisionDryRunResult result = completedDuplicateService(projector, "b".repeat(64)).execute(command());
    assertFalse(result.success());
    assertEquals(DecisionDryRunErrorCode.IDEMPOTENCY_RESULT_UNAVAILABLE, result.errorCode());
  }

  @Test
  void transactionStartFailureUsesStoreUnavailableInsteadOfCommitUnknown() {
    final DecisionDryRunService service =
        new PersistentGuardedDecisionDryRunService(
            rejectingDelegate(),
            unusedPort(),
            new com.guidinglight.decisionhub.usecase.qdr.guard.GuardTransactionBoundary() {
              @Override
              public <T> T required(final java.util.function.Supplier<T> action) {
                throw new PersistentGuardStoreException("store unavailable", null);
              }
            },
            new InMemoryDecisionAuditRepository(),
            new DecisionDryRunRequestFingerprint(),
            new DecisionDryRunSafeResultProjector(query -> null),
            properties(),
            Clock.systemUTC());

    final DecisionDryRunResult result = service.execute(command());

    assertFalse(result.success());
    assertEquals(503, result.status());
    assertEquals(DecisionDryRunErrorCode.IDEMPOTENCY_STORE_UNAVAILABLE, result.errorCode());
  }

  @Test
  void admissionCommitUnknownDoesNotExecuteOrReplayBusiness() {
    final PersistentGuardIdentity identity =
        new PersistentGuardIdentity(
            "test",
            PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
            PersistentGuardIdentity.NQ_DRYRUN_SOURCE,
            "tenant-a");
    final IdempotencyGuardPort admittedPort =
        new IdempotencyGuardPort() {
          @Override
          public IdempotencyAdmissionResult admit(final IdempotencyAdmissionCommand admission) {
            return new IdempotencyAdmissionResult(
                com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionStatus.ADMITTED,
                new IdempotencyRecordView(
                    UUID.randomUUID(),
                    identity,
                    admission.requestId(),
                    admission.requestHash(),
                    com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyState.RECEIVED,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Instant.now().plusSeconds(60),
                    Instant.now().plusSeconds(120)));
          }

          @Override
          public IdempotencyRecordView transition(final IdempotencyTransitionCommand transition) {
            throw new AssertionError("commit-unknown must not transition");
          }

          @Override
          public IdempotencyRecordView findExact(
              final PersistentGuardIdentity ignoredIdentity,
              final String requestId,
              final String requestHash) {
            throw new AssertionError("commit-unknown requires later explicit reconcile");
          }
        };
    final DecisionDryRunService service =
        new PersistentGuardedDecisionDryRunService(
            rejectingDelegate(),
            admittedPort,
            new com.guidinglight.decisionhub.usecase.qdr.guard.GuardTransactionBoundary() {
              @Override
              public <T> T required(final java.util.function.Supplier<T> action) {
                action.get();
                throw new IllegalStateException("deterministic after-admission commit unknown");
              }
            },
            new InMemoryDecisionAuditRepository(),
            new DecisionDryRunRequestFingerprint(),
            new DecisionDryRunSafeResultProjector(query -> null),
            properties(),
            Clock.systemUTC());

    final DecisionDryRunResult result = service.execute(command());

    assertFalse(result.success());
    assertEquals(503, result.status());
    assertEquals(DecisionDryRunErrorCode.IDEMPOTENCY_COMMIT_UNKNOWN, result.errorCode());
  }

  @Test
  void unverifiedScopeIsRejectedBeforePersistentAdmissionOrAudit() {
    final InMemoryDecisionAuditRepository auditRepository = new InMemoryDecisionAuditRepository();
    final DecisionDryRunService service =
        new PersistentGuardedDecisionDryRunService(
            rejectingDelegate(),
            unusedPort(),
            new com.guidinglight.decisionhub.usecase.qdr.guard.GuardTransactionBoundary() {
              @Override
              public <T> T required(final java.util.function.Supplier<T> action) {
                throw new AssertionError("unverified command must not start a persistent transaction");
              }
            },
            auditRepository,
            new DecisionDryRunRequestFingerprint(),
            new DecisionDryRunSafeResultProjector(query -> null),
            properties(),
            Clock.systemUTC());

    final DecisionDryRunResult result = service.execute(unscopedCommand());

    assertFalse(result.success());
    assertEquals(403, result.status());
    assertEquals(DecisionDryRunErrorCode.POLICY_DENIED, result.errorCode());
    assertTrue(auditRepository.auditEvents().isEmpty());
  }

  private static DecisionDryRunService rejectingDelegate() {
    return new DecisionDryRunService() {
      @Override
      public DecisionDryRunResult execute(final DecisionDryRunCommand command) {
        throw new AssertionError("delegate must not execute after guard store failure");
      }

      @Override
      public DecisionDryRunResult reject(
          final DecisionDryRunCommand command,
          final int status,
          final DecisionDryRunErrorCode errorCode,
          final String message) {
        return DecisionDryRunResult.rejected(
            status, errorCode, message, command.requestId(), command.traceId(), null);
      }
    };
  }

  private static DecisionDryRunService completedDuplicateService(
      final DecisionDryRunSafeResultProjector projector, final String storedChecksum) {
    final PersistentGuardIdentity identity = new PersistentGuardIdentity(
        "test", PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
        PersistentGuardIdentity.NQ_DRYRUN_SOURCE, "tenant-a");
    final IdempotencyGuardPort port = new IdempotencyGuardPort() {
      @Override
      public IdempotencyAdmissionResult admit(final IdempotencyAdmissionCommand admission) {
        return new IdempotencyAdmissionResult(
            com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionStatus.COMPLETED,
            new IdempotencyRecordView(UUID.randomUUID(), identity, admission.requestId(),
                admission.requestHash(), com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyState.COMPLETED,
                2, null, null, null, "DH_DECISION_OUTPUT", "result-1", storedChecksum, null,
                Instant.now().plusSeconds(60), Instant.now().plusSeconds(120)));
      }

      @Override
      public IdempotencyRecordView transition(final IdempotencyTransitionCommand transition) {
        throw new AssertionError("completed duplicate must not transition");
      }

      @Override
      public IdempotencyRecordView findExact(
          final PersistentGuardIdentity ignoredIdentity, final String requestId, final String requestHash) {
        throw new AssertionError("completed duplicate uses admission exact view");
      }
    };
    return new PersistentGuardedDecisionDryRunService(
        rejectingDelegate(), port, new com.guidinglight.decisionhub.usecase.qdr.guard.GuardTransactionBoundary() {
          @Override
          public <T> T required(final java.util.function.Supplier<T> action) {
            return action.get();
          }
        }, new InMemoryDecisionAuditRepository(), new DecisionDryRunRequestFingerprint(), projector,
        properties(), Clock.systemUTC());
  }

  private static DecisionReplayView replay() {
    final Instant now = Instant.parse("2026-07-12T00:00:00Z");
    return DecisionReplayView.found(
        new DecisionReplayRequestView("result-1", "request-1", "trace-1", "tenant-a",
            "NQ_DRYRUN", DecisionType.READ_ONLY_RECOMMENDATION, java.util.Map.of(), null,
            now, "1", now),
        new DecisionReplayContextView("result-1", "tenant-a", "trace-1", java.util.Map.of(),
            List.of(), now),
        new DecisionReplayOutputView("result-1", "tenant-a", "trace-1", "request-1",
            DecisionType.READ_ONLY_RECOMMENDATION, DecisionAction.NO_TRADE,
            DecisionRiskLevel.LOW, DecisionPolicyStatus.ALLOWED, new BigDecimal("0.5"),
            java.util.Map.of(), now),
        DecisionReplayTimelineView.empty());
  }

  private static IdempotencyGuardPort unusedPort() {
    return new IdempotencyGuardPort() {
      @Override
      public IdempotencyAdmissionResult admit(final IdempotencyAdmissionCommand command) {
        throw new AssertionError("port must not run when transaction cannot start");
      }

      @Override
      public IdempotencyRecordView transition(final IdempotencyTransitionCommand command) {
        throw new AssertionError("transition must not run when transaction cannot start");
      }

      @Override
      public IdempotencyRecordView findExact(
          final PersistentGuardIdentity identity,
          final String requestId,
          final String requestHash) {
        throw new AssertionError("query must not run when transaction cannot start");
      }
    };
  }

  private static DecisionDryRunCommand command() {
    return unscopedCommand()
        .withExecutionScope(new FeedbackExecutionScope("tenant-a", FeedbackEnvironment.DEV));
  }

  private static DecisionDryRunCommand unscopedCommand() {
    return new DecisionDryRunCommand(
        "request-1",
        "trace-1",
        "tenant-a",
        "NQ_DRYRUN",
        "DEV",
        "2026-07-12T00:00:00Z",
        "nonce-1",
        "1",
        true,
        Set.of("PLACE_ORDER"),
        new DecisionDryRunContext(
            "BTCUSDT",
            "SPOT",
            "1h",
            null,
            null,
            null,
            "snapshot-1",
            Instant.parse("2026-07-12T00:00:00Z"),
            List.of("evidence-1"),
            128),
        false,
        null);
  }

  private static DecisionDryRunGuardProperties properties() {
    return new DecisionDryRunGuardProperties(
        true,
        "test",
        60,
        10,
        Duration.ofSeconds(30),
        Duration.ofMinutes(10),
        Duration.ofHours(1));
  }
}

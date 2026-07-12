package com.guidinglight.decisionhub.usecase.decision.dryrun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.guidinglight.decisionhub.usecase.decision.InMemoryDecisionAuditRepository;
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
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Persistent idempotency wrapper 的 store/commit 分类回归。 */
class PersistentGuardedDecisionDryRunServiceTest {

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
    return new DecisionDryRunCommand(
        "request-1",
        "trace-1",
        "tenant-a",
        "NQ_DRYRUN",
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
        false);
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

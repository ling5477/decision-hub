package com.guidinglight.decisionhub.qdr7;

import static org.assertj.core.api.Assertions.assertThat;

import com.guidinglight.decisionhub.security.nq.RateLimitResult;
import com.guidinglight.decisionhub.usecase.decision.InMemoryDecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunGuardProperties;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardStoreException;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionResult;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Persistent rate bridge的store/commit错误映射与no-fallback回归。 */
class PersistentDecisionDryRunRateLimiterTest {

  @Test
  void commitUnknownNeverFallsBackToInMemoryAdmission() {
    final PersistentDecisionDryRunRateLimiter limiter =
        new PersistentDecisionDryRunRateLimiter(
            command -> {
              throw new AssertionError("port must not run after transaction failure");
            },
            new com.guidinglight.decisionhub.usecase.qdr.guard.GuardTransactionBoundary() {
              @Override
              public <T> T required(final java.util.function.Supplier<T> action) {
                throw new IllegalStateException("commit outcome unknown");
              }
            },
            new InMemoryDecisionAuditRepository(),
            properties(),
            Clock.systemUTC());

    final RateLimitResult result =
        limiter.check(
            "NQ_DRYRUN",
            "tenant-a",
            PersistentDecisionDryRunRateLimiter.ROUTE,
            null,
            "request-1",
            "trace-1");

    assertThat(result.allowed()).isFalse();
    assertThat(result.reason()).isEqualTo(RateLimitResult.REASON_COMMIT_UNKNOWN);
  }

  @Test
  void storeUnavailableRemainsDistinctFromQuotaExhaustion() {
    final PersistentDecisionDryRunRateLimiter limiter =
        new PersistentDecisionDryRunRateLimiter(
            command -> RateLimitAdmissionResult.storeUnavailable(),
            new com.guidinglight.decisionhub.usecase.qdr.guard.GuardTransactionBoundary() {
              @Override
              public <T> T required(final java.util.function.Supplier<T> action) {
                return action.get();
              }
            },
            new InMemoryDecisionAuditRepository(),
            properties(),
            Clock.systemUTC());

    final RateLimitResult result =
        limiter.check(
            "NQ_DRYRUN",
            "tenant-a",
            PersistentDecisionDryRunRateLimiter.ROUTE,
            null,
            "request-1",
            "trace-1");

    assertThat(result.reason()).isEqualTo(RateLimitResult.REASON_STORE_UNAVAILABLE);
  }

  @Test
  void transactionStartStoreFailureRemainsDistinctFromCommitUnknown() {
    final PersistentDecisionDryRunRateLimiter limiter =
        new PersistentDecisionDryRunRateLimiter(
            command -> {
              throw new AssertionError("port must not run when transaction cannot start");
            },
            new com.guidinglight.decisionhub.usecase.qdr.guard.GuardTransactionBoundary() {
              @Override
              public <T> T required(final java.util.function.Supplier<T> action) {
                throw new PersistentGuardStoreException("store unavailable", null);
              }
            },
            new InMemoryDecisionAuditRepository(),
            properties(),
            Clock.systemUTC());

    final RateLimitResult result =
        limiter.check(
            "NQ_DRYRUN",
            "tenant-a",
            PersistentDecisionDryRunRateLimiter.ROUTE,
            null,
            "request-1",
            "trace-1");

    assertThat(result.reason()).isEqualTo(RateLimitResult.REASON_STORE_UNAVAILABLE);
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

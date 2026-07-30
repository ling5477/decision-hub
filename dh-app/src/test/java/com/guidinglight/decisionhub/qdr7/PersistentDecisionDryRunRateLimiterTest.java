package com.guidinglight.decisionhub.qdr7;

import static org.assertj.core.api.Assertions.assertThat;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackExecutionScope;
import com.guidinglight.decisionhub.security.nq.RateLimitResult;
import com.guidinglight.decisionhub.usecase.decision.InMemoryDecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunGuardProperties;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardStoreException;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionResult;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
            scope(),
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
            scope(),
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
            scope(),
            "NQ_DRYRUN",
            "tenant-a",
            PersistentDecisionDryRunRateLimiter.ROUTE,
            null,
            "request-1",
            "trace-1");

    assertThat(result.reason()).isEqualTo(RateLimitResult.REASON_STORE_UNAVAILABLE);
  }

  @Test
  void oldEnvironmentlessPathAndConfigurationMismatchFailBeforeAdmission() {
    final PersistentDecisionDryRunRateLimiter limiter =
        new PersistentDecisionDryRunRateLimiter(
            command -> {
              throw new AssertionError("unverified or mismatched scope must not consume quota");
            },
            directTransactions(),
            new InMemoryDecisionAuditRepository(),
            properties(),
            Clock.systemUTC());

    assertThat(
            limiter
                .check(
                    "NQ_DRYRUN",
                    "tenant-a",
                    PersistentDecisionDryRunRateLimiter.ROUTE,
                    null,
                    "request-old",
                    "trace-old")
                .reason())
        .isEqualTo(RateLimitResult.REASON_CONFIGURATION_INVALID);
    assertThat(
            limiter
                .check(
                    new FeedbackExecutionScope("tenant-a", FeedbackEnvironment.DEV),
                    "NQ_DRYRUN",
                    "tenant-a",
                    PersistentDecisionDryRunRateLimiter.ROUTE,
                    null,
                    "request-mismatch",
                    "trace-mismatch")
                .reason())
        .isEqualTo(RateLimitResult.REASON_CONFIGURATION_INVALID);
  }

  @Test
  void acceptedRateAuditCarriesCanonicalVerifiedEnvironment() {
    final InMemoryDecisionAuditRepository audits = new InMemoryDecisionAuditRepository();
    final Instant now = Instant.parse("2026-07-30T00:00:00Z");
    final PersistentDecisionDryRunRateLimiter limiter =
        new PersistentDecisionDryRunRateLimiter(
            command ->
                new RateLimitAdmissionResult(
                    com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionStatus.ACCEPTED,
                    now,
                    now.plusSeconds(60),
                    now,
                    1L,
                    10),
            directTransactions(),
            audits,
            properties(),
            Clock.fixed(now, java.time.ZoneOffset.UTC));

    final RateLimitResult result =
        limiter.check(
            scope(),
            "NQ_DRYRUN",
            "tenant-a",
            PersistentDecisionDryRunRateLimiter.ROUTE,
            now,
            "request-audit",
            "trace-audit");

    assertThat(result.allowed()).isTrue();
    assertThat(audits.auditEvents()).hasSize(1);
    assertThat(audits.auditEvents().getFirst().eventJson())
        .containsEntry("environment", "TEST");
  }

  @Test
  void rateLimitedAuditCarriesCanonicalVerifiedEnvironment() {
    final InMemoryDecisionAuditRepository audits = new InMemoryDecisionAuditRepository();
    final Instant now = Instant.parse("2026-07-30T00:00:00Z");
    final PersistentDecisionDryRunRateLimiter limiter =
        new PersistentDecisionDryRunRateLimiter(
            command ->
                new RateLimitAdmissionResult(
                    com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionStatus
                        .RATE_LIMITED,
                    now,
                    now.plusSeconds(60),
                    now,
                    10L,
                    10),
            directTransactions(),
            audits,
            properties(),
            Clock.fixed(now, java.time.ZoneOffset.UTC));

    final RateLimitResult result =
        limiter.check(
            scope(),
            "NQ_DRYRUN",
            "tenant-a",
            PersistentDecisionDryRunRateLimiter.ROUTE,
            now,
            "request-limited-audit",
            "trace-limited-audit");

    assertThat(result.allowed()).isFalse();
    assertThat(audits.auditEvents()).hasSize(1);
    assertThat(audits.auditEvents().getFirst().eventJson())
        .containsEntry("environment", "TEST")
        .containsEntry("status", "RATE_LIMITED");
  }

  private static FeedbackExecutionScope scope() {
    return new FeedbackExecutionScope("tenant-a", FeedbackEnvironment.TEST);
  }

  private static com.guidinglight.decisionhub.usecase.qdr.guard.GuardTransactionBoundary
      directTransactions() {
    return new com.guidinglight.decisionhub.usecase.qdr.guard.GuardTransactionBoundary() {
      @Override
      public <T> T required(final java.util.function.Supplier<T> action) {
        return action.get();
      }
    };
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

package com.guidinglight.decisionhub.usecase.qdr.guard;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunGuardProperties;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Stage-QDR-7 B1 persistent rate command hard ceiling直接回归。 */
class RateLimitAdmissionCommandTest {

  @Test
  void lowerAndFrozenMaximumValuesAreAccepted() {
    assertDoesNotThrow(() -> command(1, 1));
    final RateLimitAdmissionCommand maximum =
        assertDoesNotThrow(
            () ->
                command(
                    PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS,
                    PersistentGuardHardCeilings.MAX_RATE_QUOTA));

    assertEquals(PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS, maximum.windowSeconds());
    assertEquals(PersistentGuardHardCeilings.MAX_RATE_QUOTA, maximum.limitValue());
  }

  @Test
  void windowOutsideFrozenBoundsFailsClosed() {
    assertThrows(IllegalArgumentException.class, () -> command(0, 1));
    assertThrows(
        IllegalArgumentException.class,
        () -> command(PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS + 1, 1));
  }

  @Test
  void quotaOutsideFrozenBoundsFailsClosed() {
    assertThrows(IllegalArgumentException.class, () -> command(1, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> command(1, PersistentGuardHardCeilings.MAX_RATE_QUOTA + 1));
  }

  @Test
  void missingOrInvalidIdentityFailsClosed() {
    assertThrows(NullPointerException.class, () -> new RateLimitAdmissionCommand(null, 1, 1));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PersistentGuardIdentity(
                "test",
                PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
                PersistentGuardIdentity.NQ_DRYRUN_SOURCE,
                ""));
  }

  @Test
  void overLimitConstructionPreventsAdapterInvocation() {
    final AtomicInteger adapterCalls = new AtomicInteger();
    final RateLimitAdmissionPort adapter =
        command -> {
          adapterCalls.incrementAndGet();
          return RateLimitAdmissionResult.storeUnavailable();
        };

    assertThrows(
        IllegalArgumentException.class,
        () ->
            submit(
                adapter,
                PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS + 1,
                PersistentGuardHardCeilings.MAX_RATE_QUOTA));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            submit(
                adapter,
                PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS,
                PersistentGuardHardCeilings.MAX_RATE_QUOTA + 1));
    assertEquals(0, adapterCalls.get());
  }

  @Test
  void propertiesAndCommandUseTheSharedFrozenAuthority() {
    final DecisionDryRunGuardProperties properties =
        new DecisionDryRunGuardProperties(
            true,
            "test",
            PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS,
            PersistentGuardHardCeilings.MAX_RATE_QUOTA,
            Duration.ofSeconds(PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS),
            Duration.ofSeconds(PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS + 1L),
            Duration.ofDays(1));
    final RateLimitAdmissionCommand command =
        command(
            PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS,
            PersistentGuardHardCeilings.MAX_RATE_QUOTA);

    assertEquals(PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS, properties.rateWindowSeconds());
    assertEquals(PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS, command.windowSeconds());
    assertEquals(PersistentGuardHardCeilings.MAX_RATE_QUOTA, properties.rateLimitValue());
    assertEquals(PersistentGuardHardCeilings.MAX_RATE_QUOTA, command.limitValue());
    assertEquals(
        PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS,
        properties.leaseDuration().toSeconds());
  }

  private static RateLimitAdmissionResult submit(
      final RateLimitAdmissionPort adapter, final int windowSeconds, final int quota) {
    return adapter.tryAcquire(command(windowSeconds, quota));
  }

  private static RateLimitAdmissionCommand command(final int windowSeconds, final int quota) {
    return new RateLimitAdmissionCommand(identity(), windowSeconds, quota);
  }

  private static PersistentGuardIdentity identity() {
    return new PersistentGuardIdentity(
        "test",
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
        PersistentGuardIdentity.NQ_DRYRUN_SOURCE,
        "tenant-a");
  }
}

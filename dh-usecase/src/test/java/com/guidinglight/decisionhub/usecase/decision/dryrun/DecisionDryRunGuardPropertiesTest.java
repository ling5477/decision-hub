package com.guidinglight.decisionhub.usecase.decision.dryrun;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardHardCeilings;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Stage-QDR-7 B1 persistent guard hard ceiling直接回归。 */
class DecisionDryRunGuardPropertiesTest {

  @Test
  void frozenMaximumValuesAreAccepted() {
    assertDoesNotThrow(
        () ->
            enabledProperties(
                PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS,
                PersistentGuardHardCeilings.MAX_RATE_QUOTA,
                PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS));
  }

  @Test
  void valuesAboveFrozenMaximumFailClosed() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            enabledProperties(
                PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS + 1,
                PersistentGuardHardCeilings.MAX_RATE_QUOTA,
                PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            enabledProperties(
                PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS,
                PersistentGuardHardCeilings.MAX_RATE_QUOTA + 1,
                PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            enabledProperties(
                PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS,
                PersistentGuardHardCeilings.MAX_RATE_QUOTA,
                PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS + 1));
  }

  @Test
  void enabledRuntimeRejectsZeroNegativeMissingAndInvalidEnvironment() {
    assertThrows(IllegalArgumentException.class, () -> enabledProperties(0, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> enabledProperties(-1, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> enabledProperties(1, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> enabledProperties(1, -1, 1));
    assertThrows(IllegalArgumentException.class, () -> enabledProperties(1, 1, 0));
    assertThrows(IllegalArgumentException.class, () -> enabledProperties(1, 1, -1));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DecisionDryRunGuardProperties(
                true, "", 1, 1, null, Duration.ofSeconds(2), Duration.ofSeconds(3)));
  }

  @Test
  void disabledRuntimeRetainsZeroDefaultsWithoutEnablingPersistentGuards() {
    assertDoesNotThrow(() -> new DecisionDryRunGuardProperties(false, "", 0, 0, null, null, null));
  }

  private static DecisionDryRunGuardProperties enabledProperties(
      final int rateWindowSeconds, final int rateQuota, final int leaseSeconds) {
    final Duration lease = Duration.ofSeconds(leaseSeconds);
    final Duration ttl = Duration.ofSeconds(Math.max(leaseSeconds + 1L, 2L));
    return new DecisionDryRunGuardProperties(
        true, "test", rateWindowSeconds, rateQuota, lease, ttl, Duration.ofDays(1));
  }
}

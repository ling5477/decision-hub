package com.guidinglight.decisionhub.usecase.qdr.guard;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Tenant-scoped bounded cleanup command 的 fail-closed contract 回归。 */
class GuardCleanupCommandTest {

  @Test
  void completeTenantScopeAndBatchBoundsAreAccepted() {
    final GuardCleanupCommand command = assertDoesNotThrow(() -> command("tenant-a", 1));
    assertEquals("test", command.environment());
    assertEquals("tenant-a", command.tenantId());
    assertDoesNotThrow(() -> command("tenant-a", 1_000));
  }

  @Test
  void missingBlankOrWildcardTenantFailsClosed() {
    assertThrows(NullPointerException.class, () -> command(null, 1));
    assertThrows(IllegalArgumentException.class, () -> command("", 1));
    assertThrows(IllegalArgumentException.class, () -> command(" ", 1));
    assertThrows(IllegalArgumentException.class, () -> command("*", 1));
  }

  @Test
  void missingBlankOrUnsupportedEnvironmentFailsClosed() {
    assertThrows(NullPointerException.class, () -> command(null, "tenant-a", 1));
    assertThrows(IllegalArgumentException.class, () -> command("", "tenant-a", 1));
    assertThrows(IllegalArgumentException.class, () -> command(" ", "tenant-a", 1));
    assertThrows(IllegalArgumentException.class, () -> command("all", "tenant-a", 1));
  }

  @Test
  void safetyGraceAndBatchRemainBounded() {
    assertThrows(
        IllegalArgumentException.class, () -> command("test", "tenant-a", Duration.ZERO, 1));
    assertThrows(
        IllegalArgumentException.class,
        () -> command("test", "tenant-a", Duration.ofDays(1).plusMillis(1), 1));
    assertThrows(IllegalArgumentException.class, () -> command("tenant-a", 0));
    assertThrows(IllegalArgumentException.class, () -> command("tenant-a", 1_001));
  }

  private static GuardCleanupCommand command(final String tenantId, final int batchSize) {
    return command("test", tenantId, Duration.ofSeconds(1), batchSize);
  }

  private static GuardCleanupCommand command(
      final String environment, final String tenantId, final int batchSize) {
    return command(environment, tenantId, Duration.ofSeconds(1), batchSize);
  }

  private static GuardCleanupCommand command(
      final String environment,
      final String tenantId,
      final Duration safetyGrace,
      final int batchSize) {
    return new GuardCleanupCommand(
        environment,
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
        PersistentGuardIdentity.NQ_DRYRUN_SOURCE,
        tenantId,
        safetyGrace,
        batchSize);
  }
}

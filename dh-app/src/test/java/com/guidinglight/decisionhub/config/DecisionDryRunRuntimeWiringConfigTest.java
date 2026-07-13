package com.guidinglight.decisionhub.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunGuardProperties;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunRuntimeProperties;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardHardCeilings;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * Dry-run runtime CSV binding 的 fail-closed 回归。
 *
 * <p>此测试通过实际 {@link DecisionDryRunRuntimeWiringConfig} bean factory method 验证配置错误在 Spring 装配
 * 边界抛出，而不是被 CSV parser 静默过滤后降级成另一份 source 合同。
 */
class DecisionDryRunRuntimeWiringConfigTest {

  private final DecisionDryRunRuntimeWiringConfig config = new DecisionDryRunRuntimeWiringConfig();
  private final MockEnvironment testEnvironment =
      new MockEnvironment().withProperty("spring.profiles.active", "test");

  @Test
  void canonicalCsvConfigurationBindsAfterOuterWhitespaceIsRemoved() {
    final DecisionDryRunRuntimeProperties properties =
        properties(" NQ_DRYRUN ", " tenant-a : NQ_DRYRUN ");

    assertEquals("NQ_DRYRUN", properties.allowedSources().iterator().next());
    assertEquals("tenant-a::NQ_DRYRUN", properties.allowedTenantSourcePairs().iterator().next());
  }

  @Test
  void whitespaceAndNonCanonicalCsvConfigurationFailDuringBeanCreation() {
    assertThrows(IllegalArgumentException.class, () -> properties(" ", ""));
    assertThrows(IllegalArgumentException.class, () -> properties("NQ_DRYRUN,", ""));
    assertThrows(IllegalArgumentException.class, () -> properties("nq_dryrun", ""));
    assertThrows(IllegalArgumentException.class, () -> properties("Nq_Dryrun", ""));
    assertThrows(IllegalArgumentException.class, () -> properties("unknown-source", ""));
    assertThrows(
        IllegalArgumentException.class, () -> properties("NQ_DRYRUN", "tenant-a:nq_dryrun"));
  }

  @Test
  void enabledPersistentGuardRequiresExplicitBoundedConfiguration() {
    assertThrows(
        IllegalArgumentException.class,
        () -> config.decisionDryRunGuardProperties(true, "", 0, 0, 0, 0, 0));

    final DecisionDryRunGuardProperties properties =
        config.decisionDryRunGuardProperties(true, "test", 1, 20, 30, 600, 3600);

    assertEquals("test", properties.environment());
    assertEquals(20, properties.rateLimitValue());
  }

  @Test
  void frozenHardCeilingsBindAndValuesAboveThemFailDuringBeanCreation() {
    final DecisionDryRunGuardProperties maximum =
        config.decisionDryRunGuardProperties(
            true,
            "test",
            PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS,
            PersistentGuardHardCeilings.MAX_RATE_QUOTA,
            PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS,
            PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS + 1L,
            86400);

    assertEquals(
        PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS, maximum.rateWindowSeconds());
    assertEquals(PersistentGuardHardCeilings.MAX_RATE_QUOTA, maximum.rateLimitValue());
    assertEquals(
        PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS,
        maximum.leaseDuration().toSeconds());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            guardProperties(
                PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS + 1,
                PersistentGuardHardCeilings.MAX_RATE_QUOTA,
                PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            guardProperties(
                PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS,
                PersistentGuardHardCeilings.MAX_RATE_QUOTA + 1,
                PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            guardProperties(
                PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS,
                PersistentGuardHardCeilings.MAX_RATE_QUOTA,
                PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS + 1));
  }

  @Test
  void disabledRuntimeRetainsFailClosedDefaults() {
    final DecisionDryRunGuardProperties properties =
        config.decisionDryRunGuardProperties(false, "", 0, 0, 0, 0, 0);

    assertFalse(properties.runtimeEnabled());
    assertEquals(0, properties.rateWindowSeconds());
    assertEquals(0, properties.rateLimitValue());
  }

  private DecisionDryRunGuardProperties guardProperties(
      final int rateWindowSeconds, final int rateQuota, final int leaseSeconds) {
    return config.decisionDryRunGuardProperties(
        true,
        "test",
        rateWindowSeconds,
        rateQuota,
        leaseSeconds,
        Math.max(leaseSeconds + 1L, 2L),
        86400);
  }

  private DecisionDryRunRuntimeProperties properties(
      final String allowedSources, final String allowedTenantSourcePairs) {
    return config.decisionDryRunRuntimeProperties(
        testEnvironment, false, false, false, allowedSources, allowedTenantSourcePairs, 32768);
  }
}

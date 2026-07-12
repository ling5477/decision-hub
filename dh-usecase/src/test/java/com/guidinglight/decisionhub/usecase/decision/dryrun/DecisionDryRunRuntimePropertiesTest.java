package com.guidinglight.decisionhub.usecase.decision.dryrun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Dry-run runtime source 配置的 fail-closed 回归。
 *
 * <p>这些测试锁定配置 trim 与 request wire value 的不同边界：配置只移除外层空白，而 request source 不允许任何
 * 归一化。这样 properties 层不会再次引入与 HMAC authenticator 不一致的大小写语义。
 */
class DecisionDryRunRuntimePropertiesTest {

  @Test
  void canonicalConfigurationTrimsOuterWhitespaceButPreservesCanonicalCase() {
    final DecisionDryRunRuntimeProperties properties =
        properties(Set.of(" NQ_DRYRUN "), Set.of(" tenant-a : NQ_DRYRUN "));

    assertEquals(Set.of("NQ_DRYRUN"), properties.allowedSources());
    assertEquals(Set.of("tenant-a::NQ_DRYRUN"), properties.allowedTenantSourcePairs());
    assertTrue(properties.sourceAllowed("NQ_DRYRUN"));
    assertTrue(properties.tenantSourceAllowed("tenant-a", "NQ_DRYRUN"));
  }

  @Test
  void requestWireSourceDoesNotTrimOrNormalizeCase() {
    final DecisionDryRunRuntimeProperties properties =
        properties(Set.of("NQ_DRYRUN"), Set.of("tenant-a:NQ_DRYRUN"));

    assertFalse(properties.sourceAllowed("nq_dryrun"));
    assertFalse(properties.sourceAllowed(" NQ_DRYRUN "));
    assertFalse(properties.tenantSourceAllowed("tenant-a", "nq_dryrun"));
    assertFalse(properties.tenantSourceAllowed("tenant-a", " NQ_DRYRUN "));
  }

  @Test
  void nonCanonicalSourceConfigurationFailsClosed() {
    for (final String invalidSource :
        Set.of("nq_dryrun", "Nq_Dryrun", "NQ-DRYRUN", "unknown-source", "", "   ")) {
      assertThrows(
          IllegalArgumentException.class,
          () -> properties(Set.of(invalidSource), Set.of()),
          "source must fail startup validation: " + invalidSource);
    }

  }

  @Test
  void nonCanonicalOrContradictoryTenantSourcePairFailsClosed() {
    for (final String invalidPair :
        Set.of("tenant-a:nq_dryrun", "tenant-a:Nq_Dryrun", "tenant-a:NQ-DRYRUN", "tenant-a:unknown")) {
      assertThrows(
          IllegalArgumentException.class,
          () -> properties(Set.of("NQ_DRYRUN"), Set.of(invalidPair)),
          "pair must fail startup validation: " + invalidPair);
    }

    assertThrows(
        IllegalArgumentException.class,
        () -> properties(Set.of(), Set.of("tenant-a:NQ_DRYRUN")),
        "a tenant/source pair must be represented in the source allowlist");
  }

  private static DecisionDryRunRuntimeProperties properties(
      final Set<String> sources, final Set<String> pairs) {
    return new DecisionDryRunRuntimeProperties(true, false, false, true, sources, pairs, 32768);
  }
}

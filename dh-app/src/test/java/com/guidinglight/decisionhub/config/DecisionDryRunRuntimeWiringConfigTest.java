package com.guidinglight.decisionhub.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunRuntimeProperties;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunGuardProperties;
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
    private final MockEnvironment testEnvironment = new MockEnvironment().withProperty("spring.profiles.active", "test");

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
        assertThrows(IllegalArgumentException.class, () -> properties("NQ_DRYRUN", "tenant-a:nq_dryrun"));
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

    private DecisionDryRunRuntimeProperties properties(
            final String allowedSources, final String allowedTenantSourcePairs) {
        return config.decisionDryRunRuntimeProperties(
                testEnvironment,
                false,
                false,
                false,
                allowedSources,
                allowedTenantSourcePairs,
                32768);
    }
}

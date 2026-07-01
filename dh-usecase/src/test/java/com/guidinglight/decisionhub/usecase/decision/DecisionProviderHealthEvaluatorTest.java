package com.guidinglight.decisionhub.usecase.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderFailureClass;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderHealth;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderHealthStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderLatency;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * K5 provider health evaluator 单元测试，覆盖 mock-only health 到 fail-closed 分类的映射。
 */
final class DecisionProviderHealthEvaluatorTest {

    private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

    private final DecisionProviderHealthEvaluator evaluator =
            new DefaultDecisionProviderHealthEvaluator();

    @Test
    void mockedSignalIsHealthy() {
        final DecisionProviderHealth health =
                evaluator.evaluate(
                        "MOCK_DECISION_PROVIDER",
                        DecisionSignalResult.mock(DecisionAction.NO_TRADE, List.of("MOCK_NO_TRADE")),
                        latency(12L, false),
                        NOW);

        assertEquals(DecisionProviderHealthStatus.HEALTHY, health.status());
        assertEquals(DecisionProviderFailureClass.NONE, health.failureClass());
        assertFalse(health.requiresFailClosed());
    }

    @Test
    void latencyTimeoutOverridesMockSignal() {
        final DecisionProviderHealth health =
                evaluator.evaluate(
                        "MOCK_DECISION_PROVIDER",
                        DecisionSignalResult.mock(DecisionAction.NO_TRADE, List.of("MOCK_NO_TRADE")),
                        latency(1500L, true),
                        NOW);

        assertEquals(DecisionProviderHealthStatus.UNHEALTHY, health.status());
        assertEquals(DecisionProviderFailureClass.TIMEOUT, health.failureClass());
        assertTrue(health.requiresFailClosed());
    }

    @Test
    void successStatusIsUntrustedInMockOnlyK5() {
        final DecisionProviderHealth health =
                evaluator.evaluate(
                        "MOCK_DECISION_PROVIDER",
                        new DecisionSignalResult(
                                ProviderSignalStatus.SUCCESS,
                                DecisionAction.NO_TRADE,
                                List.of("REAL_PROVIDER_SUCCESS_NOT_ALLOWED")),
                        latency(0L, false),
                        NOW);

        assertEquals(DecisionProviderHealthStatus.UNHEALTHY, health.status());
        assertEquals(DecisionProviderFailureClass.UNTRUSTED, health.failureClass());
        assertTrue(health.reasonCodes().contains("REAL_PROVIDER_SUCCESS_FORBIDDEN"));
    }

    private static DecisionProviderLatency latency(final long latencyMs, final boolean timedOut) {
        return new DecisionProviderLatency(
                "MOCK_DECISION_PROVIDER", latencyMs, 1000L, timedOut, NOW);
    }
}

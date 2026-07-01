package com.guidinglight.decisionhub.usecase.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderFailureClass;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderGuardResult;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderLatency;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * K5 provider guard 单元测试，覆盖调用前 budget gate 与调用后 health/latency gate。
 */
final class DecisionProviderGuardTest {

    private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

    @Test
    void beforeProviderCallAllowsWithinBudgetContext() {
        final DecisionProviderGuard guard = new DefaultDecisionProviderGuard();

        final DecisionProviderGuardResult result =
                guard.beforeProviderCall("MOCK_DECISION_PROVIDER", context(), NOW);

        assertFalse(result.requiresFailClosed());
        assertEquals(ProviderSignalStatus.NOT_CALLED, result.providerStatus());
    }

    @Test
    void beforeProviderCallBlocksBudgetExceeded() {
        final DecisionProviderGuard guard =
                new DefaultDecisionProviderGuard(
                        new DefaultDecisionProviderHealthEvaluator(),
                        new DefaultDecisionProviderBudgetGuard(true, 2L, 1L));

        final DecisionProviderGuardResult result =
                guard.beforeProviderCall("MOCK_DECISION_PROVIDER", context(), NOW);

        assertTrue(result.requiresFailClosed());
        assertEquals(ProviderSignalStatus.BUDGET_EXCEEDED, result.providerStatus());
        assertEquals(DecisionProviderFailureClass.BUDGET_EXCEEDED, result.failureClass());
    }

    @Test
    void afterProviderCallAllowsHealthyMockSignal() {
        final DecisionProviderGuardResult result =
                new DefaultDecisionProviderGuard()
                        .afterProviderCall(
                                "MOCK_DECISION_PROVIDER",
                                context(),
                                DecisionSignalResult.mock(DecisionAction.NO_TRADE, List.of("MOCK_NO_TRADE")),
                                latency(0L, false),
                                NOW);

        assertFalse(result.requiresFailClosed());
        assertEquals(ProviderSignalStatus.MOCKED, result.providerStatus());
        assertEquals(DecisionProviderFailureClass.NONE, result.failureClass());
    }

    @Test
    void afterProviderCallBlocksTimeoutSignal() {
        final DecisionProviderGuardResult result =
                new DefaultDecisionProviderGuard()
                        .afterProviderCall(
                                "MOCK_DECISION_PROVIDER",
                                context(),
                                DecisionSignalResult.failure(ProviderSignalStatus.TIMEOUT, "MOCK_TIMEOUT"),
                                latency(0L, false),
                                NOW);

        assertTrue(result.requiresFailClosed());
        assertEquals(ProviderSignalStatus.TIMEOUT, result.providerStatus());
        assertEquals(DecisionProviderFailureClass.TIMEOUT, result.failureClass());
    }

    @Test
    void afterProviderCallBlocksLatencyTimeoutEvenWhenSignalLooksHealthy() {
        final DecisionProviderGuardResult result =
                new DefaultDecisionProviderGuard()
                        .afterProviderCall(
                                "MOCK_DECISION_PROVIDER",
                                context(),
                                DecisionSignalResult.mock(DecisionAction.NO_TRADE, List.of("MOCK_NO_TRADE")),
                                latency(1500L, true),
                                NOW);

        assertTrue(result.requiresFailClosed());
        assertEquals(ProviderSignalStatus.TIMEOUT, result.providerStatus());
        assertEquals(DecisionProviderFailureClass.TIMEOUT, result.failureClass());
    }

    private static DecisionProviderLatency latency(final long latencyMs, final boolean timedOut) {
        return new DecisionProviderLatency(
                "MOCK_DECISION_PROVIDER", latencyMs, 1000L, timedOut, NOW);
    }

    private static DecisionContext context() {
        final DecisionRequest request =
                DecisionRequest.readOnlyRecommendation(
                        "req-1",
                        "trace-1",
                        "tenant-1",
                        "codex-test",
                        new DecisionSubject("BTC-USDT", "CRYPTO", "1h", "strategy-1", "research-1"),
                        "context://safe",
                        new DecisionContextSnapshot("snapshot-1", NOW, List.of("evidence://case-1")),
                        NOW);
        return new DefaultDecisionContextBuilder().build(request);
    }
}

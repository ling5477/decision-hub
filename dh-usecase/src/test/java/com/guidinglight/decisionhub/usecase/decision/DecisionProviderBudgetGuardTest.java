package com.guidinglight.decisionhub.usecase.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderBudget;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderBudgetStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * K5 provider budget guard 单元测试，验证本地估算阈值和 disabled fail-closed。
 */
final class DecisionProviderBudgetGuardTest {

    private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

    @Test
    void defaultBudgetIsWithinBudgetForOneMockCall() {
        final DecisionProviderBudget budget =
                new DefaultDecisionProviderBudgetGuard().check("MOCK_DECISION_PROVIDER", context());

        assertEquals(DecisionProviderBudgetStatus.WITHIN_BUDGET, budget.status());
        assertEquals(1L, budget.estimatedUnits());
        assertEquals(1L, budget.budgetLimitUnits());
    }

    @Test
    void disabledProviderFailsClosedBeforeProviderCall() {
        final DecisionProviderBudget budget =
                new DefaultDecisionProviderBudgetGuard(false, 1L, 1L)
                        .check("MOCK_DECISION_PROVIDER", context());

        assertEquals(DecisionProviderBudgetStatus.DISABLED, budget.status());
        assertTrue(budget.requiresFailClosed());
        assertTrue(budget.reasonCodes().contains("PROVIDER_DISABLED"));
    }

    @Test
    void estimatedCostAboveLimitFailsClosed() {
        final DecisionProviderBudget budget =
                new DefaultDecisionProviderBudgetGuard(true, 2L, 1L)
                        .check("MOCK_DECISION_PROVIDER", context());

        assertEquals(DecisionProviderBudgetStatus.BUDGET_EXCEEDED, budget.status());
        assertTrue(budget.requiresFailClosed());
        assertTrue(budget.reasonCodes().contains("PROVIDER_BUDGET_EXCEEDED"));
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

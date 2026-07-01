package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionProviderBudget;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderBudgetStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderFailureClass;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderGuardResult;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderHealth;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderLatency;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * K5 默认 provider guard。
 *
 * <p>该 guard 只处理 mock provider 的本地 enabled/budget/latency/health 规则。任何 disabled、unhealthy、
 * timeout、untrusted、budget exceeded 或 invalid signal 都会转为 fail-closed 结果；不会调用真实 provider、
 * HTTP、NQ、LLM、LangGraph 或交易所。
 */
public final class DefaultDecisionProviderGuard implements DecisionProviderGuard {

    private final DecisionProviderHealthEvaluator healthEvaluator;
    private final DecisionProviderBudgetGuard budgetGuard;

    /**
     * 创建默认 provider guard。
     */
    public DefaultDecisionProviderGuard() {
        this(new DefaultDecisionProviderHealthEvaluator(), new DefaultDecisionProviderBudgetGuard());
    }

    /**
     * 创建可注入依赖的 provider guard。
     *
     * @param healthEvaluator health 评估器
     * @param budgetGuard     budget guard
     */
    public DefaultDecisionProviderGuard(
            final DecisionProviderHealthEvaluator healthEvaluator,
            final DecisionProviderBudgetGuard budgetGuard) {
        this.healthEvaluator = Objects.requireNonNull(healthEvaluator, "healthEvaluator");
        this.budgetGuard = Objects.requireNonNull(budgetGuard, "budgetGuard");
    }

    @Override
    public DecisionProviderGuardResult beforeProviderCall(
            final String providerName, final DecisionContext context, final Instant checkedAt) {
        final DecisionProviderBudget budget = budgetGuard.check(providerName, context);
        if (!budget.requiresFailClosed()) {
            return DecisionProviderGuardResult.allowed(
                    providerName,
                    ProviderSignalStatus.NOT_CALLED,
                    null,
                    budget,
                    null,
                    budget.reasonCodes());
        }
        final DecisionProviderFailureClass failureClass = failureClassForBudget(budget.status());
        return DecisionProviderGuardResult.blocked(
                providerName,
                providerStatusForFailure(failureClass),
                failureClass,
                null,
                budget,
                null,
                budget.reasonCodes());
    }

    @Override
    public DecisionProviderGuardResult afterProviderCall(
            final String providerName,
            final DecisionContext context,
            final DecisionSignalResult signal,
            final DecisionProviderLatency latency,
            final Instant checkedAt) {
        final DecisionProviderBudget budget = budgetGuard.check(providerName, context);
        final DecisionProviderHealth health =
                healthEvaluator.evaluate(providerName, signal, latency, checkedAt);
        final List<String> reasons = merged(budget.reasonCodes(), health.reasonCodes());
        if (budget.requiresFailClosed()) {
            final DecisionProviderFailureClass failureClass = failureClassForBudget(budget.status());
            return DecisionProviderGuardResult.blocked(
                    providerName,
                    providerStatusForFailure(failureClass),
                    failureClass,
                    health,
                    budget,
                    latency,
                    reasons);
        }
        if (latency != null && latency.requiresFailClosed()) {
            return DecisionProviderGuardResult.blocked(
                    providerName,
                    ProviderSignalStatus.TIMEOUT,
                    DecisionProviderFailureClass.TIMEOUT,
                    health,
                    budget,
                    latency,
                    reasons);
        }
        if (health.requiresFailClosed()) {
            return DecisionProviderGuardResult.blocked(
                    providerName,
                    providerStatusForFailure(health.failureClass()),
                    health.failureClass(),
                    health,
                    budget,
                    latency,
                    reasons);
        }
        if (signal == null) {
            return DecisionProviderGuardResult.blocked(
                    providerName,
                    ProviderSignalStatus.FAILED,
                    DecisionProviderFailureClass.FAILED,
                    health,
                    budget,
                    latency,
                    reasons);
        }
        if (signal.requiresAbstain()) {
            final DecisionProviderFailureClass failureClass = failureClassForStatus(signal.status());
            return DecisionProviderGuardResult.blocked(
                    providerName,
                    signal.status(),
                    failureClass,
                    health,
                    budget,
                    latency,
                    reasons);
        }
        return DecisionProviderGuardResult.allowed(
                providerName, signal.status(), health, budget, latency, reasons);
    }

    private static DecisionProviderFailureClass failureClassForBudget(
            final DecisionProviderBudgetStatus status) {
        return switch (status) {
            case WITHIN_BUDGET -> DecisionProviderFailureClass.NONE;
            case BUDGET_EXCEEDED -> DecisionProviderFailureClass.BUDGET_EXCEEDED;
            case DISABLED -> DecisionProviderFailureClass.DISABLED;
            case BUDGET_UNKNOWN -> DecisionProviderFailureClass.UNKNOWN;
        };
    }

    private static DecisionProviderFailureClass failureClassForStatus(
            final ProviderSignalStatus status) {
        return switch (status) {
            case TIMEOUT -> DecisionProviderFailureClass.TIMEOUT;
            case FAILED -> DecisionProviderFailureClass.FAILED;
            case UNTRUSTED, SUCCESS -> DecisionProviderFailureClass.UNTRUSTED;
            case BUDGET_EXCEEDED -> DecisionProviderFailureClass.BUDGET_EXCEEDED;
            case DISABLED -> DecisionProviderFailureClass.DISABLED;
            case NOT_CALLED -> DecisionProviderFailureClass.UNKNOWN;
            case MOCKED -> DecisionProviderFailureClass.NONE;
        };
    }

    private static ProviderSignalStatus providerStatusForFailure(
            final DecisionProviderFailureClass failureClass) {
        return switch (failureClass) {
            case TIMEOUT -> ProviderSignalStatus.TIMEOUT;
            case UNTRUSTED -> ProviderSignalStatus.UNTRUSTED;
            case DISABLED -> ProviderSignalStatus.DISABLED;
            case BUDGET_EXCEEDED -> ProviderSignalStatus.BUDGET_EXCEEDED;
            case NONE -> ProviderSignalStatus.MOCKED;
            case FAILED, INVALID_SIGNAL, UNKNOWN -> ProviderSignalStatus.FAILED;
        };
    }

    private static List<String> merged(final List<String> first, final List<String> second) {
        final List<String> merged = new ArrayList<>();
        if (first != null) {
            merged.addAll(first);
        }
        if (second != null) {
            merged.addAll(second);
        }
        return List.copyOf(merged);
    }
}

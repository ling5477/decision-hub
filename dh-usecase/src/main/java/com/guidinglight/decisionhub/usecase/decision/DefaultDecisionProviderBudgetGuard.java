package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionProviderBudget;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderBudgetStatus;

import java.util.List;

/**
 * K5 默认 provider budget guard。
 *
 * <p>首版只使用固定本地估算单位：一次 mock provider 调用默认消耗 1 unit，默认 budgetLimit 也是 1 unit。
 * 这能覆盖 budget fail-closed 语义，同时不引入真实 provider 计费、token 统计或外部依赖。
 */
public final class DefaultDecisionProviderBudgetGuard implements DecisionProviderBudgetGuard {

    private final boolean providerEnabled;
    private final long estimatedUnitsPerCall;
    private final long budgetLimitUnits;

    /**
     * 创建默认预算 guard：provider enabled，单次调用 1 unit，阈值 1 unit。
     */
    public DefaultDecisionProviderBudgetGuard() {
        this(true, 1L, 1L);
    }

    /**
     * 创建可测试的预算 guard。
     *
     * @param providerEnabled       本地 provider gate 是否开启
     * @param estimatedUnitsPerCall 单次 mock 调用的 deterministic 估算单位
     * @param budgetLimitUnits      本轮允许预算单位
     */
    public DefaultDecisionProviderBudgetGuard(
            final boolean providerEnabled,
            final long estimatedUnitsPerCall,
            final long budgetLimitUnits) {
        if (estimatedUnitsPerCall < 0) {
            throw new IllegalArgumentException("estimatedUnitsPerCall must not be negative");
        }
        if (budgetLimitUnits < 0) {
            throw new IllegalArgumentException("budgetLimitUnits must not be negative");
        }
        this.providerEnabled = providerEnabled;
        this.estimatedUnitsPerCall = estimatedUnitsPerCall;
        this.budgetLimitUnits = budgetLimitUnits;
    }

    @Override
    public DecisionProviderBudget check(final String providerName, final DecisionContext context) {
        if (!providerEnabled) {
            return new DecisionProviderBudget(
                    providerName,
                    DecisionProviderBudgetStatus.DISABLED,
                    0L,
                    budgetLimitUnits,
                    List.of("PROVIDER_DISABLED"));
        }
        if (context == null || !context.hasEvidence()) {
            return new DecisionProviderBudget(
                    providerName,
                    DecisionProviderBudgetStatus.BUDGET_UNKNOWN,
                    estimatedUnitsPerCall,
                    budgetLimitUnits,
                    List.of("BUDGET_CONTEXT_MISSING"));
        }
        if (estimatedUnitsPerCall > budgetLimitUnits) {
            return new DecisionProviderBudget(
                    providerName,
                    DecisionProviderBudgetStatus.BUDGET_EXCEEDED,
                    estimatedUnitsPerCall,
                    budgetLimitUnits,
                    List.of("PROVIDER_BUDGET_EXCEEDED"));
        }
        return new DecisionProviderBudget(
                providerName,
                DecisionProviderBudgetStatus.WITHIN_BUDGET,
                estimatedUnitsPerCall,
                budgetLimitUnits,
                List.of("PROVIDER_BUDGET_OK"));
    }
}

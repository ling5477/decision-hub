package com.guidinglight.decisionhub.domain.decision;

/**
 * K5 provider budget 本地估算结果。
 *
 * <p>预算状态只用于控制 mock provider 是否允许被调用；不读取真实账单、不保存 provider credential、不表达真实模型计费。
 */
public enum DecisionProviderBudgetStatus {
    /**
     * 本地估算仍在本轮允许预算内。
     */
    WITHIN_BUDGET,

    /**
     * 本地估算超过本轮阈值，必须 fail-closed。
     */
    BUDGET_EXCEEDED,

    /**
     * 无法计算预算，必须 fail-closed，避免未知成本继续扩散。
     */
    BUDGET_UNKNOWN,

    /**
     * provider 被禁用，因此预算检查也处于禁用态。
     */
    DISABLED
}

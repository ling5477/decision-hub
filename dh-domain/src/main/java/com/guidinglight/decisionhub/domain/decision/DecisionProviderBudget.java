package com.guidinglight.decisionhub.domain.decision;

import java.util.List;
import java.util.Objects;

/**
 * K5 provider budget 本地估算值。
 *
 * <p>预算只用于本地 mock provider gate。estimatedUnits 与 budgetLimitUnits 是 deterministic 测试用单位，
 * 不是真实费用、真实 token 用量或外部 provider 账单。
 */
public record DecisionProviderBudget(
        String providerName,
        DecisionProviderBudgetStatus status,
        long estimatedUnits,
        long budgetLimitUnits,
        List<String> reasonCodes) {

    /**
     * 校验预算估算字段，避免负数或未知 provider 名称进入审计。
     */
    public DecisionProviderBudget {
        providerName = requireText(providerName, "providerName");
        status = Objects.requireNonNull(status, "status");
        if (estimatedUnits < 0) {
            throw new IllegalArgumentException("estimatedUnits must not be negative");
        }
        if (budgetLimitUnits < 0) {
            throw new IllegalArgumentException("budgetLimitUnits must not be negative");
        }
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    }

    /**
     * 返回预算状态是否要求 fail-closed。
     */
    public boolean requiresFailClosed() {
        return status != DecisionProviderBudgetStatus.WITHIN_BUDGET;
    }

    private static String requireText(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return checked;
    }
}

package com.guidinglight.decisionhub.domain.decision;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * K5 provider health 观测值。
 *
 * <p>该对象只保存 provider 名称、健康状态、失败分类、原因码和观测时间。它不保存真实 provider 原始响应、
 * 外部模型上下文、credential、token、HTTP endpoint 或 NQ 数据。
 */
public record DecisionProviderHealth(
        String providerName,
        DecisionProviderHealthStatus status,
        DecisionProviderFailureClass failureClass,
        List<String> reasonCodes,
        Instant observedAt) {

    /**
     * 规范化 provider health 观测，保证 replay 与审计字段可稳定读取。
     */
    public DecisionProviderHealth {
        providerName = requireText(providerName, "providerName");
        status = Objects.requireNonNull(status, "status");
        failureClass = failureClass == null ? DecisionProviderFailureClass.NONE : failureClass;
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
        observedAt = Objects.requireNonNull(observedAt, "observedAt");
    }

    /**
     * 返回该 health 状态是否要求调用方 fail-closed。
     */
    public boolean requiresFailClosed() {
        return status == DecisionProviderHealthStatus.UNHEALTHY
                || status == DecisionProviderHealthStatus.DISABLED
                || status == DecisionProviderHealthStatus.UNKNOWN;
    }

    private static String requireText(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return checked;
    }
}

package com.guidinglight.decisionhub.domain.decision;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * K5 provider guard 汇总结果。
 *
 * <p>该结果把 budget、health、latency 与失败分类收口成一个审计对象，供 orchestrator 决定是否继续只读
 * risk/output 编排。它不包含 raw provider response、secret、token、credential 或任何交易执行字段。
 */
public record DecisionProviderGuardResult(
        String providerName,
        boolean allowed,
        ProviderSignalStatus providerStatus,
        DecisionProviderFailureClass failureClass,
        DecisionProviderHealth health,
        DecisionProviderBudget budget,
        DecisionProviderLatency latency,
        List<String> reasonCodes) {

    /**
     * 规范化 guard 结果，并确保失败态不会被误标为 allowed。
     */
    public DecisionProviderGuardResult {
        providerName = requireText(providerName, "providerName");
        providerStatus = Objects.requireNonNull(providerStatus, "providerStatus");
        failureClass = failureClass == null ? DecisionProviderFailureClass.NONE : failureClass;
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
        if (allowed && failureClass != DecisionProviderFailureClass.NONE) {
            throw new IllegalArgumentException("allowed guard result must use NONE failureClass");
        }
    }

    /**
     * 创建允许继续调用或继续编排的 guard 结果。
     */
    public static DecisionProviderGuardResult allowed(
            final String providerName,
            final ProviderSignalStatus providerStatus,
            final DecisionProviderHealth health,
            final DecisionProviderBudget budget,
            final DecisionProviderLatency latency,
            final List<String> reasonCodes) {
        return new DecisionProviderGuardResult(
                providerName,
                true,
                providerStatus,
                DecisionProviderFailureClass.NONE,
                health,
                budget,
                latency,
                reasonCodes);
    }

    /**
     * 创建必须 fail-closed 的 guard 结果。
     */
    public static DecisionProviderGuardResult blocked(
            final String providerName,
            final ProviderSignalStatus providerStatus,
            final DecisionProviderFailureClass failureClass,
            final DecisionProviderHealth health,
            final DecisionProviderBudget budget,
            final DecisionProviderLatency latency,
            final List<String> reasonCodes) {
        final DecisionProviderFailureClass checkedFailure =
                failureClass == null ? DecisionProviderFailureClass.UNKNOWN : failureClass;
        return new DecisionProviderGuardResult(
                providerName,
                false,
                providerStatus,
                checkedFailure,
                health,
                budget,
                latency,
                merged(reasonCodes, checkedFailure.name()));
    }

    /**
     * 返回 provider call log 的安全错误码。
     */
    public String errorCode() {
        return allowed ? null : failureClass.name();
    }

    /**
     * 返回是否必须 fail-closed。
     */
    public boolean requiresFailClosed() {
        return !allowed;
    }

    private static List<String> merged(final List<String> reasonCodes, final String fallback) {
        final List<String> merged = new ArrayList<>();
        if (reasonCodes != null) {
            merged.addAll(reasonCodes);
        }
        if (merged.isEmpty() && fallback != null) {
            merged.add(fallback);
        }
        return List.copyOf(merged);
    }

    private static String requireText(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return checked;
    }
}

package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionProviderFailureClass;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderHealth;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderHealthStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderLatency;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * K5 默认 provider health evaluator。
 *
 * <p>该实现把 mock signal status 与 latency policy 映射为 health/failure class。它不会访问网络、模型、
 * NQ、数据库或 credential；`SUCCESS` 在 K5 mock-only 语义下会被视为不可信状态。
 */
public final class DefaultDecisionProviderHealthEvaluator
        implements DecisionProviderHealthEvaluator {

    @Override
    public DecisionProviderHealth evaluate(
            final String providerName,
            final DecisionSignalResult signal,
            final DecisionProviderLatency latency,
            final Instant observedAt) {
        final Instant checkedAt = Objects.requireNonNull(observedAt, "observedAt");
        if (latency != null && latency.requiresFailClosed()) {
            return health(
                    providerName,
                    DecisionProviderHealthStatus.UNHEALTHY,
                    DecisionProviderFailureClass.TIMEOUT,
                    List.of("PROVIDER_LATENCY_TIMEOUT"),
                    checkedAt);
        }
        if (signal == null) {
            return health(
                    providerName,
                    DecisionProviderHealthStatus.UNHEALTHY,
                    DecisionProviderFailureClass.FAILED,
                    List.of("PROVIDER_SIGNAL_MISSING"),
                    checkedAt);
        }
        return switch (signal.status()) {
            case MOCKED -> health(
                    providerName,
                    DecisionProviderHealthStatus.HEALTHY,
                    DecisionProviderFailureClass.NONE,
                    signal.reasonCodes(),
                    checkedAt);
            case TIMEOUT -> health(
                    providerName,
                    DecisionProviderHealthStatus.UNHEALTHY,
                    DecisionProviderFailureClass.TIMEOUT,
                    withFallback(signal.reasonCodes(), "PROVIDER_TIMEOUT"),
                    checkedAt);
            case FAILED -> health(
                    providerName,
                    DecisionProviderHealthStatus.UNHEALTHY,
                    DecisionProviderFailureClass.FAILED,
                    withFallback(signal.reasonCodes(), "PROVIDER_FAILED"),
                    checkedAt);
            case UNTRUSTED -> health(
                    providerName,
                    DecisionProviderHealthStatus.UNHEALTHY,
                    DecisionProviderFailureClass.UNTRUSTED,
                    withFallback(signal.reasonCodes(), "PROVIDER_UNTRUSTED"),
                    checkedAt);
            case BUDGET_EXCEEDED -> health(
                    providerName,
                    DecisionProviderHealthStatus.UNHEALTHY,
                    DecisionProviderFailureClass.BUDGET_EXCEEDED,
                    withFallback(signal.reasonCodes(), "PROVIDER_BUDGET_EXCEEDED"),
                    checkedAt);
            case DISABLED -> health(
                    providerName,
                    DecisionProviderHealthStatus.DISABLED,
                    DecisionProviderFailureClass.DISABLED,
                    withFallback(signal.reasonCodes(), "PROVIDER_DISABLED"),
                    checkedAt);
            case SUCCESS -> health(
                    providerName,
                    DecisionProviderHealthStatus.UNHEALTHY,
                    DecisionProviderFailureClass.UNTRUSTED,
                    List.of("REAL_PROVIDER_SUCCESS_FORBIDDEN"),
                    checkedAt);
            case NOT_CALLED -> health(
                    providerName,
                    DecisionProviderHealthStatus.UNKNOWN,
                    DecisionProviderFailureClass.UNKNOWN,
                    List.of("PROVIDER_NOT_CALLED"),
                    checkedAt);
        };
    }

    private static DecisionProviderHealth health(
            final String providerName,
            final DecisionProviderHealthStatus status,
            final DecisionProviderFailureClass failureClass,
            final List<String> reasonCodes,
            final Instant observedAt) {
        return new DecisionProviderHealth(providerName, status, failureClass, reasonCodes, observedAt);
    }

    private static List<String> withFallback(final List<String> reasonCodes, final String fallback) {
        final List<String> merged = new ArrayList<>();
        if (reasonCodes != null) {
            merged.addAll(reasonCodes);
        }
        if (merged.isEmpty()) {
            merged.add(fallback);
        }
        return List.copyOf(merged);
    }
}

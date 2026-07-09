package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.time.Instant;

/**
 * Provider health 的内部安全摘要。
 *
 * <p>status 只表示 observability / readiness state，不表示真实 provider 已授权、已上线、可交易或 LIVE
 * enabled。
 *
 * @param tenantId                tenant 边界。
 * @param providerRef             provider safe ref。
 * @param modelGatewayVersionRef  model gateway version safe ref。
 * @param status                  readiness / health status。
 * @param failureClassification   failure classification。
 * @param latencyBudgetSummary    latency / budget safe summary。
 * @param lastObservedAt          最近观察时间。
 */
public record ProviderHealthSummary(
        String tenantId,
        String providerRef,
        String modelGatewayVersionRef,
        ProviderReadinessStatus status,
        ProviderFailureClassification failureClassification,
        ProviderLatencyBudgetSummary latencyBudgetSummary,
        Instant lastObservedAt) {
}

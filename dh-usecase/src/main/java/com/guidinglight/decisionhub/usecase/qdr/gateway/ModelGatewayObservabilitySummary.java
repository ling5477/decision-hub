package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.time.Instant;

/**
 * Model gateway observability 的内部安全摘要。
 *
 * <p>该 contract 只保存 tenant/source/provider refs、hash、latency/budget summary、failure classification、
 * trust decision 与 readiness signal，不保存 raw prompt、provider raw output、credential-like material 或
 * 任何交易执行动作。
 *
 * @param tenantId                tenant 边界。
 * @param traceId                 traceId。
 * @param sourceRequestId         source request safe ref。
 * @param modelGatewayVersionRef  model gateway version safe ref。
 * @param providerRef             provider safe ref。
 * @param providerSummaryHash     provider summary SHA-256 hash。
 * @param latencyBudgetSummary    latency / budget safe summary。
 * @param failureClassification   failure classification。
 * @param trustDecisionSummary    trust decision summary。
 * @param readinessSignal         readiness signal。
 * @param createdAt               创建时间。
 */
public record ModelGatewayObservabilitySummary(
        String tenantId,
        String traceId,
        String sourceRequestId,
        String modelGatewayVersionRef,
        String providerRef,
        String providerSummaryHash,
        ProviderLatencyBudgetSummary latencyBudgetSummary,
        ProviderFailureClassification failureClassification,
        ProviderTrustDecisionSummary trustDecisionSummary,
        ProviderReadinessSignal readinessSignal,
        Instant createdAt) {
}

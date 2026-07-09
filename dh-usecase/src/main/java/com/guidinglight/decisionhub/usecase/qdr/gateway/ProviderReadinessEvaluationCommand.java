package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.time.Instant;

/**
 * Provider readiness policy evaluation 的输入命令。
 *
 * <p>该命令只允许携带 tenant/source/provider/model gateway safe refs、summary hash、B1/B2 safe
 * summary/view、policyVersion、traceId、sourceRequestId 和时间戳。命令本身不读取凭证、不读取 provider
 * 原始响应、不调用 HTTP、不访问 NQ；具体 redaction 与 fail-closed 判定由
 * {@link ProviderReadinessGuardService} 统一执行。
 *
 * @param tenantId                 tenant 边界。
 * @param sourceRef                source safe ref。
 * @param providerRef              provider safe ref。
 * @param modelGatewayVersionRef   model gateway version safe ref。
 * @param providerSummaryHash      provider summary SHA-256 hash。
 * @param failureClassification    provider failure classification。
 * @param latencyBudgetSummary     latency / budget safe summary。
 * @param trustDecisionSummary     trust decision safe summary。
 * @param readinessSignal          readiness safe signal。
 * @param providerHealthView       B2 provider health read model safe view。
 * @param policyVersion            policy version safe ref。
 * @param traceId                  traceId。
 * @param sourceRequestId          source request safe ref。
 * @param createdAt                evaluation 创建时间。
 * @param observedAt               evidence 观察时间。
 */
public record ProviderReadinessEvaluationCommand(
        String tenantId,
        String sourceRef,
        String providerRef,
        String modelGatewayVersionRef,
        String providerSummaryHash,
        ProviderFailureClassification failureClassification,
        ProviderLatencyBudgetSummary latencyBudgetSummary,
        ProviderTrustDecisionSummary trustDecisionSummary,
        ProviderReadinessSignal readinessSignal,
        ProviderHealthReadModelView providerHealthView,
        String policyVersion,
        String traceId,
        String sourceRequestId,
        Instant createdAt,
        Instant observedAt) {
}

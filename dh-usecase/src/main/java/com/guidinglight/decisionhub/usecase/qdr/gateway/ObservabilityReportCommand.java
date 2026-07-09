package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.time.Instant;

/**
 * Stage-QDR-5 B4 observability report 生成命令。
 *
 * <p>命令只接收 tenant/source safe refs、B1 observability summary、B2 provider health read model view、
 * B3 readiness evaluation result 和 report 时间戳。不接收 raw prompt、provider 原始响应、credential、
 * raw request / response payload、订单指令、执行指令或 NQ mutation 指令。
 *
 * @param tenantId                  tenant 边界。
 * @param sourceRef                 source safe ref。
 * @param observabilitySummary      B1 observability safe summary。
 * @param providerHealthView        B2 provider health read model safe view。
 * @param readinessEvaluationResult B3 readiness evaluation result；缺失时 B4 只能输出 SKIPPED。
 * @param createdAt                 report 创建时间。
 * @param observedAt                evidence 观察时间。
 */
public record ObservabilityReportCommand(
        String tenantId,
        String sourceRef,
        ModelGatewayObservabilitySummary observabilitySummary,
        ProviderHealthReadModelView providerHealthView,
        ProviderReadinessEvaluationResult readinessEvaluationResult,
        Instant createdAt,
        Instant observedAt) {
}

package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.time.Instant;
import java.util.Objects;

/**
 * Stage-QDR-5 B4 model gateway observability / acceptance support report。
 *
 * <p>Report 只输出 safe refs、tenantId、providerRef、sourceRef、modelGatewayVersionRef、traceId、
 * sourceRequestId、providerSummaryHash、summary sections、acceptance status、时间戳和 redacted summary。
 * Report 不输出 raw prompt、provider 原始响应、credential、raw request / response payload、订单指令、执行指令、
 * provider credential status、真实 provider/HTTP enablement、LIVE permission、trading permission 或交易信号。
 *
 * @param tenantId                     tenant 边界。
 * @param sourceRef                    source safe ref。
 * @param providerRef                  provider safe ref。
 * @param modelGatewayVersionRef       model gateway version safe ref。
 * @param providerSummaryHash          provider summary SHA-256 hash。
 * @param traceId                      traceId。
 * @param sourceRequestId              source request safe ref。
 * @param providerHealth               provider health report section。
 * @param providerReadiness            provider readiness report section。
 * @param failureClassificationSummary failure classification summary。
 * @param latencyBudgetReport          latency / budget report。
 * @param trustDecisionReport          trust decision report。
 * @param acceptanceSummary            acceptance summary。
 * @param acceptanceEvidence           Stage-QDR-5 acceptance evidence。
 * @param createdAt                    report 创建时间。
 * @param observedAt                   evidence 观察时间。
 * @param redactedSummary              脱敏摘要。
 */
public record ModelGatewayObservabilityReport(
        String tenantId,
        String sourceRef,
        String providerRef,
        String modelGatewayVersionRef,
        String providerSummaryHash,
        String traceId,
        String sourceRequestId,
        ProviderHealthReportSection providerHealth,
        ProviderReadinessReportSection providerReadiness,
        ProviderFailureClassificationSummary failureClassificationSummary,
        ProviderLatencyBudgetReport latencyBudgetReport,
        ProviderTrustDecisionReport trustDecisionReport,
        ProviderReadinessAcceptanceSummary acceptanceSummary,
        StageQdr5AcceptanceEvidence acceptanceEvidence,
        Instant createdAt,
        Instant observedAt,
        String redactedSummary) {

    /**
     * 校验 report 输出只有 safe refs、enum、hash 与 redacted summary。
     */
    public ModelGatewayObservabilityReport {
        tenantId = ObservabilityReportSafety.requireSafeText(tenantId, "tenantId");
        sourceRef = ObservabilityReportSafety.requireSafeText(sourceRef, "sourceRef");
        providerRef = ObservabilityReportSafety.requireSafeText(providerRef, "providerRef");
        modelGatewayVersionRef =
                ObservabilityReportSafety.requireSafeText(modelGatewayVersionRef, "modelGatewayVersionRef");
        providerSummaryHash =
                ObservabilityReportSafety.requireSha256Hex(providerSummaryHash, "providerSummaryHash");
        traceId = ObservabilityReportSafety.requireSafeText(traceId, "traceId");
        sourceRequestId = ObservabilityReportSafety.requireSafeText(sourceRequestId, "sourceRequestId");
        Objects.requireNonNull(providerHealth, "providerHealth");
        Objects.requireNonNull(providerReadiness, "providerReadiness");
        Objects.requireNonNull(failureClassificationSummary, "failureClassificationSummary");
        Objects.requireNonNull(latencyBudgetReport, "latencyBudgetReport");
        Objects.requireNonNull(trustDecisionReport, "trustDecisionReport");
        Objects.requireNonNull(acceptanceSummary, "acceptanceSummary");
        Objects.requireNonNull(acceptanceEvidence, "acceptanceEvidence");
        ObservabilityReportSafety.requireInstant(createdAt, "createdAt");
        ObservabilityReportSafety.requireInstant(observedAt, "observedAt");
        redactedSummary = ObservabilityReportSafety.requireSafeText(redactedSummary, "redactedSummary");
    }

    /**
     * 便捷返回 B4 acceptance status。
     *
     * @return PASS / WARN / FAIL / SKIPPED。
     */
    public ProviderReadinessAcceptanceStatus acceptanceStatus() {
        return acceptanceSummary.acceptanceStatus();
    }
}

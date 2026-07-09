package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.Objects;

/**
 * B4 provider readiness acceptance support 摘要。
 *
 * <p>该摘要只用于 Stage-QDR-5 final close 前的内部验收支持。`PASS` 只是内部 evidence 通过，不代表 provider
 * 授权、真实 provider/HTTP 启用、LIVE permission、交易许可或 NQ execution 许可。
 *
 * @param acceptanceStatus       PASS / WARN / FAIL / SKIPPED。
 * @param readinessDecision      readiness decision。
 * @param readinessFinding       readiness finding safe summary。
 * @param readinessPolicyVersion readiness policy version safe ref。
 * @param traceId                traceId。
 * @param sourceRequestId        source request safe ref。
 * @param providerRef            provider safe ref。
 * @param redactedSummary        脱敏摘要。
 */
public record ProviderReadinessAcceptanceSummary(
        ProviderReadinessAcceptanceStatus acceptanceStatus,
        ProviderReadinessDecision readinessDecision,
        String readinessFinding,
        String readinessPolicyVersion,
        String traceId,
        String sourceRequestId,
        String providerRef,
        String redactedSummary) {

    /**
     * 校验 acceptance summary 只输出内部验收 evidence。
     */
    public ProviderReadinessAcceptanceSummary {
        acceptanceStatus = Objects.requireNonNull(acceptanceStatus, "acceptanceStatus");
        readinessDecision = Objects.requireNonNull(readinessDecision, "readinessDecision");
        readinessFinding = ObservabilityReportSafety.requireSafeText(readinessFinding, "readinessFinding");
        readinessPolicyVersion =
                ObservabilityReportSafety.requireSafeText(readinessPolicyVersion, "readinessPolicyVersion");
        traceId = ObservabilityReportSafety.requireSafeText(traceId, "traceId");
        sourceRequestId = ObservabilityReportSafety.requireSafeText(sourceRequestId, "sourceRequestId");
        providerRef = ObservabilityReportSafety.requireSafeText(providerRef, "providerRef");
        redactedSummary = ObservabilityReportSafety.requireSafeText(redactedSummary, "redactedSummary");
    }
}

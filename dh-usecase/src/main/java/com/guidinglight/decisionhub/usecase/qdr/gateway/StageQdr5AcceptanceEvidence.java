package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.time.Instant;
import java.util.Objects;

/**
 * Stage-QDR-5 B4 final close 前内部验收 evidence。
 *
 * <p>该 evidence 只记录 report 是否满足 Stage-QDR-5 internal acceptance support；它不是 provider
 * authorization、不是真实 provider enablement、不是真实 HTTP enablement、不是 LIVE permission、不是交易许可，
 * 也不是 NQ execution approval。
 *
 * @param tenantId                    tenant 边界。
 * @param providerRef                 provider safe ref。
 * @param traceId                     traceId。
 * @param sourceRequestId             source request safe ref。
 * @param acceptanceStatus            PASS / WARN / FAIL / SKIPPED。
 * @param acceptanceEvidenceRef       acceptance safe ref。
 * @param securityBoundaryEvidenceRef security boundary safe ref。
 * @param createdAt                   evidence 创建时间。
 */
public record StageQdr5AcceptanceEvidence(
        String tenantId,
        String providerRef,
        String traceId,
        String sourceRequestId,
        ProviderReadinessAcceptanceStatus acceptanceStatus,
        String acceptanceEvidenceRef,
        String securityBoundaryEvidenceRef,
        Instant createdAt) {

    /**
     * 校验 Stage-QDR-5 B4 acceptance evidence 的只读边界。
     */
    public StageQdr5AcceptanceEvidence {
        tenantId = ObservabilityReportSafety.requireSafeText(tenantId, "tenantId");
        providerRef = ObservabilityReportSafety.requireSafeText(providerRef, "providerRef");
        traceId = ObservabilityReportSafety.requireSafeText(traceId, "traceId");
        sourceRequestId = ObservabilityReportSafety.requireSafeText(sourceRequestId, "sourceRequestId");
        acceptanceStatus = Objects.requireNonNull(acceptanceStatus, "acceptanceStatus");
        acceptanceEvidenceRef =
                ObservabilityReportSafety.requireSafeText(acceptanceEvidenceRef, "acceptanceEvidenceRef");
        securityBoundaryEvidenceRef = ObservabilityReportSafety.requireSafeText(
                securityBoundaryEvidenceRef, "securityBoundaryEvidenceRef");
        createdAt = ObservabilityReportSafety.requireInstant(createdAt, "createdAt");
    }
}

package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.Objects;

/**
 * B4 report 中的 readiness finding safe view。
 *
 * <p>该 view 只暴露 severity、safe code 与 summary ref，不包含 raw prompt、provider 原始响应、credential、
 * order instruction、execution instruction 或 mutation instruction。
 *
 * @param severity   finding severity。
 * @param code       safe finding code。
 * @param summaryRef safe finding summary ref。
 */
public record ProviderReadinessEvidenceView(
        ProviderReadinessSeverity severity, String code, String summaryRef) {

    /**
     * 校验 readiness evidence 的 safe output 边界。
     */
    public ProviderReadinessEvidenceView {
        severity = Objects.requireNonNull(severity, "severity");
        code = ObservabilityReportSafety.requireSafeText(code, "code");
        summaryRef = ObservabilityReportSafety.requireSafeText(summaryRef, "summaryRef");
    }

    static ProviderReadinessEvidenceView from(final ProviderReadinessFinding finding) {
        final ProviderReadinessFinding checked = Objects.requireNonNull(finding, "finding");
        return new ProviderReadinessEvidenceView(
                checked.severity(), checked.code(), checked.summaryRef());
    }
}

package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.Objects;

/**
 * B4 report 中的 failure classification 摘要。
 *
 * <p>该结构只输出稳定 enum 与 safe summary ref，不输出原始异常、provider payload、raw response 或敏感材料。
 *
 * @param classification failure classification。
 * @param summaryRef     failure summary safe ref。
 */
public record ProviderFailureClassificationSummary(
        ProviderFailureClassification classification, String summaryRef) {

    /**
     * 校验 failure classification summary 的 safe output 边界。
     */
    public ProviderFailureClassificationSummary {
        classification = Objects.requireNonNull(classification, "classification");
        summaryRef = ObservabilityReportSafety.requireSafeText(summaryRef, "summaryRef");
    }

    static ProviderFailureClassificationSummary from(final ProviderFailureClassification classification) {
        final ProviderFailureClassification checked =
                Objects.requireNonNull(classification, "classification");
        return new ProviderFailureClassificationSummary(
                checked, "failure:" + checked.name().toLowerCase().replace('_', '-'));
    }
}

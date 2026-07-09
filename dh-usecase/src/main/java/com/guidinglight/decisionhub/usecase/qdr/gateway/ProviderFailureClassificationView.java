package com.guidinglight.decisionhub.usecase.qdr.gateway;

import com.guidinglight.decisionhub.usecase.qdr.model.QdrPersistenceSafety;

import java.util.Objects;

/**
 * Provider failure classification 的安全只读视图。
 *
 * <p>只暴露稳定 enum 和 safe summary ref，不返回原始异常、provider payload 或敏感材料。
 *
 * @param classification failure classification。
 * @param summaryRef     failure summary safe ref。
 */
public record ProviderFailureClassificationView(
        ProviderFailureClassification classification, String summaryRef) {

    /** 校验 failure classification view 的安全边界。 */
    public ProviderFailureClassificationView {
        classification = Objects.requireNonNull(classification, "classification");
        summaryRef = QdrPersistenceSafety.requireSafeText(summaryRef, "summaryRef");
    }

    static ProviderFailureClassificationView from(
            final ProviderFailureClassification classification, final String summaryRef) {
        return new ProviderFailureClassificationView(classification, summaryRef);
    }
}

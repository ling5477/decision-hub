package com.guidinglight.decisionhub.domain.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.RiskLevel;

import java.util.List;

/**
 * QDR expected decision summary。
 *
 * <p>该合同只描述结构化预期摘要。`LONG_BIAS` / `SHORT_BIAS` 只是方向性偏好，不是 BUY / SELL，
 * 也不得扩展为订单类型。
 */
public record ExpectedDecisionSummary(
        String decisionType,
        String actionLabel,
        String confidenceBand,
        RiskLevel riskLevel,
        List<String> requiredEvidenceRefs,
        List<String> forbiddenActions) {

    /**
     * 规范化 summary 文本和集合，具体安全语义由 usecase 合同服务 fail-closed 校验。
     */
    public ExpectedDecisionSummary {
        decisionType = trimToNull(decisionType);
        actionLabel = trimToNull(actionLabel);
        confidenceBand = trimToNull(confidenceBand);
        requiredEvidenceRefs = copyTexts(requiredEvidenceRefs);
        forbiddenActions = copyTexts(forbiddenActions);
    }

    private static List<String> copyTexts(final List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(ExpectedDecisionSummary::trimToNull)
                .toList();
    }

    private static String trimToNull(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

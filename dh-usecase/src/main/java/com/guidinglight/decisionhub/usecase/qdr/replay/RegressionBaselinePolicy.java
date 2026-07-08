package com.guidinglight.decisionhub.usecase.qdr.replay;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * B3 mock gateway regression baseline policy。
 *
 * <p>policy 固化本轮 comparator 的容忍度、版本 ref 与 fail-closed 选择。它只控制本地
 * deterministic comparison，不授权 provider、HTTP、Agent、LangGraph、NQ 或 LIVE 行为。
 *
 * @param policyVersion                        policy version。
 * @param confidenceTolerance                  confidence drift 容忍度，范围 0..1。
 * @param warnOnConfidenceDriftWithinTolerance 容忍范围内是否记录 WARN finding。
 * @param failOnConfidenceDriftBeyondTolerance 超出容忍度是否 FAIL，否则 WARN。
 * @param failOnRiskLevelIncrease              riskLevel 升高是否 FAIL，否则 WARN。
 * @param failOnProviderSummaryHashMismatch    provider summary hash mismatch 是否 FAIL，否则 WARN。
 * @param skipOnPolicyVersionMismatch          policy version mismatch 是否 SKIPPED。
 * @param modelGatewayVersionRef               expected model gateway version ref。
 * @param promptVersionRef                     expected prompt version ref。
 * @param providerSummaryHash                  expected provider summary hash。
 */
public record RegressionBaselinePolicy(
        String policyVersion,
        BigDecimal confidenceTolerance,
        boolean warnOnConfidenceDriftWithinTolerance,
        boolean failOnConfidenceDriftBeyondTolerance,
        boolean failOnRiskLevelIncrease,
        boolean failOnProviderSummaryHashMismatch,
        boolean skipOnPolicyVersionMismatch,
        String modelGatewayVersionRef,
        String promptVersionRef,
        String providerSummaryHash) {

    /**
     * 校验 policy 字段，使 comparator 在缺失版本或 hash 时 fail-fast。
     */
    public RegressionBaselinePolicy {
        policyVersion = QdrRegressionSafety.requireSafeText(policyVersion, "policyVersion");
        confidenceTolerance = Objects.requireNonNull(confidenceTolerance, "confidenceTolerance");
        if (confidenceTolerance.compareTo(BigDecimal.ZERO) < 0
                || confidenceTolerance.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("confidenceTolerance must be between 0 and 1");
        }
        modelGatewayVersionRef =
                QdrRegressionSafety.requireSafeText(modelGatewayVersionRef, "modelGatewayVersionRef");
        promptVersionRef = QdrRegressionSafety.requireSafeText(promptVersionRef, "promptVersionRef");
        providerSummaryHash =
                QdrRegressionSafety.requireSha256Hex(providerSummaryHash, "providerSummaryHash");
    }

    /**
     * 创建 B3 strict-safe 默认 policy。
     *
     * @param policyVersion          policy version。
     * @param modelGatewayVersionRef expected model gateway version ref。
     * @param promptVersionRef       expected prompt version ref。
     * @param providerSummaryHash    expected provider summary hash。
     * @return strict-safe policy。
     */
    public static RegressionBaselinePolicy strictSafe(
            final String policyVersion,
            final String modelGatewayVersionRef,
            final String promptVersionRef,
            final String providerSummaryHash) {
        return new RegressionBaselinePolicy(
                policyVersion,
                new BigDecimal("0.10"),
                true,
                true,
                true,
                true,
                true,
                modelGatewayVersionRef,
                promptVersionRef,
                providerSummaryHash);
    }
}

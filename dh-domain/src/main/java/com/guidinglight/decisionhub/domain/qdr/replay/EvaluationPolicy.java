package com.guidinglight.decisionhub.domain.qdr.replay;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/**
 * QDR evaluation policy。
 *
 * <p>policy 只允许结构化 summary、risk 和 evidence ref 参与比较；不得把 raw provider response 作为比较输入。
 */
public record EvaluationPolicy(
        String policyVersion,
        BigDecimal confidenceTolerance,
        int riskLevelTolerance,
        RequiredEvidenceMode requiredEvidenceMode,
        boolean allowProviderSummaryOnly) {

    /**
     * 校验 policy 基础字段和容忍度范围。
     */
    public EvaluationPolicy {
        policyVersion = requireText(policyVersion, "policyVersion");
        confidenceTolerance = normalizeTolerance(confidenceTolerance);
        if (riskLevelTolerance < 0) {
            throw new IllegalArgumentException("riskLevelTolerance must not be negative");
        }
        requiredEvidenceMode = Objects.requireNonNull(requiredEvidenceMode, "requiredEvidenceMode");
    }

    /**
     * 该合同永远不允许 raw provider response 参与比较。
     *
     * @return 固定 false。
     */
    public boolean usesRawProviderResponseDependency() {
        return false;
    }

    private static BigDecimal normalizeTolerance(final BigDecimal value) {
        final BigDecimal checked = Objects.requireNonNull(value, "confidenceTolerance");
        if (checked.compareTo(BigDecimal.ZERO) < 0 || checked.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("confidenceTolerance must be between 0 and 1");
        }
        return checked;
    }

    private static String requireText(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return checked;
    }

    /**
     * Required evidence 比对模式。
     */
    public enum RequiredEvidenceMode {
        /**
         * requiredEvidenceRefs 必须完全匹配。
         */
        STRICT,

        /**
         * actual summary 可以包含 expected refs 的安全超集。
         */
        ALLOW_SUPERSET,

        /**
         * 只允许使用脱敏 provider summary ref，不允许 raw provider response。
         */
        PROVIDER_SUMMARY_ONLY;

        /**
         * 从合同字符串解析 evidence mode，并拒绝 raw provider response 依赖。
         *
         * @param value 合同字符串。
         * @return evidence mode。
         */
        public static RequiredEvidenceMode fromContractValue(final String value) {
            final String checked = requireText(value, "requiredEvidenceMode")
                    .toUpperCase(Locale.ROOT);
            if ("RAW_PROVIDER_RESPONSE".equals(checked)) {
                throw new IllegalArgumentException("raw provider response comparison is not allowed");
            }
            return RequiredEvidenceMode.valueOf(checked);
        }
    }
}

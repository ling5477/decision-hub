package com.guidinglight.decisionhub.domain.qdr.replay;

import java.util.List;
import java.util.Objects;

/**
 * QDR replay / evaluation regression verdict。
 *
 * <p>verdict 只表示回放评估结果。`PASS` 不授权交易，`FAIL` 必须携带原因或 finding 以便审计复核。
 */
public record RegressionVerdict(Status status, String failureReason, List<RegressionFinding> findings) {

    /**
     * 校验 verdict 状态、失败原因和 finding 列表。
     */
    public RegressionVerdict {
        status = Objects.requireNonNull(status, "status");
        failureReason = trimToNull(failureReason);
        findings = List.copyOf(Objects.requireNonNullElse(findings, List.of()));
        if (status == Status.FAIL && failureReason == null && findings.isEmpty()) {
            throw new IllegalArgumentException("FAIL verdict requires failureReason or findings");
        }
    }

    /**
     * 生成通过 verdict。
     *
     * @return PASS verdict。
     */
    public static RegressionVerdict pass() {
        return new RegressionVerdict(Status.PASS, null, List.of());
    }

    /**
     * 生成失败 verdict。
     *
     * @param failureReason 固定失败原因。
     * @return FAIL verdict。
     */
    public static RegressionVerdict fail(final String failureReason) {
        return fail(failureReason, List.of());
    }

    /**
     * 生成失败 verdict。
     *
     * @param failureReason 固定失败原因。
     * @param findings      结构化 finding 列表。
     * @return FAIL verdict。
     */
    public static RegressionVerdict fail(
            final String failureReason, final List<RegressionFinding> findings) {
        return new RegressionVerdict(Status.FAIL, failureReason, findings);
    }

    /**
     * 生成警告 verdict。
     *
     * @param reason   警告原因。
     * @param findings 结构化 finding 列表。
     * @return WARN verdict。
     */
    public static RegressionVerdict warn(
            final String reason, final List<RegressionFinding> findings) {
        return new RegressionVerdict(Status.WARN, reason, findings);
    }

    /**
     * 生成跳过 verdict；用于 B1 合同阶段默认不执行真实 replay。
     *
     * @param reason   跳过原因。
     * @param findings 结构化 finding 列表。
     * @return SKIPPED verdict。
     */
    public static RegressionVerdict skipped(
            final String reason, final List<RegressionFinding> findings) {
        return new RegressionVerdict(Status.SKIPPED, reason, findings);
    }

    private static String trimToNull(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * Regression verdict 状态集合。
     */
    public enum Status {
        /**
         * 合同校验或 regression 比对通过。
         */
        PASS,

        /**
         * 合同校验或 regression 比对失败，调用方必须 fail-closed。
         */
        FAIL,

        /**
         * 存在非阻断差异或残余风险。
         */
        WARN,

        /**
         * 本轮未执行真实 replay 或被策略跳过。
         */
        SKIPPED
    }
}

package com.guidinglight.decisionhub.usecase.qdr.evidence;

/**
 * Evidence aggregate 的脱敏 finding。
 *
 * <p>finding 只携带固定 code、类型、safe ref 和简短说明；构造期复用统一安全校验，禁止把原始 prompt、
 * provider response、凭证或交易执行词写入结果。
 *
 * @param code         稳定 finding code。
 * @param severity     finding 严重级别。
 * @param evidenceType 可选关联 evidence 类型。
 * @param safeRef      可选安全引用。
 * @param message      不含原始材料的固定说明。
 */
public record DecisionEvidenceFinding(
        String code,
        Severity severity,
        DecisionEvidencePolicy.EvidenceType evidenceType,
        String safeRef,
        String message) {

    /** 校验 finding 仅保留可安全显示的结构化字段。 */
    public DecisionEvidenceFinding {
        code = DecisionEvidencePolicy.requireSafeText(code, "code");
        severity = DecisionEvidencePolicy.requireSeverity(severity);
        safeRef = DecisionEvidencePolicy.optionalSafeText(safeRef, "safeRef");
        message = DecisionEvidencePolicy.requireSafeText(message, "message");
    }

    /** Evidence aggregate 支持的固定严重级别。 */
    public enum Severity {
        /** 信息性 finding，不影响完整度。 */
        INFO,
        /** 需要关注但不单独使 aggregate 无效。 */
        WARN,
        /** 不满足 contract 的错误。 */
        ERROR,
        /** 必须 fail-closed 的阻断错误。 */
        BLOCKER
    }
}

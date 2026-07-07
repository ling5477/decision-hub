package com.guidinglight.decisionhub.usecase.qdr.model;

import java.util.List;

/**
 * Prompt render 结果。
 *
 * <p>如果包含 renderedText，也只允许作为内存返回值使用；B1 不提供任何持久化 raw prompt 的能力。
 *
 * @param allowed      true 表示 render 安全通过。
 * @param renderedText 内存中的 render 文本；denied 时必须为 null。
 * @param failureCode  denied 时的固定失败码。
 * @param violations   prompt injection violations。
 */
public record PromptRenderResult(
        boolean allowed,
        String renderedText,
        String failureCode,
        List<PromptInjectionViolation> violations) {

    /**
     * 校验 result 形态，避免 denied 结果携带 renderedText。
     */
    public PromptRenderResult {
        violations = List.copyOf(violations == null ? List.of() : violations);
        if (!allowed && renderedText != null) {
            throw new IllegalArgumentException("denied prompt render must not contain rendered text");
        }
    }

    /**
     * 创建 allowed render result。
     *
     * @param renderedText 内存 render 文本。
     * @return allowed result。
     */
    public static PromptRenderResult allowed(final String renderedText) {
        return new PromptRenderResult(true, renderedText, null, List.of());
    }

    /**
     * 创建 denied render result。
     *
     * @param failureCode 固定失败码。
     * @param violations  拒绝原因。
     * @return denied result。
     */
    public static PromptRenderResult denied(
            final String failureCode, final List<PromptInjectionViolation> violations) {
        return new PromptRenderResult(false, null, failureCode, violations);
    }
}

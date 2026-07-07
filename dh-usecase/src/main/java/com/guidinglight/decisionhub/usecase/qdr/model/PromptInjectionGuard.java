package com.guidinglight.decisionhub.usecase.qdr.model;

/**
 * Deterministic prompt injection guard contract。
 *
 * <p>B1 guard 只允许本地规则判断，不调用 LLM、不访问 HTTP、不调用 provider。任何拒绝或异常都必须
 * fail-closed。
 */
public interface PromptInjectionGuard {

    /**
     * 校验 prompt 或 render input。
     *
     * @param tenantId tenant 边界。
     * @param input    待检查文本。
     * @return guard decision。
     */
    PromptInjectionDecision evaluate(String tenantId, String input);
}

package com.guidinglight.decisionhub.usecase.qdr.model;

import java.util.Objects;

/**
 * Prompt injection guard violation。
 *
 * @param code    拒绝类型。
 * @param message 固定安全消息，不回显危险输入。
 */
public record PromptInjectionViolation(PromptInjectionViolationCode code, String message) {

    /**
     * 校验 violation 字段。
     */
    public PromptInjectionViolation {
        code = Objects.requireNonNull(code, "code");
        message = Objects.requireNonNull(message, "message");
    }

    /**
     * 创建固定安全消息 violation。
     *
     * @param code 拒绝类型。
     * @return violation。
     */
    public static PromptInjectionViolation of(final PromptInjectionViolationCode code) {
        return new PromptInjectionViolation(code, "prompt input rejected");
    }
}

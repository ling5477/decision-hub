package com.guidinglight.decisionhub.domain.qdr.model;

/**
 * Prompt domain 校验异常。
 *
 * <p>异常消息只返回固定安全原因，不回显 raw prompt、危险输入或疑似密钥材料。
 */
public final class PromptValidationException extends IllegalArgumentException {

    private PromptValidationException(final String message) {
        super(message);
    }

    /**
     * 创建疑似密钥材料异常。
     *
     * @param field 字段名。
     * @return 安全异常。
     */
    public static PromptValidationException secretLikeMaterial(final String field) {
        return new PromptValidationException(field + " rejected: secret-like material");
    }

    /**
     * 创建可执行交易指令异常。
     *
     * @param field 字段名。
     * @return 安全异常。
     */
    public static PromptValidationException executableTradingInstruction(final String field) {
        return new PromptValidationException(field + " rejected: executable trading instruction");
    }

    /**
     * 创建 checksum mismatch 异常。
     *
     * @return 安全异常。
     */
    public static PromptValidationException checksumMismatch() {
        return new PromptValidationException("prompt version checksum mismatch");
    }
}

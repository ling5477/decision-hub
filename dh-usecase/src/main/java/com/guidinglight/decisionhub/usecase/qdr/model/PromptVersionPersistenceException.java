package com.guidinglight.decisionhub.usecase.qdr.model;

/**
 * PromptVersion persistence fail-closed exception。
 *
 * <p>异常消息必须保持固定安全文案，不回显 prompt 正文、render input、provider response 或凭证材料。
 */
public class PromptVersionPersistenceException extends RuntimeException {

    /**
     * 创建持久化异常。
     *
     * @param message 安全文案。
     */
    public PromptVersionPersistenceException(final String message) {
        super(message);
    }

    /**
     * 创建持久化异常。
     *
     * @param message 安全文案。
     * @param cause   原始异常。
     */
    public PromptVersionPersistenceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

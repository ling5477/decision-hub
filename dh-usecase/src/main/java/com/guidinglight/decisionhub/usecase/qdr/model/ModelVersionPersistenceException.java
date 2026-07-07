package com.guidinglight.decisionhub.usecase.qdr.model;

/**
 * ModelVersion persistence fail-closed exception。
 *
 * <p>异常消息必须保持固定安全文案，不回显 provider 原始响应、endpoint、credential 或调用上下文。
 */
public class ModelVersionPersistenceException extends RuntimeException {

    /**
     * 创建持久化异常。
     *
     * @param message 安全文案。
     */
    public ModelVersionPersistenceException(final String message) {
        super(message);
    }

    /**
     * 创建持久化异常。
     *
     * @param message 安全文案。
     * @param cause   原始异常。
     */
    public ModelVersionPersistenceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

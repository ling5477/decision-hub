package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * ModelGatewayCall persistence fail-closed exception。
 *
 * <p>异常消息必须保持固定安全文案，不回显 prompt、provider response、render input 或凭证材料。
 */
public class ModelGatewayCallPersistenceException extends RuntimeException {

    /**
     * 创建持久化异常。
     *
     * @param message 安全文案。
     */
    public ModelGatewayCallPersistenceException(final String message) {
        super(message);
    }

    /**
     * 创建持久化异常。
     *
     * @param message 安全文案。
     * @param cause   原始异常。
     */
    public ModelGatewayCallPersistenceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

package com.guidinglight.decisionhub.usecase.qdr.model;

/**
 * ModelVersion registry fail-closed exception。
 *
 * <p>异常消息只包含固定原因，不输出 model metadata、prompt、provider response 或敏感材料。
 */
public final class ModelVersionRegistryException extends IllegalArgumentException {

    /**
     * 创建 registry 异常。
     *
     * @param message 固定安全错误消息。
     */
    public ModelVersionRegistryException(final String message) {
        super(message);
    }
}

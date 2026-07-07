package com.guidinglight.decisionhub.usecase.qdr.model;

/**
 * PromptVersion registry fail-closed 异常。
 *
 * <p>异常消息不回显 raw prompt 或危险输入，只暴露固定错误原因。
 */
public final class PromptVersionRegistryException extends IllegalArgumentException {

    /**
     * 创建 registry 异常。
     *
     * @param message 固定安全消息。
     */
    public PromptVersionRegistryException(final String message) {
        super(message);
    }
}

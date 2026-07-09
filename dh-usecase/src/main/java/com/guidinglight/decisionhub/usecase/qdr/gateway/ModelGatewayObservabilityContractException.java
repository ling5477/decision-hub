package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Model gateway observability contract fail-closed exception。
 *
 * <p>message 只包含固定 code，不拼接外部输入，避免 raw material 或 credential-like material 泄漏到日志。
 */
public final class ModelGatewayObservabilityContractException extends RuntimeException {

    private final String code;

    /**
     * 创建 fail-closed contract exception。
     *
     * @param code 固定错误码。
     */
    public ModelGatewayObservabilityContractException(final String code) {
        super("model gateway observability contract rejected: " + code);
        this.code = code;
    }

    /**
     * @return 固定错误码。
     */
    public String code() {
        return code;
    }

    /**
     * @return true 表示该异常总是 fail-closed。
     */
    public boolean failClosed() {
        return true;
    }
}

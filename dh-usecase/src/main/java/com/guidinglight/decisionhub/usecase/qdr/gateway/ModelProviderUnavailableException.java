package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Mock provider unavailable exception。
 */
public final class ModelProviderUnavailableException extends ModelGatewayException {

    /**
     * 创建 provider unavailable 异常。
     */
    public ModelProviderUnavailableException() {
        super(ModelGatewayFailureCode.PROVIDER_UNAVAILABLE);
    }
}

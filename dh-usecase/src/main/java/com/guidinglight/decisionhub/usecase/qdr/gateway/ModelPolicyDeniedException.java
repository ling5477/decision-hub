package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Provider trust policy denied exception。
 */
public final class ModelPolicyDeniedException extends ModelGatewayException {

    /**
     * 创建 policy denied 异常。
     */
    public ModelPolicyDeniedException() {
        super(ModelGatewayFailureCode.POLICY_DENIED);
    }
}

package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Model redaction failure exception。
 */
public final class ModelRedactionFailureException extends ModelGatewayException {

    /**
     * 创建 redaction failure 异常。
     */
    public ModelRedactionFailureException() {
        super(ModelGatewayFailureCode.REDACTION_FAILED);
    }
}

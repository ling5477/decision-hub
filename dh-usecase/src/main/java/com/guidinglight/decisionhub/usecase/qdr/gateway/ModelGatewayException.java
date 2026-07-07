package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Model gateway fail-closed exception base。
 *
 * <p>异常只携带固定 failure code，不回显 raw prompt、raw response 或敏感材料。Service 会将该异常转换
 * 为 {@link ModelGatewayResult}。
 */
public class ModelGatewayException extends RuntimeException {

    private final ModelGatewayFailureCode failureCode;

    /**
     * 创建 gateway 异常。
     *
     * @param failureCode failure code。
     */
    public ModelGatewayException(final ModelGatewayFailureCode failureCode) {
        super(ModelGatewayFailure.of(failureCode).message());
        this.failureCode = failureCode;
    }

    /**
     * 返回结构化 failure code。
     *
     * @return failure code。
     */
    public ModelGatewayFailureCode failureCode() {
        return failureCode;
    }
}

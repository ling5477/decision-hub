package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.Objects;

/**
 * QDR mock gateway integration fail-closed exception。
 *
 * <p>异常只暴露固定 failure code，不携带 raw prompt、raw provider response、credential 或用户输入原文。
 */
public final class QdrModelGatewayIntegrationException extends RuntimeException {

    private final ModelGatewayFailureCode failureCode;

    /**
     * 创建 fail-closed exception。
     *
     * @param failureCode gateway failure code。
     * @param message     固定脱敏 message。
     */
    public QdrModelGatewayIntegrationException(
            final ModelGatewayFailureCode failureCode, final String message) {
        super(message);
        this.failureCode = Objects.requireNonNull(failureCode, "failureCode");
    }

    /**
     * @return gateway failure code。
     */
    public ModelGatewayFailureCode failureCode() {
        return failureCode;
    }
}

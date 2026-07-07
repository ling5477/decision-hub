package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Model gateway call 持久化状态。
 */
public enum ModelGatewayCallStatus {
    /**
     * mock gateway call 成功并写入脱敏 metadata。
     */
    SUCCEEDED,
    /**
     * gateway call fail-closed，并写入结构化 failure code。
     */
    FAILED
}

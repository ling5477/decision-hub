package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Model gateway call 的 ProviderTrustPolicy 持久化摘要。
 */
public enum ModelGatewayCallTrustDecision {
    /**
     * ProviderTrustPolicy 允许本次 mock call 继续。
     */
    ALLOWED,
    /**
     * ProviderTrustPolicy 拒绝本次 call，调用必须 fail-closed。
     */
    DENIED
}

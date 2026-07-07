package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Model gateway 统一入口。
 *
 * <p>业务代码必须通过该 port 调用 model provider；不得绕过 ProviderTrustPolicy 直接调用 provider、
 * HTTP client、provider SDK 或 Agent runtime。
 */
public interface ModelGatewayPort {

    /**
     * 执行一次 fail-closed model gateway call。
     *
     * @param request gateway request。
     * @return structured success/failure result。
     */
    ModelGatewayResult call(ModelGatewayRequest request);
}

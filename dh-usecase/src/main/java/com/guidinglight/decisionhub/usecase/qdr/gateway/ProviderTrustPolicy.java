package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Provider trust policy contract。
 *
 * <p>ModelGatewayService 必须在调用 ModelProviderPort 之前执行该 policy。policy 拒绝、未知 provider、
 * disabled provider 或 policy 异常都必须 fail-closed，不存在 fallback allow。
 */
public interface ProviderTrustPolicy {

    /**
     * 校验 provider 是否允许被当前 gateway request 使用。
     *
     * @param request gateway request。
     * @return trust decision。
     */
    ModelProviderTrustDecision evaluate(ModelGatewayRequest request);
}

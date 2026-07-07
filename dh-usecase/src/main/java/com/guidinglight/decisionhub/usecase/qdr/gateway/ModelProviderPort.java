package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Model provider adapter port。
 *
 * <p>B2 只能由 deterministic mock provider 实现，不允许真实 HTTP、provider SDK、credential 或
 * NQ runtime access。
 */
public interface ModelProviderPort {

    /**
     * 调用 provider。
     *
     * <p>调用方必须先完成 registry、budget、redaction、prompt injection 和 ProviderTrustPolicy。
     *
     * @param request        gateway request。
     * @param renderedPrompt 内存中的 rendered prompt，不得持久化。
     * @return structured mock provider result。
     */
    MockModelProviderResult invoke(ModelGatewayRequest request, String renderedPrompt);
}

package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * QDR decision pipeline 的 mock ModelGateway integration port。
 *
 * <p>实现必须调用 {@link ModelGatewayPort}，不得直连 provider。失败必须抛出
 * {@link QdrModelGatewayIntegrationException}，调用方不得 fallback success。
 */
public interface QdrModelGatewayIntegrationPort {

    /**
     * 调用 mock gateway 并写入 call metadata / trace / audit refs。
     *
     * @param command QDR gateway integration command。
     * @return safe refs。
     */
    QdrModelGatewayIntegrationResult invoke(QdrModelGatewayIntegrationCommand command);
}

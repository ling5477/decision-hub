package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * QDR mock gateway baseline bootstrap port。
 *
 * <p>实现必须只创建 deterministic mock prompt/model/provider refs，并强制写入 B1/B2 registry 与 B3
 * persistence baseline。任何 persistence 或 registry mismatch 都必须向上抛出，使 pipeline fail-closed。
 */
public interface QdrModelGatewayBaselinePort {

    /**
     * 准备当前 tenant 的 mock prompt/model/provider baseline。
     *
     * @param command bootstrap command。
     * @return 完整 baseline refs。
     */
    QdrModelGatewayBaseline prepare(QdrModelGatewayBaselineCommand command);
}

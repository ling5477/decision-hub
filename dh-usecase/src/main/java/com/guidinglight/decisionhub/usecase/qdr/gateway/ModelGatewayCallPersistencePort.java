package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.Optional;

/**
 * ModelGatewayCall persistence port。
 *
 * <p>所有方法都必须 tenant-bound；不得提供 UUID-only 查询或更新。写入失败必须 fail-closed，不能静默丢失
 * gateway call 审计 metadata。
 */
public interface ModelGatewayCallPersistencePort {

    /**
     * 保存 model gateway call 脱敏 metadata。
     *
     * @param command 保存命令。
     * @return 持久化后的脱敏 record。
     */
    ModelGatewayCallRecord save(SaveModelGatewayCallCommand command);

    /**
     * 按 tenant + modelCallRef 查询 gateway call。
     *
     * @param tenantId     tenant 边界。
     * @param modelCallRef model call ref。
     * @return 命中 record；跨 tenant 必须 empty 或 fail-closed。
     */
    Optional<ModelGatewayCallRecord> findByTenantAndModelCallRef(String tenantId, String modelCallRef);
}

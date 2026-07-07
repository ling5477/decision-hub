package com.guidinglight.decisionhub.usecase.qdr.model;

import java.util.Optional;
import java.util.UUID;

/**
 * ModelVersion persistence port。
 *
 * <p>所有方法都必须 tenant-bound；不得提供 UUID-only 查询或更新。重复保存相同 checksum 可幂等返回，
 * 重复保存不同 checksum 必须 fail-closed。
 */
public interface ModelVersionPersistencePort {

    /**
     * 保存 model profile metadata 与 immutable model version。
     *
     * @param command 保存命令。
     * @return 持久化后的脱敏 record。
     */
    ModelVersionRecord save(SaveModelVersionCommand command);

    /**
     * 按 tenant + modelVersionId 查询 model version。
     *
     * @param tenantId       tenant 边界。
     * @param modelVersionId model version id。
     * @return 命中 record；跨 tenant 必须 empty 或 fail-closed。
     */
    Optional<ModelVersionRecord> findByTenantAndModelVersionId(String tenantId, UUID modelVersionId);
}

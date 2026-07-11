package com.guidinglight.decisionhub.usecase.qdr.model;

import java.util.Optional;
import java.util.UUID;

/**
 * PromptVersion persistence port。
 *
 * <p>所有方法都必须 tenant-bound；不得提供 UUID-only 查询或更新。重复保存相同 checksum 可幂等返回， 重复保存不同 checksum 必须
 * fail-closed。
 */
public interface PromptVersionPersistencePort {

    /**
     * 保存 prompt template metadata 与 immutable prompt version。
     *
     * @param command 保存命令。
     * @return 持久化后的脱敏 record。
     */
    PromptVersionRecord save(SavePromptVersionCommand command);

    /**
     * 按 tenant + template + version 查询 prompt version。
     *
     * @param tenantId         tenant 边界。
     * @param promptTemplateId prompt template id。
     * @param version          version 字符串。
     * @return 命中 record；跨 tenant 必须 empty 或 fail-closed。
     */
    Optional<PromptVersionRecord> findByTenantAndTemplateVersion(
            String tenantId, UUID promptTemplateId, String version);

  /**
   * 按 tenant + promptVersionId 精确查询 immutable prompt version。
   *
   * <p>该查询不得 fallback 到 active/latest version；属于其他 tenant 的 UUID 必须不可见。
   *
   * @param tenantId tenant 第一安全边界。
   * @param promptVersionId prompt version 物理 UUID。
   * @return 精确命中 record；跨 tenant 返回 empty。
   */
  Optional<PromptVersionRecord> findByTenantAndPromptVersionId(
      String tenantId, UUID promptVersionId);
}

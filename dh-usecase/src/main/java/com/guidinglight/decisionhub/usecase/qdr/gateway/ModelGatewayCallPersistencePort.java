package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.Optional;
import java.util.UUID;

/**
 * ModelGatewayCall persistence port。
 *
 * <p>所有方法都必须 tenant-bound；不得提供 UUID-only 查询或更新。写入失败必须 fail-closed，不能静默丢失 gateway call 审计 metadata。
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
  Optional<ModelGatewayCallRecord> findByTenantAndModelCallRef(
      String tenantId, String modelCallRef);

  /**
   * 按 tenant + decisionRunId + safe modelCallRef 精确查询 gateway call。
   *
   * <p>三项都必须参与 SQL 与结果校验；不得按 trace、时间、provider 或相似字符串 fallback。
   *
   * @param tenantId tenant 第一安全边界。
   * @param decisionRunId V6 decision run 物理 UUID。
   * @param modelCallRef V8 gateway call 安全业务引用。
   * @return 精确命中的脱敏 record；任一 identity 不匹配返回 empty。
   */
  Optional<ModelGatewayCallRecord> findByTenantAndDecisionRunAndModelCallRef(
      String tenantId, UUID decisionRunId, String modelCallRef);
}

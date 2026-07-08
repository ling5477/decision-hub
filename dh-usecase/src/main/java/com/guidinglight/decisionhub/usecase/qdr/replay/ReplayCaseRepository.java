package com.guidinglight.decisionhub.usecase.qdr.replay;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * QDR replay case tenant-bound repository port。
 *
 * <p>所有读取方法必须显式接收 tenantId，不提供 UUID-only 或 caseId-only 查询。实现只能持久化结构化
 * replay/evaluation baseline，不执行 replay、不调用 provider/HTTP/NQ、不产生交易信号。
 */
public interface ReplayCaseRepository {

    /**
     * 保存 replay case baseline。
     *
     * @param command 保存命令。
     * @return 已保存或同 checksum 幂等命中的 replay case。
     */
    ReplayCaseRecord save(SaveReplayCaseCommand command);

    /**
     * 按 tenant 和主键查询。
     *
     * @param tenantId tenant ID。
     * @param id       replay case 主键。
     * @return 同租户 replay case。
     */
    Optional<ReplayCaseRecord> findById(String tenantId, UUID id);

    /**
     * 按 tenant 和 caseId 查询。
     *
     * @param tenantId tenant ID。
     * @param caseId   租户内 case ID。
     * @return 同租户 replay case。
     */
    Optional<ReplayCaseRecord> findByCaseId(String tenantId, String caseId);

    /**
     * 按 tenant 和 traceId 查询。
     *
     * @param tenantId tenant ID。
     * @param traceId  traceId。
     * @param limit    page size，最大 100。
     * @param offset   offset。
     * @return 同租户 replay case 列表。
     */
    List<ReplayCaseRecord> listByTraceId(String tenantId, String traceId, int limit, int offset);

    /**
     * 按 tenant 和 source request 查询。
     *
     * @param tenantId        tenant ID。
     * @param sourceRequestId source request ID。
     * @param limit           page size，最大 100。
     * @param offset          offset。
     * @return 同租户 replay case 列表。
     */
    List<ReplayCaseRecord> listBySourceRequestId(
            String tenantId, String sourceRequestId, int limit, int offset);

    /**
     * 按 tenant 分页查询。
     *
     * @param tenantId tenant ID。
     * @param limit    page size，最大 100。
     * @param offset   offset。
     * @return 同租户 replay case 列表。
     */
    List<ReplayCaseRecord> listByTenant(String tenantId, int limit, int offset);
}

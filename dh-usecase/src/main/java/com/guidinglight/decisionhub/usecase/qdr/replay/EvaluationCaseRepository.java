package com.guidinglight.decisionhub.usecase.qdr.replay;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * QDR evaluation case tenant-bound repository port。
 *
 * <p>所有查询都以 tenantId 为第一边界，保存时必须验证 replay case 引用在同 tenant 内存在或 fail-closed。
 * 本端口不授权 API、provider、HTTP、Agent、LangGraph、NQ 或 LIVE 连接。
 */
public interface EvaluationCaseRepository {

    /**
     * 保存 evaluation case baseline。
     *
     * @param command 保存命令。
     * @return 已保存或同 checksum 幂等命中的 evaluation case。
     */
    EvaluationCaseRecord save(SaveEvaluationCaseCommand command);

    /**
     * 按 tenant 和主键查询。
     *
     * @param tenantId tenant ID。
     * @param id       evaluation case 主键。
     * @return 同租户 evaluation case。
     */
    Optional<EvaluationCaseRecord> findById(String tenantId, UUID id);

    /**
     * 按 tenant 和 evaluationId 查询。
     *
     * @param tenantId     tenant ID。
     * @param evaluationId 租户内 evaluation ID。
     * @return 同租户 evaluation case。
     */
    Optional<EvaluationCaseRecord> findByEvaluationId(String tenantId, String evaluationId);

    /**
     * 按 tenant 和 caseId 分页查询。
     *
     * @param tenantId tenant ID。
     * @param caseId   租户内 case ID。
     * @param limit    page size，最大 100。
     * @param offset   offset。
     * @return 同租户 evaluation case 列表。
     */
    List<EvaluationCaseRecord> listByCaseId(String tenantId, String caseId, int limit, int offset);

    /**
     * 按 tenant 分页查询。
     *
     * @param tenantId tenant ID。
     * @param limit    page size，最大 100。
     * @param offset   offset。
     * @return 同租户 evaluation case 列表。
     */
    List<EvaluationCaseRecord> listByTenant(String tenantId, int limit, int offset);
}

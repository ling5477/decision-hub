package com.guidinglight.decisionhub.usecase.qdr.replay;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * QDR regression verdict tenant-bound repository port。
 *
 * <p>verdict 只表示 replay/evaluation regression 结果，不是交易建议。所有查询必须带 tenantId，
 * finding 保存也必须绑定同租户 verdict。
 */
public interface RegressionVerdictRepository {

    /**
     * 保存 regression verdict baseline。
     *
     * @param command 保存命令。
     * @return 已保存 regression verdict。
     */
    RegressionVerdictRecord save(SaveRegressionVerdictCommand command);

    /**
     * 保存 verdict findings。
     *
     * @param tenantId  tenant ID。
     * @param verdictId 租户内 verdict ID。
     * @param commands  finding 保存命令。
     * @return 已保存 finding 列表。
     */
    List<RegressionFindingRecord> saveFindings(
            String tenantId, String verdictId, List<SaveRegressionFindingCommand> commands);

    /**
     * 按 tenant 和主键查询。
     *
     * @param tenantId tenant ID。
     * @param id       verdict 主键。
     * @return 同租户 verdict。
     */
    Optional<RegressionVerdictRecord> findById(String tenantId, UUID id);

    /**
     * 按 tenant 和 verdictId 查询。
     *
     * @param tenantId  tenant ID。
     * @param verdictId 租户内 verdict ID。
     * @return 同租户 verdict。
     */
    Optional<RegressionVerdictRecord> findByVerdictId(String tenantId, String verdictId);

    /**
     * 按 tenant 和 evaluationId 查询。
     *
     * @param tenantId     tenant ID。
     * @param evaluationId 租户内 evaluation ID。
     * @return 同租户 verdict。
     */
    Optional<RegressionVerdictRecord> findByEvaluationId(String tenantId, String evaluationId);

    /**
     * 按 tenant 和 verdictId 查询 finding。
     *
     * @param tenantId  tenant ID。
     * @param verdictId 租户内 verdict ID。
     * @param limit     page size，最大 100。
     * @param offset    offset。
     * @return 同租户 finding 列表。
     */
    List<RegressionFindingRecord> listFindingsByVerdictId(
            String tenantId, String verdictId, int limit, int offset);

    /**
     * 按 tenant 分页查询 verdict。
     *
     * @param tenantId tenant ID。
     * @param limit    page size，最大 100。
     * @param offset   offset。
     * @return 同租户 verdict 列表。
     */
    List<RegressionVerdictRecord> listByTenant(String tenantId, int limit, int offset);
}

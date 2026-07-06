package com.guidinglight.decisionhub.usecase.qdr;

import com.guidinglight.decisionhub.domain.qdr.QuantDecision;
import java.util.List;
import java.util.UUID;

/**
 * `quant_decision` 主线 repository port。
 *
 * <p>实现必须按 `decision_run_id` 查询 quant decision；写失败必须 fail-closed。
 */
public interface QuantDecisionRepository {

  /**
   * 保存 quant decision。
   *
   * @param decision 只读 quant decision。
   */
  void save(QuantDecision decision);

  /**
   * 按 run ID 查询 quant decision。
   *
   * @param decisionRunId run ID。
   * @return decision 列表。
   */
  List<QuantDecision> findByDecisionRunId(UUID decisionRunId);
}

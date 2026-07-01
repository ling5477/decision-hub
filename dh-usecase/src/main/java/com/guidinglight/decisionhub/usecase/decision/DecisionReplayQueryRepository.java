package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;

/**
 * K4 replay read model repository port。
 *
 * <p>实现只能读取 DH-owned K3 audit / snapshot / trace / provider / output / audit 表；不得写库、不得查询
 * NQ DB、不得调用 provider、不得调用 orchestrator，也不得暴露 raw secret / credential。
 */
public interface DecisionReplayQueryRepository {

  /**
   * 按 tenantId + decisionId 查询 replay view。
   *
   * @param query 已校验的只读查询条件。
   * @return 结构化 replay view；not found / incomplete / corrupted / blocked 均必须用状态表达。
   */
  DecisionReplayView findReplay(DecisionReplayQuery query);
}

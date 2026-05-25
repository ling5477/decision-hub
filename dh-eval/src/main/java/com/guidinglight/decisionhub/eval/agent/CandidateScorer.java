package com.guidinglight.decisionhub.eval.agent;

import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import java.util.Map;

/**
 * Stage1：策略候选评分器接口。
 *
 * <p>对应工单 4.4：CandidateScorer。第一阶段采用规则评分，不做自动实盘决策。
 */
public interface CandidateScorer {

  /**
   * 对单个候选评分。
   *
   * @param candidate 候选。
   * @return 包含至少 {"score": double} 的快照，可附加 reasons/weights 等结构化信息。
   */
  Map<String, Object> score(StrategyCandidate candidate);
}

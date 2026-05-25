package com.guidinglight.decisionhub.eval.agent;

import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import java.util.Map;

/**
 * Stage1：基于风险启发式的候选评分器接口。
 *
 * <p>对应工单 4.4：RiskHeuristicScorer。
 */
public interface RiskHeuristicScorer {

  /**
   * 对候选给出风险维度评分（越小越好）。
   *
   * @return 包含 {"riskScore": double, "rejected": boolean} 的结构化快照。
   */
  Map<String, Object> score(StrategyCandidate candidate);
}

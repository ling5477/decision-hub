package com.guidinglight.decisionhub.eval.agent.rule;

import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import com.guidinglight.decisionhub.eval.agent.RiskHeuristicScorer;
import java.util.HashMap;
import java.util.Map;

/** Stage1：默认风险启发式评分器。规则：payload.maxDrawdown >= 0.3 视为 rejected。 */
public final class DefaultRiskHeuristicScorer implements RiskHeuristicScorer {

  private static final double DEFAULT_REJECT_DRAWDOWN = 0.3;

  @Override
  public Map<String, Object> score(final StrategyCandidate candidate) {
    double riskScore = 0.5;
    boolean rejected = false;

    final Object dd = candidate.getPayloadJson().get("maxDrawdown");
    if (dd instanceof Number n) {
      riskScore = n.doubleValue();
      if (riskScore >= DEFAULT_REJECT_DRAWDOWN) {
        rejected = true;
      }
    }

    final Map<String, Object> result = new HashMap<>();
    result.put("riskScore", riskScore);
    result.put("rejected", rejected);
    result.put("source", "DefaultRiskHeuristicScorer");
    return Map.copyOf(result);
  }
}

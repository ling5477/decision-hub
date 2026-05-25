package com.guidinglight.decisionhub.eval.agent.rule;

import com.guidinglight.decisionhub.eval.agent.JudgeAggregator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage1：默认 Judge 聚合器。
 *
 * <p>规则：加权平均 score / riskScore（取反） / evidenceScore；任一 scorer 标记 rejected=true 则 selected=false。
 */
public final class DefaultJudgeAggregator implements JudgeAggregator {

  private static final double SELECT_THRESHOLD = 0.55;

  @Override
  public Map<String, Object> aggregate(final List<Map<String, Object>> scoreSnapshots) {
    double sumScore = 0.0;
    int countScore = 0;
    double sumRisk = 0.0;
    int countRisk = 0;
    double sumEvidence = 0.0;
    int countEvidence = 0;
    boolean rejectedByAny = false;

    for (Map<String, Object> snap : scoreSnapshots) {
      final Object score = snap.get("score");
      if (score instanceof Number n) {
        sumScore += n.doubleValue();
        countScore++;
      }
      final Object risk = snap.get("riskScore");
      if (risk instanceof Number rn) {
        sumRisk += rn.doubleValue();
        countRisk++;
      }
      final Object rejected = snap.get("rejected");
      if (rejected instanceof Boolean b && b) {
        rejectedByAny = true;
      }
      final Object ev = snap.get("evidenceScore");
      if (ev instanceof Number en) {
        sumEvidence += en.doubleValue();
        countEvidence++;
      }
    }

    final double avgScore = countScore == 0 ? 0.0 : sumScore / countScore;
    final double avgRisk = countRisk == 0 ? 0.5 : sumRisk / countRisk;
    final double avgEvidence = countEvidence == 0 ? 0.0 : sumEvidence / countEvidence;
    final double finalScore = (avgScore * 0.5) + ((1.0 - avgRisk) * 0.3) + (avgEvidence * 0.2);
    final boolean selected = !rejectedByAny && finalScore >= SELECT_THRESHOLD;

    final List<String> reasons = new ArrayList<>();
    reasons.add("avgScore=" + avgScore);
    reasons.add("avgRisk=" + avgRisk);
    reasons.add("avgEvidence=" + avgEvidence);
    if (rejectedByAny) {
      reasons.add("rejectedByRiskHeuristic");
    }

    final Map<String, Object> result = new HashMap<>();
    result.put("finalScore", finalScore);
    result.put("selected", selected);
    result.put("reasons", List.copyOf(reasons));
    result.put("source", "DefaultJudgeAggregator");
    return Map.copyOf(result);
  }
}

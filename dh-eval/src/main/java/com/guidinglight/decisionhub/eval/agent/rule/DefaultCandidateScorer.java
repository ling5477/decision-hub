package com.guidinglight.decisionhub.eval.agent.rule;

import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import com.guidinglight.decisionhub.eval.agent.CandidateScorer;
import java.util.HashMap;
import java.util.Map;

/**
 * Stage1：默认规则候选评分器。
 *
 * <p>规则：基础分 0.5，证据数量每个 +0.05，payload 中 expectedSharpe 字段（若存在）乘 0.1 累加。最高封顶 1.0。
 */
public final class DefaultCandidateScorer implements CandidateScorer {

  @Override
  public Map<String, Object> score(final StrategyCandidate candidate) {
    double score = 0.5;
    final int evidenceCount = candidate.getEvidenceRefs().size();
    score += evidenceCount * 0.05;

    final Object sharpe = candidate.getPayloadJson().get("expectedSharpe");
    if (sharpe instanceof Number n) {
      score += n.doubleValue() * 0.1;
    }
    if (score > 1.0) {
      score = 1.0;
    }
    if (score < 0.0) {
      score = 0.0;
    }

    final Map<String, Object> result = new HashMap<>();
    result.put("score", score);
    result.put("evidenceCount", evidenceCount);
    result.put("source", "DefaultCandidateScorer");
    return Map.copyOf(result);
  }
}

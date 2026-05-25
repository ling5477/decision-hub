package com.guidinglight.decisionhub.eval.agent.rule;

import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import com.guidinglight.decisionhub.eval.agent.EvidenceQualityScorer;
import java.util.HashMap;
import java.util.Map;

/** Stage1：默认证据质量评分器。规则：evidenceCount/5 截断为 [0,1]。 */
public final class DefaultEvidenceQualityScorer implements EvidenceQualityScorer {

  @Override
  public Map<String, Object> score(final StrategyCandidate candidate) {
    final int count = candidate.getEvidenceRefs().size();
    double evidenceScore = count / 5.0;
    if (evidenceScore > 1.0) {
      evidenceScore = 1.0;
    }

    final Map<String, Object> result = new HashMap<>();
    result.put("evidenceScore", evidenceScore);
    result.put("evidenceCount", count);
    result.put("source", "DefaultEvidenceQualityScorer");
    return Map.copyOf(result);
  }
}

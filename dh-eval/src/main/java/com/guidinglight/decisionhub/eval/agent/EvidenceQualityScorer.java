package com.guidinglight.decisionhub.eval.agent;

import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import java.util.Map;

/**
 * Stage1：证据质量评分器接口。
 *
 * <p>对应工单 4.4：EvidenceQualityScorer。
 */
public interface EvidenceQualityScorer {

  /**
   * 评分候选所依赖的证据集合。
   *
   * @return 包含 {"evidenceScore": double, "evidenceCount": int} 的结构化快照。
   */
  Map<String, Object> score(StrategyCandidate candidate);
}

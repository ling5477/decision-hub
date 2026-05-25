package com.guidinglight.decisionhub.usecase.agent.impl;

import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.candidate.CandidateStatus;
import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import com.guidinglight.decisionhub.usecase.agent.CandidateReviewService;
import com.guidinglight.decisionhub.usecase.agent.StrategyCandidateRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage1：默认候选评审服务。
 *
 * <p>规则评分 + 直接落到 candidate.scoreSnapshot。 Stage1 不调真实 LLM 评审，但所有评审输出必须结构化。
 */
public final class DefaultCandidateReviewService implements CandidateReviewService {

  private static final double REJECT_RISK_THRESHOLD = 0.3;

  private final StrategyCandidateRepository candidateRepository;

  /** 构造。 */
  public DefaultCandidateReviewService(final StrategyCandidateRepository candidateRepository) {
    this.candidateRepository = candidateRepository;
  }

  @Override
  public List<Map<String, Object>> review(final List<StrategyCandidate> candidates) {
    final List<Map<String, Object>> out = new ArrayList<>();
    for (StrategyCandidate candidate : candidates) {
      final Map<String, Object> review = new HashMap<>();
      final double baseScore = 0.5 + (candidate.getEvidenceRefs().size() * 0.05);
      double riskScore = 0.5;
      final Object dd = candidate.getPayloadJson().get("maxDrawdown");
      if (dd instanceof Number n) {
        riskScore = n.doubleValue();
      }
      final boolean rejected = riskScore >= REJECT_RISK_THRESHOLD;
      review.put("score", Math.min(baseScore, 1.0));
      review.put("riskScore", riskScore);
      review.put("evidenceScore", Math.min(candidate.getEvidenceRefs().size() / 5.0, 1.0));
      review.put("rejected", rejected);
      review.put("reviewerStrategy", "DefaultCandidateReviewService");
      final Map<String, Object> frozen = Map.copyOf(review);
      candidate.applyScoreSnapshot(frozen, TimeProvider.now());
      if (rejected) {
        candidate.transitionTo(CandidateStatus.FILTERED, TimeProvider.now());
      }
      candidateRepository.save(candidate);
      out.add(frozen);
    }
    return List.copyOf(out);
  }
}

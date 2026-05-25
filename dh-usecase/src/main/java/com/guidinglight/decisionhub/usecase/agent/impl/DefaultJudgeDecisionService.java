package com.guidinglight.decisionhub.usecase.agent.impl;

import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.candidate.CandidateStatus;
import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import com.guidinglight.decisionhub.domain.judge.JudgeDecision;
import com.guidinglight.decisionhub.domain.judge.JudgeDecisionStatus;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.usecase.agent.JudgeDecisionRepository;
import com.guidinglight.decisionhub.usecase.agent.JudgeDecisionService;
import com.guidinglight.decisionhub.usecase.agent.StrategyCandidateRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage1：默认 Judge 仲裁服务。
 *
 * <p>聚合各候选的评审快照，按 finalScore 阈值挑选候选；标记 SELECTED / REJECTED。
 */
public final class DefaultJudgeDecisionService implements JudgeDecisionService {

  private static final double SELECT_THRESHOLD = 0.55;

  private final StrategyCandidateRepository candidateRepository;
  private final JudgeDecisionRepository judgeDecisionRepository;

  /** 构造。 */
  public DefaultJudgeDecisionService(
      final StrategyCandidateRepository candidateRepository,
      final JudgeDecisionRepository judgeDecisionRepository) {
    this.candidateRepository = candidateRepository;
    this.judgeDecisionRepository = judgeDecisionRepository;
  }

  @Override
  public JudgeDecision judge(
      final ResearchRun run,
      final List<StrategyCandidate> candidates,
      final List<Map<String, Object>> reviewSnapshots) {
    final List<String> selected = new ArrayList<>();
    final List<String> rejected = new ArrayList<>();
    final List<Map<String, Object>> reasons = new ArrayList<>();

    for (int i = 0; i < candidates.size(); i++) {
      final StrategyCandidate candidate = candidates.get(i);
      final Map<String, Object> review = i < reviewSnapshots.size() ? reviewSnapshots.get(i) : Map.of();
      final double finalScore = computeFinalScore(review);
      final boolean rejectedByRisk = isRejected(review);
      final boolean accepted = !rejectedByRisk && finalScore >= SELECT_THRESHOLD;

      final Map<String, Object> reasonEntry = new HashMap<>();
      reasonEntry.put("candidateId", candidate.getCandidateId());
      reasonEntry.put("finalScore", finalScore);
      reasonEntry.put("rejectedByRisk", rejectedByRisk);
      reasonEntry.put("selected", accepted);
      reasons.add(Map.copyOf(reasonEntry));

      if (accepted) {
        selected.add(candidate.getCandidateId());
        candidate.transitionTo(CandidateStatus.SELECTED, TimeProvider.now());
      } else {
        rejected.add(candidate.getCandidateId());
        candidate.transitionTo(CandidateStatus.REJECTED, TimeProvider.now());
      }
      candidateRepository.save(candidate);
    }

    final Map<String, Object> payload = new HashMap<>();
    payload.put("threshold", SELECT_THRESHOLD);
    payload.put("perCandidate", List.copyOf(reasons));
    payload.put("source", "DefaultJudgeDecisionService");

    final JudgeDecision draft =
        JudgeDecision.draft(
            run.getRunId(),
            run.getTenantId(),
            run.getTraceId(),
            List.copyOf(selected),
            List.copyOf(rejected),
            Map.copyOf(payload),
            TimeProvider.now());
    draft.transitionTo(
        selected.isEmpty() ? JudgeDecisionStatus.REJECTED : JudgeDecisionStatus.FINALIZED,
        TimeProvider.now());
    judgeDecisionRepository.save(draft);
    return draft;
  }

  private static double computeFinalScore(final Map<String, Object> review) {
    double score = 0.0;
    double risk = 0.5;
    double evidence = 0.0;
    final Object s = review.get("score");
    if (s instanceof Number n) {
      score = n.doubleValue();
    }
    final Object r = review.get("riskScore");
    if (r instanceof Number rn) {
      risk = rn.doubleValue();
    }
    final Object e = review.get("evidenceScore");
    if (e instanceof Number en) {
      evidence = en.doubleValue();
    }
    return (score * 0.5) + ((1.0 - risk) * 0.3) + (evidence * 0.2);
  }

  private static boolean isRejected(final Map<String, Object> review) {
    final Object r = review.get("rejected");
    return r instanceof Boolean b && b;
  }
}

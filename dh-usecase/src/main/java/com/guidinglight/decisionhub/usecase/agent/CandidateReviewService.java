package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import java.util.List;
import java.util.Map;

/**
 * Stage1：候选评审用例。
 *
 * <p>对应工单 4.2：CandidateReviewService。轻量评分 + 过滤；不接真实 LLM 评审。
 */
public interface CandidateReviewService {

  /**
   * 对一组候选进行评审，写入评分快照并设置 FILTERED/GENERATED 状态。
   *
   * @param candidates 候选列表。
   * @return 评审快照集合（每个候选一份），用于 Judge 聚合。
   */
  List<Map<String, Object>> review(List<StrategyCandidate> candidates);
}

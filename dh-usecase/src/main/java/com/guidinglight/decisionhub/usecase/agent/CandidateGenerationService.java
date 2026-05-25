package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import java.util.List;

/**
 * Stage1：策略候选生成用例。
 *
 * <p>对应工单 4.2 + 5.1：CandidateGenerationService。蜂群机制的工程落点：
 * 同一 ResearchRun 下并行生成多个 StrategyCandidate，每个候选记录 sourceAgent/searchPath/evidenceRefs。
 */
public interface CandidateGenerationService {

  /**
   * 为指定 run 生成多个候选。
   *
   * @param run 当前 run。
   * @return 已落库的候选列表，状态为 GENERATED。
   */
  List<StrategyCandidate> generate(ResearchRun run);
}

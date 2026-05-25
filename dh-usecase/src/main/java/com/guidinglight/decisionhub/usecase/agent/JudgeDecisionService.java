package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import com.guidinglight.decisionhub.domain.judge.JudgeDecision;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import java.util.List;
import java.util.Map;

/**
 * Stage1：Judge 仲裁用例。
 *
 * <p>对应工单 4.2 + 5.3：JudgeDecisionService。
 *
 * <p>所有最终输出必须经过 JudgeDecision，不允许单 Agent 直接给最终结论。
 */
public interface JudgeDecisionService {

  /**
   * 基于候选与评审快照产出 Judge 仲裁结果。
   *
   * @param run 当前 run。
   * @param candidates 候选列表。
   * @param reviewSnapshots 各候选的评审快照（顺序与 candidates 对齐）。
   * @return 已落库的 Judge 仲裁结果，状态为 FINALIZED 或 REJECTED。
   */
  JudgeDecision judge(
      ResearchRun run,
      List<StrategyCandidate> candidates,
      List<Map<String, Object>> reviewSnapshots);
}

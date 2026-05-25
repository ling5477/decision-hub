package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.agent.AgentTask;
import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import com.guidinglight.decisionhub.domain.judge.JudgeDecision;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import java.util.List;
import java.util.Optional;

/**
 * Stage1：ResearchRun 查询端用例。
 *
 * <p>对应工单 4.2：ResearchRunQueryService。
 */
public interface ResearchRunQueryService {

  /** 列出某租户的全部 run。 */
  List<ResearchRun> listRuns(String tenantId);

  /** 查询某 run 详情。 */
  Optional<ResearchRun> findRun(String runId);

  /** 查询某 run 的任务图。 */
  Optional<AgentTask> findTask(String runId);

  /** 查询某 run 的全部候选。 */
  List<StrategyCandidate> listCandidates(String runId);

  /** 查询某 run 的 Judge 仲裁结果。 */
  Optional<JudgeDecision> findJudgeDecision(String runId);
}

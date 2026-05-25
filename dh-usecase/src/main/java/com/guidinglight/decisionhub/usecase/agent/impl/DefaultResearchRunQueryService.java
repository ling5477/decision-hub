package com.guidinglight.decisionhub.usecase.agent.impl;

import com.guidinglight.decisionhub.domain.agent.AgentTask;
import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import com.guidinglight.decisionhub.domain.judge.JudgeDecision;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.usecase.agent.AgentTaskRepository;
import com.guidinglight.decisionhub.usecase.agent.JudgeDecisionRepository;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunQueryService;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunRepository;
import com.guidinglight.decisionhub.usecase.agent.StrategyCandidateRepository;
import java.util.List;
import java.util.Optional;

/** Stage1：默认 ResearchRun 查询端实现。 */
public final class DefaultResearchRunQueryService implements ResearchRunQueryService {

  private final ResearchRunRepository runRepository;
  private final AgentTaskRepository taskRepository;
  private final StrategyCandidateRepository candidateRepository;
  private final JudgeDecisionRepository judgeDecisionRepository;

  /** 构造。 */
  public DefaultResearchRunQueryService(
      final ResearchRunRepository runRepository,
      final AgentTaskRepository taskRepository,
      final StrategyCandidateRepository candidateRepository,
      final JudgeDecisionRepository judgeDecisionRepository) {
    this.runRepository = runRepository;
    this.taskRepository = taskRepository;
    this.candidateRepository = candidateRepository;
    this.judgeDecisionRepository = judgeDecisionRepository;
  }

  @Override
  public List<ResearchRun> listRuns(final String tenantId) {
    return runRepository.listByTenant(tenantId);
  }

  @Override
  public Optional<ResearchRun> findRun(final String runId) {
    return runRepository.find(runId);
  }

  @Override
  public Optional<AgentTask> findTask(final String runId) {
    return taskRepository.findByRun(runId);
  }

  @Override
  public List<StrategyCandidate> listCandidates(final String runId) {
    return candidateRepository.listByRun(runId);
  }

  @Override
  public Optional<JudgeDecision> findJudgeDecision(final String runId) {
    return judgeDecisionRepository.findByRun(runId);
  }
}

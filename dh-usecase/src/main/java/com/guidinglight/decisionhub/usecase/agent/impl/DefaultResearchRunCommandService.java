package com.guidinglight.decisionhub.usecase.agent.impl;

import com.guidinglight.decisionhub.common.error.BizException;
import com.guidinglight.decisionhub.common.error.CommonErrorCodes;
import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.agent.AgentTask;
import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.domain.research.ResearchRunStatus;
import com.guidinglight.decisionhub.usecase.agent.AgentTaskPlanner;
import com.guidinglight.decisionhub.usecase.agent.AgentTaskRepository;
import com.guidinglight.decisionhub.usecase.agent.CandidateGenerationService;
import com.guidinglight.decisionhub.usecase.agent.CandidateReviewService;
import com.guidinglight.decisionhub.usecase.agent.JudgeDecisionService;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunCommandService;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunRepository;
import java.util.List;
import java.util.Map;

/**
 * Stage1：默认 ResearchRun 命令端实现。
 *
 * <p>串行执行 plan -> generate -> review -> judge，并按阶段推进状态机。
 */
public final class DefaultResearchRunCommandService implements ResearchRunCommandService {

  private final ResearchRunRepository runRepository;
  private final AgentTaskRepository taskRepository;
  private final AgentTaskPlanner planner;
  private final CandidateGenerationService candidateGenerationService;
  private final CandidateReviewService candidateReviewService;
  private final JudgeDecisionService judgeDecisionService;

  /** 构造。 */
  public DefaultResearchRunCommandService(
      final ResearchRunRepository runRepository,
      final AgentTaskRepository taskRepository,
      final AgentTaskPlanner planner,
      final CandidateGenerationService candidateGenerationService,
      final CandidateReviewService candidateReviewService,
      final JudgeDecisionService judgeDecisionService) {
    this.runRepository = runRepository;
    this.taskRepository = taskRepository;
    this.planner = planner;
    this.candidateGenerationService = candidateGenerationService;
    this.candidateReviewService = candidateReviewService;
    this.judgeDecisionService = judgeDecisionService;
  }

  @Override
  public ResearchRun create(
      final String tenantId, final String topic, final Map<String, Object> payloadJson) {
    final ResearchRun run = ResearchRun.create(tenantId, topic, payloadJson, TimeProvider.now());
    runRepository.save(run);
    return run;
  }

  @Override
  public ResearchRun start(final String runId) {
    final ResearchRun run =
        runRepository
            .find(runId)
            .orElseThrow(
                () ->
                    new BizException(
                        CommonErrorCodes.NOT_FOUND,
                        "research run not found: " + runId,
                        null,
                        null));

    if (run.getStatus() != ResearchRunStatus.CREATED) {
      throw new BizException(
          CommonErrorCodes.CONFLICT,
          "research run cannot start in status " + run.getStatus(),
          null,
          null);
    }

    run.transitionTo(ResearchRunStatus.PLANNING, TimeProvider.now());
    runRepository.save(run);
    final AgentTask task = planner.plan(run);
    taskRepository.save(task);

    run.transitionTo(ResearchRunStatus.EXPLORING, TimeProvider.now());
    runRepository.save(run);
    final List<StrategyCandidate> candidates = candidateGenerationService.generate(run);

    run.transitionTo(ResearchRunStatus.REVIEWING, TimeProvider.now());
    runRepository.save(run);
    final List<Map<String, Object>> reviews = candidateReviewService.review(candidates);

    run.transitionTo(ResearchRunStatus.JUDGING, TimeProvider.now());
    runRepository.save(run);
    judgeDecisionService.judge(run, candidates, reviews);

    run.transitionTo(ResearchRunStatus.COMPLETED, TimeProvider.now());
    runRepository.save(run);
    return run;
  }
}

package com.guidinglight.decisionhub.usecase.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.agent.AgentTask;
import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import com.guidinglight.decisionhub.domain.experience.ExperienceEntry;
import com.guidinglight.decisionhub.domain.feedback.FeedbackSource;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.domain.judge.JudgeDecision;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.domain.research.ResearchRunStatus;
import com.guidinglight.decisionhub.memory.agent.ExperienceStore;
import com.guidinglight.decisionhub.memory.agent.FailureCaseStore;
import com.guidinglight.decisionhub.memory.agent.PheromoneStore;
import com.guidinglight.decisionhub.memory.agent.inmemory.InMemoryExperienceStore;
import com.guidinglight.decisionhub.memory.agent.inmemory.InMemoryFailureCaseStore;
import com.guidinglight.decisionhub.memory.agent.inmemory.InMemoryPheromoneStore;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultAgentTaskPlanner;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultCandidateGenerationService;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultCandidateReviewService;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultExperienceFeedbackService;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultJudgeDecisionService;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultNqIntegrationUseCase;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultResearchRunCommandService;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultResearchRunQueryService;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryAgentTaskRepository;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryJudgeDecisionRepository;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryNqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryResearchRunRepository;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryStrategyCandidateRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Stage1 闭环兼容性测试：feedback ingress 只追加事件；mutable learning 仅在测试显式直调 service 时发生。
 */
class ResearchRunStage1ClosedLoopTest {

  @Test
  void feedbackIngressIsAppendOnly_andExplicitLearningRemainsIsolated() {
    final ResearchRunRepository runRepo = new InMemoryResearchRunRepository();
    final AgentTaskRepository taskRepo = new InMemoryAgentTaskRepository();
    final StrategyCandidateRepository candidateRepo = new InMemoryStrategyCandidateRepository();
    final JudgeDecisionRepository judgeRepo = new InMemoryJudgeDecisionRepository();
    final NqFeedbackEventRepository feedbackEventRepo = new InMemoryNqFeedbackEventRepository();
    final ExperienceStore experienceStore = new InMemoryExperienceStore();
    final PheromoneStore pheromoneStore = new InMemoryPheromoneStore();
    final FailureCaseStore failureCaseStore = new InMemoryFailureCaseStore();

    final AgentTaskPlanner planner = new DefaultAgentTaskPlanner();
    final CandidateGenerationService generation =
        new DefaultCandidateGenerationService(candidateRepo);
    final CandidateReviewService review = new DefaultCandidateReviewService(candidateRepo);
    final JudgeDecisionService judge = new DefaultJudgeDecisionService(candidateRepo, judgeRepo);
    final ResearchRunCommandService command =
        new DefaultResearchRunCommandService(
            runRepo, taskRepo, planner, generation, review, judge);
    final ResearchRunQueryService query =
        new DefaultResearchRunQueryService(runRepo, taskRepo, candidateRepo, judgeRepo);
    final ExperienceFeedbackService experienceFeedback =
        new DefaultExperienceFeedbackService(experienceStore, pheromoneStore, failureCaseStore);
    final NqIntegrationUseCase nqIntegration = new DefaultNqIntegrationUseCase(feedbackEventRepo);

    final ResearchRun created =
        command.create("t-test", "topic-a", Map.of("hint", "stage1-test"));
    assertEquals(ResearchRunStatus.CREATED, created.getStatus());
    assertNotNull(created.getRunId());
    assertNotNull(created.getTraceId());

    final ResearchRun started = command.start(created.getRunId());
    assertEquals(ResearchRunStatus.COMPLETED, started.getStatus());

    final Optional<AgentTask> task = query.findTask(created.getRunId());
    assertTrue(task.isPresent(), "AgentTask should exist after start");
    assertFalse(task.get().getNodes().isEmpty(), "task graph should have nodes");

    final List<StrategyCandidate> candidates = query.listCandidates(created.getRunId());
    assertFalse(candidates.isEmpty(), "should produce candidates");

    final Optional<JudgeDecision> decision = query.findJudgeDecision(created.getRunId());
    assertTrue(decision.isPresent(), "Judge decision must be persisted");
    assertEquals(created.getTraceId(), decision.get().getTraceId());

    final Map<String, Object> payload = new HashMap<>();
    payload.put("strategyPattern", "momentum");
    payload.put("marketRegime", "trend-up");
    payload.put("dataSource", "internal-bars");
    payload.put("agentRole", "STRATEGY");
    final NqFeedbackEvent positiveEvent =
        NqFeedbackEvent.create(
            "t-test",
            created.getRunId(),
            candidates.get(0).getCandidateId(),
            created.getTraceId(),
            FeedbackSource.BACKTEST,
            "BacktestCompleted",
            true,
            payload,
            Instant.now(),
            TimeProvider.now());
    nqIntegration.onFeedback(positiveEvent);

    final List<NqFeedbackEvent> persisted =
        feedbackEventRepo.listByRun("t-test", created.getRunId());
    assertEquals(1, persisted.size(), "feedback event should be persisted");
    assertTrue(experienceStore.listAll("t-test").isEmpty(), "ingress must not write experience");
    assertTrue(
        pheromoneStore.listByFrom("t-test", "trend-up").isEmpty(),
        "ingress must not write pheromone");
    assertTrue(
        failureCaseStore.listByRun("t-test", created.getRunId()).isEmpty(),
        "ingress must not write failure cases");

    final NqFeedbackEvent riskRejection =
        NqFeedbackEvent.create(
            "t-test",
            created.getRunId(),
            candidates.get(0).getCandidateId(),
            created.getTraceId(),
            FeedbackSource.RISK,
            "RiskRejected",
            false,
            payload,
            Instant.now(),
            TimeProvider.now());
    nqIntegration.onFeedback(riskRejection);

    assertEquals(
        2,
        feedbackEventRepo.listByRun("t-test", created.getRunId()).size(),
        "append-only compatibility entry must retain both events");
    assertTrue(experienceStore.listAll("t-test").isEmpty());
    assertTrue(pheromoneStore.listByFrom("t-test", "trend-up").isEmpty());
    assertTrue(failureCaseStore.listByRun("t-test", created.getRunId()).isEmpty());

    // Explicit learning capability remains testable only through direct service invocation.
    experienceFeedback.apply(positiveEvent);
    experienceFeedback.apply(riskRejection);

    final List<ExperienceEntry> experiences = experienceStore.listAll("t-test");
    assertEquals(1, experiences.size(), "explicit learning should create one experience entry");
    final ExperienceEntry afterPenalty = experiences.get(0);
    assertEquals(1L, afterPenalty.getSuccessCount());
    assertEquals(1L, afterPenalty.getFailureCount());
    assertEquals(
        1,
        pheromoneStore.listByFrom("t-test", "trend-up").size(),
        "explicit learning should preserve the positive trail behavior");
    assertFalse(
        failureCaseStore.listByRun("t-test", created.getRunId()).isEmpty(),
        "risk rejection must be captured into failure-case store");
  }
}

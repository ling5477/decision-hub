package com.guidinglight.decisionhub.usecase.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.agent.AgentRole;
import com.guidinglight.decisionhub.domain.agent.AgentTask;
import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import com.guidinglight.decisionhub.domain.checkpoint.CheckpointEntry;
import com.guidinglight.decisionhub.domain.checkpoint.CheckpointStatus;
import com.guidinglight.decisionhub.domain.checkpoint.CheckpointType;
import com.guidinglight.decisionhub.domain.judge.JudgeDecision;
import com.guidinglight.decisionhub.domain.reflection.ReflectionEntry;
import com.guidinglight.decisionhub.domain.reflection.ReflectionType;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.domain.research.ResearchRunStatus;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultCandidateGenerationService;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultCandidateReviewService;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultJudgeDecisionService;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultReflectionCheckpointService;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultResearchRunCommandService;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultResearchRunQueryService;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryAgentTaskRepository;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryCheckpointEntryRepository;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryJudgeDecisionRepository;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryReflectionEntryRepository;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryResearchRunRepository;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryStrategyCandidateRepository;
import com.guidinglight.decisionhub.usecase.agent.planner.DynamicAgentTaskPlanner;
import com.guidinglight.decisionhub.usecase.agent.planner.PlannerStrategy;
import com.guidinglight.decisionhub.usecase.agent.planner.PlannerStrategyRegistry;
import com.guidinglight.decisionhub.usecase.agent.planner.impl.DefaultPlannerStrategyResolver;
import com.guidinglight.decisionhub.usecase.agent.planner.strategy.BearFocusedPlannerStrategyHandler;
import com.guidinglight.decisionhub.usecase.agent.planner.strategy.BullFocusedPlannerStrategyHandler;
import com.guidinglight.decisionhub.usecase.agent.planner.strategy.DefaultPlannerStrategyHandler;
import com.guidinglight.decisionhub.usecase.agent.planner.strategy.VolatileDiversifiedPlannerStrategyHandler;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Stage2-PoC-B5：完整 Stage2 闭环验收测试。
 *
 * <p>编排：DynamicAgentTaskPlanner（4 个策略 handler + resolver） -&gt; CandidateGeneration -&gt;
 * CandidateReview -&gt; ReflectionCheckpointService（过程证据） -&gt; JudgeDecision（唯一最终出口）。
 *
 * <p>无 LLM、无 HTTP、无真实下单、无 TradingAgents Python 代码、无 Kronos、无 global-stock-data。
 */
class Stage2ClosedLoopTest {

  @Test
  void dynamic_planner_drives_reflection_checkpoint_and_judge_decision() {
    final ResearchRunRepository runRepo = new InMemoryResearchRunRepository();
    final AgentTaskRepository taskRepo = new InMemoryAgentTaskRepository();
    final StrategyCandidateRepository candidateRepo = new InMemoryStrategyCandidateRepository();
    final JudgeDecisionRepository judgeRepo = new InMemoryJudgeDecisionRepository();
    final InMemoryReflectionEntryRepository reflectionRepo =
        new InMemoryReflectionEntryRepository();
    final InMemoryCheckpointEntryRepository checkpointRepo =
        new InMemoryCheckpointEntryRepository();

    final PlannerStrategyRegistry registry =
        new PlannerStrategyRegistry(
            List.of(
                new DefaultPlannerStrategyHandler(),
                new BullFocusedPlannerStrategyHandler(),
                new BearFocusedPlannerStrategyHandler(),
                new VolatileDiversifiedPlannerStrategyHandler()));
    final DynamicAgentTaskPlanner planner =
        new DynamicAgentTaskPlanner(new DefaultPlannerStrategyResolver(), registry);
    final CandidateGenerationService generation =
        new DefaultCandidateGenerationService(candidateRepo);
    final CandidateReviewService review = new DefaultCandidateReviewService(candidateRepo);
    final JudgeDecisionService judge = new DefaultJudgeDecisionService(candidateRepo, judgeRepo);
    final ResearchRunCommandService command =
        new DefaultResearchRunCommandService(
            runRepo, taskRepo, planner, generation, review, judge);
    final ResearchRunQueryService query =
        new DefaultResearchRunQueryService(runRepo, taskRepo, candidateRepo, judgeRepo);
    final ReflectionCheckpointService reflectionCheckpoint =
        new DefaultReflectionCheckpointService(reflectionRepo, checkpointRepo);

    final ResearchRun created =
        command.create("t-test", "topic-stage2", Map.of("marketRegime", "bullish"));
    assertEquals(ResearchRunStatus.CREATED, created.getStatus());

    final ResearchRun started = command.start(created.getRunId());
    assertEquals(ResearchRunStatus.COMPLETED, started.getStatus());

    final Optional<AgentTask> task = query.findTask(created.getRunId());
    assertTrue(task.isPresent(), "AgentTask should exist after dynamic planner runs");
    assertEquals(
        PlannerStrategy.BULL_FOCUSED.name(),
        task.get().getPayloadJson().get("plannerStrategy"),
        "bullish regime must resolve to BULL_FOCUSED strategy");

    reflectionCheckpoint.recordReflection(
        started,
        2,
        AgentRole.STRATEGY,
        ReflectionType.STEP_REFLECTION,
        "strategy synthesized momentum candidate",
        null);
    reflectionCheckpoint.recordReflection(
        started,
        0,
        AgentRole.SCOUT,
        ReflectionType.STEP_REFLECTION,
        "scout discovered bullish setup",
        null);
    reflectionCheckpoint.recordReflection(
        started,
        1,
        AgentRole.ANALYST,
        ReflectionType.STEP_REFLECTION,
        "analyst confirmed signal quality",
        null);

    final List<ReflectionEntry> reflections =
        reflectionCheckpoint.listReflections(created.getRunId());
    assertEquals(3, reflections.size(), "all process reflections must be recorded");
    assertEquals(0, reflections.get(0).getStepIndex());
    assertEquals(1, reflections.get(1).getStepIndex());
    assertEquals(2, reflections.get(2).getStepIndex(), "reflections must be ordered by stepIndex");

    reflectionCheckpoint.recordCheckpoint(
        started,
        1,
        CheckpointType.JUDGE_DECISION,
        CheckpointStatus.RECORDED,
        "{\"phase\":\"judge\"}");
    reflectionCheckpoint.recordCheckpoint(
        started,
        0,
        CheckpointType.CANDIDATE_FROZEN,
        CheckpointStatus.RECORDED,
        "{\"phase\":\"freeze\"}");

    final List<CheckpointEntry> checkpoints =
        reflectionCheckpoint.listCheckpoints(created.getRunId());
    assertEquals(2, checkpoints.size());
    assertEquals(
        CheckpointType.CANDIDATE_FROZEN,
        checkpoints.get(0).getType(),
        "checkpoints must be ordered by checkpointIndex (frozen first)");
    assertEquals(CheckpointType.JUDGE_DECISION, checkpoints.get(1).getType());

    final List<StrategyCandidate> candidates = query.listCandidates(created.getRunId());
    assertFalse(candidates.isEmpty(), "candidates should be produced");

    final Optional<JudgeDecision> decision = query.findJudgeDecision(created.getRunId());
    assertTrue(decision.isPresent(), "JudgeDecision must be the sole final exit");
    assertNotNull(decision.get().getTraceId());
    assertEquals(created.getTraceId(), decision.get().getTraceId());

    for (ReflectionEntry r : reflections) {
      assertFalse(
          r.getType() == ReflectionType.RUN_RETROSPECTIVE
              && r.getPayloadJson() != null
              && r.getPayloadJson().contains("finalCandidateId"),
          "ReflectionEntry must not encode final recommendation; JudgeDecision is sole exit");
    }
  }

  @Test
  void bear_regime_run_still_terminates_with_judge_decision_only() {
    final ResearchRunRepository runRepo = new InMemoryResearchRunRepository();
    final AgentTaskRepository taskRepo = new InMemoryAgentTaskRepository();
    final StrategyCandidateRepository candidateRepo = new InMemoryStrategyCandidateRepository();
    final JudgeDecisionRepository judgeRepo = new InMemoryJudgeDecisionRepository();
    final InMemoryReflectionEntryRepository reflectionRepo =
        new InMemoryReflectionEntryRepository();
    final InMemoryCheckpointEntryRepository checkpointRepo =
        new InMemoryCheckpointEntryRepository();

    final PlannerStrategyRegistry registry =
        new PlannerStrategyRegistry(
            List.of(
                new DefaultPlannerStrategyHandler(),
                new BullFocusedPlannerStrategyHandler(),
                new BearFocusedPlannerStrategyHandler(),
                new VolatileDiversifiedPlannerStrategyHandler()));
    final DynamicAgentTaskPlanner planner =
        new DynamicAgentTaskPlanner(new DefaultPlannerStrategyResolver(), registry);
    final ResearchRunCommandService command =
        new DefaultResearchRunCommandService(
            runRepo,
            taskRepo,
            planner,
            new DefaultCandidateGenerationService(candidateRepo),
            new DefaultCandidateReviewService(candidateRepo),
            new DefaultJudgeDecisionService(candidateRepo, judgeRepo));
    final ResearchRunQueryService query =
        new DefaultResearchRunQueryService(runRepo, taskRepo, candidateRepo, judgeRepo);
    final ReflectionCheckpointService reflectionCheckpoint =
        new DefaultReflectionCheckpointService(reflectionRepo, checkpointRepo);

    final ResearchRun created =
        command.create("t-test", "topic-stage2-bear", Map.of("marketRegime", "bear"));
    final ResearchRun started = command.start(created.getRunId());

    assertEquals(ResearchRunStatus.COMPLETED, started.getStatus());
    assertEquals(
        PlannerStrategy.BEAR_FOCUSED.name(),
        query.findTask(created.getRunId()).orElseThrow().getPayloadJson().get("plannerStrategy"));

    reflectionCheckpoint.recordReflection(
        started,
        0,
        AgentRole.RISK_REVIEWER,
        ReflectionType.STEP_REFLECTION,
        "risk reviewer detected drawdown stress",
        "{\"risk\":\"elevated\"}");
    reflectionCheckpoint.recordCheckpoint(
        started,
        0,
        CheckpointType.PIVOT_TRIGGERED,
        CheckpointStatus.RECORDED,
        "{\"reason\":\"pivot to defensive\"}");

    final Optional<JudgeDecision> decision = query.findJudgeDecision(created.getRunId());
    assertTrue(
        decision.isPresent(),
        "even in bear regime with pivot checkpoint, JudgeDecision must be the sole final exit");

    assertFalse(reflectionCheckpoint.listReflections(created.getRunId()).isEmpty());
    assertFalse(reflectionCheckpoint.listCheckpoints(created.getRunId()).isEmpty());
  }

  @SuppressWarnings("unused")
  private static ResearchRun isolatedRun(final Map<String, Object> payload) {
    return ResearchRun.create("t-test", "topic-stage2", payload, TimeProvider.now());
  }
}

package com.guidinglight.decisionhub.usecase.agent.planner.strategy;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import com.guidinglight.decisionhub.domain.agent.AgentRole;
import com.guidinglight.decisionhub.domain.agent.AgentTask;
import com.guidinglight.decisionhub.domain.agent.TaskNode;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.usecase.agent.planner.PlannerStrategy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage2-PoC-B4：BULL_FOCUSED 策略 Handler。
 *
 * <p>进攻型链路：SCOUT -&gt; ANALYST -&gt; STRATEGY x2 -&gt; STRATEGY_REVIEWER -&gt; JUDGE。 多生成候选、单道复核。
 * 不修改 dh-domain；不引入新 AgentRole，仅在节点 name 上区分多实例。
 */
public final class BullFocusedPlannerStrategyHandler implements PlannerStrategyHandler {

  /** 默认构造。 */
  public BullFocusedPlannerStrategyHandler() {
    // no-op
  }

  @Override
  public PlannerStrategy strategy() {
    return PlannerStrategy.BULL_FOCUSED;
  }

  @Override
  public AgentTask buildTask(final ResearchRun run, final Instant now) {
    final List<TaskNode> nodes = new ArrayList<>();
    final TaskNode scout = TaskNode.create(AgentRole.SCOUT, "scout", List.of(), Map.of());
    final TaskNode analyst =
        TaskNode.create(AgentRole.ANALYST, "analyst", List.of(scout.getNodeId()), Map.of());
    final TaskNode strategyPrimary =
        TaskNode.create(
            AgentRole.STRATEGY, "strategy-primary", List.of(analyst.getNodeId()), Map.of());
    final TaskNode strategySecondary =
        TaskNode.create(
            AgentRole.STRATEGY, "strategy-secondary", List.of(analyst.getNodeId()), Map.of());
    final TaskNode strategyReviewer =
        TaskNode.create(
            AgentRole.STRATEGY_REVIEWER,
            "strategy-reviewer",
            List.of(strategyPrimary.getNodeId(), strategySecondary.getNodeId()),
            Map.of());
    final TaskNode judge =
        TaskNode.create(
            AgentRole.JUDGE, "judge", List.of(strategyReviewer.getNodeId()), Map.of());

    nodes.add(scout);
    nodes.add(analyst);
    nodes.add(strategyPrimary);
    nodes.add(strategySecondary);
    nodes.add(strategyReviewer);
    nodes.add(judge);

    final Map<String, Object> payload = new HashMap<>();
    payload.put("planSchemaVersion", "stage2-b4-v1");
    payload.put("planId", IdGenerator.newId());
    payload.put("plannerStrategy", PlannerStrategy.BULL_FOCUSED.name());

    return AgentTask.create(
        run.getRunId(),
        run.getTenantId(),
        run.getTraceId(),
        nodes,
        Map.copyOf(payload),
        now);
  }
}

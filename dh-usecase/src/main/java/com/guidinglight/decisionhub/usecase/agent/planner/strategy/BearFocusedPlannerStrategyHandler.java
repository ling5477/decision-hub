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
 * Stage2-PoC-B4：BEAR_FOCUSED 策略 Handler。
 *
 * <p>防守型链路：SCOUT -&gt; ANALYST -&gt; STRATEGY -&gt; RISK_REVIEWER x2 -&gt; JUDGE。 单候选、双道风险复核。
 */
public final class BearFocusedPlannerStrategyHandler implements PlannerStrategyHandler {

  /** 默认构造。 */
  public BearFocusedPlannerStrategyHandler() {
    // no-op
  }

  @Override
  public PlannerStrategy strategy() {
    return PlannerStrategy.BEAR_FOCUSED;
  }

  @Override
  public AgentTask buildTask(final ResearchRun run, final Instant now) {
    final List<TaskNode> nodes = new ArrayList<>();
    final TaskNode scout = TaskNode.create(AgentRole.SCOUT, "scout", List.of(), Map.of());
    final TaskNode analyst =
        TaskNode.create(AgentRole.ANALYST, "analyst", List.of(scout.getNodeId()), Map.of());
    final TaskNode strategy =
        TaskNode.create(AgentRole.STRATEGY, "strategy", List.of(analyst.getNodeId()), Map.of());
    final TaskNode riskPrimary =
        TaskNode.create(
            AgentRole.RISK_REVIEWER, "risk-primary", List.of(strategy.getNodeId()), Map.of());
    final TaskNode riskSecondary =
        TaskNode.create(
            AgentRole.RISK_REVIEWER, "risk-secondary", List.of(strategy.getNodeId()), Map.of());
    final TaskNode judge =
        TaskNode.create(
            AgentRole.JUDGE,
            "judge",
            List.of(riskPrimary.getNodeId(), riskSecondary.getNodeId()),
            Map.of());

    nodes.add(scout);
    nodes.add(analyst);
    nodes.add(strategy);
    nodes.add(riskPrimary);
    nodes.add(riskSecondary);
    nodes.add(judge);

    final Map<String, Object> payload = new HashMap<>();
    payload.put("planSchemaVersion", "stage2-b4-v1");
    payload.put("planId", IdGenerator.newId());
    payload.put("plannerStrategy", PlannerStrategy.BEAR_FOCUSED.name());

    return AgentTask.create(
        run.getRunId(),
        run.getTenantId(),
        run.getTraceId(),
        nodes,
        Map.copyOf(payload),
        now);
  }
}

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
 * Stage2-PoC-B4：DEFAULT 策略 Handler。
 *
 * <p>保留 Stage1 已验证的固定 6 节点骨架：SCOUT -&gt; ANALYST -&gt; STRATEGY -&gt; RISK_REVIEWER +
 * STRATEGY_REVIEWER -&gt; JUDGE，确保 Stage1 闭环测试在引入 Dynamic Planner 之后依旧通过。
 */
public final class DefaultPlannerStrategyHandler implements PlannerStrategyHandler {

  /** 默认构造。 */
  public DefaultPlannerStrategyHandler() {
    // no-op
  }

  @Override
  public PlannerStrategy strategy() {
    return PlannerStrategy.DEFAULT;
  }

  @Override
  public AgentTask buildTask(final ResearchRun run, final Instant now) {
    final List<TaskNode> nodes = new ArrayList<>();
    final TaskNode scout = TaskNode.create(AgentRole.SCOUT, "scout", List.of(), Map.of());
    final TaskNode analyst =
        TaskNode.create(AgentRole.ANALYST, "analyst", List.of(scout.getNodeId()), Map.of());
    final TaskNode strategy =
        TaskNode.create(AgentRole.STRATEGY, "strategy", List.of(analyst.getNodeId()), Map.of());
    final TaskNode riskReviewer =
        TaskNode.create(
            AgentRole.RISK_REVIEWER,
            "risk-reviewer",
            List.of(strategy.getNodeId()),
            Map.of());
    final TaskNode strategyReviewer =
        TaskNode.create(
            AgentRole.STRATEGY_REVIEWER,
            "strategy-reviewer",
            List.of(strategy.getNodeId()),
            Map.of());
    final TaskNode judge =
        TaskNode.create(
            AgentRole.JUDGE,
            "judge",
            List.of(riskReviewer.getNodeId(), strategyReviewer.getNodeId()),
            Map.of());

    nodes.add(scout);
    nodes.add(analyst);
    nodes.add(strategy);
    nodes.add(riskReviewer);
    nodes.add(strategyReviewer);
    nodes.add(judge);

    final Map<String, Object> payload = new HashMap<>();
    payload.put("planSchemaVersion", "stage2-b4-v1");
    payload.put("planId", IdGenerator.newId());
    payload.put("plannerStrategy", PlannerStrategy.DEFAULT.name());

    return AgentTask.create(
        run.getRunId(),
        run.getTenantId(),
        run.getTraceId(),
        nodes,
        Map.copyOf(payload),
        now);
  }
}

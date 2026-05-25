package com.guidinglight.decisionhub.usecase.agent.impl;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.agent.AgentRole;
import com.guidinglight.decisionhub.domain.agent.AgentTask;
import com.guidinglight.decisionhub.domain.agent.TaskNode;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.usecase.agent.AgentTaskPlanner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage1：默认 Leader 规划器。
 *
 * <p>生成固定的“狼群骨架”任务图：SCOUT -&gt; ANALYST -&gt; STRATEGY -&gt; RISK_REVIEWER -&gt; STRATEGY_REVIEWER
 * -&gt; JUDGE。Stage1 不做动态分支。
 */
public final class DefaultAgentTaskPlanner implements AgentTaskPlanner {

  /** 默认构造。 */
  public DefaultAgentTaskPlanner() {
    // no-op
  }

  @Override
  public AgentTask plan(final ResearchRun run) {
    final List<TaskNode> nodes = new ArrayList<>();
    final TaskNode scout = TaskNode.create(AgentRole.SCOUT, "scout", List.of(), Map.of());
    final TaskNode analyst =
        TaskNode.create(AgentRole.ANALYST, "analyst", List.of(scout.getNodeId()), Map.of());
    final TaskNode strategy =
        TaskNode.create(
            AgentRole.STRATEGY, "strategy", List.of(analyst.getNodeId()), Map.of());
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
    payload.put("planSchemaVersion", "stage1-v1");
    payload.put("planId", IdGenerator.newId());

    return AgentTask.create(
        run.getRunId(),
        run.getTenantId(),
        run.getTraceId(),
        nodes,
        Map.copyOf(payload),
        TimeProvider.now());
  }
}

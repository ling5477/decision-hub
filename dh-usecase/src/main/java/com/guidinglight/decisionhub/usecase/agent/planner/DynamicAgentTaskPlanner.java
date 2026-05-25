package com.guidinglight.decisionhub.usecase.agent.planner;

import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.agent.AgentTask;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.usecase.agent.AgentTaskPlanner;
import com.guidinglight.decisionhub.usecase.agent.planner.strategy.PlannerStrategyHandler;
import java.util.Objects;

/**
 * Stage2-PoC-B4：基于 resolver + registry 的轻量动态 Planner。
 *
 * <p>编排顺序：
 *
 * <ol>
 *   <li>{@link PlannerStrategyResolver} 从 ResearchRun 解析 strategy。
 *   <li>{@link PlannerStrategyRegistry} 按 strategy 查 handler；未注册者回退到 DEFAULT。
 *   <li>Handler 产出 AgentTask。
 * </ol>
 *
 * <p>本类没有 LLM 调用、graph scheduler、TradingAgents Python 代码或真实下单副作用。
 * JudgeDecision 仍是唯一最终出口，本类只产出过程任务图。
 */
public final class DynamicAgentTaskPlanner implements AgentTaskPlanner {

  private final PlannerStrategyResolver resolver;
  private final PlannerStrategyRegistry registry;

  /** 构造。 */
  public DynamicAgentTaskPlanner(
      final PlannerStrategyResolver resolver, final PlannerStrategyRegistry registry) {
    this.resolver = Objects.requireNonNull(resolver, "resolver");
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  @Override
  public AgentTask plan(final ResearchRun run) {
    final PlannerStrategy strategy = resolver.resolve(run);
    final PlannerStrategyHandler handler = registry.handlerFor(strategy);
    return handler.buildTask(run, TimeProvider.now());
  }
}

package com.guidinglight.decisionhub.usecase.agent.planner.strategy;

import com.guidinglight.decisionhub.domain.agent.AgentTask;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.usecase.agent.planner.PlannerStrategy;
import java.time.Instant;

/**
 * Stage2-PoC-B4：单个 PlannerStrategy 的轻量任务图构造器。
 *
 * <p>每个 Handler 只负责按一种策略产出 {@link AgentTask}；如何选边由 {@code
 * PlannerStrategyResolver} 决定，注册查找由 {@code PlannerStrategyRegistry} 完成。
 *
 * <p>禁止在 Handler 中引入：LLM 调用、graph scheduler、TradingAgents Python 代码、真实下单或绕过 NQ
 * 风控的副作用。JudgeDecision 仍是唯一最终出口，Handler 只产出过程任务图。
 */
public interface PlannerStrategyHandler {

  /** 该 Handler 对应的策略。 */
  PlannerStrategy strategy();

  /**
   * 为指定 run 构造 AgentTask。
   *
   * @param run 已 transition 到 PLANNING 状态的 run。
   * @param now 任务图创建时间。
   * @return 任务图，节点列表非空。
   */
  AgentTask buildTask(ResearchRun run, Instant now);
}

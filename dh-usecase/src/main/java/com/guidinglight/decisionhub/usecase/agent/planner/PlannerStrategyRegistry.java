package com.guidinglight.decisionhub.usecase.agent.planner;

import com.guidinglight.decisionhub.usecase.agent.planner.strategy.PlannerStrategyHandler;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Stage2-PoC-B4：PlannerStrategy -&gt; Handler 注册表。
 *
 * <p>必须显式注入 DEFAULT Handler；其它 strategy 缺失时统一 fallback 到 DEFAULT。 注册表本身不依赖 LLM、graph
 * scheduler 或任何 TradingAgents Python 代码。
 */
public final class PlannerStrategyRegistry {

  private final Map<PlannerStrategy, PlannerStrategyHandler> handlers =
      new EnumMap<>(PlannerStrategy.class);
  private final PlannerStrategyHandler defaultHandler;

  /**
   * 构造。
   *
   * @param handlerList 所有可用 handler；必须包含 strategy == {@link PlannerStrategy#DEFAULT} 的一项。
   * @throws IllegalArgumentException 缺少 DEFAULT handler 或同一 strategy 重复注册时抛出。
   */
  public PlannerStrategyRegistry(final List<PlannerStrategyHandler> handlerList) {
    Objects.requireNonNull(handlerList, "handlerList");
    for (PlannerStrategyHandler handler : handlerList) {
      final PlannerStrategy key = handler.strategy();
      if (handlers.putIfAbsent(key, handler) != null) {
        throw new IllegalArgumentException(
            "duplicate PlannerStrategyHandler for strategy " + key);
      }
    }
    final PlannerStrategyHandler def = handlers.get(PlannerStrategy.DEFAULT);
    if (def == null) {
      throw new IllegalArgumentException(
          "PlannerStrategyRegistry must contain DEFAULT handler");
    }
    this.defaultHandler = def;
  }

  /**
   * 按 strategy 查找 handler；未注册的 strategy 必须 fallback 到 DEFAULT。
   *
   * @param strategy 选定的 strategy，允许传 null（按 DEFAULT 处理）。
   * @return 命中的 handler 或 DEFAULT。
   */
  public PlannerStrategyHandler handlerFor(final PlannerStrategy strategy) {
    if (strategy == null) {
      return defaultHandler;
    }
    final PlannerStrategyHandler handler = handlers.get(strategy);
    return handler == null ? defaultHandler : handler;
  }

  /** 是否注册了指定 strategy。 */
  public boolean contains(final PlannerStrategy strategy) {
    return strategy != null && handlers.containsKey(strategy);
  }
}

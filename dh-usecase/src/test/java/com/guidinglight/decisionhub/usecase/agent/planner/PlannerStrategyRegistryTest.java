package com.guidinglight.decisionhub.usecase.agent.planner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.agent.AgentTask;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.usecase.agent.planner.strategy.BearFocusedPlannerStrategyHandler;
import com.guidinglight.decisionhub.usecase.agent.planner.strategy.BullFocusedPlannerStrategyHandler;
import com.guidinglight.decisionhub.usecase.agent.planner.strategy.DefaultPlannerStrategyHandler;
import com.guidinglight.decisionhub.usecase.agent.planner.strategy.PlannerStrategyHandler;
import com.guidinglight.decisionhub.usecase.agent.planner.strategy.VolatileDiversifiedPlannerStrategyHandler;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Stage2-PoC-B4：PlannerStrategyRegistry 单元测试。
 *
 * <p>覆盖：DEFAULT 强制注册、未知 strategy fallback DEFAULT、null strategy fallback DEFAULT、重复注册拒绝。
 */
class PlannerStrategyRegistryTest {

  @Test
  void registry_without_default_handler_is_rejected() {
    final IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> new PlannerStrategyRegistry(List.of(new BullFocusedPlannerStrategyHandler())));
    assertTrue(ex.getMessage().contains("DEFAULT"));
  }

  @Test
  void duplicate_handler_for_same_strategy_is_rejected() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PlannerStrategyRegistry(
                List.of(
                    new DefaultPlannerStrategyHandler(),
                    new DefaultPlannerStrategyHandler())));
  }

  @Test
  void registry_returns_registered_handler() {
    final DefaultPlannerStrategyHandler def = new DefaultPlannerStrategyHandler();
    final BullFocusedPlannerStrategyHandler bull = new BullFocusedPlannerStrategyHandler();
    final BearFocusedPlannerStrategyHandler bear = new BearFocusedPlannerStrategyHandler();
    final VolatileDiversifiedPlannerStrategyHandler volatileH =
        new VolatileDiversifiedPlannerStrategyHandler();
    final PlannerStrategyRegistry registry =
        new PlannerStrategyRegistry(List.of(def, bull, bear, volatileH));

    assertSame(def, registry.handlerFor(PlannerStrategy.DEFAULT));
    assertSame(bull, registry.handlerFor(PlannerStrategy.BULL_FOCUSED));
    assertSame(bear, registry.handlerFor(PlannerStrategy.BEAR_FOCUSED));
    assertSame(volatileH, registry.handlerFor(PlannerStrategy.VOLATILE_DIVERSIFIED));
    assertTrue(registry.contains(PlannerStrategy.BULL_FOCUSED));
  }

  @Test
  void missing_strategy_falls_back_to_default_handler() {
    final DefaultPlannerStrategyHandler def = new DefaultPlannerStrategyHandler();
    final PlannerStrategyRegistry registry = new PlannerStrategyRegistry(List.of(def));

    assertSame(def, registry.handlerFor(PlannerStrategy.BULL_FOCUSED));
    assertSame(def, registry.handlerFor(PlannerStrategy.BEAR_FOCUSED));
    assertSame(def, registry.handlerFor(PlannerStrategy.VOLATILE_DIVERSIFIED));
    assertSame(def, registry.handlerFor(null));
    assertFalse(registry.contains(PlannerStrategy.BULL_FOCUSED));
  }

  @Test
  void each_handler_produces_non_empty_task() {
    final ResearchRun run =
        ResearchRun.create("t-test", "topic", Map.of(), TimeProvider.now());
    final Instant now = TimeProvider.now();
    final List<PlannerStrategyHandler> all =
        List.of(
            new DefaultPlannerStrategyHandler(),
            new BullFocusedPlannerStrategyHandler(),
            new BearFocusedPlannerStrategyHandler(),
            new VolatileDiversifiedPlannerStrategyHandler());
    for (PlannerStrategyHandler handler : all) {
      final AgentTask task = handler.buildTask(run, now);
      assertFalse(
          task.getNodes().isEmpty(),
          () -> "handler " + handler.strategy() + " produced empty node list");
      assertEquals(run.getRunId(), task.getRunId());
      assertEquals(run.getTraceId(), task.getTraceId());
      assertEquals(handler.strategy().name(), task.getPayloadJson().get("plannerStrategy"));
    }
  }
}

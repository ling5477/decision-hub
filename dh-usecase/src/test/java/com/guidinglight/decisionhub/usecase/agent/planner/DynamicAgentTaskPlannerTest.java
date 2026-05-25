package com.guidinglight.decisionhub.usecase.agent.planner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.agent.AgentRole;
import com.guidinglight.decisionhub.domain.agent.AgentTask;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.usecase.agent.planner.impl.DefaultPlannerStrategyResolver;
import com.guidinglight.decisionhub.usecase.agent.planner.strategy.BearFocusedPlannerStrategyHandler;
import com.guidinglight.decisionhub.usecase.agent.planner.strategy.BullFocusedPlannerStrategyHandler;
import com.guidinglight.decisionhub.usecase.agent.planner.strategy.DefaultPlannerStrategyHandler;
import com.guidinglight.decisionhub.usecase.agent.planner.strategy.VolatileDiversifiedPlannerStrategyHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Stage2-PoC-B4：DynamicAgentTaskPlanner 单元测试。
 *
 * <p>覆盖：DEFAULT regime 走 Stage1 行为；bull / bear / volatile regime 切换到对应 handler；
 * 显式 plannerStrategy 覆盖 regime；缺失 strategy 注册时 fallback 到 DEFAULT。
 *
 * <p>断言均建立在节点组合特征上，不依赖任何 LLM 或外部服务。
 */
class DynamicAgentTaskPlannerTest {

  private final PlannerStrategyRegistry registry =
      new PlannerStrategyRegistry(
          List.of(
              new DefaultPlannerStrategyHandler(),
              new BullFocusedPlannerStrategyHandler(),
              new BearFocusedPlannerStrategyHandler(),
              new VolatileDiversifiedPlannerStrategyHandler()));
  private final DynamicAgentTaskPlanner planner =
      new DynamicAgentTaskPlanner(new DefaultPlannerStrategyResolver(), registry);

  @Test
  void default_regime_uses_default_handler() {
    final AgentTask task = planner.plan(newRun(Map.of()));
    assertEquals(PlannerStrategy.DEFAULT.name(), task.getPayloadJson().get("plannerStrategy"));
    assertEquals(6, task.getNodes().size());
    assertEquals(1, countByRole(task, AgentRole.STRATEGY));
    assertEquals(1, countByRole(task, AgentRole.RISK_REVIEWER));
    assertEquals(1, countByRole(task, AgentRole.STRATEGY_REVIEWER));
  }

  @Test
  void bull_regime_uses_bull_focused_handler() {
    final AgentTask task = planner.plan(newRun(Map.of("marketRegime", "bullish")));
    assertEquals(
        PlannerStrategy.BULL_FOCUSED.name(), task.getPayloadJson().get("plannerStrategy"));
    assertEquals(2, countByRole(task, AgentRole.STRATEGY));
    assertEquals(1, countByRole(task, AgentRole.STRATEGY_REVIEWER));
    assertEquals(0, countByRole(task, AgentRole.RISK_REVIEWER));
    assertEquals(1, countByRole(task, AgentRole.JUDGE));
  }

  @Test
  void bear_regime_uses_bear_focused_handler() {
    final AgentTask task = planner.plan(newRun(Map.of("marketRegime", "bear")));
    assertEquals(
        PlannerStrategy.BEAR_FOCUSED.name(), task.getPayloadJson().get("plannerStrategy"));
    assertEquals(2, countByRole(task, AgentRole.RISK_REVIEWER));
    assertEquals(1, countByRole(task, AgentRole.STRATEGY));
    assertEquals(0, countByRole(task, AgentRole.STRATEGY_REVIEWER));
    assertEquals(1, countByRole(task, AgentRole.JUDGE));
  }

  @Test
  void volatile_regime_uses_volatile_diversified_handler() {
    final AgentTask task = planner.plan(newRun(Map.of("marketRegime", "high_volatility")));
    assertEquals(
        PlannerStrategy.VOLATILE_DIVERSIFIED.name(),
        task.getPayloadJson().get("plannerStrategy"));
    assertEquals(1, countByRole(task, AgentRole.LEADER));
    assertEquals(1, countByRole(task, AgentRole.SCOUT));
    assertEquals(1, countByRole(task, AgentRole.ANALYST));
    assertEquals(1, countByRole(task, AgentRole.STRATEGY));
    assertEquals(1, countByRole(task, AgentRole.RISK_REVIEWER));
    assertEquals(1, countByRole(task, AgentRole.STRATEGY_REVIEWER));
    assertEquals(1, countByRole(task, AgentRole.JUDGE));
  }

  @Test
  void explicit_planner_strategy_overrides_regime() {
    final Map<String, Object> payload = new HashMap<>();
    payload.put("marketRegime", "bullish");
    payload.put("plannerStrategy", "BEAR_FOCUSED");
    final AgentTask task = planner.plan(newRun(payload));
    assertEquals(
        PlannerStrategy.BEAR_FOCUSED.name(), task.getPayloadJson().get("plannerStrategy"));
    assertEquals(2, countByRole(task, AgentRole.RISK_REVIEWER));
  }

  @Test
  void missing_strategy_in_registry_falls_back_to_default_handler() {
    final PlannerStrategyRegistry partial =
        new PlannerStrategyRegistry(List.of(new DefaultPlannerStrategyHandler()));
    final DynamicAgentTaskPlanner partialPlanner =
        new DynamicAgentTaskPlanner(new DefaultPlannerStrategyResolver(), partial);

    final AgentTask task = partialPlanner.plan(newRun(Map.of("marketRegime", "bullish")));
    // resolver 说 BULL_FOCUSED，但 registry 里只有 DEFAULT；最终必须走 DEFAULT 链路。
    assertEquals(PlannerStrategy.DEFAULT.name(), task.getPayloadJson().get("plannerStrategy"));
    assertEquals(6, task.getNodes().size());
  }

  @Test
  void judge_node_is_present_in_every_strategy() {
    // 验证 JudgeDecision 仍是唯一最终出口：任一策略图都必须包含 JUDGE 终点。
    final List<Map<String, Object>> payloads =
        List.of(
            Map.of(),
            Map.of("marketRegime", "bullish"),
            Map.of("marketRegime", "bear"),
            Map.of("marketRegime", "high_volatility"));
    for (Map<String, Object> payload : payloads) {
      final AgentTask task = planner.plan(newRun(payload));
      assertNotNull(task);
      assertTrue(
          task.getNodes().stream().anyMatch(n -> n.getRole() == AgentRole.JUDGE),
          () -> "JUDGE node missing for payload " + payload);
    }
  }

  private static long countByRole(final AgentTask task, final AgentRole role) {
    return task.getNodes().stream().filter(n -> n.getRole() == role).count();
  }

  private static ResearchRun newRun(final Map<String, Object> payload) {
    return ResearchRun.create("t-test", "topic-dynamic-planner", payload, TimeProvider.now());
  }
}

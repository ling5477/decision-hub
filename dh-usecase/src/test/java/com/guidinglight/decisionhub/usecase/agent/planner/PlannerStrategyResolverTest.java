package com.guidinglight.decisionhub.usecase.agent.planner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.usecase.agent.planner.impl.DefaultPlannerStrategyResolver;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Stage2-PoC-B4：PlannerStrategyResolver 单元测试。
 *
 * <p>覆盖：DEFAULT 兜底、bull/bear/volatile 关键字识别、显式 plannerStrategy 覆盖 regime。 不允许依赖 LLM / TradingAgents
 * Python 代码 / graph scheduler / 真实下单。
 */
class PlannerStrategyResolverTest {

  private final DefaultPlannerStrategyResolver resolver = new DefaultPlannerStrategyResolver();

  @Test
  void null_run_falls_back_to_default() {
    assertEquals(PlannerStrategy.DEFAULT, resolver.resolve(null));
  }

  @Test
  void empty_payload_falls_back_to_default() {
    final ResearchRun run = newRun(Map.of());
    assertEquals(PlannerStrategy.DEFAULT, resolver.resolve(run));
  }

  @Test
  void unknown_regime_falls_back_to_default() {
    final ResearchRun run = newRun(Map.of("marketRegime", "neutral-sideways"));
    assertEquals(PlannerStrategy.DEFAULT, resolver.resolve(run));
  }

  @Test
  void bullish_keyword_maps_to_bull_focused() {
    assertEquals(
        PlannerStrategy.BULL_FOCUSED,
        resolver.resolve(newRun(Map.of("marketRegime", "bullish"))));
    assertEquals(
        PlannerStrategy.BULL_FOCUSED,
        resolver.resolve(newRun(Map.of("marketRegime", "BULL"))));
    assertEquals(
        PlannerStrategy.BULL_FOCUSED,
        resolver.resolve(newRun(Map.of("marketRegime", "mid-cap bull cycle"))));
  }

  @Test
  void bearish_keyword_maps_to_bear_focused() {
    assertEquals(
        PlannerStrategy.BEAR_FOCUSED,
        resolver.resolve(newRun(Map.of("marketRegime", "bearish"))));
    assertEquals(
        PlannerStrategy.BEAR_FOCUSED,
        resolver.resolve(newRun(Map.of("marketRegime", "Bear market"))));
  }

  @Test
  void volatile_keyword_maps_to_volatile_diversified() {
    assertEquals(
        PlannerStrategy.VOLATILE_DIVERSIFIED,
        resolver.resolve(newRun(Map.of("marketRegime", "volatile"))));
    assertEquals(
        PlannerStrategy.VOLATILE_DIVERSIFIED,
        resolver.resolve(newRun(Map.of("marketRegime", "high_volatility window"))));
  }

  @Test
  void volatile_is_evaluated_before_bull_or_bear() {
    // "bullish but volatile" 应该走 VOLATILE，因为 volatile 是更具体的 regime 信号。
    final ResearchRun run = newRun(Map.of("marketRegime", "bullish but volatile"));
    assertEquals(PlannerStrategy.VOLATILE_DIVERSIFIED, resolver.resolve(run));
  }

  @Test
  void explicit_planner_strategy_overrides_regime() {
    final Map<String, Object> payload = new HashMap<>();
    payload.put("marketRegime", "bullish");
    payload.put("plannerStrategy", "BEAR_FOCUSED");
    assertEquals(PlannerStrategy.BEAR_FOCUSED, resolver.resolve(newRun(payload)));
  }

  @Test
  void invalid_explicit_strategy_falls_back_to_regime() {
    final Map<String, Object> payload = new HashMap<>();
    payload.put("marketRegime", "bearish");
    payload.put("plannerStrategy", "NOT_A_REAL_STRATEGY");
    assertEquals(PlannerStrategy.BEAR_FOCUSED, resolver.resolve(newRun(payload)));
  }

  private static ResearchRun newRun(final Map<String, Object> payload) {
    return ResearchRun.create("t-test", "topic-resolver", payload, TimeProvider.now());
  }
}

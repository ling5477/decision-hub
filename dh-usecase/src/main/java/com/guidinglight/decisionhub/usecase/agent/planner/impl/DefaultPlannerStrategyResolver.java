package com.guidinglight.decisionhub.usecase.agent.planner.impl;

import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.usecase.agent.planner.PlannerStrategy;
import com.guidinglight.decisionhub.usecase.agent.planner.PlannerStrategyResolver;
import java.util.Locale;
import java.util.Map;

/**
 * Stage2-PoC-B4：基于 marketRegime 字符串关键词匹配的默认 resolver。
 *
 * <p>规则按优先级：
 *
 * <ol>
 *   <li>payloadJson 显式指定 plannerStrategy（枚举名一致）-&gt; 直接使用。
 *   <li>payloadJson.marketRegime 包含 bullish / bull -&gt; BULL_FOCUSED。
 *   <li>payloadJson.marketRegime 包含 bearish / bear -&gt; BEAR_FOCUSED。
 *   <li>payloadJson.marketRegime 包含 high_volatility / volatile -&gt; VOLATILE_DIVERSIFIED。
 *   <li>其余 / 未提供 / null -&gt; DEFAULT。
 * </ol>
 *
 * <p>不调用 LLM；不发起 HTTP；不引入任何 TradingAgents Python 代码。
 */
public final class DefaultPlannerStrategyResolver implements PlannerStrategyResolver {

  /** payloadJson 中 marketRegime 字段的标准键。 */
  public static final String FIELD_MARKET_REGIME = "marketRegime";

  /** payloadJson 中显式 plannerStrategy 字段的标准键。 */
  public static final String FIELD_PLANNER_STRATEGY = "plannerStrategy";

  /** 默认构造。 */
  public DefaultPlannerStrategyResolver() {
    // no-op
  }

  @Override
  public PlannerStrategy resolve(final ResearchRun run) {
    if (run == null) {
      return PlannerStrategy.DEFAULT;
    }
    final Map<String, Object> payload = run.getPayloadJson();
    if (payload == null || payload.isEmpty()) {
      return PlannerStrategy.DEFAULT;
    }

    final PlannerStrategy explicit = parseExplicit(payload.get(FIELD_PLANNER_STRATEGY));
    if (explicit != null) {
      return explicit;
    }

    final Object regimeValue = payload.get(FIELD_MARKET_REGIME);
    if (!(regimeValue instanceof String)) {
      return PlannerStrategy.DEFAULT;
    }
    final String regime = ((String) regimeValue).toLowerCase(Locale.ROOT);
    if (regime.isBlank()) {
      return PlannerStrategy.DEFAULT;
    }

    // 顺序很重要：先匹配更窄的关键词，再退到宽匹配；high_volatility 必须在 bull/bear 之前。
    if (regime.contains("high_volatility") || regime.contains("volatile")) {
      return PlannerStrategy.VOLATILE_DIVERSIFIED;
    }
    if (regime.contains("bullish") || regime.contains("bull")) {
      return PlannerStrategy.BULL_FOCUSED;
    }
    if (regime.contains("bearish") || regime.contains("bear")) {
      return PlannerStrategy.BEAR_FOCUSED;
    }
    return PlannerStrategy.DEFAULT;
  }

  private static PlannerStrategy parseExplicit(final Object raw) {
    if (!(raw instanceof String)) {
      return null;
    }
    final String name = ((String) raw).trim();
    if (name.isEmpty()) {
      return null;
    }
    try {
      return PlannerStrategy.valueOf(name.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}

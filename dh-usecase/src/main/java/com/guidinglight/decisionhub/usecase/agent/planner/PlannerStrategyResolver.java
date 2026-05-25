package com.guidinglight.decisionhub.usecase.agent.planner;

import com.guidinglight.decisionhub.domain.research.ResearchRun;

/**
 * Stage2-PoC-B4：从 ResearchRun 解析 PlannerStrategy。
 *
 * <p>实现必须满足：
 *
 * <ul>
 *   <li>未识别 / 中性 regime 一律回退到 {@link PlannerStrategy#DEFAULT}。
 *   <li>marketRegime 包含 bull / bullish -&gt; {@link PlannerStrategy#BULL_FOCUSED}。
 *   <li>marketRegime 包含 bear / bearish -&gt; {@link PlannerStrategy#BEAR_FOCUSED}。
 *   <li>marketRegime 包含 volatile / high_volatility -&gt; {@link
 *       PlannerStrategy#VOLATILE_DIVERSIFIED}。
 *   <li>不允许依赖 LLM；不允许调用外部 HTTP；不允许引入 TradingAgents Python 代码。
 * </ul>
 */
public interface PlannerStrategyResolver {

  /**
   * 为指定 run 决定 PlannerStrategy。
   *
   * @param run 已 transition 到 PLANNING 状态的 run。
   * @return 解析得到的 strategy，永不返回 null。
   */
  PlannerStrategy resolve(ResearchRun run);
}

package com.guidinglight.decisionhub.usecase.agent.planner;

/**
 * Stage2-PoC-B4：planner 选边的轻量策略枚举。
 *
 * <p>放在 usecase 层是因为本批次显式禁止修改 dh-domain；DH 仍然只用枚举做"选边"，不引入任何 LLM
 * 或重型 agent graph runtime，也不复制 TradingAgents 的任何 Python 代码。
 *
 * <ul>
 *   <li>{@link #DEFAULT} - 保持 Stage1 已经验证过的 SCOUT -> ANALYST -> STRATEGY ->
 *       RISK_REVIEWER + STRATEGY_REVIEWER -> JUDGE 顺序，是所有未知/中性 regime 的兜底。
 *   <li>{@link #BULL_FOCUSED} - bull/bullish regime 下进攻型：多生成候选 + 一道复核。
 *   <li>{@link #BEAR_FOCUSED} - bear/bearish regime 下防守型：多道风险复核。
 *   <li>{@link #VOLATILE_DIVERSIFIED} - volatile/high_volatility regime 下多样化：每种角色各一次。
 * </ul>
 */
public enum PlannerStrategy {
  /** Stage1 默认链路；任何未知 / 中性 regime 都必须回退到这里。 */
  DEFAULT,
  /** Bull regime 进攻型：双 STRATEGY + 单 STRATEGY_REVIEWER。 */
  BULL_FOCUSED,
  /** Bear regime 防守型：双 RISK_REVIEWER + 单 STRATEGY。 */
  BEAR_FOCUSED,
  /** Volatile regime 多样化：每种 AgentRole 各 1。 */
  VOLATILE_DIVERSIFIED
}

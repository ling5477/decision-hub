package com.guidinglight.decisionhub.domain.agent;

/**
 * Stage1 狼群角色化任务编排中的 Agent 身份。
 * 所有最终输出必须经过 JUDGE 仲裁，不允许单 Agent 直接给最终结论。
 */
public enum AgentRole {
  /** 任务规划者。 */
  LEADER,
  /** 信息探子，负责数据/信号探索。 */
  SCOUT,
  /** 证据分析与解释。 */
  ANALYST,
  /** 候选策略生成。 */
  STRATEGY,
  /** 风险审查。 */
  RISK_REVIEWER,
  /** 策略审查。 */
  STRATEGY_REVIEWER,
  /** 最终仲裁与结构化输出。 */
  JUDGE
}

package com.guidinglight.decisionhub.domain.reflection;

/**
 * Stage2-PoC-B1：reflection 类型。
 *
 * <p>轻量吸收 TradingAgents 风格，但不复制其状态机。 反映"step 级"、"agent 级"、"run 总结"三个粒度。
 */
public enum ReflectionType {
  /** 单个 task step 完成后的回顾。 */
  STEP_REFLECTION,
  /** 单个 agent 多 step 后的横向回顾。 */
  AGENT_REFLECTION,
  /** 整个 ResearchRun 结束后的总结。 */
  RUN_RETROSPECTIVE
}

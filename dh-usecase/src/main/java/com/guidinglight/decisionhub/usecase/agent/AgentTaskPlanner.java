package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.agent.AgentTask;
import com.guidinglight.decisionhub.domain.research.ResearchRun;

/**
 * Stage1：Leader 角色的任务规划用例。
 *
 * <p>对应工单 4.2：AgentTaskPlanner。Stage1 用固定模板生成节点序列：
 * SCOUT -&gt; ANALYST -&gt; STRATEGY -&gt; RISK_REVIEWER -&gt; STRATEGY_REVIEWER -&gt; JUDGE。
 */
public interface AgentTaskPlanner {

  /**
   * 为指定 run 规划任务图。
   *
   * @param run 已 transition 到 PLANNING 状态的 run。
   * @return 规划好的任务图。
   */
  AgentTask plan(ResearchRun run);
}

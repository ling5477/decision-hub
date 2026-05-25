package com.guidinglight.decisionhub.domain.run;

/**
 * @deprecated Stage1-CLOSE：旧 Run 步骤枚举；新链路通过
 *     {@link com.guidinglight.decisionhub.domain.agent.AgentRole} + TaskNode 表达节点种类。
 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public enum StepType {
  GATHER_EVIDENCE,
  DISCUSS,
  GATE_EVAL,
  DECIDE,
  SUMMARIZE
}

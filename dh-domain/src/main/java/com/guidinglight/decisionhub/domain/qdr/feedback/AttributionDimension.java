package com.guidinglight.decisionhub.domain.qdr.feedback;

/** 可解释归因使用的封闭维度集合，枚举顺序同时定义稳定输出顺序。 */
public enum AttributionDimension {
  /** 输入证据本身的质量。 */
  EVIDENCE_QUALITY,
  /** 决策推理与观察结果的一致程度。 */
  DECISION_CONSISTENCY,
  /** 风险约束是否得到遵守。 */
  RISK_DISCIPLINE,
  /** 观察结果的稳定性。 */
  OUTCOME_STABILITY
}

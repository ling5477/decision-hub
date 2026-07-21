package com.guidinglight.decisionhub.domain.qdr.feedback;

/** 单个维度贡献的方向，不表示任何交易动作。 */
public enum AttributionImpact {
  /** 正向贡献。 */
  POSITIVE,
  /** 中性贡献。 */
  NEUTRAL,
  /** 负向贡献。 */
  NEGATIVE
}

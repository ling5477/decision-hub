package com.guidinglight.decisionhub.domain.qdr.feedback;

/**
 * 有界归因置信度。
 *
 * @param value 确定性计算得到的 {@code [0,1]} 数值
 */
public record FeedbackConfidence(double value) {

  /** 拒绝 NaN、Infinity 与边界外数值。 */
  public FeedbackConfidence {
    if (!Double.isFinite(value) || value < 0.0d || value > 1.0d) {
      throw new IllegalArgumentException("confidence must be finite and within [0,1]");
    }
    value = value == 0.0d ? 0.0d : value;
  }

  /** 创建零置信度。 */
  public static FeedbackConfidence zero() {
    return new FeedbackConfidence(0.0d);
  }
}

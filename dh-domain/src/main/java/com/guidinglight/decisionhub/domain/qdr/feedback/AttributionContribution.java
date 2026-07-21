package com.guidinglight.decisionhub.domain.qdr.feedback;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 单个归因维度的可解释贡献。
 *
 * @param dimension 归因维度
 * @param contribution 有界贡献值 {@code [-1,1]}
 * @param impact 贡献方向
 * @param confidence 有界置信度
 * @param reasonCode 稳定原因码
 * @param evidenceReference 不含原始 payload 的安全证据引用
 */
public record AttributionContribution(
    AttributionDimension dimension,
    BigDecimal contribution,
    AttributionImpact impact,
    FeedbackConfidence confidence,
    String reasonCode,
    String evidenceReference)
    implements Comparable<AttributionContribution> {

  /** 构造期校验贡献方向、置信度、原因码与安全引用。 */
  public AttributionContribution {
    dimension = Objects.requireNonNull(dimension, "dimension");
    contribution =
        FeedbackContractGuard.requireUnitRange(contribution, "contribution");
    impact = Objects.requireNonNull(impact, "impact");
    confidence = Objects.requireNonNull(confidence, "confidence");
    reasonCode = FeedbackContractGuard.requireReasonCode(reasonCode, "reasonCode");
    evidenceReference =
        FeedbackContractGuard.requireSafeText(
            evidenceReference,
            "evidenceReference",
            FeedbackContractGuard.MAX_SAFE_TEXT_LENGTH);
    final AttributionImpact expected =
        contribution.signum() > 0
            ? AttributionImpact.POSITIVE
            : contribution.signum() < 0
                ? AttributionImpact.NEGATIVE
                : AttributionImpact.NEUTRAL;
    if (impact != expected) {
      throw new IllegalArgumentException("impact must match contribution sign");
    }
  }

  /** 按封闭维度枚举顺序提供稳定排序。 */
  @Override
  public int compareTo(final AttributionContribution other) {
    return dimension.compareTo(Objects.requireNonNull(other, "other").dimension);
  }
}

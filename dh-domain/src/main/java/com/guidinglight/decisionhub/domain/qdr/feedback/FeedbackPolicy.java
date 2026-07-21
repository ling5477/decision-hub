package com.guidinglight.decisionhub.domain.qdr.feedback;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 确定性归因策略身份与边界。
 *
 * <p>{@code evaluationTime} 是显式输入，禁止 use case 隐式读取系统时间。策略集合按 enum 顺序稳定化。
 *
 * @param policyId 策略身份
 * @param policyVersion 策略版本
 * @param evaluationTime 显式评估时间
 * @param maxObservationAge 最大观察时延
 * @param allowedSources 允许来源
 * @param requiredDimensions 必需维度
 * @param dimensionWeights 维度权重，范围 {@code [0,1]}
 */
public record FeedbackPolicy(
    String policyId,
    String policyVersion,
    Instant evaluationTime,
    Duration maxObservationAge,
    Set<OutcomeSource> allowedSources,
    Set<AttributionDimension> requiredDimensions,
    Map<AttributionDimension, BigDecimal> dimensionWeights) {

  private static final Duration MAX_OBSERVATION_AGE = Duration.ofDays(365);

  /** 构造期冻结策略集合并拒绝空策略、未知值或无界观察窗口。 */
  public FeedbackPolicy {
    policyId = FeedbackContractGuard.requireId(policyId, "policyId");
    policyVersion = FeedbackContractGuard.requireId(policyVersion, "policyVersion");
    evaluationTime = Objects.requireNonNull(evaluationTime, "evaluationTime");
    maxObservationAge = Objects.requireNonNull(maxObservationAge, "maxObservationAge");
    if (maxObservationAge.isZero()
        || maxObservationAge.isNegative()
        || maxObservationAge.compareTo(MAX_OBSERVATION_AGE) > 0) {
      throw new IllegalArgumentException("maxObservationAge must be within (0,365d]");
    }
    allowedSources = immutableSources(allowedSources);
    requiredDimensions = immutableDimensions(requiredDimensions);
    dimensionWeights = immutableWeights(dimensionWeights);
    if (!dimensionWeights.keySet().containsAll(requiredDimensions)) {
      throw new IllegalArgumentException("dimensionWeights must cover requiredDimensions");
    }
  }

  private static Set<OutcomeSource> immutableSources(final Set<OutcomeSource> source) {
    Objects.requireNonNull(source, "allowedSources");
    if (source.isEmpty()) {
      throw new IllegalArgumentException("allowedSources must not be empty");
    }
    final EnumSet<OutcomeSource> copy = EnumSet.copyOf(source);
    return Collections.unmodifiableSet(copy);
  }

  private static Set<AttributionDimension> immutableDimensions(
      final Set<AttributionDimension> source) {
    Objects.requireNonNull(source, "requiredDimensions");
    if (source.isEmpty()) {
      throw new IllegalArgumentException("requiredDimensions must not be empty");
    }
    final EnumSet<AttributionDimension> copy = EnumSet.copyOf(source);
    return Collections.unmodifiableSet(copy);
  }

  private static Map<AttributionDimension, BigDecimal> immutableWeights(
      final Map<AttributionDimension, BigDecimal> source) {
    Objects.requireNonNull(source, "dimensionWeights");
    if (source.isEmpty()) {
      throw new IllegalArgumentException("dimensionWeights must not be empty");
    }
    final EnumMap<AttributionDimension, BigDecimal> copy =
        new EnumMap<>(AttributionDimension.class);
    source.forEach(
        (dimension, value) -> {
          final BigDecimal checked =
              FeedbackContractGuard.requireUnitRange(value, "dimensionWeight");
          if (checked.signum() < 0) {
            throw new IllegalArgumentException("dimensionWeight must be within [0,1]");
          }
          copy.put(Objects.requireNonNull(dimension, "weight dimension"), checked);
        });
    return Collections.unmodifiableMap(copy);
  }
}

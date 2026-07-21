package com.guidinglight.decisionhub.domain.qdr.feedback;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 已验证结构化结果观察。
 *
 * <p>该对象只承载有界指标、安全引用和安全 metadata，不保存原始 payload；时间有效性和与 subject 的 scope
 * 关联由 use case 在 canonicalization 前 fail-closed 校验。
 *
 * @param tenantId 观察所属租户
 * @param environment 观察所属环境
 * @param decisionId 已存在的决策标识
 * @param traceId 已存在的追踪标识
 * @param observationId 租户和环境内唯一观察标识
 * @param source 封闭结果来源
 * @param outcome 结构化结果状态
 * @param observedAt 显式 UTC instant
 * @param measurements 按维度表达的 {@code [-1,1]} 指标
 * @param evidenceReferences 各维度的安全证据引用
 * @param safeMetadata 不参与执行的有界安全 metadata
 */
public record OutcomeObservation(
    String tenantId,
    FeedbackEnvironment environment,
    String decisionId,
    String traceId,
    String observationId,
    OutcomeSource source,
    ObservedDecisionOutcome outcome,
    Instant observedAt,
    Map<AttributionDimension, BigDecimal> measurements,
    Map<AttributionDimension, String> evidenceReferences,
    Map<String, String> safeMetadata) {

  /** 构造时完成空值、边界、稳定顺序与 defensive-copy 校验。 */
  public OutcomeObservation {
    tenantId = FeedbackContractGuard.requireId(tenantId, "tenantId");
    environment = Objects.requireNonNull(environment, "environment");
    decisionId = FeedbackContractGuard.requireId(decisionId, "decisionId");
    traceId = FeedbackContractGuard.requireId(traceId, "traceId");
    observationId = FeedbackContractGuard.requireId(observationId, "observationId");
    source = Objects.requireNonNull(source, "source");
    outcome = Objects.requireNonNull(outcome, "outcome");
    observedAt = Objects.requireNonNull(observedAt, "observedAt");
    measurements = immutableMeasurements(measurements);
    evidenceReferences = immutableEvidenceReferences(evidenceReferences);
    safeMetadata = immutableMetadata(safeMetadata);
  }

  private static Map<AttributionDimension, BigDecimal> immutableMeasurements(
      final Map<AttributionDimension, BigDecimal> source) {
    Objects.requireNonNull(source, "measurements");
    if (source.isEmpty() || source.size() > FeedbackContractGuard.MAX_COLLECTION_SIZE) {
      throw new IllegalArgumentException("measurements size must be within [1,32]");
    }
    final EnumMap<AttributionDimension, BigDecimal> copy =
        new EnumMap<>(AttributionDimension.class);
    source.forEach(
        (dimension, value) ->
            copy.put(
                Objects.requireNonNull(dimension, "measurement dimension"),
                FeedbackContractGuard.requireUnitRange(value, "measurement")));
    return Collections.unmodifiableMap(copy);
  }

  private static Map<AttributionDimension, String> immutableEvidenceReferences(
      final Map<AttributionDimension, String> source) {
    Objects.requireNonNull(source, "evidenceReferences");
    if (source.size() > FeedbackContractGuard.MAX_COLLECTION_SIZE) {
      throw new IllegalArgumentException("evidenceReferences exceeds max size 32");
    }
    final EnumMap<AttributionDimension, String> copy =
        new EnumMap<>(AttributionDimension.class);
    source.forEach(
        (dimension, value) ->
            copy.put(
                Objects.requireNonNull(dimension, "evidence dimension"),
                FeedbackContractGuard.requireSafeText(
                    value,
                    "evidenceReference",
                    FeedbackContractGuard.MAX_SAFE_TEXT_LENGTH)));
    return Collections.unmodifiableMap(copy);
  }

  private static Map<String, String> immutableMetadata(final Map<String, String> source) {
    Objects.requireNonNull(source, "safeMetadata");
    if (source.size() > FeedbackContractGuard.MAX_COLLECTION_SIZE) {
      throw new IllegalArgumentException("safeMetadata exceeds max size 32");
    }
    final Map<String, String> copy = new TreeMap<>();
    source.forEach(
        (key, value) ->
            copy.put(
                FeedbackContractGuard.requireSafeText(key, "metadata key", 64),
                FeedbackContractGuard.requireSafeText(
                    value, "metadata value", FeedbackContractGuard.MAX_SAFE_TEXT_LENGTH)));
    return Collections.unmodifiableMap(copy);
  }
}

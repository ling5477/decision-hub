package com.guidinglight.decisionhub.usecase.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionContribution;
import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionDimension;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

/**
 * Stage-QDR-8 的 deterministic canonicalizer 与 domain-separated SHA-256 计算器。
 *
 * <p>实现不读取系统时间、不使用随机数；所有影响归因的 subject、observation 与 policy 字段都进入 canonical value。
 */
public final class FeedbackCanonicalizer {

  private static final String INPUT_DOMAIN = "DH-QDR8-FEEDBACK-ATTRIBUTION-INPUT-V1";
  private static final String KEY_DOMAIN = "DH-QDR8-FEEDBACK-ATTRIBUTION-V1";
  private static final String RESULT_DOMAIN = "DH-QDR8-FEEDBACK-ATTRIBUTION-RESULT-V1";

  /** 生成完整规范输入、canonical hash 与冻结幂等 key。 */
  public FeedbackCanonicalHash canonicalize(final FeedbackAttributionCommand command) {
    final FeedbackAttributionCommand checked = Objects.requireNonNull(command, "command");
    final StringBuilder canonical = new StringBuilder();
    append(canonical, "tenantId", checked.subject().tenantId());
    append(canonical, "environment", checked.subject().environment().name());
    append(canonical, "decisionId", checked.subject().decisionId());
    append(canonical, "traceId", checked.subject().traceId());
    append(canonical, "observation.tenantId", checked.observation().tenantId());
    append(canonical, "observation.environment", checked.observation().environment().name());
    append(canonical, "observation.decisionId", checked.observation().decisionId());
    append(canonical, "observation.traceId", checked.observation().traceId());
    append(canonical, "observation.id", checked.observation().observationId());
    append(canonical, "observation.source", checked.observation().source().name());
    append(canonical, "observation.outcome", checked.observation().outcome().name());
    append(canonical, "observation.observedAt", checked.observation().observedAt().toString());
    appendMeasurements(canonical, checked.observation().measurements());
    appendDimensionStrings(canonical, "evidence", checked.observation().evidenceReferences());
    checked.observation().safeMetadata().forEach((key, value) -> append(canonical, "metadata." + key, value));
    append(canonical, "policy.id", checked.policy().policyId());
    append(canonical, "policy.version", checked.policy().policyVersion());
    append(canonical, "policy.evaluationTime", checked.policy().evaluationTime().toString());
    append(canonical, "policy.maxObservationAge", checked.policy().maxObservationAge().toString());
    for (OutcomeSource source : checked.policy().allowedSources()) {
      append(canonical, "policy.allowedSource", source.name());
    }
    for (AttributionDimension dimension : checked.policy().requiredDimensions()) {
      append(canonical, "policy.requiredDimension", dimension.name());
    }
    appendMeasurements(canonical, checked.policy().dimensionWeights());

    final String canonicalValue = canonical.toString();
    final String inputHash = digest(INPUT_DOMAIN, canonicalValue);
    final StringBuilder key = new StringBuilder();
    append(key, "tenantId", checked.subject().tenantId());
    append(key, "environment", checked.subject().environment().name());
    append(key, "decisionId", checked.subject().decisionId());
    append(key, "observationId", checked.observation().observationId());
    return new FeedbackCanonicalHash(canonicalValue, inputHash, digest(KEY_DOMAIN, key.toString()));
  }

  /** 根据 canonical hash 与确定性 evaluation 生成稳定结果身份。 */
  public String resultIdentity(
      final FeedbackCanonicalHash canonicalHash,
      final FeedbackAttributionPolicyEvaluator.Evaluation evaluation) {
    final StringBuilder value = new StringBuilder();
    append(value, "canonicalHash", Objects.requireNonNull(canonicalHash, "canonicalHash").value());
    append(value, "status", Objects.requireNonNull(evaluation, "evaluation").status().name());
    append(value, "errorCode", evaluation.errorCode().name());
    append(value, "confidence", Double.toString(evaluation.confidence().value()));
    for (AttributionContribution contribution : evaluation.contributions()) {
      append(value, "dimension", contribution.dimension().name());
      append(value, "contribution", decimal(contribution.contribution()));
      append(value, "impact", contribution.impact().name());
      append(value, "dimensionConfidence", Double.toString(contribution.confidence().value()));
      append(value, "reasonCode", contribution.reasonCode());
      append(value, "evidenceReference", contribution.evidenceReference());
    }
    return digest(RESULT_DOMAIN, value.toString());
  }

  private static void appendMeasurements(
      final StringBuilder target, final Map<AttributionDimension, BigDecimal> values) {
    values.forEach(
        (dimension, value) -> append(target, "dimension." + dimension.name(), decimal(value)));
  }

  private static void appendDimensionStrings(
      final StringBuilder target,
      final String prefix,
      final Map<AttributionDimension, String> values) {
    values.forEach((dimension, value) -> append(target, prefix + "." + dimension.name(), value));
  }

  private static String decimal(final BigDecimal value) {
    final BigDecimal normalized = value.stripTrailingZeros();
    return normalized.signum() == 0 ? "0" : normalized.toPlainString();
  }

  private static void append(
      final StringBuilder target, final String field, final String value) {
    target
        .append(field.length())
        .append(':')
        .append(field)
        .append('=')
        .append(value.length())
        .append(':')
        .append(value)
        .append(';');
  }

  private static String digest(final String domain, final String value) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(domain.getBytes(StandardCharsets.UTF_8));
      digest.update((byte) 0);
      digest.update(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 unavailable", error);
    }
  }
}

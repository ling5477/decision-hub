package com.guidinglight.decisionhub.usecase.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionContribution;
import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionDimension;
import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionImpact;
import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionResult;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackAuditReference;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackStatus;
import com.guidinglight.decisionhub.domain.qdr.feedback.ObservedDecisionOutcome;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeObservation;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Stage-QDR-9 persistence port 使用的不可变结构化 records。
 *
 * <p>records 从 Stage-QDR-8 command、canonical hash 与已完成 audit 的 result 映射，不复制 attribution
 * 计算模型，不保存 canonical value、safeMetadata、raw prompt、Provider response、credential 或完整 payload。
 */
public final class FeedbackPersistenceRecords {

  private static final int MAX_ID_LENGTH = 128;
  private static final int MAX_REFERENCE_LENGTH = 256;
  private static final int MAX_CONTRIBUTIONS = 32;
  private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
  private static final Pattern REASON_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");
  private static final Pattern SENSITIVE_OR_EXECUTABLE =
      Pattern.compile(
          "(?i)(authorization|api[-_]?key|api[-_]?secret|password|private[-_]?key|"
              + "mnemonic|cookie|credential|raw[-_]?prompt|raw[-_]?provider|"
              + "place[-_]?order|cancel[-_]?order|market[-_]?order|\\bbuy\\b|\\bsell\\b)");

  private FeedbackPersistenceRecords() {}

  /**
   * 将 Stage-QDR-8 已审计结果映射为 persistence aggregate。
   *
   * <p>所有 UUID 由调用方显式生成；本方法不读取时钟、不生成随机值、不执行 IO。
   */
  public static FeedbackPersistenceAggregate from(
      final FeedbackAttributionCommand command,
      final FeedbackCanonicalHash canonicalHash,
      final FeedbackAttributionResult result,
      final PersistenceRowIdentities identities) {
    final FeedbackAttributionCommand checkedCommand =
        Objects.requireNonNull(command, "command");
    final FeedbackCanonicalHash checkedHash =
        Objects.requireNonNull(canonicalHash, "canonicalHash");
    final FeedbackAttributionResult checkedResult =
        Objects.requireNonNull(result, "result");
    final AttributionResult domainResult =
        Objects.requireNonNull(
            checkedResult.attributionResult(),
            "only completed audited attribution results can be persisted");
    final PersistenceRowIdentities checkedIdentities =
        Objects.requireNonNull(identities, "identities");
    validateSourceCorrelation(checkedCommand, checkedHash, domainResult);

    final OutcomeObservation observation = checkedCommand.observation();
    final OutcomeObservationRecord observationRecord =
        new OutcomeObservationRecord(
            checkedIdentities.observationRowId(),
            checkedCommand.subject().tenantId(),
            checkedCommand.subject().environment(),
            checkedCommand.subject().decisionId(),
            checkedCommand.subject().traceId(),
            observation.observationId(),
            checkedHash.idempotencyKey(),
            observation.source(),
            observation.outcome(),
            observation.observedAt(),
            checkedCommand.policy().evaluationTime(),
            checkedHash.value());
    final FeedbackAttributionRecord attributionRecord =
        new FeedbackAttributionRecord(
            checkedIdentities.attributionRowId(),
            domainResult.subject().tenantId(),
            domainResult.subject().environment(),
            domainResult.observationId(),
            domainResult.subject().decisionId(),
            domainResult.subject().traceId(),
            observation.observedAt(),
            domainResult.auditReference().resultIdentity(),
            domainResult.policyId(),
            domainResult.policyVersion(),
            domainResult.status(),
            BigDecimal.valueOf(domainResult.confidence().value()),
            domainResult.canonicalHash(),
            checkedResult.errorCode());
    final List<AttributionContributionRecord> contributions =
        contributionRecords(checkedCommand, domainResult, checkedIdentities);
    final List<AttributionReferenceRecord> references =
        referenceRecords(domainResult, checkedIdentities);
    return new FeedbackPersistenceAggregate(
        observationRecord, attributionRecord, contributions, references);
  }

  /** 返回给定已审计结果需要的稳定 reference identity 数量。 */
  public static int requiredReferenceCount(final FeedbackAttributionResult result) {
    final AttributionResult domainResult =
        Objects.requireNonNull(
            Objects.requireNonNull(result, "result").attributionResult(),
            "attributionResult");
    return referenceValues(domainResult).size();
  }

  /** Persistence observation row。 */
  public record OutcomeObservationRecord(
      UUID id,
      String tenantId,
      FeedbackEnvironment environment,
      String decisionId,
      String traceId,
      String observationId,
      String idempotencyKey,
      OutcomeSource outcomeSource,
      ObservedDecisionOutcome outcomeStatus,
      Instant observedAt,
      Instant evaluationTime,
      String canonicalHash) {

    /** 拒绝缺失、越界、敏感或时间漂移字段。 */
    public OutcomeObservationRecord {
      id = Objects.requireNonNull(id, "id");
      tenantId = requireId(tenantId, "tenantId");
      environment = Objects.requireNonNull(environment, "environment");
      decisionId = requireId(decisionId, "decisionId");
      traceId = requireId(traceId, "traceId");
      observationId = requireId(observationId, "observationId");
      idempotencyKey = requireHash(idempotencyKey, "idempotencyKey");
      outcomeSource = Objects.requireNonNull(outcomeSource, "outcomeSource");
      outcomeStatus = Objects.requireNonNull(outcomeStatus, "outcomeStatus");
      observedAt = Objects.requireNonNull(observedAt, "observedAt");
      evaluationTime = Objects.requireNonNull(evaluationTime, "evaluationTime");
      canonicalHash = requireHash(canonicalHash, "canonicalHash");
      if (observedAt.isAfter(evaluationTime)) {
        throw new IllegalArgumentException("observedAt must not be after evaluationTime");
      }
    }
  }

  /** Persistence attribution row。 */
  public record FeedbackAttributionRecord(
      UUID id,
      String tenantId,
      FeedbackEnvironment environment,
      String observationId,
      String decisionId,
      String traceId,
      Instant observedAt,
      String attributionId,
      String policyId,
      String policyVersion,
      FeedbackStatus attributionStatus,
      BigDecimal confidence,
      String canonicalHash,
      FeedbackAttributionErrorCode errorCode) {

    /** 拒绝缺失 scope、非法 hash、非法 confidence 与不稳定错误码。 */
    public FeedbackAttributionRecord {
      id = Objects.requireNonNull(id, "id");
      tenantId = requireId(tenantId, "tenantId");
      environment = Objects.requireNonNull(environment, "environment");
      observationId = requireId(observationId, "observationId");
      decisionId = requireId(decisionId, "decisionId");
      traceId = requireId(traceId, "traceId");
      observedAt = Objects.requireNonNull(observedAt, "observedAt");
      attributionId = requireHash(attributionId, "attributionId");
      policyId = requireId(policyId, "policyId");
      policyVersion = requireId(policyVersion, "policyVersion");
      attributionStatus = Objects.requireNonNull(attributionStatus, "attributionStatus");
      confidence = requireRange(confidence, BigDecimal.ZERO, BigDecimal.ONE, "confidence");
      canonicalHash = requireHash(canonicalHash, "canonicalHash");
      errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }
  }

  /** Persistence contribution row。 */
  public record AttributionContributionRecord(
      UUID id,
      String tenantId,
      FeedbackEnvironment environment,
      String attributionId,
      AttributionDimension dimension,
      BigDecimal measurement,
      BigDecimal contribution,
      AttributionImpact impact,
      BigDecimal confidence,
      String reasonCode,
      String evidenceRef,
      int sortOrder) {

    /** 拒绝非法范围、方向、排序、原因码与敏感 evidence。 */
    public AttributionContributionRecord {
      id = Objects.requireNonNull(id, "id");
      tenantId = requireId(tenantId, "tenantId");
      environment = Objects.requireNonNull(environment, "environment");
      attributionId = requireHash(attributionId, "attributionId");
      dimension = Objects.requireNonNull(dimension, "dimension");
      measurement =
          requireRange(measurement, BigDecimal.ONE.negate(), BigDecimal.ONE, "measurement");
      contribution =
          requireRange(contribution, BigDecimal.ONE.negate(), BigDecimal.ONE, "contribution");
      impact = Objects.requireNonNull(impact, "impact");
      confidence = requireRange(confidence, BigDecimal.ZERO, BigDecimal.ONE, "confidence");
      reasonCode = requireReasonCode(reasonCode);
      evidenceRef = requireSafeReference(evidenceRef, "evidenceRef");
      if (sortOrder < 0 || sortOrder >= MAX_CONTRIBUTIONS) {
        throw new IllegalArgumentException("sortOrder must be within [0,31]");
      }
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
  }

  /** Persistence reference row。 */
  public record AttributionReferenceRecord(
      UUID id,
      String tenantId,
      FeedbackEnvironment environment,
      String attributionId,
      ReferenceType referenceType,
      String referenceValue,
      ReferenceStatus referenceStatus) {

    /** 拒绝缺失 scope、非法 scheme、敏感引用与非 ACTIVE 初始状态。 */
    public AttributionReferenceRecord {
      id = Objects.requireNonNull(id, "id");
      tenantId = requireId(tenantId, "tenantId");
      environment = Objects.requireNonNull(environment, "environment");
      attributionId = requireHash(attributionId, "attributionId");
      referenceType = Objects.requireNonNull(referenceType, "referenceType");
      referenceValue = normalizeReference(referenceType, referenceValue);
      referenceStatus = Objects.requireNonNull(referenceStatus, "referenceStatus");
      if (referenceStatus != ReferenceStatus.ACTIVE) {
        throw new IllegalArgumentException("new reference status must be ACTIVE");
      }
    }
  }

  /** 完整 observation/attribution/contribution/reference aggregate。 */
  public record FeedbackPersistenceAggregate(
      OutcomeObservationRecord observation,
      FeedbackAttributionRecord attribution,
      List<AttributionContributionRecord> contributions,
      List<AttributionReferenceRecord> references) {

    /** 冻结 children，并重新校验 tenant/environment/identity 与稳定排序。 */
    public FeedbackPersistenceAggregate {
      observation = Objects.requireNonNull(observation, "observation");
      attribution = Objects.requireNonNull(attribution, "attribution");
      contributions =
          List.copyOf(Objects.requireNonNull(contributions, "contributions"));
      references = List.copyOf(Objects.requireNonNull(references, "references"));
      validateAggregate(observation, attribution, contributions, references);
    }
  }

  /** 调用方生成的全部 row UUID；构造期冻结并拒绝重复。 */
  public record PersistenceRowIdentities(
      UUID observationRowId,
      UUID attributionRowId,
      List<UUID> contributionRowIds,
      List<UUID> referenceRowIds) {

    /** 冻结 identity 列表并拒绝空 UUID 或跨表重复 UUID。 */
    public PersistenceRowIdentities {
      observationRowId = Objects.requireNonNull(observationRowId, "observationRowId");
      attributionRowId = Objects.requireNonNull(attributionRowId, "attributionRowId");
      contributionRowIds =
          List.copyOf(Objects.requireNonNull(contributionRowIds, "contributionRowIds"));
      referenceRowIds =
          List.copyOf(Objects.requireNonNull(referenceRowIds, "referenceRowIds"));
      final List<UUID> all = new ArrayList<>();
      all.add(observationRowId);
      all.add(attributionRowId);
      all.addAll(contributionRowIds);
      all.addAll(referenceRowIds);
      all.forEach(value -> Objects.requireNonNull(value, "row identity"));
      if (new HashSet<>(all).size() != all.size()) {
        throw new IllegalArgumentException("row identities must be unique");
      }
    }
  }

  /** Reference target 的封闭类型。 */
  public enum ReferenceType {
    /** 已存在的 decision audit event。 */
    AUDIT("audit:"),
    /** Replay case 或 canonical replay snapshot。 */
    REPLAY("replay-case:", "canonical-snapshot:", "replay:"),
    /** 已存在的 evaluation case。 */
    EVALUATION("evaluation:"),
    /** 有界安全 evidence identity。 */
    EVIDENCE("evidence:");

    private final List<String> allowedPrefixes;

    ReferenceType(final String... allowedPrefixes) {
      this.allowedPrefixes = List.of(allowedPrefixes);
    }
  }

  /** Reference lifecycle 的封闭状态；B1 只允许创建 ACTIVE。 */
  public enum ReferenceStatus {
    /** 活跃引用。 */
    ACTIVE,
    /** 后续 B4 显式释放。 */
    RELEASED,
    /** 后续完整性检查标记无效。 */
    INVALID
  }

  private static List<AttributionContributionRecord> contributionRecords(
      final FeedbackAttributionCommand command,
      final AttributionResult result,
      final PersistenceRowIdentities identities) {
    if (identities.contributionRowIds().size() != result.contributions().size()) {
      throw new IllegalArgumentException("contribution row identity count mismatch");
    }
    final List<AttributionContributionRecord> records = new ArrayList<>();
    for (int index = 0; index < result.contributions().size(); index++) {
      final AttributionContribution contribution = result.contributions().get(index);
      final BigDecimal measurement =
          Objects.requireNonNull(
              command.observation().measurements().get(contribution.dimension()),
              "contribution measurement");
      final String evidence =
          Objects.requireNonNull(
              command.observation().evidenceReferences().get(contribution.dimension()),
              "contribution evidence");
      if (!evidence.equals(contribution.evidenceReference())) {
        throw new IllegalArgumentException("contribution evidence must match observation");
      }
      records.add(
          new AttributionContributionRecord(
              identities.contributionRowIds().get(index),
              result.subject().tenantId(),
              result.subject().environment(),
              result.auditReference().resultIdentity(),
              contribution.dimension(),
              measurement,
              contribution.contribution(),
              contribution.impact(),
              BigDecimal.valueOf(contribution.confidence().value()),
              contribution.reasonCode(),
              evidence,
              index));
    }
    return List.copyOf(records);
  }

  private static List<AttributionReferenceRecord> referenceRecords(
      final AttributionResult result, final PersistenceRowIdentities identities) {
    final List<ReferenceValue> values = referenceValues(result);
    if (identities.referenceRowIds().size() != values.size()) {
      throw new IllegalArgumentException("reference row identity count mismatch");
    }
    final List<AttributionReferenceRecord> records = new ArrayList<>();
    for (int index = 0; index < values.size(); index++) {
      final ReferenceValue value = values.get(index);
      records.add(
          new AttributionReferenceRecord(
              identities.referenceRowIds().get(index),
              result.subject().tenantId(),
              result.subject().environment(),
              result.auditReference().resultIdentity(),
              value.type(),
              value.value(),
              ReferenceStatus.ACTIVE));
    }
    return List.copyOf(records);
  }

  private static List<ReferenceValue> referenceValues(final AttributionResult result) {
    final FeedbackAuditReference audit = result.auditReference();
    final Set<ReferenceValue> values = new HashSet<>();
    values.add(new ReferenceValue(ReferenceType.AUDIT, audit.auditReference()));
    values.add(new ReferenceValue(ReferenceType.REPLAY, audit.replayReference()));
    result.contributions().stream()
        .map(AttributionContribution::evidenceReference)
        .forEach(value -> values.add(new ReferenceValue(ReferenceType.EVIDENCE, value)));
    return values.stream()
        .sorted(
            Comparator.comparing((ReferenceValue value) -> value.type().ordinal())
                .thenComparing(ReferenceValue::value))
        .toList();
  }

  private static void validateSourceCorrelation(
      final FeedbackAttributionCommand command,
      final FeedbackCanonicalHash canonicalHash,
      final AttributionResult result) {
    if (!command.subject().equals(result.subject())
        || !command.observation().observationId().equals(result.observationId())
        || !command.policy().policyId().equals(result.policyId())
        || !command.policy().policyVersion().equals(result.policyVersion())
        || !canonicalHash.value().equals(result.canonicalHash())
        || !canonicalHash.value().equals(result.auditReference().canonicalHash())) {
      throw new IllegalArgumentException("persistence source correlation mismatch");
    }
  }

  private static void validateAggregate(
      final OutcomeObservationRecord observation,
      final FeedbackAttributionRecord attribution,
      final List<AttributionContributionRecord> contributions,
      final List<AttributionReferenceRecord> references) {
    if (!observation.tenantId().equals(attribution.tenantId())) {
      throw new FeedbackPersistenceException(
          FeedbackPersistenceErrorCode.TENANT_SCOPE_MISMATCH,
          "feedback persistence tenant mismatch");
    }
    if (observation.environment() != attribution.environment()) {
      throw new FeedbackPersistenceException(
          FeedbackPersistenceErrorCode.ENVIRONMENT_SCOPE_MISMATCH,
          "feedback persistence environment mismatch");
    }
    if (!observation.observationId().equals(attribution.observationId())
        || !observation.decisionId().equals(attribution.decisionId())
        || !observation.traceId().equals(attribution.traceId())
        || !observation.observedAt().equals(attribution.observedAt())
        || !observation.canonicalHash().equals(attribution.canonicalHash())) {
      throw new IllegalArgumentException("feedback persistence observation correlation mismatch");
    }
    if (contributions.size() > MAX_CONTRIBUTIONS) {
      throw new IllegalArgumentException("contributions exceeds max size 32");
    }
    final Set<AttributionDimension> dimensions = new HashSet<>();
    for (int index = 0; index < contributions.size(); index++) {
      final AttributionContributionRecord contribution =
          Objects.requireNonNull(contributions.get(index), "contribution");
      validateChildScope(
          observation.tenantId(),
          observation.environment(),
          attribution.attributionId(),
          contribution.tenantId(),
          contribution.environment(),
          contribution.attributionId());
      if (contribution.sortOrder() != index) {
        throw new IllegalArgumentException("contributions must use contiguous stable sortOrder");
      }
      if (!dimensions.add(contribution.dimension())) {
        throw new IllegalArgumentException("duplicate contribution dimension");
      }
    }
    final Set<String> referenceKeys = new HashSet<>();
    for (AttributionReferenceRecord reference : references) {
      final AttributionReferenceRecord checked =
          Objects.requireNonNull(reference, "reference");
      validateChildScope(
          observation.tenantId(),
          observation.environment(),
          attribution.attributionId(),
          checked.tenantId(),
          checked.environment(),
          checked.attributionId());
      final String key = checked.referenceType() + "\u0000" + checked.referenceValue();
      if (!referenceKeys.add(key)) {
        throw new IllegalArgumentException("duplicate attribution reference");
      }
    }
  }

  private static void validateChildScope(
      final String expectedTenant,
      final FeedbackEnvironment expectedEnvironment,
      final String expectedAttribution,
      final String actualTenant,
      final FeedbackEnvironment actualEnvironment,
      final String actualAttribution) {
    if (!expectedTenant.equals(actualTenant)) {
      throw new FeedbackPersistenceException(
          FeedbackPersistenceErrorCode.TENANT_SCOPE_MISMATCH,
          "feedback persistence child tenant mismatch");
    }
    if (expectedEnvironment != actualEnvironment) {
      throw new FeedbackPersistenceException(
          FeedbackPersistenceErrorCode.ENVIRONMENT_SCOPE_MISMATCH,
          "feedback persistence child environment mismatch");
    }
    if (!expectedAttribution.equals(actualAttribution)) {
      throw new IllegalArgumentException("feedback persistence child attribution mismatch");
    }
  }

  private static String requireId(final String value, final String field) {
    return requireSafeText(value, field, MAX_ID_LENGTH);
  }

  private static String requireHash(final String value, final String field) {
    final String checked = requireSafeText(value, field, 64).toLowerCase(Locale.ROOT);
    if (!SHA_256.matcher(checked).matches()) {
      throw new IllegalArgumentException(field + " must be lowercase SHA-256 hex");
    }
    return checked;
  }

  private static String requireReasonCode(final String value) {
    final String checked = requireSafeText(value, "reasonCode", 64).toUpperCase(Locale.ROOT);
    if (!REASON_CODE.matcher(checked).matches()) {
      throw new IllegalArgumentException("reasonCode must be an uppercase stable code");
    }
    return checked;
  }

  private static String requireSafeReference(final String value, final String field) {
    return requireSafeText(value, field, MAX_REFERENCE_LENGTH);
  }

  private static String requireSafeText(
      final String value, final String field, final int maxLength) {
    final String checked = Objects.requireNonNull(value, field).trim();
    if (checked.isEmpty() || checked.length() > maxLength) {
      throw new IllegalArgumentException(field + " must be within [1," + maxLength + "]");
    }
    if (SENSITIVE_OR_EXECUTABLE.matcher(checked).find()) {
      throw new IllegalArgumentException(field + " rejected by safe persistence guard");
    }
    return checked;
  }

  private static BigDecimal requireRange(
      final BigDecimal value,
      final BigDecimal minimum,
      final BigDecimal maximum,
      final String field) {
    final BigDecimal checked = Objects.requireNonNull(value, field).stripTrailingZeros();
    if (checked.compareTo(minimum) < 0 || checked.compareTo(maximum) > 0) {
      throw new IllegalArgumentException(field + " is outside the allowed range");
    }
    return checked;
  }

  private static String normalizeReference(
      final ReferenceType type, final String referenceValue) {
    final String checked = requireSafeReference(referenceValue, "referenceValue");
    final String lower = checked.toLowerCase(Locale.ROOT);
    for (String prefix : type.allowedPrefixes) {
      if (lower.startsWith(prefix)) {
        return prefix + checked.substring(prefix.length());
      }
    }
    if (type == ReferenceType.EVIDENCE) {
      return "evidence:" + checked;
    }
    throw new IllegalArgumentException("referenceValue scheme does not match referenceType");
  }

  private record ReferenceValue(ReferenceType type, String value) {

    private ReferenceValue {
      type = Objects.requireNonNull(type, "type");
      value = normalizeReference(type, value);
    }
  }
}

package com.guidinglight.decisionhub.domain.qdr.feedback;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 确定性结构化归因的不可变领域结果。
 *
 * @param subject 归因主体
 * @param observationId 观察标识
 * @param status 稳定归因状态
 * @param contributions 按维度稳定排序的可解释贡献
 * @param confidence 整体置信度
 * @param policyId 策略身份
 * @param policyVersion 策略版本
 * @param canonicalHash 影响归因的全部规范输入 hash
 * @param auditReference audit/replay 安全引用
 */
public record AttributionResult(
    FeedbackSubject subject,
    String observationId,
    FeedbackStatus status,
    List<AttributionContribution> contributions,
    FeedbackConfidence confidence,
    String policyId,
    String policyVersion,
    String canonicalHash,
    FeedbackAuditReference auditReference) {

  /** 构造期冻结排序并校验 audit 引用与结果 scope 完全一致。 */
  public AttributionResult {
    subject = Objects.requireNonNull(subject, "subject");
    observationId = FeedbackContractGuard.requireId(observationId, "observationId");
    status = Objects.requireNonNull(status, "status");
    contributions = immutableContributions(contributions);
    confidence = Objects.requireNonNull(confidence, "confidence");
    policyId = FeedbackContractGuard.requireId(policyId, "policyId");
    policyVersion = FeedbackContractGuard.requireId(policyVersion, "policyVersion");
    canonicalHash = FeedbackContractGuard.requireHash(canonicalHash, "canonicalHash");
    auditReference = Objects.requireNonNull(auditReference, "auditReference");
    if (status == FeedbackStatus.ATTRIBUTED && contributions.isEmpty()) {
      throw new IllegalArgumentException("ATTRIBUTED result requires contributions");
    }
    if (!subject.tenantId().equals(auditReference.tenantId())
        || subject.environment() != auditReference.environment()
        || !subject.decisionId().equals(auditReference.decisionId())
        || !subject.traceId().equals(auditReference.traceId())
        || !observationId.equals(auditReference.observationId())
        || !policyId.equals(auditReference.policyId())
        || !policyVersion.equals(auditReference.policyVersion())
        || !canonicalHash.equals(auditReference.canonicalHash())) {
      throw new IllegalArgumentException("auditReference must match attribution scope");
    }
  }

  private static List<AttributionContribution> immutableContributions(
      final List<AttributionContribution> source) {
    Objects.requireNonNull(source, "contributions");
    if (source.size() > AttributionDimension.values().length) {
      throw new IllegalArgumentException("contributions exceeds dimension count");
    }
    final List<AttributionContribution> copy = new ArrayList<>(source);
    copy.forEach(value -> Objects.requireNonNull(value, "contribution"));
    copy.sort(null);
    for (int index = 1; index < copy.size(); index++) {
      if (copy.get(index - 1).dimension() == copy.get(index).dimension()) {
        throw new IllegalArgumentException("duplicate attribution dimension");
      }
    }
    return List.copyOf(copy);
  }
}

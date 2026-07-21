package com.guidinglight.decisionhub.domain.qdr.feedback;

import java.util.Objects;

/**
 * 归因结果的不可变 audit/replay 安全引用。
 *
 * <p>引用只保存身份、hash 和脱敏位置，不保存凭据、原始 prompt、Provider 响应或完整外部 payload。
 *
 * @param tenantId 租户标识
 * @param environment 环境标识
 * @param decisionId 决策标识
 * @param traceId 追踪标识
 * @param observationId 观察标识
 * @param resultIdentity 确定性结果身份
 * @param policyId 策略身份
 * @param policyVersion 策略版本
 * @param canonicalHash 规范输入 hash
 * @param auditReference 审计安全引用
 * @param replayReference 既有 replay/evidence 体系可消费的安全引用
 */
public record FeedbackAuditReference(
    String tenantId,
    FeedbackEnvironment environment,
    String decisionId,
    String traceId,
    String observationId,
    String resultIdentity,
    String policyId,
    String policyVersion,
    String canonicalHash,
    String auditReference,
    String replayReference) {

  /** 构造期拒绝缺失关联、非法 hash 和敏感引用。 */
  public FeedbackAuditReference {
    tenantId = FeedbackContractGuard.requireId(tenantId, "tenantId");
    environment = Objects.requireNonNull(environment, "environment");
    decisionId = FeedbackContractGuard.requireId(decisionId, "decisionId");
    traceId = FeedbackContractGuard.requireId(traceId, "traceId");
    observationId = FeedbackContractGuard.requireId(observationId, "observationId");
    resultIdentity = FeedbackContractGuard.requireHash(resultIdentity, "resultIdentity");
    policyId = FeedbackContractGuard.requireId(policyId, "policyId");
    policyVersion = FeedbackContractGuard.requireId(policyVersion, "policyVersion");
    canonicalHash = FeedbackContractGuard.requireHash(canonicalHash, "canonicalHash");
    auditReference =
        FeedbackContractGuard.requireSafeText(
            auditReference, "auditReference", FeedbackContractGuard.MAX_SAFE_TEXT_LENGTH);
    replayReference =
        FeedbackContractGuard.requireSafeText(
            replayReference, "replayReference", FeedbackContractGuard.MAX_SAFE_TEXT_LENGTH);
  }
}

package com.guidinglight.decisionhub.domain.qdr.feedback;

import java.util.Objects;

/**
 * 归因主体的 tenant/environment/decision/trace 绑定。
 *
 * @param tenantId 租户标识
 * @param environment 仅允许 DEV/TEST 的环境
 * @param decisionId 已存在的决策标识
 * @param traceId 与决策同 scope 的追踪标识
 */
public record FeedbackSubject(
    String tenantId, FeedbackEnvironment environment, String decisionId, String traceId) {

  /** 在构造期拒绝缺失 scope 与空白标识。 */
  public FeedbackSubject {
    tenantId = FeedbackContractGuard.requireId(tenantId, "tenantId");
    environment = Objects.requireNonNull(environment, "environment");
    decisionId = FeedbackContractGuard.requireId(decisionId, "decisionId");
    traceId = FeedbackContractGuard.requireId(traceId, "traceId");
  }
}

package com.guidinglight.decisionhub.domain.qdr.feedback;

import java.util.Objects;

/**
 * 一个 tenant 与 DEV/TEST 环境的不可变 verified execution authority。
 *
 * <p>该值只能在签名、tenant/source/environment 联合授权成功后由认证根创建；不携带 header、nonce、
 * signature、credential 或 raw request material。
 *
 * @param tenantId 已认证 tenant
 * @param environment 已认证 DEV/TEST 环境
 */
public record FeedbackExecutionScope(String tenantId, FeedbackEnvironment environment) {

  /** 构造时 fail-closed 校验，不归一化或默认 tenant/environment。 */
  public FeedbackExecutionScope {
    tenantId = Objects.requireNonNull(tenantId, "tenantId");
    if (tenantId.isBlank() || !tenantId.equals(tenantId.trim())) {
      throw new FeedbackExecutionScopeException(
          "feedback execution tenant must be non-blank without outer whitespace");
    }
    environment = Objects.requireNonNull(environment, "environment");
  }
}

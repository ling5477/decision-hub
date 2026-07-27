package com.guidinglight.decisionhub.security;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import java.util.Objects;
import java.util.Set;

/**
 * 已认证调用方上下文。
 *
 * <p>职责：把可信认证结果传递给 API 层，避免 controller 从未校验 header 或请求体中直接推导 tenant。
 * roles 仅表示本服务内部权限标签，不承载外部密钥或 token 明文。
 */
public record AuthContext(
    String userId,
    String tenantId,
    Set<String> roles,
    String source,
    FeedbackEnvironment environment) {

  /** Preserves generic bearer-token contexts that do not carry feedback authority. */
  public AuthContext(final String userId, final String tenantId, final Set<String> roles) {
    this(userId, tenantId, roles, null, null);
  }

  /**
   * Returns a context enriched only after the signed tenant/source/environment contract is verified.
   *
   * @param verifiedSource exact verified source identity
   * @param verifiedEnvironment canonical verified environment
   * @return immutable verified feedback context
   */
  public AuthContext withVerifiedFeedbackAuthority(
      final String verifiedSource, final FeedbackEnvironment verifiedEnvironment) {
    final String checkedSource = Objects.requireNonNull(verifiedSource, "verifiedSource").trim();
    if (checkedSource.isEmpty()) {
      throw new IllegalArgumentException("verifiedSource must not be blank");
    }
    return new AuthContext(
        userId, tenantId, roles, checkedSource, Objects.requireNonNull(verifiedEnvironment, "environment"));
  }
}

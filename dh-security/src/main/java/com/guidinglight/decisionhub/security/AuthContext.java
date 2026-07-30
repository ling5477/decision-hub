package com.guidinglight.decisionhub.security;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import java.util.Set;

/**
 * 已认证调用方上下文。
 *
 * <p>职责：把可信认证结果传递给 API 层，避免 controller 从未校验 header 或请求体中直接推导 tenant。
 * roles 仅表示本服务内部权限标签，不承载外部密钥或 token 明文。
 */
public record AuthContext(
    String userId, String tenantId, Set<String> roles, FeedbackEnvironment environment) {

  /** 保留非 dry-run API 的既有认证上下文，不为其推断环境。 */
  public AuthContext(final String userId, final String tenantId, final Set<String> roles) {
    this(userId, tenantId, roles, null);
  }

  /** 防止可变 roles 集合逃逸；environment 保持显式 null 或 canonical DEV/TEST。 */
  public AuthContext {
    roles = roles == null ? Set.of() : Set.copyOf(roles);
  }
}

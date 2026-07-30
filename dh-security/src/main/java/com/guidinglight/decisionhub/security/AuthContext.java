package com.guidinglight.decisionhub.security;

import java.util.Set;

/**
 * 已认证调用方上下文。
 *
 * <p>职责：把可信认证结果传递给 API 层，避免 controller 从未校验 header 或请求体中直接推导 tenant。
 * roles 仅表示本服务内部权限标签，不承载外部密钥或 token 明文。
 */
public record AuthContext(String userId, String tenantId, Set<String> roles) {}

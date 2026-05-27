package com.guidinglight.decisionhub.security;

/**
 * DH API 认证端口。
 *
 * <p>用途：由 Web 层传入 bearer token，返回已校验的用户与 tenant 上下文。实现必须只返回可信
 * AuthContext；无法校验或 token 不可信时返回 null，不得把 token 明文写入日志或异常。
 */
public interface TokenVerifier {
  /**
   * 校验 bearer token。
   *
   * @param token Authorization bearer token 明文，仅在内存中做哈希/等值校验，不允许持久化或输出。
   * @return 可信认证上下文；返回 null 表示未认证。
   */
  AuthContext verify(String token);
}

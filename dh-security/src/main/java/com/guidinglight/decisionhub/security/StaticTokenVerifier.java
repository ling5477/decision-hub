package com.guidinglight.decisionhub.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Set;

/**
 * 基于 SHA-256 摘要的最小 bearer token verifier。
 *
 * <p>Why：DH-AUDIT-FIX 需要在不引入外部认证依赖的前提下关闭 API 裸奔风险。本实现只接受配置中的
 * token 摘要，不在代码或日志中保存 token 明文；生产可替换为网关/JWT/mTLS 实现。
 */
public final class StaticTokenVerifier implements TokenVerifier {

  private final String expectedSha256Hex;
  private final AuthContext authContext;

  /**
   * 构造 verifier。
   *
   * @param expectedSha256Hex 受信 token 的 SHA-256 hex；为空时表示拒绝所有请求。
   * @param userId 认证成功后的用户标识。
   * @param tenantId 认证成功后的租户标识。
   * @param roles 认证成功后的内部角色标签。
   */
  public StaticTokenVerifier(
      final String expectedSha256Hex,
      final String userId,
      final String tenantId,
      final Set<String> roles) {
    this.expectedSha256Hex = normalize(expectedSha256Hex);
    this.authContext =
        new AuthContext(
            defaultValue(userId, "api-user"),
            defaultValue(tenantId, ""),
            roles == null ? Set.of() : Set.copyOf(roles));
  }

  @Override
  public AuthContext verify(final String token) {
    if (expectedSha256Hex.isBlank() || token == null || token.isBlank()) {
      return null;
    }
    final String actualHash = sha256Hex(token);
    if (MessageDigest.isEqual(
        expectedSha256Hex.getBytes(StandardCharsets.UTF_8),
        actualHash.getBytes(StandardCharsets.UTF_8))) {
      return authContext;
    }
    return null;
  }

  /**
   * 计算 SHA-256 hex。
   *
   * <p>该方法仅用于测试与配置校验，不输出入参明文。
   */
  public static String sha256Hex(final String value) {
    Objects.requireNonNull(value, "value");
    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      final byte[] bytes = md.digest(value.getBytes(StandardCharsets.UTF_8));
      final StringBuilder sb = new StringBuilder(bytes.length * 2);
      for (final byte b : bytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private static String normalize(final String value) {
    return value == null ? "" : value.trim().toLowerCase();
  }

  private static String defaultValue(final String value, final String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}

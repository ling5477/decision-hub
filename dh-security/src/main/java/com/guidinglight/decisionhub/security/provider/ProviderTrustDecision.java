package com.guidinglight.decisionhub.security.provider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Provider trust 判定结果。
 *
 * <p>auditFields 只暴露 baseUrlHash，不输出完整 baseURL 中可能携带的 query 或凭据。
 */
public record ProviderTrustDecision(
    boolean allowed, String provider, String baseUrlHash, ProviderTrustLevel trustLevel, String reason) {

  /** 构造允许访问的判定结果。 */
  public static ProviderTrustDecision allow(
      final String provider, final String baseUrl, final ProviderTrustLevel trustLevel) {
    return new ProviderTrustDecision(true, provider, sha256Short(baseUrl), trustLevel, "ALLOWED");
  }

  /** 构造拒绝访问的判定结果。 */
  public static ProviderTrustDecision deny(
      final String provider, final String baseUrl, final ProviderTrustLevel trustLevel, final String reason) {
    return new ProviderTrustDecision(false, provider, sha256Short(baseUrl), trustLevel, reason);
  }

  /**
   * 输出审计字段。
   *
   * @param traceId 当前请求 traceId。
   * @return 不含敏感明文的审计字段。
   */
  public Map<String, Object> auditFields(final String traceId) {
    return Map.of(
        "provider", provider == null ? "" : provider,
        "baseUrlHash", baseUrlHash == null ? "" : baseUrlHash,
        "trustLevel", trustLevel.name(),
        "traceId", traceId == null ? "" : traceId);
  }

  private static String sha256Short(final String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      final byte[] bytes = md.digest(value.getBytes(StandardCharsets.UTF_8));
      final StringBuilder sb = new StringBuilder(16);
      for (int i = 0; i < 8 && i < bytes.length; i++) {
        sb.append(String.format("%02x", bytes[i]));
      }
      return sb.toString();
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}

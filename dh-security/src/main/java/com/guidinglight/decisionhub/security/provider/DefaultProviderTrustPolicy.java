package com.guidinglight.decisionhub.security.provider;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 默认 ProviderTrustPolicy。
 *
 * <p>Why：当前 DH 不接真实 provider，但未来一旦引入 baseURL，必须先有拒绝 UNKNOWN / UNTRUSTED_RELAY
 * 的框架。该实现只做 allowlist 与中转站关键词识别，不发起任何网络请求。
 */
public final class DefaultProviderTrustPolicy implements ProviderTrustPolicy {

  private final Map<String, ProviderTrustLevel> allowlist;

  /**
   * 构造 trust policy。
   *
   * @param allowlist key 为规范化后的 baseURL，value 为显式信任等级。
   */
  public DefaultProviderTrustPolicy(final Map<String, ProviderTrustLevel> allowlist) {
    this.allowlist =
        allowlist == null
            ? Map.of()
            : allowlist.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .collect(
                    java.util.stream.Collectors.toUnmodifiableMap(
                        e -> normalizeBaseUrl(e.getKey()), Map.Entry::getValue));
  }

  @Override
  public ProviderTrustDecision evaluate(final String provider, final String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      return ProviderTrustDecision.deny(provider, baseUrl, ProviderTrustLevel.UNKNOWN, "BASE_URL_EMPTY");
    }
    final String normalized = normalizeBaseUrl(baseUrl);
    if (looksLikeUntrustedRelay(normalized)) {
      return ProviderTrustDecision.deny(
          provider, baseUrl, ProviderTrustLevel.UNTRUSTED_RELAY, "UNTRUSTED_RELAY_PATTERN");
    }
    final ProviderTrustLevel trustLevel = allowlist.get(normalized);
    if (trustLevel == null || trustLevel == ProviderTrustLevel.UNKNOWN) {
      return ProviderTrustDecision.deny(provider, baseUrl, ProviderTrustLevel.UNKNOWN, "BASE_URL_NOT_ALLOWED");
    }
    if (trustLevel == ProviderTrustLevel.UNTRUSTED_RELAY) {
      return ProviderTrustDecision.deny(provider, baseUrl, trustLevel, "UNTRUSTED_RELAY");
    }
    return ProviderTrustDecision.allow(provider, baseUrl, trustLevel);
  }

  private static boolean looksLikeUntrustedRelay(final String baseUrl) {
    final String host = hostOf(baseUrl);
    return host.contains("openrouter")
        || host.contains("siliconflow")
        || host.contains("new-api")
        || host.contains("one-api")
        || host.contains("relay")
        || host.contains("proxy");
  }

  private static String hostOf(final String baseUrl) {
    try {
      final URI uri = URI.create(baseUrl);
      return Objects.toString(uri.getHost(), baseUrl).toLowerCase(Locale.ROOT);
    } catch (final RuntimeException e) {
      return baseUrl.toLowerCase(Locale.ROOT);
    }
  }

  private static String normalizeBaseUrl(final String baseUrl) {
    final String trimmed = baseUrl.trim();
    final String noTrailingSlash =
        trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    return noTrailingSlash.toLowerCase(Locale.ROOT);
  }
}

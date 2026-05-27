package com.guidinglight.decisionhub.config;

import com.guidinglight.decisionhub.security.StaticTokenVerifier;
import com.guidinglight.decisionhub.security.TokenVerifier;
import com.guidinglight.decisionhub.security.nq.HmacNqFeedbackAuthenticator;
import com.guidinglight.decisionhub.security.nq.InMemoryNonceReplayGuard;
import com.guidinglight.decisionhub.security.nq.NqFeedbackAuthenticator;
import com.guidinglight.decisionhub.security.provider.DefaultPromptContextRedactionGate;
import com.guidinglight.decisionhub.security.provider.DefaultProviderTrustPolicy;
import com.guidinglight.decisionhub.security.provider.PromptContextRedactionGate;
import com.guidinglight.decisionhub.security.provider.ProviderTrustLevel;
import com.guidinglight.decisionhub.security.provider.ProviderTrustPolicy;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DH-AUDIT-FIX 最小安全装配。
 *
 * <p>默认策略是 fail closed：未配置 API token 摘要或 NQ HMAC secret 时，受保护 API 拒绝请求。
 * 本类不引入真实 provider、不发起外部调用，只提供认证与出站信任闸门框架。
 */
@Configuration
public class SecurityWiringConfig {

  /**
   * API bearer token verifier。
   *
   * @param tokenSha256 配置中的 token SHA-256 hex；为空时拒绝所有 API 请求。
   * @param tenantId 默认绑定 tenant；生产应由网关/JWT verifier 替换。
   * @return TokenVerifier bean。
   */
  @Bean
  public TokenVerifier tokenVerifier(
      @Value("${decisionhub.security.api.token-sha256:}") final String tokenSha256,
      @Value("${decisionhub.security.api.tenant-id:}") final String tenantId) {
    return new StaticTokenVerifier(tokenSha256, "dh-api", tenantId, Set.of("DH_API"));
  }

  /**
   * NQ feedback HMAC authenticator。
   *
   * @param secret HMAC 共享密钥；为空时拒绝所有 NQ feedback。
   * @param allowedSources 逗号分隔的 sourceSystem allowlist。
   * @param maxPayloadBytes payload 最大字节数。
   * @param maxClockSkewSeconds timestamp 最大偏移秒数。
   * @return NqFeedbackAuthenticator bean。
   */
  @Bean
  public NqFeedbackAuthenticator nqFeedbackAuthenticator(
      @Value("${decisionhub.security.nq-feedback.hmac-secret:}") final String secret,
      @Value("${decisionhub.security.nq-feedback.allowed-sources:nexus-quant}")
          final String allowedSources,
      @Value("${decisionhub.security.nq-feedback.max-payload-bytes:65536}")
          final long maxPayloadBytes,
      @Value("${decisionhub.security.nq-feedback.max-clock-skew-seconds:300}")
          final long maxClockSkewSeconds) {
    return new HmacNqFeedbackAuthenticator(
        splitCsv(allowedSources),
        secret,
        Duration.ofSeconds(maxClockSkewSeconds),
        maxPayloadBytes,
        new InMemoryNonceReplayGuard());
  }

  /** Provider trust policy 默认无 allowlist，因此拒绝 UNKNOWN / UNTRUSTED_RELAY。 */
  @Bean
  public ProviderTrustPolicy providerTrustPolicy() {
    return new DefaultProviderTrustPolicy(Map.<String, ProviderTrustLevel>of());
  }

  /** Prompt/context 出站闸门。 */
  @Bean
  public PromptContextRedactionGate promptContextRedactionGate() {
    return new DefaultPromptContextRedactionGate();
  }

  private static Set<String> splitCsv(final String value) {
    if (value == null || value.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .collect(Collectors.toUnmodifiableSet());
  }
}

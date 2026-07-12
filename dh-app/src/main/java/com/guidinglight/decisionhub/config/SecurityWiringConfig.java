package com.guidinglight.decisionhub.config;

import com.guidinglight.decisionhub.infra.jdbc.JdbcNonceReplayGuard;
import com.guidinglight.decisionhub.security.StaticTokenVerifier;
import com.guidinglight.decisionhub.security.TokenVerifier;
import com.guidinglight.decisionhub.security.nq.HmacNqFeedbackAuthenticator;
import com.guidinglight.decisionhub.security.nq.InMemoryNonceReplayGuard;
import com.guidinglight.decisionhub.security.nq.NonceReplayGuard;
import com.guidinglight.decisionhub.security.nq.NonceReplayGuardType;
import com.guidinglight.decisionhub.security.nq.InMemoryRateLimiter;
import com.guidinglight.decisionhub.security.nq.NqFeedbackAuthenticator;
import com.guidinglight.decisionhub.security.nq.RateLimiter;
import com.guidinglight.decisionhub.security.provider.DefaultPromptContextRedactionGate;
import com.guidinglight.decisionhub.security.provider.DefaultProviderTrustPolicy;
import com.guidinglight.decisionhub.security.provider.PromptContextRedactionGate;
import com.guidinglight.decisionhub.security.provider.ProviderTrustLevel;
import com.guidinglight.decisionhub.security.provider.ProviderTrustPolicy;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DH-AUDIT-FIX 最小安全装配。
 *
 * <p>默认策略是 fail closed：未配置 API token 摘要或 NQ HMAC secret 时，受保护 API 拒绝请求。 本类不引入真实
 * provider、不发起外部调用，只提供认证与出站信任闸门框架。
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
          final long maxClockSkewSeconds,
      final NonceReplayGuard nonceReplayGuard) {
    return new HmacNqFeedbackAuthenticator(
        splitCsv(allowedSources),
        secret,
        Duration.ofSeconds(maxClockSkewSeconds),
        maxPayloadBytes,
        nonceReplayGuard);
  }

  /**
   * NQ feedback 防重放 guard 条件装配。
   *
   * <p>Why：原实现硬编码无界、单实例的 {@link InMemoryNonceReplayGuard}，多实例/重启后防重放失效。本 bean 按 {@code
   * decisionhub.security.nq-feedback.replay.guard-type} 与 active profile 选择实现：dev/test 允许
   * in-memory；非 dev/test（prod / real channel）默认持久化 {@link JdbcNonceReplayGuard}。 选择策略 fail-closed（见
   * {@link NonceReplayGuardType#select}）：配置缺失或无法判定 profile 时绝不退回 in-memory；要求 jdbc 但无 {@link
   * JdbcTemplate} 时启动失败而非静默降级。
   *
   * @param environment 用于读取 active profiles。
   * @param jdbcTemplateProvider JdbcTemplate 提供者（jdbc 类型时必需）。
   * @param guardType 配置项，缺省时按 profile 推断。
   * @param cleanupEnabled jdbc 实现是否惰性清理过期行。
   * @return 选定的 NonceReplayGuard bean。
   */
  @Bean
  public NonceReplayGuard nqFeedbackNonceReplayGuard(
      final Environment environment,
      final ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
      @Value("${decisionhub.security.nq-feedback.replay.guard-type:}") final String guardType,
      @Value("${decisionhub.security.nq-feedback.replay.cleanup-enabled:true}")
          final boolean cleanupEnabled,
      @Value("${decisionhub.security.nq-feedback.replay.in-memory.max-entries:10000}")
          final int inMemoryMaxEntries,
      @Value("${decisionhub.security.nq-feedback.replay.in-memory.ttl-seconds:600}")
          final long inMemoryTtlSeconds) {
    final boolean devOrTest =
        NonceReplayGuardType.isDevOrTest(List.of(environment.getActiveProfiles()));
    final NonceReplayGuardType resolved = NonceReplayGuardType.select(guardType, devOrTest);
    if (resolved == NonceReplayGuardType.JDBC) {
      final JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
      if (jdbcTemplate == null) {
        throw new IllegalStateException(
            "nonce replay guard-type=jdbc requires a JdbcTemplate/DataSource but none is configured; "
                + "refusing to fall back to in-memory (fail-closed)");
      }
      return new JdbcNonceReplayGuard(jdbcTemplate, cleanupEnabled);
    }
    // 仅在 dev/test 且策略已校验通过时到达此分支。in-memory 也有界：上限 + TTL，超限 fail-closed。
    // 非法配置（上限非正 / TTL 非正）由 InMemoryNonceReplayGuard 构造抛 IllegalArgumentException -> 启动失败。
    return new InMemoryNonceReplayGuard(
        inMemoryMaxEntries, Duration.ofSeconds(inMemoryTtlSeconds), Clock.systemUTC());
  }

  /**
   * NQ feedback 入站限流 bean（DH-P1-4 Batch 3）。
   *
   * <p>Why：在本批之前 {@code NqFeedbackController -> HmacNqFeedbackAuthenticator} 链路完全无限流。本 bean 提供
   * bounded in-memory limiter（仅 dev/test 或单实例辅助路径；真实多实例需集中式 limiter，另起任务）。作用点由
   * controller 置于 HMAC 认证之前；key=source+tenant+route；超限 429 RATE_LIMITED。
   *
   * <p>fail-closed：window / max-requests / max-keys 非正时由 {@link InMemoryRateLimiter} 构造抛
   * {@link IllegalArgumentException} -> bean 初始化失败 -> 启动失败，绝不静默 fail-open。
   *
   * @param windowSeconds 固定窗口秒数（保守默认 1）。
   * @param maxRequests 单窗口最大请求数（保守默认 20，per source+tenant+route）。
   * @param maxKeys key 上限（保守默认 10000，兜底防无界增长）。
   * @return 有界 fail-closed 限流器。
   */
  @Bean
  @Primary
  public RateLimiter nqFeedbackRateLimiter(
      @Value("${decisionhub.security.nq-feedback.rate-limit.window-seconds:1}")
          final int windowSeconds,
      @Value("${decisionhub.security.nq-feedback.rate-limit.max-requests:20}") final int maxRequests,
      @Value("${decisionhub.security.nq-feedback.rate-limit.max-keys:10000}") final int maxKeys) {
    return new InMemoryRateLimiter(windowSeconds, maxRequests, maxKeys, Clock.systemUTC());
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

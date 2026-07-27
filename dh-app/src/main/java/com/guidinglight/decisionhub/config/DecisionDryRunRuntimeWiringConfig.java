package com.guidinglight.decisionhub.config;

import com.guidinglight.decisionhub.security.nq.HmacNqDryRunAuthenticator;
import com.guidinglight.decisionhub.security.nq.NonceReplayGuard;
import com.guidinglight.decisionhub.security.nq.NonceReplayGuardType;
import com.guidinglight.decisionhub.security.nq.RateLimiter;
import com.guidinglight.decisionhub.infra.jdbc.qdr.guard.JdbcGuardCleanupAdapter;
import com.guidinglight.decisionhub.infra.jdbc.qdr.guard.JdbcIdempotencyGuardAdapter;
import com.guidinglight.decisionhub.infra.jdbc.qdr.guard.JdbcRateLimitAdmissionAdapter;
import com.guidinglight.decisionhub.qdr7.PersistentDecisionDryRunRateLimiter;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionOrchestrator;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQueryRepository;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunGuardProperties;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunRequestFingerprint;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunRuntimeProperties;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunSafeResultProjector;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunService;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DefaultDecisionDryRunService;
import com.guidinglight.decisionhub.usecase.decision.dryrun.LimitedDryRunRuntimePolicy;
import com.guidinglight.decisionhub.usecase.decision.dryrun.NoSideEffectDecisionContract;
import com.guidinglight.decisionhub.usecase.decision.dryrun.PersistentGuardedDecisionDryRunService;
import com.guidinglight.decisionhub.usecase.decision.dryrun.RuntimeConcurrencyPolicy;
import com.guidinglight.decisionhub.usecase.decision.dryrun.RuntimeDeadlinePolicy;
import com.guidinglight.decisionhub.usecase.decision.dryrun.RuntimeEnvironmentPolicy;
import com.guidinglight.decisionhub.usecase.decision.dryrun.RuntimeFeatureFlag;
import com.guidinglight.decisionhub.usecase.decision.dryrun.RuntimeKillSwitch;
import com.guidinglight.decisionhub.usecase.qdr.DecisionRequestRepository;
import com.guidinglight.decisionhub.usecase.qdr.DecisionRunRepository;
import com.guidinglight.decisionhub.usecase.qdr.QuantDecisionRepository;
import com.guidinglight.decisionhub.usecase.qdr.QuantSignalRepository;
import com.guidinglight.decisionhub.usecase.qdr.gateway.QdrModelGatewayIntegrationPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.GuardCleanupPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.GuardTransactionBoundary;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyGuardPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardStoreException;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionPort;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration-1 limited dry-run endpoint 装配。
 *
 * <p>本配置只装配 DH inbound endpoint 所需的 feature gate、HMAC authenticator 和 usecase service；不创建
 * NQ client、不发起 outbound HTTP、不接真实 provider、不启用 Agent/LangGraph runtime，也不打开 LIVE。
 */
@Configuration
public class DecisionDryRunRuntimeWiringConfig {

    /**
     * 装配B2 persistent guard配置。Runtime关闭时0值保持惰性；一旦开启，缺失或非法值立即阻断启动。
     */
    @Bean
    @ConditionalOnMissingBean
    public DecisionDryRunGuardProperties decisionDryRunGuardProperties(
            @Value("${decisionhub.integration1.runtime.enabled:false}") final boolean enabled,
            @Value("${decisionhub.integration1.runtime.guard.environment:}") final String environment,
            @Value("${decisionhub.integration1.runtime.guard.rate-window-seconds:0}") final int rateWindowSeconds,
            @Value("${decisionhub.integration1.runtime.guard.rate-limit-value:0}") final int rateLimitValue,
            @Value("${decisionhub.integration1.runtime.guard.lease-seconds:0}") final long leaseSeconds,
            @Value("${decisionhub.integration1.runtime.guard.idempotency-ttl-seconds:0}") final long ttlSeconds,
            @Value("${decisionhub.integration1.runtime.guard.retention-seconds:0}") final long retentionSeconds) {
        return new DecisionDryRunGuardProperties(
                enabled,
                environment,
                rateWindowSeconds,
                rateLimitValue,
                Duration.ofSeconds(leaseSeconds),
                Duration.ofSeconds(ttlSeconds),
                Duration.ofSeconds(retentionSeconds));
    }

    /** 装配DB UTC fixed-window production port；无in-memory fallback。 */
    @Bean
    @ConditionalOnMissingBean
    public RateLimitAdmissionPort rateLimitAdmissionPort(final JdbcTemplate jdbcTemplate) {
        return new JdbcRateLimitAdmissionAdapter(jdbcTemplate);
    }

    /** 装配exact identity/CAS idempotency production port。 */
    @Bean
    @ConditionalOnMissingBean
    public IdempotencyGuardPort idempotencyGuardPort(final JdbcTemplate jdbcTemplate) {
        return new JdbcIdempotencyGuardAdapter(jdbcTemplate);
    }

    /** 装配bounded cleanup primitives；本轮不创建调度频率。 */
    @Bean
    @ConditionalOnMissingBean
    public GuardCleanupPort guardCleanupPort(final JdbcTemplate jdbcTemplate) {
        return new JdbcGuardCleanupAdapter(jdbcTemplate);
    }

    /**
     * 装配 required local PostgreSQL transaction boundary；缺少 transaction manager 会阻断 bean 创建。
     *
     * <p>事务无法开始属于明确的 store unavailable；事务已开始后的提交异常保留原异常并由调用方按
     * commit-unknown fail-closed，不能自动重放业务执行。
     *
     * @param transactionManager DH datasource 对应的 transaction manager。
     * @return 只暴露 required transaction 能力的 guard boundary。
     */
    @Bean
    @ConditionalOnMissingBean
    public GuardTransactionBoundary guardTransactionBoundary(
            final PlatformTransactionManager transactionManager) {
        final TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return new GuardTransactionBoundary() {
            @Override
            public <T> T required(final java.util.function.Supplier<T> action) {
                try {
                    final T result = transaction.execute(status -> action.get());
                    if (result == null) {
                        throw new IllegalStateException("guard transaction returned null");
                    }
                    return result;
                } catch (final CannotCreateTransactionException error) {
                    throw new PersistentGuardStoreException(
                            "persistent guard transaction store unavailable", error);
                }
            }
        };
    }

    /** 装配canonical request fingerprint，不暴露canonical bytes。 */
    @Bean
    @ConditionalOnMissingBean
    public DecisionDryRunRequestFingerprint decisionDryRunRequestFingerprint() {
        return new DecisionDryRunRequestFingerprint();
    }

    /** 装配existing dh_decision_output safe-result projector。 */
    @Bean
    @ConditionalOnMissingBean
    public DecisionDryRunSafeResultProjector decisionDryRunSafeResultProjector(
            final DecisionReplayQueryRepository replayRepository) {
        return new DecisionDryRunSafeResultProjector(replayRepository);
    }

    /** 装配dry-run专用persistent rate bridge；Controller通过qualifier选择，不影响NQ feedback。 */
    @Bean("decisionDryRunRateLimiter")
    public RateLimiter decisionDryRunRateLimiter(
            final RateLimitAdmissionPort admissionPort,
            final GuardTransactionBoundary transactions,
            final DecisionAuditRepository auditRepository,
            final DecisionDryRunGuardProperties properties) {
        return new PersistentDecisionDryRunRateLimiter(
                admissionPort, transactions, auditRepository, properties, Clock.systemUTC());
    }

    /**
     * 装配 dry-run runtime feature gate 配置。
     *
     * @param environment              Spring environment，用于判断 dev/test profile。
     * @param enabled                  endpoint enabled flag；默认 false。
     * @param productionEnabled        非 dev/test profile 是否允许启用；默认 false。
     * @param killSwitchEnabled        kill switch；true 时立即 fail-closed。
     * @param allowedSources           source allowlist。
     * @param allowedTenantSourcePairs tenant/source pair allowlist。
     * @param allowedSourceEnvironmentPairs source/environment pair allowlist。
     * @param allowedTenantEnvironmentPairs tenant/environment pair allowlist。
     * @param allowedTenantSourceEnvironmentTriples full tenant/source/environment allowlist。
     * @param memoryCapBytes           decisionContext 内存上限。
     * @return dry-run runtime 配置快照。
     */
    @Bean
    @ConditionalOnMissingBean
    public DecisionDryRunRuntimeProperties decisionDryRunRuntimeProperties(
            final Environment environment,
            @Value("${decisionhub.integration1.runtime.enabled:false}") final boolean enabled,
            @Value("${decisionhub.integration1.runtime.production-enabled:false}") final boolean productionEnabled,
            @Value("${decisionhub.integration1.runtime.kill-switch-enabled:false}") final boolean killSwitchEnabled,
            @Value("${decisionhub.integration1.runtime.allowed-sources:NQ_DRYRUN}") final String allowedSources,
            @Value("${decisionhub.integration1.runtime.allowed-tenant-source-pairs:}") final String allowedTenantSourcePairs,
            @Value("${decisionhub.integration1.runtime.allowed-source-environment-pairs:}") final String allowedSourceEnvironmentPairs,
            @Value("${decisionhub.integration1.runtime.allowed-tenant-environment-pairs:}") final String allowedTenantEnvironmentPairs,
            @Value("${decisionhub.integration1.runtime.allowed-tenant-source-environment-triples:}") final String allowedTenantSourceEnvironmentTriples,
            @Value("${decisionhub.integration1.runtime.memory-cap-bytes:32768}") final int memoryCapBytes) {
        final boolean devOrTest =
                NonceReplayGuardType.isDevOrTest(List.of(environment.getActiveProfiles()));
        return new DecisionDryRunRuntimeProperties(
                enabled,
                productionEnabled,
                killSwitchEnabled,
                devOrTest,
                splitRequiredSourceCsv(allowedSources),
                splitOptionalPairCsv(allowedTenantSourcePairs),
                splitOptionalPairCsv(allowedSourceEnvironmentPairs),
                splitOptionalPairCsv(allowedTenantEnvironmentPairs),
                splitOptionalPairCsv(allowedTenantSourceEnvironmentTriples),
                memoryCapBytes);
    }

    /**
     * 装配 Stage-QDR-7 B3 limited runtime policy 启动期快照。
     *
     * <p>Provider kind 与 retry count 不开放外部配置：B3 固定为 deterministic MOCK、retry 0。Runtime 启用时
     * deadline/concurrency/queue 非法会在入口 fail-closed，且不会创建无界 executor。
     *
     * @param environment Spring active profiles。
     * @param enabled runtime feature flag。
     * @param productionEnabled production gate；B3 必须保持 false。
     * @param killSwitchEnabled emergency kill switch。
     * @param deadlineMillis 总 deadline 毫秒。
     * @param maxConcurrency 最大并发数。
     * @param queueCapacity bounded queue 容量。
     * @param maxQueueWaitMillis 最大排队等待毫秒。
     * @return immutable limited runtime policy。
     */
    @Bean
    @ConditionalOnMissingBean
    public LimitedDryRunRuntimePolicy limitedDryRunRuntimePolicy(
            final Environment environment,
            @Value("${decisionhub.integration1.runtime.enabled:false}") final boolean enabled,
            @Value("${decisionhub.integration1.runtime.production-enabled:false}") final boolean productionEnabled,
            @Value("${decisionhub.integration1.runtime.kill-switch-enabled:false}") final boolean killSwitchEnabled,
            @Value("${decisionhub.integration1.runtime.deadline-ms:5000}") final long deadlineMillis,
            @Value("${decisionhub.integration1.runtime.max-concurrency:4}") final int maxConcurrency,
            @Value("${decisionhub.integration1.runtime.queue-capacity:8}") final int queueCapacity,
            @Value("${decisionhub.integration1.runtime.max-queue-wait-ms:100}") final long maxQueueWaitMillis) {
        return new LimitedDryRunRuntimePolicy(
                new RuntimeFeatureFlag(enabled),
                new RuntimeEnvironmentPolicy(
                        Set.copyOf(Arrays.asList(environment.getActiveProfiles())), productionEnabled),
                RuntimeKillSwitch.fromEmergencyFlag(killSwitchEnabled),
                new RuntimeDeadlinePolicy(
                        Duration.ofMillis(deadlineMillis), Duration.ofMillis(maxQueueWaitMillis)),
                new RuntimeConcurrencyPolicy(maxConcurrency, queueCapacity),
                LimitedDryRunRuntimePolicy.MOCK_PROVIDER_KIND,
                0,
                new NoSideEffectDecisionContract());
    }

    /**
     * 装配 dry-run HMAC authenticator。
     *
     * @param properties          dry-run runtime 配置。
     * @param hmacSecret          dry-run HMAC secret；为空时 authenticator fail-closed。
     * @param maxPayloadBytes     raw body 最大字节数。
     * @param maxClockSkewSeconds timestamp 最大偏移秒数。
     * @param nonceReplayGuard    既有 replay guard；生产由 SecurityWiringConfig 选择 JDBC。
     * @return dry-run HMAC authenticator。
     */
    @Bean
    @ConditionalOnMissingBean
    public HmacNqDryRunAuthenticator hmacNqDryRunAuthenticator(
            final DecisionDryRunRuntimeProperties properties,
            @Value("${decisionhub.integration1.runtime.hmac-secret:}") final String hmacSecret,
            @Value("${decisionhub.integration1.runtime.max-payload-bytes:65536}") final long maxPayloadBytes,
            @Value("${decisionhub.integration1.runtime.max-clock-skew-seconds:300}") final long maxClockSkewSeconds,
            final NonceReplayGuard nonceReplayGuard) {
        return new HmacNqDryRunAuthenticator(
                properties.allowedSources(),
                properties.allowedTenantSourcePairs(),
                properties.allowedSourceEnvironmentPairs(),
                properties.allowedTenantEnvironmentPairs(),
                properties.allowedTenantSourceEnvironmentTriples(),
                hmacSecret,
                Duration.ofSeconds(maxClockSkewSeconds),
                maxPayloadBytes,
                nonceReplayGuard);
    }

    /**
     * 装配 dry-run usecase service。
     *
     * @param orchestrator              既有 mock-only DecisionOrchestrator。
     * @param auditRepository           既有 DH-owned audit repository。
     * @param decisionRequestRepository stage-qdr-1 request 主线 repository。
     * @param decisionRunRepository     stage-qdr-1 run 主线 repository。
     * @param quantSignalRepository     stage-qdr-1 signal 主线 repository。
     * @param quantDecisionRepository   stage-qdr-1 decision 主线 repository。
     * @param qdrModelGatewayIntegration stage-qdr-3 B4 mock gateway integration。
     * @param fingerprint                不返回 canonical bytes 的 request fingerprint service。
     * @param resultProjector            tenant-bound immutable safe result projector。
     * @param idempotencyGuardPort       exact identity/CAS persistent idempotency port。
     * @param transactions               usecase-owned required transaction boundary。
     * @param guardProperties            persistent guard 严格配置快照。
     * @param properties                dry-run runtime 配置。
     * @return 由 persistent idempotency wrapper 封闭的 dry-run usecase service。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public DecisionDryRunService decisionDryRunService(
            final DecisionOrchestrator orchestrator,
            final DecisionAuditRepository auditRepository,
            final DecisionRequestRepository decisionRequestRepository,
            final DecisionRunRepository decisionRunRepository,
            final QuantSignalRepository quantSignalRepository,
            final QuantDecisionRepository quantDecisionRepository,
            final QdrModelGatewayIntegrationPort qdrModelGatewayIntegration,
            final DecisionDryRunRequestFingerprint fingerprint,
            final DecisionDryRunSafeResultProjector resultProjector,
            final IdempotencyGuardPort idempotencyGuardPort,
            final GuardTransactionBoundary transactions,
            final DecisionDryRunGuardProperties guardProperties,
            final DecisionDryRunRuntimeProperties properties,
            final LimitedDryRunRuntimePolicy runtimePolicy) {
        final DecisionDryRunService delegate = new DefaultDecisionDryRunService(
                orchestrator,
                auditRepository,
                qdrModelGatewayIntegration,
                decisionRequestRepository,
                decisionRunRepository,
                quantSignalRepository,
                quantDecisionRepository,
                properties,
                Clock.systemUTC());
        return new PersistentGuardedDecisionDryRunService(
                delegate,
                idempotencyGuardPort,
                transactions,
                auditRepository,
                fingerprint,
                resultProjector,
                guardProperties,
                Clock.systemUTC(),
                runtimePolicy);
    }

    /**
     * 将 source allowlist CSV 保留为显式配置项，不能将空/空白 source 静默折叠为空集合。
     *
     * <p>真正的 source 合法性由 {@link DecisionDryRunRuntimeProperties} 校验；这里使用 {@code -1} 保留末尾
     * 空条目，确保 {@code NQ_DRYRUN,} 与空白值均在 Spring bean 创建时 fail-closed。唯一例外是完全空的
     * CSV，它表达 production profile 的空 allowlist，而不是一个 source 条目。
     */
    private static Set<String> splitRequiredSourceCsv(final String value) {
        if (value == null || value.isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(value.split(",", -1))
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 将可选 tenant/source pair CSV 映射为集合。
     *
     * <p>空字符串表示当前 profile 未授权任何 tenant/source 组合；但非空字符串中的空白或非法 pair 会由
     * {@link DecisionDryRunRuntimeProperties} 拒绝，不能被 parser 丢弃。
     */
    private static Set<String> splitOptionalPairCsv(final String value) {
        if (value == null || value.isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(value.split(",", -1))
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());
    }
}

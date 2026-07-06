package com.guidinglight.decisionhub.config;

import com.guidinglight.decisionhub.security.nq.HmacNqDryRunAuthenticator;
import com.guidinglight.decisionhub.security.nq.NonceReplayGuard;
import com.guidinglight.decisionhub.security.nq.NonceReplayGuardType;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionOrchestrator;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunRuntimeProperties;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunService;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DefaultDecisionDryRunService;
import com.guidinglight.decisionhub.usecase.qdr.DecisionRequestRepository;
import com.guidinglight.decisionhub.usecase.qdr.DecisionRunRepository;
import com.guidinglight.decisionhub.usecase.qdr.QuantDecisionRepository;
import com.guidinglight.decisionhub.usecase.qdr.QuantSignalRepository;

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

/**
 * Integration-1 limited dry-run endpoint 装配。
 *
 * <p>本配置只装配 DH inbound endpoint 所需的 feature gate、HMAC authenticator 和 usecase service；不创建
 * NQ client、不发起 outbound HTTP、不接真实 provider、不启用 Agent/LangGraph runtime，也不打开 LIVE。
 */
@Configuration
public class DecisionDryRunRuntimeWiringConfig {

    /**
     * 装配 dry-run runtime feature gate 配置。
     *
     * @param environment              Spring environment，用于判断 dev/test profile。
     * @param enabled                  endpoint enabled flag；默认 false。
     * @param productionEnabled        非 dev/test profile 是否允许启用；默认 false。
     * @param killSwitchEnabled        kill switch；true 时立即 fail-closed。
     * @param allowedSources           source allowlist。
     * @param allowedTenantSourcePairs tenant/source pair allowlist。
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
            @Value("${decisionhub.integration1.runtime.memory-cap-bytes:32768}") final int memoryCapBytes) {
        final boolean devOrTest =
                NonceReplayGuardType.isDevOrTest(List.of(environment.getActiveProfiles()));
        return new DecisionDryRunRuntimeProperties(
                enabled,
                productionEnabled,
                killSwitchEnabled,
                devOrTest,
                splitCsv(allowedSources),
                splitCsv(allowedTenantSourcePairs),
                memoryCapBytes);
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
     * @param properties                dry-run runtime 配置。
     * @return dry-run usecase service。
     */
    @Bean
    @ConditionalOnMissingBean
    public DecisionDryRunService decisionDryRunService(
            final DecisionOrchestrator orchestrator,
            final DecisionAuditRepository auditRepository,
            final DecisionRequestRepository decisionRequestRepository,
            final DecisionRunRepository decisionRunRepository,
            final QuantSignalRepository quantSignalRepository,
            final QuantDecisionRepository quantDecisionRepository,
            final DecisionDryRunRuntimeProperties properties) {
        return new DefaultDecisionDryRunService(
                orchestrator,
                auditRepository,
                decisionRequestRepository,
                decisionRunRepository,
                quantSignalRepository,
                quantDecisionRepository,
                properties,
                Clock.systemUTC());
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

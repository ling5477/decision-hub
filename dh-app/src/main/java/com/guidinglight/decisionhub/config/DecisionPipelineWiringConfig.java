package com.guidinglight.decisionhub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.infra.jdbc.decision.JdbcDecisionAuditRepository;
import com.guidinglight.decisionhub.infra.jdbc.decision.JdbcDecisionReplayQueryRepository;
import com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcDecisionCoreRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionOrchestrator;
import com.guidinglight.decisionhub.usecase.decision.DecisionOutputAssembler;
import com.guidinglight.decisionhub.usecase.decision.DecisionProviderBudgetGuard;
import com.guidinglight.decisionhub.usecase.decision.DecisionProviderGuard;
import com.guidinglight.decisionhub.usecase.decision.DecisionProviderHealthEvaluator;
import com.guidinglight.decisionhub.usecase.decision.DecisionProviderLatencyRecorder;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQueryRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQueryService;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionContextBuilder;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionOrchestrator;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionPolicyChecker;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionProviderBudgetGuard;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionProviderGuard;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionProviderHealthEvaluator;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionProviderLatencyRecorder;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionReplayQueryService;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionRiskReviewer;
import com.guidinglight.decisionhub.usecase.decision.MockDecisionSignalProvider;
import com.guidinglight.decisionhub.usecase.qdr.DecisionRequestRepository;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DH Stage4 K3/K4 decision pipeline 装配。
 *
 * <p>本配置只接 DH 自身 JDBC 审计表、mock-only orchestrator 与 K4 replay read model；不新增 Controller、不暴露
 * API、不接真实 provider、不接 NQ runtime、不启用 LangGraph 或 LIVE。
 */
@Configuration
public class DecisionPipelineWiringConfig {

    /**
     * 装配 stage-qdr-1 Decision Core 主线 JDBC repository。
     *
     * @param jdbcTemplate DH 应用 datasource 的 JdbcTemplate。
     * @return 同时实现 request / run / signal / decision 四个 repository port 的 adapter。
     */
    @Bean
    @ConditionalOnMissingBean(DecisionRequestRepository.class)
    public JdbcDecisionCoreRepository decisionCoreRepository(final JdbcTemplate jdbcTemplate) {
        return new JdbcDecisionCoreRepository(jdbcTemplate, decisionPersistenceObjectMapper());
    }

    /**
     * 装配 DH-owned decision audit JDBC repository。
     *
     * @param jdbcTemplate DH 应用 datasource 的 JdbcTemplate。
     * @return decision audit repository。
     */
    @Bean
    @ConditionalOnMissingBean
    public DecisionAuditRepository decisionAuditRepository(final JdbcTemplate jdbcTemplate) {
        return new JdbcDecisionAuditRepository(jdbcTemplate, decisionPersistenceObjectMapper());
    }

    /**
     * 装配 DH-owned decision replay JDBC read repository。
     *
     * @param jdbcTemplate DH 应用 datasource 的 JdbcTemplate。
     * @return decision replay read repository。
     */
    @Bean
    @ConditionalOnMissingBean
    public DecisionReplayQueryRepository decisionReplayQueryRepository(
            final JdbcTemplate jdbcTemplate) {
        return new JdbcDecisionReplayQueryRepository(jdbcTemplate, decisionPersistenceObjectMapper());
    }

    /**
     * 装配 K4 replay read model 查询服务。
     *
     * @param decisionReplayQueryRepository 只读 replay repository port。
     * @return decision replay query service。
     */
    @Bean
    @ConditionalOnMissingBean
    public DecisionReplayQueryService decisionReplayQueryService(
            final DecisionReplayQueryRepository decisionReplayQueryRepository) {
        return new DefaultDecisionReplayQueryService(decisionReplayQueryRepository);
    }

    /**
     * 装配 K5 provider health evaluator。
     *
     * @return mock-only provider health evaluator。
     */
    @Bean
    @ConditionalOnMissingBean
    public DecisionProviderHealthEvaluator decisionProviderHealthEvaluator() {
        return new DefaultDecisionProviderHealthEvaluator();
    }

    /**
     * 装配 K5 provider budget guard。
     *
     * @return 本地固定阈值 budget guard；默认不接真实 provider 账单。
     */
    @Bean
    @ConditionalOnMissingBean
    public DecisionProviderBudgetGuard decisionProviderBudgetGuard() {
        return new DefaultDecisionProviderBudgetGuard();
    }

    /**
     * 装配 K5 provider latency recorder。
     *
     * @return 本地 latency recorder；默认 timeout 只影响 guard fail-closed。
     */
    @Bean
    @ConditionalOnMissingBean
    public DecisionProviderLatencyRecorder decisionProviderLatencyRecorder() {
        return new DefaultDecisionProviderLatencyRecorder();
    }

    /**
     * 装配 K5 provider guard。
     *
     * @param healthEvaluator provider health evaluator。
     * @param budgetGuard     provider budget guard。
     * @return mock-only provider guard。
     */
    @Bean
    @ConditionalOnMissingBean
    public DecisionProviderGuard decisionProviderGuard(
            final DecisionProviderHealthEvaluator healthEvaluator,
            final DecisionProviderBudgetGuard budgetGuard) {
        return new DefaultDecisionProviderGuard(healthEvaluator, budgetGuard);
    }

    /**
     * 装配 mock-only DecisionOrchestrator。
     *
     * @param decisionAuditRepository         K3 audit / snapshot / trace / output 持久化端口。
     * @param decisionProviderGuard           K5 provider guard。
     * @param decisionProviderLatencyRecorder K5 provider latency recorder。
     * @return 只读 decision orchestrator。
     */
    @Bean
    @ConditionalOnMissingBean
    public DecisionOrchestrator decisionOrchestrator(
            final DecisionAuditRepository decisionAuditRepository,
            final DecisionProviderGuard decisionProviderGuard,
            final DecisionProviderLatencyRecorder decisionProviderLatencyRecorder) {
        return new DefaultDecisionOrchestrator(
                new DefaultDecisionContextBuilder(),
                new DefaultDecisionPolicyChecker(),
                new MockDecisionSignalProvider(),
                new DefaultDecisionRiskReviewer(),
                new DecisionOutputAssembler(),
                decisionAuditRepository,
                decisionProviderGuard,
                decisionProviderLatencyRecorder,
                Clock.systemUTC());
    }

    private static ObjectMapper decisionPersistenceObjectMapper() {
        return new ObjectMapper();
    }
}

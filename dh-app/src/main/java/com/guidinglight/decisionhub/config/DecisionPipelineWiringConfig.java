package com.guidinglight.decisionhub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.infra.jdbc.decision.JdbcDecisionAuditRepository;
import com.guidinglight.decisionhub.infra.jdbc.decision.JdbcDecisionReplayQueryRepository;
import com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcDecisionCoreRepository;
import com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcDecisionReadModelQueryAdapter;
import com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcHumanApprovalPacketRepository;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalStatusTransitionPolicy;
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
import com.guidinglight.decisionhub.usecase.qdr.approval.ApprovalWriteBoundary;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketCommandService;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketRepository;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketService;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelQueryPort;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelService;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * DH Stage4 K3/K4 decision pipeline 装配。
 *
 * <p>本配置只接 DH 自身 JDBC 审计表、mock-only orchestrator、K4 replay read model 与 QDR B2
 * 只读 read model、B4 Human Approval Packet 内部 API command service；不接真实 provider、不接 NQ
 * runtime、不启用 LangGraph 或 LIVE。
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
     * 装配 stage-qdr-2 B2 read model tenant-bound JDBC query adapter。
     *
     * @param jdbcTemplate DH 应用 datasource 的 JdbcTemplate。
     * @return 只读 read model query port。
     */
    @Bean
    @ConditionalOnMissingBean
    public DecisionReadModelQueryPort decisionReadModelQueryPort(final JdbcTemplate jdbcTemplate) {
        return new JdbcDecisionReadModelQueryAdapter(jdbcTemplate);
    }

    /**
     * 装配 stage-qdr-2 B2 read model 查询服务。
     *
     * @param decisionReadModelQueryPort tenant-bound 只读查询 port。
     * @return read model query service。
     */
    @Bean
    @ConditionalOnMissingBean
    public DecisionReadModelService decisionReadModelService(
            final DecisionReadModelQueryPort decisionReadModelQueryPort) {
        return new DecisionReadModelService(decisionReadModelQueryPort);
    }

    /**
     * 装配 stage-qdr-2 B3/B4 human approval packet repository。
     *
     * @param jdbcTemplate DH 应用 datasource 的 JdbcTemplate。
     * @return tenant-bound approval repository。
     */
    @Bean
    @ConditionalOnMissingBean
    public HumanApprovalPacketRepository humanApprovalPacketRepository(
            final JdbcTemplate jdbcTemplate) {
        return new JdbcHumanApprovalPacketRepository(
                jdbcTemplate, decisionPersistenceObjectMapper());
    }

    /**
     * 装配 B3 approval packet domain service。
     *
     * @param humanApprovalPacketRepository tenant-bound approval repository。
     * @return approval packet service；内部强制状态机。
     */
    @Bean
    @ConditionalOnMissingBean
    public HumanApprovalPacketService humanApprovalPacketService(
            final HumanApprovalPacketRepository humanApprovalPacketRepository) {
        return new HumanApprovalPacketService(
                humanApprovalPacketRepository,
                new ApprovalStatusTransitionPolicy(),
                Clock.systemUTC());
    }

    /**
     * 装配 B4 approval 写入边界。
     *
     * <p>生产应用存在 Spring transaction manager 时，create / submit decision 的 approval write 与
     * audit write 会处在同一个事务内；audit 写失败抛出 RuntimeException 后回滚 approval 状态。若窄口
     * wiring 测试没有事务管理器，则回退为直通边界，避免测试误建数据库连接。
     *
     * @param transactionManagers Spring transaction manager provider。
     * @return approval 写入边界。
     */
    @Bean
    @ConditionalOnMissingBean
    public ApprovalWriteBoundary approvalWriteBoundary(
            final ObjectProvider<PlatformTransactionManager> transactionManagers) {
        final PlatformTransactionManager transactionManager = transactionManagers.getIfAvailable();
        if (transactionManager == null) {
            return ApprovalWriteBoundary.direct();
        }
        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return new ApprovalWriteBoundary() {
            @Override
            public <T> T execute(final ApprovalWriteBoundary.ApprovalWriteAction<T> action) {
                return transactionTemplate.execute(status -> action.get());
            }
        };
    }

    /**
     * 装配 B4 approval command service。
     *
     * @param humanApprovalPacketRepository tenant-bound approval repository。
     * @param humanApprovalPacketService    B3 状态机 service。
     * @param decisionReadModelService      B2 read model service，用于校验 decision_run 归属。
     * @param decisionAuditRepository       V5 audit event port；失败必须 fail-closed。
     * @param approvalWriteBoundary         approval write + audit write 事务边界。
     * @return approval command service。
     */
    @Bean
    @ConditionalOnMissingBean
    public HumanApprovalPacketCommandService humanApprovalPacketCommandService(
            final HumanApprovalPacketRepository humanApprovalPacketRepository,
            final HumanApprovalPacketService humanApprovalPacketService,
            final DecisionReadModelService decisionReadModelService,
            final DecisionAuditRepository decisionAuditRepository,
            final ApprovalWriteBoundary approvalWriteBoundary) {
        return new HumanApprovalPacketCommandService(
                humanApprovalPacketRepository,
                humanApprovalPacketService,
                decisionReadModelService,
                decisionAuditRepository,
                Clock.systemUTC(),
                approvalWriteBoundary);
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

package com.guidinglight.decisionhub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.infra.jdbc.decision.JdbcDecisionAuditRepository;
import com.guidinglight.decisionhub.infra.jdbc.decision.JdbcDecisionReplayQueryRepository;
import com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcDecisionCoreRepository;
import com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcDecisionReadModelQueryAdapter;
import com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcEvaluationCaseRepository;
import com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcHumanApprovalPacketRepository;
import com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcCanonicalReplaySnapshotRepository;
import com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcRegressionVerdictRepository;
import com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcReplayCaseRepository;
import com.guidinglight.decisionhub.infra.jdbc.qdr.ReplayInputSnapshotAssemblyService;
import com.guidinglight.decisionhub.infra.jdbc.qdr.feedback.JdbcFeedbackAttributionRepository;
import com.guidinglight.decisionhub.infra.jdbc.qdr.feedback.JdbcFeedbackPersistenceTransactionBoundary;
import com.guidinglight.decisionhub.infra.jdbc.qdr.feedback.JdbcFeedbackReferenceValidationAdapter;
import com.guidinglight.decisionhub.infra.jdbc.qdr.model.JdbcModelGatewayCallRepository;
import com.guidinglight.decisionhub.infra.jdbc.qdr.model.JdbcModelVersionRepository;
import com.guidinglight.decisionhub.infra.jdbc.qdr.model.JdbcPromptVersionRepository;
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
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceAggregateService;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackAttributionPersistenceService;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackAttributionRepository;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceTransactionBoundary;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackReferenceValidationPort;
import com.guidinglight.decisionhub.usecase.qdr.gateway.DefaultQdrMockModelGatewayBaseline;
import com.guidinglight.decisionhub.usecase.qdr.gateway.DefaultQdrModelGatewayIntegrationService;
import com.guidinglight.decisionhub.usecase.qdr.gateway.DeterministicProviderTrustPolicy;
import com.guidinglight.decisionhub.usecase.qdr.gateway.InMemoryProviderProfileRegistry;
import com.guidinglight.decisionhub.usecase.qdr.gateway.MockModelProvider;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayPort;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayService;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelProviderPort;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ObservabilityReportService;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderProfileRegistryPort;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderTrustPolicy;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessGuardService;
import com.guidinglight.decisionhub.usecase.qdr.gateway.QdrModelGatewayBaselinePort;
import com.guidinglight.decisionhub.usecase.qdr.gateway.QdrModelGatewayIntegrationPort;
import com.guidinglight.decisionhub.usecase.qdr.model.DeterministicPromptInjectionGuard;
import com.guidinglight.decisionhub.usecase.qdr.model.DeterministicPromptRenderPolicy;
import com.guidinglight.decisionhub.usecase.qdr.model.InMemoryModelVersionRegistry;
import com.guidinglight.decisionhub.usecase.qdr.model.InMemoryPromptVersionRegistry;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionRegistryPort;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptInjectionGuard;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptRenderPolicy;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionRegistryPort;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelQueryPort;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelService;
import com.guidinglight.decisionhub.usecase.qdr.replay.EvaluationCaseRepository;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionVerdictRepository;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayCaseRepository;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotAssembler;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotHasher;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.Qdr6CanonicalJson;

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
 * 只读 read model、B4 Human Approval Packet 内部 API command service，以及 stage-qdr-3 B4 mock
 * ModelGateway integration；不接真实 provider、不接 NQ runtime、不启用 LangGraph 或 LIVE。
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

    /** 装配现有 V9 replay case JDBC port；只增加内部 bean wiring，不改变 port 或 SQL。 */
    @Bean
    @ConditionalOnMissingBean
    public ReplayCaseRepository replayCaseRepository(final JdbcTemplate jdbcTemplate) {
        return new JdbcReplayCaseRepository(jdbcTemplate, decisionPersistenceObjectMapper());
    }

    /** 装配现有 V9 evaluation JDBC port；只增加内部 bean wiring。 */
    @Bean
    @ConditionalOnMissingBean
    public EvaluationCaseRepository evaluationCaseRepository(final JdbcTemplate jdbcTemplate) {
        return new JdbcEvaluationCaseRepository(jdbcTemplate, decisionPersistenceObjectMapper());
    }

    /** 装配现有 V9 regression verdict JDBC port；不新增查询能力。 */
    @Bean
    @ConditionalOnMissingBean
    public RegressionVerdictRepository regressionVerdictRepository(final JdbcTemplate jdbcTemplate) {
        return new JdbcRegressionVerdictRepository(jdbcTemplate);
    }

    /**
     * 装配 Stage-QDR-9 B2 feedback aggregate JDBC repository。
     *
     * @param jdbcTemplate DH-owned PostgreSQL datasource。
     * @return 仅支持 V15 aggregate write / exact scoped reload 的 repository。
     */
    @Bean
    @ConditionalOnMissingBean(FeedbackAttributionRepository.class)
    public FeedbackAttributionRepository feedbackAttributionRepository(final JdbcTemplate jdbcTemplate) {
        return new JdbcFeedbackAttributionRepository(jdbcTemplate);
    }

    /**
     * 装配 V15 reference 的 tenant-bound internal validator。
     *
     * @param jdbcTemplate DH-owned PostgreSQL datasource。
     * @return 不调用 HTTP、Provider、NQ 或外部 API 的 reference validator。
     */
    @Bean
    @ConditionalOnMissingBean(FeedbackReferenceValidationPort.class)
    public FeedbackReferenceValidationPort feedbackReferenceValidationPort(
            final JdbcTemplate jdbcTemplate) {
        return new JdbcFeedbackReferenceValidationAdapter(jdbcTemplate);
    }

    /**
     * 装配 B2 required/repeatable-read aggregate transaction boundary。
     *
     * @param transactionManager Spring transaction manager；缺失时应用 fail-fast。
     * @return 单 transaction boundary。
     */
    @Bean
    @ConditionalOnMissingBean(FeedbackPersistenceTransactionBoundary.class)
    public FeedbackPersistenceTransactionBoundary feedbackPersistenceTransactionBoundary(
            final PlatformTransactionManager transactionManager) {
        return new JdbcFeedbackPersistenceTransactionBoundary(transactionManager);
    }

    /**
     * 装配 internal-only B2 feedback persistence service。
     *
     * <p>该 bean 不创建 Controller、REST endpoint、runtime entry 或自动学习链路。
     */
    @Bean
    @ConditionalOnMissingBean
    public FeedbackAttributionPersistenceService feedbackAttributionPersistenceService(
            final FeedbackAttributionRepository feedbackAttributionRepository,
            final FeedbackReferenceValidationPort feedbackReferenceValidationPort,
            final FeedbackPersistenceTransactionBoundary feedbackPersistenceTransactionBoundary) {
        return new FeedbackAttributionPersistenceService(
                feedbackAttributionRepository,
                feedbackReferenceValidationPort,
                feedbackPersistenceTransactionBoundary);
    }

    /** 装配现有 V10 immutable snapshot persistence port；不扩展 insert/find 合同。 */
    @Bean
    @ConditionalOnMissingBean
    public CanonicalReplaySnapshotPersistencePort canonicalReplaySnapshotPersistencePort(
            final JdbcTemplate jdbcTemplate) {
        return new JdbcCanonicalReplaySnapshotRepository(
                jdbcTemplate, decisionPersistenceObjectMapper());
    }

    /** 装配 Stage-QDR-6 B2 evidence aggregate，供 P3 在 transaction 内重新读取 safe refs。 */
    @Bean
    @ConditionalOnMissingBean
    public DecisionEvidenceAggregateService decisionEvidenceAggregateService(
            final DecisionReplayQueryRepository replayQueryRepository,
            final DecisionReadModelQueryPort decisionReadModelQueryPort,
            final ReplayCaseRepository replayCaseRepository,
            final EvaluationCaseRepository evaluationCaseRepository,
            final RegressionVerdictRepository regressionVerdictRepository,
            final ModelGatewayCallPersistencePort gatewayCallPersistencePort) {
        return new DecisionEvidenceAggregateService(
                replayQueryRepository,
                decisionReadModelQueryPort,
                replayCaseRepository,
                evaluationCaseRepository,
                regressionVerdictRepository,
                gatewayCallPersistencePort,
                new ProviderReadinessGuardService(),
                new ObservabilityReportService());
    }

    /** 装配冻结的 QDR6-CJSON-1 encoder；不注册为 HTTP ObjectMapper。 */
    @Bean
    @ConditionalOnMissingBean
    public Qdr6CanonicalJson qdr6CanonicalJson() {
        return new Qdr6CanonicalJson();
    }

    /** 装配 canonical snapshot pure assembler。 */
    @Bean
    @ConditionalOnMissingBean
    public CanonicalReplaySnapshotAssembler canonicalReplaySnapshotAssembler(
            final Qdr6CanonicalJson canonicalJson) {
        return new CanonicalReplaySnapshotAssembler(canonicalJson);
    }

    /** 装配 domain-separated SHA-256 calculator。 */
    @Bean
    @ConditionalOnMissingBean
    public CanonicalReplaySnapshotHasher canonicalReplaySnapshotHasher(
            final Qdr6CanonicalJson canonicalJson) {
        return new CanonicalReplaySnapshotHasher(canonicalJson);
    }

    /**
     * 装配 P3 internal transaction service。
     *
     * <p>{@link PlatformTransactionManager} 为强制依赖；缺失会阻断 bean 创建，不允许 direct/default
     * isolation fallback。该 bean 没有 Controller/API 或外部 runtime 入口。
     */
    @Bean
    @ConditionalOnMissingBean
    public ReplayInputSnapshotAssemblyService replayInputSnapshotAssemblyService(
            final DecisionReplayQueryRepository replayQueryRepository,
            final DecisionReadModelQueryPort decisionReadModelQueryPort,
            final PromptVersionPersistencePort promptVersionPersistencePort,
            final ModelVersionPersistencePort modelVersionPersistencePort,
            final ModelGatewayCallPersistencePort gatewayCallPersistencePort,
            final ReplayCaseRepository replayCaseRepository,
            final EvaluationCaseRepository evaluationCaseRepository,
            final RegressionVerdictRepository regressionVerdictRepository,
            final DecisionEvidenceAggregateService evidenceAggregateService,
            final CanonicalReplaySnapshotPersistencePort snapshotPersistencePort,
            final CanonicalReplaySnapshotAssembler assembler,
            final CanonicalReplaySnapshotHasher hasher,
            final PlatformTransactionManager transactionManager) {
        return new ReplayInputSnapshotAssemblyService(
                replayQueryRepository,
                decisionReadModelQueryPort,
                promptVersionPersistencePort,
                modelVersionPersistencePort,
                gatewayCallPersistencePort,
                replayCaseRepository,
                evaluationCaseRepository,
                regressionVerdictRepository,
                evidenceAggregateService,
                snapshotPersistencePort,
                assembler,
                hasher,
                transactionManager);
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
     * 装配 stage-qdr-3 B1 prompt registry。
     *
     * <p>registry 仅驻留内存，由 B4 deterministic mock bootstrap 按 tenant 注册 prompt version；不访问
     * HTTP、不读取 secret、不代表 provider runtime。
     *
     * @return prompt version registry。
     */
    @Bean
    @ConditionalOnMissingBean
    public PromptVersionRegistryPort promptVersionRegistryPort() {
        return new InMemoryPromptVersionRegistry();
    }

    /**
     * 装配 stage-qdr-3 B2 model registry。
     *
     * @return model version registry。
     */
    @Bean
    @ConditionalOnMissingBean
    public ModelVersionRegistryPort modelVersionRegistryPort() {
        return new InMemoryModelVersionRegistry();
    }

    /**
     * 装配 B4 mock provider profile registry。
     *
     * @return provider profile registry；只保存 mock identity metadata。
     */
    @Bean
    @ConditionalOnMissingBean
    public ProviderProfileRegistryPort providerProfileRegistryPort() {
        return new InMemoryProviderProfileRegistry();
    }

    /**
     * 装配 B3 prompt version JDBC persistence port。
     *
     * @param jdbcTemplate DH datasource。
     * @return prompt version persistence port。
     */
    @Bean
    @ConditionalOnMissingBean
    public PromptVersionPersistencePort promptVersionPersistencePort(final JdbcTemplate jdbcTemplate) {
        return new JdbcPromptVersionRepository(jdbcTemplate);
    }

    /**
     * 装配 B3 model version JDBC persistence port。
     *
     * @param jdbcTemplate DH datasource。
     * @return model version persistence port。
     */
    @Bean
    @ConditionalOnMissingBean
    public ModelVersionPersistencePort modelVersionPersistencePort(final JdbcTemplate jdbcTemplate) {
        return new JdbcModelVersionRepository(jdbcTemplate);
    }

    /**
     * 装配 B3 model gateway call JDBC persistence port。
     *
     * @param jdbcTemplate DH datasource。
     * @return gateway call persistence port。
     */
    @Bean
    @ConditionalOnMissingBean
    public ModelGatewayCallPersistencePort modelGatewayCallPersistencePort(final JdbcTemplate jdbcTemplate) {
        return new JdbcModelGatewayCallRepository(jdbcTemplate);
    }

    /**
     * 装配 deterministic prompt injection guard。
     *
     * @return prompt injection guard。
     */
    @Bean
    @ConditionalOnMissingBean
    public PromptInjectionGuard promptInjectionGuard() {
        return new DeterministicPromptInjectionGuard();
    }

    /**
     * 装配 deterministic prompt render policy。
     *
     * @param promptInjectionGuard prompt injection guard。
     * @return prompt render policy。
     */
    @Bean
    @ConditionalOnMissingBean
    public PromptRenderPolicy promptRenderPolicy(final PromptInjectionGuard promptInjectionGuard) {
        return new DeterministicPromptRenderPolicy(promptInjectionGuard);
    }

    /**
     * 装配 B4 mock-only ProviderTrustPolicy。
     *
     * @param providerProfileRegistry provider profile registry。
     * @return provider trust policy。
     */
    @Bean
    @ConditionalOnMissingBean
    public ProviderTrustPolicy qdrProviderTrustPolicy(
            final ProviderProfileRegistryPort providerProfileRegistry) {
        return new DeterministicProviderTrustPolicy(providerProfileRegistry);
    }

    /**
     * 装配 deterministic mock model provider。
     *
     * @return mock provider；不发 HTTP、不读 credential、不访问 NQ。
     */
    @Bean
    @ConditionalOnMissingBean
    public ModelProviderPort modelProviderPort() {
        return new MockModelProvider();
    }

    /**
     * 装配 B2 ModelGatewayPort。
     *
     * @param promptVersionRegistry prompt registry。
     * @param modelVersionRegistry model registry。
     * @param promptRenderPolicy prompt render policy。
     * @param promptInjectionGuard prompt injection guard。
     * @param providerTrustPolicy mock-only provider trust policy。
     * @param modelProvider mock provider。
     * @return model gateway service。
     */
    @Bean
    @ConditionalOnMissingBean
    public ModelGatewayPort modelGatewayPort(
            final PromptVersionRegistryPort promptVersionRegistry,
            final ModelVersionRegistryPort modelVersionRegistry,
            final PromptRenderPolicy promptRenderPolicy,
            final PromptInjectionGuard promptInjectionGuard,
            final ProviderTrustPolicy providerTrustPolicy,
            final ModelProviderPort modelProvider) {
        return new ModelGatewayService(
                promptVersionRegistry,
                modelVersionRegistry,
                promptRenderPolicy,
                promptInjectionGuard,
                providerTrustPolicy,
                modelProvider);
    }

    /**
     * 装配 B4 deterministic mock baseline bootstrap。
     *
     * @param promptVersionRegistry prompt registry。
     * @param modelVersionRegistry model registry。
     * @param providerProfileRegistry provider profile registry。
     * @param promptVersionPersistence prompt persistence。
     * @param modelVersionPersistence model persistence。
     * @return mock baseline bootstrap port。
     */
    @Bean
    @ConditionalOnMissingBean
    public QdrModelGatewayBaselinePort qdrModelGatewayBaselinePort(
            final PromptVersionRegistryPort promptVersionRegistry,
            final ModelVersionRegistryPort modelVersionRegistry,
            final ProviderProfileRegistryPort providerProfileRegistry,
            final PromptVersionPersistencePort promptVersionPersistence,
            final ModelVersionPersistencePort modelVersionPersistence) {
        return new DefaultQdrMockModelGatewayBaseline(
                promptVersionRegistry,
                modelVersionRegistry,
                providerProfileRegistry,
                promptVersionPersistence,
                modelVersionPersistence,
                Clock.systemUTC());
    }

    /**
     * 装配 B4 QDR mock gateway integration。
     *
     * @param baselinePort mock baseline bootstrap。
     * @param modelGatewayPort model gateway port。
     * @param modelGatewayCallPersistencePort gateway call persistence。
     * @param decisionAuditRepository audit / trace repository。
     * @return QDR gateway integration port。
     */
    @Bean
    @ConditionalOnMissingBean
    public QdrModelGatewayIntegrationPort qdrModelGatewayIntegrationPort(
            final QdrModelGatewayBaselinePort baselinePort,
            final ModelGatewayPort modelGatewayPort,
            final ModelGatewayCallPersistencePort modelGatewayCallPersistencePort,
            final DecisionAuditRepository decisionAuditRepository) {
        return new DefaultQdrModelGatewayIntegrationService(
                baselinePort,
                modelGatewayPort,
                modelGatewayCallPersistencePort,
                decisionAuditRepository,
                Clock.systemUTC());
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

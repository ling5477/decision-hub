package com.guidinglight.decisionhub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionOrchestrator;
import com.guidinglight.decisionhub.usecase.decision.DecisionProviderBudgetGuard;
import com.guidinglight.decisionhub.usecase.decision.DecisionProviderGuard;
import com.guidinglight.decisionhub.usecase.decision.DecisionProviderHealthEvaluator;
import com.guidinglight.decisionhub.usecase.decision.DecisionProviderLatencyRecorder;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQueryRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQueryService;
import com.guidinglight.decisionhub.usecase.qdr.approval.ApprovalWriteBoundary;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketCommandService;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketRepository;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketService;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackAttributionPersistenceService;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackAttributionRepository;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceTransactionBoundary;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackReferenceValidationPort;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelQueryPort;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelService;
import com.guidinglight.decisionhub.infra.jdbc.qdr.ReplayInputSnapshotAssemblyService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * K3/K4/QDR B2 decision pipeline 装配回归测试。
 *
 * <p>防止 K3/K4 专用 JSON mapper 注册为第二个全局 {@link ObjectMapper} bean，导致 Spring WebMVC HTTP
 * message converter 在应用启动时出现 ObjectMapper 歧义。
 */
final class DecisionPipelineWiringConfigTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withBean(JdbcTemplate.class, () -> new JdbcTemplate(unusedDataSource()))
                    .withBean(
                            PlatformTransactionManager.class,
                            () -> mock(PlatformTransactionManager.class))
                    .withUserConfiguration(AgentRuntimeWiringConfig.class, DecisionPipelineWiringConfig.class);

    @Test
    void decisionPersistenceMapper_doesNotCompeteWithGlobalHttpObjectMapper() {
        runner.run(
                ctx -> {
                    assertThat(ctx).hasSingleBean(ObjectMapper.class);
                    assertThat(ctx).hasBean("nqFeedbackObjectMapper");
                    assertThat(ctx).doesNotHaveBean("decisionPersistenceObjectMapper");
                    assertThat(ctx).hasSingleBean(DecisionAuditRepository.class);
                    assertThat(ctx).hasSingleBean(DecisionReplayQueryRepository.class);
                    assertThat(ctx).hasSingleBean(DecisionReplayQueryService.class);
                    assertThat(ctx).hasSingleBean(DecisionReadModelQueryPort.class);
                    assertThat(ctx).hasSingleBean(DecisionReadModelService.class);
                    assertThat(ctx).hasSingleBean(ReplayInputSnapshotAssemblyService.class);
                    assertThat(ctx).hasSingleBean(HumanApprovalPacketRepository.class);
                    assertThat(ctx).hasSingleBean(HumanApprovalPacketService.class);
                    assertThat(ctx).hasSingleBean(ApprovalWriteBoundary.class);
                    assertThat(ctx).hasSingleBean(HumanApprovalPacketCommandService.class);
                    assertThat(ctx).hasSingleBean(FeedbackAttributionRepository.class);
                    assertThat(ctx).hasSingleBean(FeedbackReferenceValidationPort.class);
                    assertThat(ctx).hasSingleBean(FeedbackPersistenceTransactionBoundary.class);
                    assertThat(ctx).hasSingleBean(FeedbackAttributionPersistenceService.class);
                    assertThat(ctx).hasSingleBean(DecisionProviderHealthEvaluator.class);
                    assertThat(ctx).hasSingleBean(DecisionProviderBudgetGuard.class);
                    assertThat(ctx).hasSingleBean(DecisionProviderLatencyRecorder.class);
                    assertThat(ctx).hasSingleBean(DecisionProviderGuard.class);
                    assertThat(ctx).hasSingleBean(DecisionOrchestrator.class);
                });
    }

    @Test
    void canonicalSnapshotServiceFailsApplicationContextWhenTransactionManagerIsMissing() {
        new ApplicationContextRunner()
                .withBean(JdbcTemplate.class, () -> new JdbcTemplate(unusedDataSource()))
                .withUserConfiguration(
                        AgentRuntimeWiringConfig.class, DecisionPipelineWiringConfig.class)
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            assertThat(context.getStartupFailure())
                                    .hasMessageContaining("PlatformTransactionManager");
                        });
    }

    private static DriverManagerDataSource unusedDataSource() {
        final DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl("jdbc:postgresql://127.0.0.1:1/decision_hub_unused");
        dataSource.setUsername("decision_hub_test");
        dataSource.setPassword("decision_hub_test");
        return dataSource;
    }
}

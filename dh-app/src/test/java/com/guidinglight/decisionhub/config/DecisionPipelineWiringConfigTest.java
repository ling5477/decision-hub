package com.guidinglight.decisionhub.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionOrchestrator;
import com.guidinglight.decisionhub.usecase.decision.DecisionProviderBudgetGuard;
import com.guidinglight.decisionhub.usecase.decision.DecisionProviderGuard;
import com.guidinglight.decisionhub.usecase.decision.DecisionProviderHealthEvaluator;
import com.guidinglight.decisionhub.usecase.decision.DecisionProviderLatencyRecorder;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQueryRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * K3/K4 decision pipeline 装配回归测试。
 *
 * <p>防止 K3/K4 专用 JSON mapper 注册为第二个全局 {@link ObjectMapper} bean，导致 Spring WebMVC HTTP
 * message converter 在应用启动时出现 ObjectMapper 歧义。
 */
final class DecisionPipelineWiringConfigTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withBean(JdbcTemplate.class, () -> new JdbcTemplate(unusedDataSource()))
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
          assertThat(ctx).hasSingleBean(DecisionProviderHealthEvaluator.class);
          assertThat(ctx).hasSingleBean(DecisionProviderBudgetGuard.class);
          assertThat(ctx).hasSingleBean(DecisionProviderLatencyRecorder.class);
          assertThat(ctx).hasSingleBean(DecisionProviderGuard.class);
          assertThat(ctx).hasSingleBean(DecisionOrchestrator.class);
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

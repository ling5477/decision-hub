package com.guidinglight.decisionhub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.connector.research.ResearchSnapshotStore;
import com.guidinglight.decisionhub.connector.tools.ForecastArtifactStore;
import com.guidinglight.decisionhub.infra.jdbc.JdbcCheckpointEntryRepository;
import com.guidinglight.decisionhub.infra.jdbc.JdbcExternalMarketSnapshotRepository;
import com.guidinglight.decisionhub.infra.jdbc.JdbcForecastArtifactRepository;
import com.guidinglight.decisionhub.infra.jdbc.JdbcNqFeedbackEventRepository;
import com.guidinglight.decisionhub.infra.jdbc.JdbcReflectionEntryRepository;
import com.guidinglight.decisionhub.usecase.agent.CheckpointEntryRepository;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.ReflectionEntryRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Stage2-PoC-B5：JDBC 仓储装配。
 *
 * <p>默认禁用；通过 {@code decisionhub.stage2.jdbc.enabled=true} 启用，启用后会覆盖 {@link
 * AgentRuntimeWiringConfig} 中以 {@code @ConditionalOnMissingBean} 标注的 InMemory 仓储默认 bean。
 *
 * <p>装配范围：仅 Stage2 新增 / 替换的 5 个 JDBC 仓储。Stage1 现存的 6 个 InMemory（ResearchRun / AgentTask /
 * StrategyCandidate / JudgeDecision / AgentArtifact / Memory 5 个 Store）保持 InMemory，留待 Stage3 持久化。
 */
@Configuration
@ConditionalOnProperty(
    prefix = "decisionhub.stage2.jdbc",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
public class Stage2JdbcWiringConfig {

  @Bean
  public NqFeedbackEventRepository nqFeedbackEventRepository(
      final JdbcTemplate jdbcTemplate, final ObjectMapper nqFeedbackObjectMapper) {
    return new JdbcNqFeedbackEventRepository(jdbcTemplate, nqFeedbackObjectMapper);
  }

  @Bean
  public ReflectionEntryRepository reflectionEntryRepository(final JdbcTemplate jdbcTemplate) {
    return new JdbcReflectionEntryRepository(jdbcTemplate);
  }

  @Bean
  public CheckpointEntryRepository checkpointEntryRepository(final JdbcTemplate jdbcTemplate) {
    return new JdbcCheckpointEntryRepository(jdbcTemplate);
  }

  @Bean
  public ForecastArtifactStore forecastArtifactStore(
      final JdbcTemplate jdbcTemplate, final ObjectMapper nqFeedbackObjectMapper) {
    return new JdbcForecastArtifactRepository(jdbcTemplate, nqFeedbackObjectMapper);
  }

  @Bean
  public ResearchSnapshotStore researchSnapshotStore(
      final JdbcTemplate jdbcTemplate, final ObjectMapper nqFeedbackObjectMapper) {
    return new JdbcExternalMarketSnapshotRepository(jdbcTemplate, nqFeedbackObjectMapper);
  }
}

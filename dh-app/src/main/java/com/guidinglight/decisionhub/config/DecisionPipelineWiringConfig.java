package com.guidinglight.decisionhub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.infra.jdbc.decision.JdbcDecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionOrchestrator;
import com.guidinglight.decisionhub.usecase.decision.DecisionOutputAssembler;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionContextBuilder;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionOrchestrator;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionPolicyChecker;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionRiskReviewer;
import com.guidinglight.decisionhub.usecase.decision.MockDecisionSignalProvider;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * GateK K3 decision pipeline 装配。
 *
 * <p>本配置只接 DH 自身 JDBC 审计表与 mock-only orchestrator；不新增 Controller、不暴露 API、不接真实 provider、
 * 不接 NQ runtime、不启用 LangGraph 或 LIVE。
 */
@Configuration
public class DecisionPipelineWiringConfig {

  /** K3 持久化 JSON 序列化器，独立命名避免与既有 NQ feedback ObjectMapper 混淆。 */
  @Bean
  @ConditionalOnMissingBean(name = "decisionPersistenceObjectMapper")
  public ObjectMapper decisionPersistenceObjectMapper() {
    return new ObjectMapper();
  }

  /**
   * 装配 DH-owned decision audit JDBC repository。
   *
   * @param jdbcTemplate DH 应用 datasource 的 JdbcTemplate。
   * @param objectMapper K3 专用 JSON 序列化器。
   * @return decision audit repository。
   */
  @Bean
  @ConditionalOnMissingBean
  public DecisionAuditRepository decisionAuditRepository(
      final JdbcTemplate jdbcTemplate,
      @Qualifier("decisionPersistenceObjectMapper") final ObjectMapper objectMapper) {
    return new JdbcDecisionAuditRepository(jdbcTemplate, objectMapper);
  }

  /**
   * 装配 mock-only DecisionOrchestrator。
   *
   * @param decisionAuditRepository K3 audit / snapshot / trace / output 持久化端口。
   * @return 只读 decision orchestrator。
   */
  @Bean
  @ConditionalOnMissingBean
  public DecisionOrchestrator decisionOrchestrator(
      final DecisionAuditRepository decisionAuditRepository) {
    return new DefaultDecisionOrchestrator(
        new DefaultDecisionContextBuilder(),
        new DefaultDecisionPolicyChecker(),
        new MockDecisionSignalProvider(),
        new DefaultDecisionRiskReviewer(),
        new DecisionOutputAssembler(),
        decisionAuditRepository,
        Clock.systemUTC());
  }
}

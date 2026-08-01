package com.guidinglight.decisionhub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.NqIntegrationUseCase;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunRepository;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackContractValidator;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackEventHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackEventTypeRouter;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionService;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionUnitOfWork;
import com.guidinglight.decisionhub.usecase.agent.feedback.handler.BacktestResultReadyHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.handler.PaperRunAlertRaisedHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.handler.PaperRunCreatedHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.handler.PaperRunDailyReportGeneratedHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.handler.PaperRunRecoveryEventRecordedHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.handler.PaperRunStabilityCheckCompletedHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.handler.PaperRunStartedHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.handler.PaperRunStoppedHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.impl.DefaultNqFeedbackContractValidator;
import com.guidinglight.decisionhub.usecase.agent.feedback.impl.DefaultNqFeedbackEventTypeRouter;
import com.guidinglight.decisionhub.usecase.agent.feedback.impl.DefaultNqFeedbackIngestionService;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultNqIntegrationUseCase;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryNqFeedbackEventRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Legacy NQ feedback 的 ingest-only Spring 装配。
 *
 * <p>该配置只建立 validation、envelope/event append 与 routing 链路，不依赖或查找任何 mutable learning
 * service/store。显式 learning capability 继续由独立 wiring 保留，但不会进入 inbound dependency graph。
 */
@Configuration
public class FeedbackIngestionWiringConfig {

  /** 装配有界的 legacy feedback event repository。 */
  @Bean
  @ConditionalOnMissingBean(NqFeedbackEventRepository.class)
  public InMemoryNqFeedbackEventRepository nqFeedbackEventRepository(
      @Value("${decisionhub.security.nq-feedback.feedback-store.max-events:10000}")
          final int maxEvents,
      @Value("${decisionhub.security.nq-feedback.feedback-store.per-tenant-max-events:1000}")
          final int perTenantMaxEvents,
      @Value("${decisionhub.security.nq-feedback.feedback-store.retention-seconds:86400}")
          final long retentionSeconds) {
    return new InMemoryNqFeedbackEventRepository(
        maxEvents, perTenantMaxEvents, Duration.ofSeconds(retentionSeconds), Clock.systemUTC());
  }

  @Bean
  public ObjectMapper nqFeedbackObjectMapper() {
    return new ObjectMapper();
  }

  @Bean
  public NqFeedbackContractValidator nqFeedbackContractValidator(
      final ResearchRunRepository researchRunRepository,
      final ObjectMapper nqFeedbackObjectMapper) {
    return new DefaultNqFeedbackContractValidator(researchRunRepository, nqFeedbackObjectMapper);
  }

  @Bean
  public NqFeedbackEventHandler paperRunCreatedHandler(
      final NqFeedbackEventRepository repository, final ObjectMapper nqFeedbackObjectMapper) {
    return new PaperRunCreatedHandler(repository, nqFeedbackObjectMapper);
  }

  @Bean
  public NqFeedbackEventHandler paperRunStartedHandler(
      final NqFeedbackEventRepository repository, final ObjectMapper nqFeedbackObjectMapper) {
    return new PaperRunStartedHandler(repository, nqFeedbackObjectMapper);
  }

  @Bean
  public NqFeedbackEventHandler paperRunStoppedHandler(
      final NqFeedbackEventRepository repository, final ObjectMapper nqFeedbackObjectMapper) {
    return new PaperRunStoppedHandler(repository, nqFeedbackObjectMapper);
  }

  @Bean
  public NqFeedbackEventHandler paperRunDailyReportGeneratedHandler(
      final NqFeedbackEventRepository repository, final ObjectMapper nqFeedbackObjectMapper) {
    return new PaperRunDailyReportGeneratedHandler(repository, nqFeedbackObjectMapper);
  }

  @Bean
  public NqFeedbackEventHandler paperRunAlertRaisedHandler(
      final NqFeedbackEventRepository repository, final ObjectMapper nqFeedbackObjectMapper) {
    return new PaperRunAlertRaisedHandler(repository, nqFeedbackObjectMapper);
  }

  @Bean
  public NqFeedbackEventHandler paperRunRecoveryEventRecordedHandler(
      final NqFeedbackEventRepository repository, final ObjectMapper nqFeedbackObjectMapper) {
    return new PaperRunRecoveryEventRecordedHandler(repository, nqFeedbackObjectMapper);
  }

  @Bean
  public NqFeedbackEventHandler paperRunStabilityCheckCompletedHandler(
      final NqFeedbackEventRepository repository, final ObjectMapper nqFeedbackObjectMapper) {
    return new PaperRunStabilityCheckCompletedHandler(repository, nqFeedbackObjectMapper);
  }

  @Bean
  public NqFeedbackEventHandler backtestResultReadyHandler(
      final NqFeedbackEventRepository repository, final ObjectMapper nqFeedbackObjectMapper) {
    return new BacktestResultReadyHandler(repository, nqFeedbackObjectMapper);
  }

  @Bean
  public NqFeedbackEventTypeRouter nqFeedbackEventTypeRouter(
      final List<NqFeedbackEventHandler> handlers) {
    return new DefaultNqFeedbackEventTypeRouter(handlers);
  }

  @Bean
  public NqFeedbackIngestionService nqFeedbackIngestionService(
      final NqFeedbackContractValidator validator,
      final NqFeedbackEventRepository repository,
      final NqFeedbackEventTypeRouter router,
      final NqFeedbackIngestionUnitOfWork unitOfWork) {
    return new DefaultNqFeedbackIngestionService(validator, repository, router, unitOfWork);
  }

  @Bean
  public NqIntegrationUseCase nqIntegrationUseCase(
      final NqFeedbackEventRepository feedbackEventRepository) {
    return new DefaultNqIntegrationUseCase(feedbackEventRepository);
  }
}

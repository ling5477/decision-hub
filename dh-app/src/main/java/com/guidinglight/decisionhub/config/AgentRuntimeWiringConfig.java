package com.guidinglight.decisionhub.config;

import com.guidinglight.decisionhub.connector.nq.NqBacktestClient;
import com.guidinglight.decisionhub.connector.nq.NqContractVerifier;
import com.guidinglight.decisionhub.connector.nq.NqFeedbackClient;
import com.guidinglight.decisionhub.connector.nq.NqStrategyCandidateMapper;
import com.guidinglight.decisionhub.connector.nq.fake.DefaultNqContractVerifier;
import com.guidinglight.decisionhub.connector.nq.fake.DefaultNqStrategyCandidateMapper;
import com.guidinglight.decisionhub.connector.nq.fake.FakeNqBacktestClient;
import com.guidinglight.decisionhub.connector.nq.fake.FakeNqFeedbackClient;
import com.guidinglight.decisionhub.eval.agent.BacktestResultScorer;
import com.guidinglight.decisionhub.eval.agent.CandidateScorer;
import com.guidinglight.decisionhub.eval.agent.EvidenceQualityScorer;
import com.guidinglight.decisionhub.eval.agent.JudgeAggregator;
import com.guidinglight.decisionhub.eval.agent.RiskHeuristicScorer;
import com.guidinglight.decisionhub.eval.agent.rule.DefaultBacktestResultScorer;
import com.guidinglight.decisionhub.eval.agent.rule.DefaultCandidateScorer;
import com.guidinglight.decisionhub.eval.agent.rule.DefaultEvidenceQualityScorer;
import com.guidinglight.decisionhub.eval.agent.rule.DefaultJudgeAggregator;
import com.guidinglight.decisionhub.eval.agent.rule.DefaultRiskHeuristicScorer;
import com.guidinglight.decisionhub.memory.agent.ExperienceStore;
import com.guidinglight.decisionhub.memory.agent.FailureCaseStore;
import com.guidinglight.decisionhub.memory.agent.MarketRegimeMemory;
import com.guidinglight.decisionhub.memory.agent.PheromoneStore;
import com.guidinglight.decisionhub.memory.agent.StrategyPatternMemory;
import com.guidinglight.decisionhub.memory.agent.inmemory.InMemoryExperienceStore;
import com.guidinglight.decisionhub.memory.agent.inmemory.InMemoryFailureCaseStore;
import com.guidinglight.decisionhub.memory.agent.inmemory.InMemoryMarketRegimeMemory;
import com.guidinglight.decisionhub.memory.agent.inmemory.InMemoryPheromoneStore;
import com.guidinglight.decisionhub.memory.agent.inmemory.InMemoryStrategyPatternMemory;
import com.guidinglight.decisionhub.usecase.agent.AgentArtifactRepository;
import com.guidinglight.decisionhub.usecase.agent.AgentTaskPlanner;
import com.guidinglight.decisionhub.usecase.agent.AgentTaskRepository;
import com.guidinglight.decisionhub.usecase.agent.CandidateGenerationService;
import com.guidinglight.decisionhub.usecase.agent.CandidateReviewService;
import com.guidinglight.decisionhub.usecase.agent.ExperienceFeedbackService;
import com.guidinglight.decisionhub.usecase.agent.JudgeDecisionRepository;
import com.guidinglight.decisionhub.usecase.agent.JudgeDecisionService;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.NqIntegrationUseCase;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunCommandService;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunQueryService;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunRepository;
import com.guidinglight.decisionhub.usecase.agent.StrategyCandidateRepository;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackContractValidator;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackEventHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackEventTypeRouter;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionService;
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
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultAgentTaskPlanner;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultCandidateGenerationService;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultCandidateReviewService;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultExperienceFeedbackService;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultJudgeDecisionService;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultNqIntegrationUseCase;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultResearchRunCommandService;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultResearchRunQueryService;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryAgentArtifactRepository;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryAgentTaskRepository;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryJudgeDecisionRepository;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryNqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryResearchRunRepository;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryStrategyCandidateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Stage1：Agent 运行时骨架的 Spring 装配。
 *
 * <p>Stage1 默认绑定内存实现 / Fake 实现；Stage2 起可替换为 dh-infra 的 JDBC/Repository 实现。
 */
@Configuration
public class AgentRuntimeWiringConfig {

  @Bean
  public ResearchRunRepository researchRunRepository() {
    return new InMemoryResearchRunRepository();
  }

  @Bean
  public AgentTaskRepository agentTaskRepository() {
    return new InMemoryAgentTaskRepository();
  }

  @Bean
  public StrategyCandidateRepository strategyCandidateRepository() {
    return new InMemoryStrategyCandidateRepository();
  }

  @Bean
  public JudgeDecisionRepository judgeDecisionRepository() {
    return new InMemoryJudgeDecisionRepository();
  }

  @Bean
  public NqFeedbackEventRepository nqFeedbackEventRepository() {
    return new InMemoryNqFeedbackEventRepository();
  }

  @Bean
  public AgentArtifactRepository agentArtifactRepository() {
    return new InMemoryAgentArtifactRepository();
  }

  @Bean
  public ExperienceStore experienceStore() {
    return new InMemoryExperienceStore();
  }

  @Bean
  public PheromoneStore pheromoneStore() {
    return new InMemoryPheromoneStore();
  }

  @Bean
  public FailureCaseStore failureCaseStore() {
    return new InMemoryFailureCaseStore();
  }

  @Bean
  public MarketRegimeMemory marketRegimeMemory() {
    return new InMemoryMarketRegimeMemory();
  }

  @Bean
  public StrategyPatternMemory strategyPatternMemory() {
    return new InMemoryStrategyPatternMemory();
  }

  @Bean
  public CandidateScorer candidateScorer() {
    return new DefaultCandidateScorer();
  }

  @Bean
  public RiskHeuristicScorer riskHeuristicScorer() {
    return new DefaultRiskHeuristicScorer();
  }

  @Bean
  public EvidenceQualityScorer evidenceQualityScorer() {
    return new DefaultEvidenceQualityScorer();
  }

  @Bean
  public BacktestResultScorer backtestResultScorer() {
    return new DefaultBacktestResultScorer();
  }

  @Bean
  public JudgeAggregator judgeAggregator() {
    return new DefaultJudgeAggregator();
  }

  @Bean
  public NqBacktestClient nqBacktestClient() {
    return new FakeNqBacktestClient();
  }

  @Bean
  public NqFeedbackClient nqFeedbackClient() {
    return new FakeNqFeedbackClient();
  }

  @Bean
  public NqStrategyCandidateMapper nqStrategyCandidateMapper() {
    return new DefaultNqStrategyCandidateMapper();
  }

  @Bean
  public NqContractVerifier nqContractVerifier() {
    return new DefaultNqContractVerifier();
  }

  @Bean
  public AgentTaskPlanner agentTaskPlanner() {
    return new DefaultAgentTaskPlanner();
  }

  @Bean
  public CandidateGenerationService candidateGenerationService(
      final StrategyCandidateRepository candidateRepository) {
    return new DefaultCandidateGenerationService(candidateRepository);
  }

  @Bean
  public CandidateReviewService candidateReviewService(
      final StrategyCandidateRepository candidateRepository) {
    return new DefaultCandidateReviewService(candidateRepository);
  }

  @Bean
  public JudgeDecisionService judgeDecisionService(
      final StrategyCandidateRepository candidateRepository,
      final JudgeDecisionRepository judgeDecisionRepository) {
    return new DefaultJudgeDecisionService(candidateRepository, judgeDecisionRepository);
  }

  @Bean
  public ResearchRunCommandService researchRunCommandService(
      final ResearchRunRepository runRepository,
      final AgentTaskRepository taskRepository,
      final AgentTaskPlanner planner,
      final CandidateGenerationService candidateGenerationService,
      final CandidateReviewService candidateReviewService,
      final JudgeDecisionService judgeDecisionService) {
    return new DefaultResearchRunCommandService(
        runRepository,
        taskRepository,
        planner,
        candidateGenerationService,
        candidateReviewService,
        judgeDecisionService);
  }

  @Bean
  public ResearchRunQueryService researchRunQueryService(
      final ResearchRunRepository runRepository,
      final AgentTaskRepository taskRepository,
      final StrategyCandidateRepository candidateRepository,
      final JudgeDecisionRepository judgeDecisionRepository) {
    return new DefaultResearchRunQueryService(
        runRepository, taskRepository, candidateRepository, judgeDecisionRepository);
  }

  @Bean
  public ExperienceFeedbackService experienceFeedbackService(
      final ExperienceStore experienceStore,
      final PheromoneStore pheromoneStore,
      final FailureCaseStore failureCaseStore) {
    return new DefaultExperienceFeedbackService(experienceStore, pheromoneStore, failureCaseStore);
  }

  @Bean
  public NqIntegrationUseCase nqIntegrationUseCase(
      final NqFeedbackEventRepository feedbackEventRepository,
      final ExperienceFeedbackService experienceFeedbackService) {
    return new DefaultNqIntegrationUseCase(feedbackEventRepository, experienceFeedbackService);
  }

  // ===== Stage2-PoC-B2: NQ feedback ingestion =====

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
      final ExperienceFeedbackService experienceFeedbackService,
      final NqFeedbackEventRepository feedbackEventRepository,
      final ObjectMapper nqFeedbackObjectMapper) {
    return new PaperRunCreatedHandler(
        experienceFeedbackService, feedbackEventRepository, nqFeedbackObjectMapper);
  }

  @Bean
  public NqFeedbackEventHandler paperRunStartedHandler(
      final ExperienceFeedbackService experienceFeedbackService,
      final NqFeedbackEventRepository feedbackEventRepository,
      final ObjectMapper nqFeedbackObjectMapper) {
    return new PaperRunStartedHandler(
        experienceFeedbackService, feedbackEventRepository, nqFeedbackObjectMapper);
  }

  @Bean
  public NqFeedbackEventHandler paperRunStoppedHandler(
      final ExperienceFeedbackService experienceFeedbackService,
      final NqFeedbackEventRepository feedbackEventRepository,
      final ObjectMapper nqFeedbackObjectMapper) {
    return new PaperRunStoppedHandler(
        experienceFeedbackService, feedbackEventRepository, nqFeedbackObjectMapper);
  }

  @Bean
  public NqFeedbackEventHandler paperRunDailyReportGeneratedHandler(
      final ExperienceFeedbackService experienceFeedbackService,
      final NqFeedbackEventRepository feedbackEventRepository,
      final ObjectMapper nqFeedbackObjectMapper) {
    return new PaperRunDailyReportGeneratedHandler(
        experienceFeedbackService, feedbackEventRepository, nqFeedbackObjectMapper);
  }

  @Bean
  public NqFeedbackEventHandler paperRunAlertRaisedHandler(
      final ExperienceFeedbackService experienceFeedbackService,
      final NqFeedbackEventRepository feedbackEventRepository,
      final ObjectMapper nqFeedbackObjectMapper) {
    return new PaperRunAlertRaisedHandler(
        experienceFeedbackService, feedbackEventRepository, nqFeedbackObjectMapper);
  }

  @Bean
  public NqFeedbackEventHandler paperRunRecoveryEventRecordedHandler(
      final ExperienceFeedbackService experienceFeedbackService,
      final NqFeedbackEventRepository feedbackEventRepository,
      final ObjectMapper nqFeedbackObjectMapper) {
    return new PaperRunRecoveryEventRecordedHandler(
        experienceFeedbackService, feedbackEventRepository, nqFeedbackObjectMapper);
  }

  @Bean
  public NqFeedbackEventHandler paperRunStabilityCheckCompletedHandler(
      final ExperienceFeedbackService experienceFeedbackService,
      final NqFeedbackEventRepository feedbackEventRepository,
      final ObjectMapper nqFeedbackObjectMapper) {
    return new PaperRunStabilityCheckCompletedHandler(
        experienceFeedbackService, feedbackEventRepository, nqFeedbackObjectMapper);
  }

  @Bean
  public NqFeedbackEventHandler backtestResultReadyHandler(
      final ExperienceFeedbackService experienceFeedbackService,
      final NqFeedbackEventRepository feedbackEventRepository,
      final ObjectMapper nqFeedbackObjectMapper) {
    return new BacktestResultReadyHandler(
        experienceFeedbackService, feedbackEventRepository, nqFeedbackObjectMapper);
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
      final NqFeedbackEventTypeRouter router) {
    return new DefaultNqFeedbackIngestionService(validator, repository, router);
  }
}

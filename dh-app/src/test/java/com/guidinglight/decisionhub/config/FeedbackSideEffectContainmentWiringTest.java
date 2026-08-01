package com.guidinglight.decisionhub.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.guidinglight.decisionhub.api.IdempotencyFilter;
import com.guidinglight.decisionhub.api.feedback.NqFeedbackController;
import com.guidinglight.decisionhub.api.security.DhApiAuthenticationFilter;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.memory.agent.ExperienceStore;
import com.guidinglight.decisionhub.memory.agent.FailureCaseStore;
import com.guidinglight.decisionhub.memory.agent.PheromoneStore;
import com.guidinglight.decisionhub.security.nq.NqFeedbackAuthenticator;
import com.guidinglight.decisionhub.security.nq.RateLimiter;
import com.guidinglight.decisionhub.usecase.agent.ExperienceFeedbackService;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.NqIntegrationUseCase;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunRepository;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionCommand;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionOutcome;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackEventHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionService;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionUnitOfWork;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryResearchRunRepository;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Production wiring guard for the feedback ingest-only security boundary. */
@Testcontainers
@SpringBootTest(
    properties = {
      "spring.profiles.active=test",
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
    })
class FeedbackSideEffectContainmentWiringTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("decision_hub")
          .withUsername("decision_hub")
          .withPassword("decision_hub");

  @DynamicPropertySource
  static void postgresProperties(final DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private ApplicationContext context;
  @Autowired private NqFeedbackIngestionService ingestionService;
  @Autowired private NqFeedbackEventRepository eventRepository;
  @Autowired private NqFeedbackIngestionUnitOfWork unitOfWork;
  @Autowired private ResearchRunRepository researchRunRepository;
  @Autowired private ExperienceStore experienceStore;
  @Autowired private PheromoneStore pheromoneStore;
  @Autowired private FailureCaseStore failureCaseStore;

  @Test
  void productionContextKeepsSecurityFiltersAndEightHandlersWired() {
    assertThat(context.getBeansOfType(NqFeedbackEventHandler.class)).hasSize(8);
    assertThat(context.getBeansOfType(DhApiAuthenticationFilter.class)).hasSize(1);
    assertThat(context.getBeansOfType(IdempotencyFilter.class)).hasSize(1);
    assertThat(context.getBeansOfType(NqFeedbackAuthenticator.class)).hasSize(1);
    assertThat(context.getBeansOfType(RateLimiter.class)).isNotEmpty();
    assertThat(unitOfWork).isSameAs(eventRepository);
  }

  @Test
  void inboundControllerAndHandlerGraphContainsNoLearningCapability() {
    final ConfigurableListableBeanFactory beanFactory =
        (ConfigurableListableBeanFactory) context.getAutowireCapableBeanFactory();
    final String controllerBean = context.getBeanNamesForType(NqFeedbackController.class)[0];
    final Set<String> dependencies = transitiveDependencies(beanFactory, controllerBean);

    for (String dependency : dependencies) {
      final Class<?> dependencyType = context.getType(dependency);
      assertThat(isForbiddenLearningType(dependencyType))
          .as("inbound dependency %s must not be a learning capability", dependency)
          .isFalse();
    }

    for (NqFeedbackEventHandler handler : context.getBeansOfType(NqFeedbackEventHandler.class).values()) {
      Arrays.stream(handler.getClass().getConstructors())
          .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
          .forEach(
              parameterType ->
                  assertThat(isForbiddenLearningType(parameterType))
                      .as("handler constructor must be append-only: %s", handler.getClass())
                      .isFalse());
    }
  }

  @Test
  void feedbackIngestionConfigAssemblesWithoutLearningBeans() {
    new ApplicationContextRunner()
        .withBean(ResearchRunRepository.class, InMemoryResearchRunRepository::new)
        .withUserConfiguration(FeedbackIngestionWiringConfig.class)
        .run(
            isolated -> {
              assertThat(isolated).hasNotFailed();
              assertThat(isolated).hasSingleBean(NqFeedbackIngestionService.class);
              assertThat(isolated).hasSingleBean(NqFeedbackIngestionUnitOfWork.class);
              assertThat(isolated.getBean(NqFeedbackIngestionUnitOfWork.class))
                  .isSameAs(isolated.getBean(NqFeedbackEventRepository.class));
              assertThat(isolated).hasSingleBean(NqIntegrationUseCase.class);
              assertThat(isolated.getBeansOfType(NqFeedbackEventHandler.class)).hasSize(8);
              assertThat(isolated).doesNotHaveBean(ExperienceFeedbackService.class);
              assertThat(isolated).doesNotHaveBean(ExperienceStore.class);
              assertThat(isolated).doesNotHaveBean(PheromoneStore.class);
              assertThat(isolated).doesNotHaveBean(FailureCaseStore.class);
            });
  }

  @Test
  void acceptedAndRejectedIngressLeaveAllLearningStoresUntouched() {
    final String tenant = "tenant-feedback-containment";
    final Instant now = Instant.parse("2026-08-01T08:00:00Z");
    final ResearchRun run = ResearchRun.create(tenant, "containment", Map.of(), now);
    researchRunRepository.save(run);

    final int experienceBefore = experienceStore.listAll(tenant).size();
    final int pheromoneBefore = pheromoneStore.listByFrom(tenant, "unused-regime").size();
    final int failureBefore = failureCaseStore.listByRun(tenant, run.getRunId()).size();

    final IngestionCommand accepted = command(run, "evt-containment-accepted", "1.0.0", now);
    final IngestionCommand rejected = command(run, "evt-containment-rejected", "0.9.0", now);

    assertThat(ingestionService.ingest(accepted).getOutcome()).isEqualTo(IngestionOutcome.ACCEPTED);
    assertThat(ingestionService.ingest(rejected).getOutcome()).isEqualTo(IngestionOutcome.REJECTED);
    assertThat(eventRepository.listByRun(tenant, run.getRunId())).hasSize(1);
    assertThat(experienceStore.listAll(tenant)).hasSize(experienceBefore);
    assertThat(pheromoneStore.listByFrom(tenant, "unused-regime")).hasSize(pheromoneBefore);
    assertThat(failureCaseStore.listByRun(tenant, run.getRunId())).hasSize(failureBefore);
  }

  private static IngestionCommand command(
      final ResearchRun run, final String eventId, final String schemaVersion, final Instant now) {
    return IngestionCommand.of(
        run.getTenantId(),
        eventId,
        NqFeedbackEventType.PAPER_RUN_CREATED.name(),
        now,
        NqFeedbackEnvelope.SOURCE_SYSTEM_NEXUS_QUANT,
        "source-job-containment",
        run.getRunId(),
        "request-" + eventId,
        "correlation-" + eventId,
        schemaVersion,
        "{\"paperRunId\":\"pr-1\",\"candidateId\":\"cand-1\","
            + "\"strategyName\":\"S1\",\"requestedBy\":\"test\","
            + "\"createdAt\":\"2026-08-01T08:00:00Z\","
            + "\"rawPayloadJson\":\"{}\"}",
        now);
  }

  private static Set<String> transitiveDependencies(
      final ConfigurableListableBeanFactory beanFactory, final String root) {
    final Set<String> visited = new HashSet<>();
    final ArrayDeque<String> queue = new ArrayDeque<>();
    queue.add(root);
    while (!queue.isEmpty()) {
      final String current = queue.removeFirst();
      for (String dependency : beanFactory.getDependenciesForBean(current)) {
        if (visited.add(dependency)) {
          queue.addLast(dependency);
        }
      }
    }
    return visited;
  }

  private static boolean isForbiddenLearningType(final Class<?> type) {
    if (type == null) {
      return false;
    }
    return ExperienceFeedbackService.class.isAssignableFrom(type)
        || ExperienceStore.class.isAssignableFrom(type)
        || PheromoneStore.class.isAssignableFrom(type)
        || FailureCaseStore.class.isAssignableFrom(type);
  }
}

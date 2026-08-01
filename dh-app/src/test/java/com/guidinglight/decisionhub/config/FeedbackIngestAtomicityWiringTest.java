package com.guidinglight.decisionhub.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.guidinglight.decisionhub.infra.jdbc.JdbcNqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunRepository;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionService;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionUnitOfWork;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryResearchRunRepository;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/** Default/JDBC profile 都只能装配一个受 unit-of-work 保护的 ingestion root。 */
class FeedbackIngestAtomicityWiringTest {

  @Test
  void defaultProfileUsesSameInMemoryBeanForRepositoryAndUnitOfWork() {
    new ApplicationContextRunner()
        .withBean(ResearchRunRepository.class, InMemoryResearchRunRepository::new)
        .withUserConfiguration(FeedbackIngestionWiringConfig.class)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(NqFeedbackEventRepository.class);
              assertThat(context).hasSingleBean(NqFeedbackIngestionUnitOfWork.class);
              assertThat(context).hasSingleBean(NqFeedbackIngestionService.class);
              assertThat(context.getBean(NqFeedbackIngestionUnitOfWork.class))
                  .isSameAs(context.getBean(NqFeedbackEventRepository.class));
            });
  }

  @Test
  void jdbcProfileUsesSameJdbcBeanAndMatchingTransactionManager() {
    final DataSource dataSource =
        new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:1/not-used", "none", "none");
    new ApplicationContextRunner()
        .withPropertyValues("decisionhub.stage2.jdbc.enabled=true")
        .withBean(ResearchRunRepository.class, InMemoryResearchRunRepository::new)
        .withBean(DataSource.class, () -> dataSource)
        .withBean(JdbcTemplate.class, () -> new JdbcTemplate(dataSource))
        .withBean(
            PlatformTransactionManager.class,
            () -> new DataSourceTransactionManager(dataSource))
        .withUserConfiguration(
            Stage2JdbcWiringConfig.class, FeedbackIngestionWiringConfig.class)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(NqFeedbackEventRepository.class);
              assertThat(context).hasSingleBean(NqFeedbackIngestionUnitOfWork.class);
              assertThat(context).hasSingleBean(NqFeedbackIngestionService.class);
              assertThat(context.getBean(NqFeedbackEventRepository.class))
                  .isInstanceOf(JdbcNqFeedbackEventRepository.class)
                  .isSameAs(context.getBean(NqFeedbackIngestionUnitOfWork.class));
            });
  }
}

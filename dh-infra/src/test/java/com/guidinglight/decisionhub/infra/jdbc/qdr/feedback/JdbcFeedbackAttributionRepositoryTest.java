package com.guidinglight.decisionhub.infra.jdbc.qdr.feedback;

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionDimension;
import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionImpact;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackStatus;
import com.guidinglight.decisionhub.domain.qdr.feedback.ObservedDecisionOutcome;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeSource;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackAttributionErrorCode;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackAttributionPersistenceService;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackAttributionPersistenceService.PersistenceStatus;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackAttributionRepository;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceErrorCode;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.AttributionContributionRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.AttributionReferenceRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.FeedbackAttributionRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.FeedbackPersistenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.OutcomeObservationRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceStatus;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceType;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceTransactionBoundary;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackReferenceValidationPort;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** V15 feedback aggregate JDBC 的真实 PostgreSQL 事务、幂等、并发与重连回归。 */
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(OrderAnnotation.class)
class JdbcFeedbackAttributionRepositoryTest {

  private static final Instant OBSERVED_AT = Instant.parse("2026-07-25T09:59:00.123456Z");
  private static final Instant EVALUATION_TIME = Instant.parse("2026-07-25T10:00:00Z");
  private static final int WORKERS = 16;
  private static final int POSTGRES_PORT = findFreeLoopbackPort();

  @Container
  static final FixedPortPostgreSqlContainer POSTGRES =
      new FixedPortPostgreSqlContainer(POSTGRES_PORT)
          .withDatabaseName("decision_hub")
          .withUsername("decision_hub")
          .withPassword("decision_hub");

  private JdbcTemplate jdbc;
  private FeedbackAttributionRepository repository;
  private FeedbackPersistenceTransactionBoundary transactionBoundary;
  private FeedbackAttributionPersistenceService service;

  @BeforeEach
  void resetSchema() {
    configureDatabase();
    jdbc.execute("drop table if exists qdr_feedback_attribution_reference cascade");
    jdbc.execute("drop table if exists qdr_feedback_attribution_contribution cascade");
    jdbc.execute("drop table if exists qdr_feedback_attribution cascade");
    jdbc.execute("drop table if exists qdr_feedback_outcome_observation cascade");
    executeV15Migration();
  }

  @Test
  @Order(1)
  void persistsAllFourRelationsAndReloadsTheExactCompleteAggregate() {
    final FeedbackPersistenceAggregate aggregate =
        aggregate("tenant-a", FeedbackEnvironment.DEV, hash('a'), hash('b'));

    final var created = service.persist(aggregate);
    final var reloaded =
        repository
            .findByIdempotencyKey("tenant-a", FeedbackEnvironment.DEV, hash('a'))
            .orElseThrow();

    assertThat(created.status()).isEqualTo(PersistenceStatus.CREATED);
    assertThat(reloaded).isEqualTo(aggregate);
    assertThat(count("qdr_feedback_outcome_observation")).isOne();
    assertThat(count("qdr_feedback_attribution")).isOne();
    assertThat(count("qdr_feedback_attribution_contribution")).isOne();
    assertThat(count("qdr_feedback_attribution_reference")).isEqualTo(3);
    assertThat(reloaded.contributions()).extracting(AttributionContributionRecord::sortOrder).containsExactly(0);
    assertThat(reloaded.references()).extracting(AttributionReferenceRecord::referenceType)
        .containsExactly(ReferenceType.AUDIT, ReferenceType.REPLAY, ReferenceType.EVIDENCE);
  }

  @Test
  @Order(2)
  void reusesSameHashConflictsDifferentHashAndKeepsTenantAndEnvironmentIsolated() {
    final String idempotencyKey = hash('a');
    final FeedbackPersistenceAggregate original =
        aggregate("tenant-a", FeedbackEnvironment.DEV, idempotencyKey, hash('b'));

    assertThat(service.persist(original).status()).isEqualTo(PersistenceStatus.CREATED);
    assertThat(service.persist(original).status()).isEqualTo(PersistenceStatus.REUSED);
    assertThat(
            service
                .persist(aggregate("tenant-a", FeedbackEnvironment.DEV, idempotencyKey, hash('c')))
                .status())
        .isEqualTo(PersistenceStatus.IDEMPOTENCY_CONFLICT);
    assertThat(
            service
                .persist(aggregate("tenant-b", FeedbackEnvironment.DEV, idempotencyKey, hash('d')))
                .status())
        .isEqualTo(PersistenceStatus.CREATED);
    assertThat(
            service
                .persist(aggregate("tenant-a", FeedbackEnvironment.TEST, idempotencyKey, hash('e')))
                .status())
        .isEqualTo(PersistenceStatus.CREATED);

    assertThat(count("qdr_feedback_outcome_observation")).isEqualTo(3);
    assertThat(
            repository.findByIdempotencyKey("tenant-b", FeedbackEnvironment.DEV, idempotencyKey))
        .hasValueSatisfying(value -> assertThat(value.observation().canonicalHash()).isEqualTo(hash('d')));
    assertThat(
            repository.findByIdempotencyKey("tenant-a", FeedbackEnvironment.TEST, idempotencyKey))
        .hasValueSatisfying(value -> assertThat(value.observation().canonicalHash()).isEqualTo(hash('e')));
  }

  @Test
  @Order(3)
  void validationContributionAndReferenceFailuresRollbackTheEntireAggregate() {
    final FeedbackPersistenceAggregate aggregate =
        aggregate("tenant-a", FeedbackEnvironment.DEV, hash('a'), hash('b'));
    final FeedbackReferenceValidationPort invalidReference =
        (reference, observation) -> {
          throw new FeedbackPersistenceException(
              FeedbackPersistenceErrorCode.REFERENCE_INVALID, "controlled invalid reference");
        };
    final FeedbackAttributionPersistenceService validationFailure =
        new FeedbackAttributionPersistenceService(repository, invalidReference, transactionBoundary);

    assertThatThrownBy(() -> validationFailure.persist(aggregate))
        .isInstanceOf(FeedbackPersistenceException.class);
    assertAllAggregateTablesEmpty();

    assertThatThrownBy(
            () ->
                service(failingRepository(repository, FailureStage.CONTRIBUTION)).persist(aggregate))
        .isInstanceOf(FeedbackPersistenceException.class);
    assertAllAggregateTablesEmpty();

    assertThatThrownBy(
            () -> service(failingRepository(repository, FailureStage.REFERENCE)).persist(aggregate))
        .isInstanceOf(FeedbackPersistenceException.class);
    assertAllAggregateTablesEmpty();
  }

  @Test
  @Order(4)
  void databaseUniqueConstraintDeterminesAllConcurrentSameAndConflictingKeyOutcomes() throws Exception {
    final String idempotencyKey = hash('a');
    final FeedbackPersistenceAggregate sameInput =
        aggregate("tenant-a", FeedbackEnvironment.DEV, idempotencyKey, hash('b'));

    final List<FeedbackAttributionPersistenceService.PersistenceResult> sameResults =
        concurrently(WORKERS, () -> service.persist(sameInput));

    assertThat(sameResults).extracting(FeedbackAttributionPersistenceService.PersistenceResult::status)
        .containsOnly(PersistenceStatus.CREATED, PersistenceStatus.REUSED)
        .contains(PersistenceStatus.CREATED);
    assertThat(count("qdr_feedback_outcome_observation")).isOne();
    assertThat(count("qdr_feedback_attribution")).isOne();
    assertThat(count("qdr_feedback_attribution_contribution")).isOne();
    assertThat(count("qdr_feedback_attribution_reference")).isEqualTo(3);

    resetSchema();
    final List<FeedbackPersistenceAggregate> conflicting =
        new ArrayList<>();
    for (int index = 0; index < WORKERS; index++) {
      conflicting.add(
          aggregate(
              "tenant-a",
              FeedbackEnvironment.DEV,
              idempotencyKey,
              hash("0123456789abcdef".charAt(index))));
    }
    final AtomicInteger sequence = new AtomicInteger();
    final List<FeedbackAttributionPersistenceService.PersistenceResult> conflictResults =
        concurrently(WORKERS, () -> service.persist(conflicting.get(sequence.getAndIncrement())));

    assertThat(conflictResults).extracting(FeedbackAttributionPersistenceService.PersistenceResult::status)
        .containsExactlyInAnyOrder(
            PersistenceStatus.CREATED,
            PersistenceStatus.IDEMPOTENCY_CONFLICT,
            PersistenceStatus.IDEMPOTENCY_CONFLICT,
            PersistenceStatus.IDEMPOTENCY_CONFLICT,
            PersistenceStatus.IDEMPOTENCY_CONFLICT,
            PersistenceStatus.IDEMPOTENCY_CONFLICT,
            PersistenceStatus.IDEMPOTENCY_CONFLICT,
            PersistenceStatus.IDEMPOTENCY_CONFLICT,
            PersistenceStatus.IDEMPOTENCY_CONFLICT,
            PersistenceStatus.IDEMPOTENCY_CONFLICT,
            PersistenceStatus.IDEMPOTENCY_CONFLICT,
            PersistenceStatus.IDEMPOTENCY_CONFLICT,
            PersistenceStatus.IDEMPOTENCY_CONFLICT,
            PersistenceStatus.IDEMPOTENCY_CONFLICT,
            PersistenceStatus.IDEMPOTENCY_CONFLICT,
            PersistenceStatus.IDEMPOTENCY_CONFLICT);
    assertThat(count("qdr_feedback_outcome_observation")).isOne();
    assertThat(count("qdr_feedback_attribution")).isOne();
    assertThat(count("qdr_feedback_attribution_contribution")).isOne();
    assertThat(count("qdr_feedback_attribution_reference")).isEqualTo(3);
  }

  @Test
  @Order(5)
  void commitOutcomeUnknownIsFailClosedWithoutAnAutomaticRetry() {
    final JdbcFeedbackPersistenceTransactionBoundary boundary =
        new JdbcFeedbackPersistenceTransactionBoundary(new CommitUnknownTransactionManager());
    final AtomicInteger actions = new AtomicInteger();

    assertThatThrownBy(
            () ->
                boundary.requiredRepeatableRead(
                    () -> {
                      actions.incrementAndGet();
                      return "never-success";
                    }))
        .isInstanceOf(FeedbackPersistenceException.class)
        .extracting(error -> ((FeedbackPersistenceException) error).errorCode())
        .isEqualTo(FeedbackPersistenceErrorCode.COMMIT_OUTCOME_UNKNOWN);
    assertThat(actions).hasValue(1);
  }

  @Test
  @Order(6)
  void persistedAggregateSurvivesPostgreSqlRestartAndRemainsIdempotentlyReusable() throws Exception {
    final FeedbackPersistenceAggregate aggregate =
        aggregate("tenant-a", FeedbackEnvironment.DEV, hash('a'), hash('b'));

    assertThat(service.persist(aggregate).status()).isEqualTo(PersistenceStatus.CREATED);
    assertThat(POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT))
        .isEqualTo(POSTGRES_PORT);
    POSTGRES.getDockerClient().restartContainerCmd(POSTGRES.getContainerId()).withTimeout(10).exec();
    final var ready =
        POSTGRES.execInContainer(
            "bash",
            "-lc",
            "until pg_isready -U decision_hub -d decision_hub >/dev/null 2>&1; do sleep 0.1; done");
    assertThat(ready.getExitCode())
        .withFailMessage("postgres restart stdout=%s stderr=%s", ready.getStdout(), ready.getStderr())
        .isZero();
    configureDatabase();
    awaitDatabaseReady();

    final var reused = service.persist(aggregate);
    assertThat(reused.status()).isEqualTo(PersistenceStatus.REUSED);
    assertThat(reused.aggregate()).isEqualTo(aggregate);
    assertThat(count("qdr_feedback_outcome_observation")).isOne();
  }

  private void configureDatabase() {
    final DataSource dataSource =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    jdbc = new JdbcTemplate(dataSource);
    repository = new JdbcFeedbackAttributionRepository(jdbc);
    transactionBoundary = new JdbcFeedbackPersistenceTransactionBoundary(new DataSourceTransactionManager(dataSource));
    service = service(repository);
  }

  private void awaitDatabaseReady() throws InterruptedException {
    RuntimeException lastFailure = null;
    for (int attempt = 0; attempt < 50; attempt++) {
      try {
        jdbc.queryForObject("select 1", Integer.class);
        return;
      } catch (final RuntimeException error) {
        lastFailure = error;
        Thread.sleep(100);
      }
    }
    throw new IllegalStateException("PostgreSQL did not accept reconnect after restart", lastFailure);
  }

  private FeedbackAttributionPersistenceService service(
      final FeedbackAttributionRepository persistenceRepository) {
    return new FeedbackAttributionPersistenceService(
        persistenceRepository, (reference, observation) -> {}, transactionBoundary);
  }

  private void executeV15Migration() {
    final String sql;
    try {
      sql = Files.readString(migrationPath());
    } catch (final IOException error) {
      throw new IllegalStateException("cannot read V15 migration for JDBC integration test", error);
    }
    final var dataSource =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    try (var connection = dataSource.getConnection()) {
      ScriptUtils.executeSqlScript(connection, new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
    } catch (final Exception error) {
      throw new IllegalStateException("cannot apply V15 migration for JDBC integration test", error);
    }
  }

  private static Path migrationPath() {
    final List<Path> candidates =
        List.of(
            Path.of("dh-app", "src", "main", "resources", "db", "migration", "V15__qdr9_structured_feedback_persistence.sql"),
            Path.of("..", "dh-app", "src", "main", "resources", "db", "migration", "V15__qdr9_structured_feedback_persistence.sql"));
    return candidates.stream()
        .filter(Files::isRegularFile)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("V15 migration path is unavailable"));
  }

  private static int findFreeLoopbackPort() {
    try (ServerSocket socket = new ServerSocket()) {
      socket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
      return socket.getLocalPort();
    } catch (final IOException unavailable) {
      throw new ExceptionInInitializerError(unavailable);
    }
  }

  private int count(final String table) {
    return jdbc.queryForObject("select count(*) from " + table, Integer.class);
  }

  private void assertAllAggregateTablesEmpty() {
    assertThat(count("qdr_feedback_outcome_observation")).isZero();
    assertThat(count("qdr_feedback_attribution")).isZero();
    assertThat(count("qdr_feedback_attribution_contribution")).isZero();
    assertThat(count("qdr_feedback_attribution_reference")).isZero();
  }

  private static List<FeedbackAttributionPersistenceService.PersistenceResult> concurrently(
      final int workers,
      final Callable<FeedbackAttributionPersistenceService.PersistenceResult> action)
      throws Exception {
    final ExecutorService executor = Executors.newFixedThreadPool(workers);
    final CyclicBarrier start = new CyclicBarrier(workers);
    try {
      final List<Future<FeedbackAttributionPersistenceService.PersistenceResult>> futures =
          new ArrayList<>();
      for (int index = 0; index < workers; index++) {
        futures.add(executor.submit(() -> {
          start.await(30, TimeUnit.SECONDS);
          return action.call();
        }));
      }
      final List<FeedbackAttributionPersistenceService.PersistenceResult> results = new ArrayList<>();
      for (Future<FeedbackAttributionPersistenceService.PersistenceResult> future : futures) {
        results.add(future.get(30, TimeUnit.SECONDS));
      }
      return results;
    } finally {
      executor.shutdownNow();
    }
  }

  private static FeedbackAttributionRepository failingRepository(
      final FeedbackAttributionRepository delegate, final FailureStage failureStage) {
    return new FeedbackAttributionRepository() {
      @Override
      public java.util.Optional<FeedbackPersistenceAggregate> findByIdempotencyKey(
          final String tenantId,
          final FeedbackEnvironment environment,
          final String idempotencyKey) {
        return delegate.findByIdempotencyKey(tenantId, environment, idempotencyKey);
      }

      @Override
      public java.util.Optional<FeedbackPersistenceAggregate> findByAttributionId(
          final String tenantId,
          final FeedbackEnvironment environment,
          final String attributionId) {
        return delegate.findByAttributionId(tenantId, environment, attributionId);
      }

      @Override
      public void insertObservation(final OutcomeObservationRecord observation) {
        delegate.insertObservation(observation);
      }

      @Override
      public void insertAttribution(final FeedbackAttributionRecord attribution) {
        delegate.insertAttribution(attribution);
      }

      @Override
      public void insertContributions(final List<AttributionContributionRecord> contributions) {
        delegate.insertContributions(contributions);
        if (failureStage == FailureStage.CONTRIBUTION) {
          throw failure("controlled contribution persistence failure");
        }
      }

      @Override
      public void insertReferences(final List<AttributionReferenceRecord> references) {
        delegate.insertReferences(references);
        if (failureStage == FailureStage.REFERENCE) {
          throw failure("controlled reference persistence failure");
        }
      }
    };
  }

  private static FeedbackPersistenceException failure(final String message) {
    return new FeedbackPersistenceException(FeedbackPersistenceErrorCode.PERSISTENCE_FAILURE, message);
  }

  private static FeedbackPersistenceAggregate aggregate(
      final String tenantId,
      final FeedbackEnvironment environment,
      final String idempotencyKey,
      final String canonicalHash) {
    final String correlation = tenantId + environment + idempotencyKey + canonicalHash;
    final String attributionId = hash((char) ('d' + (canonicalHash.charAt(0) % 2)));
    final OutcomeObservationRecord observation =
        new OutcomeObservationRecord(
            uuid("observation-" + correlation),
            tenantId,
            environment,
            "decision-" + tenantId,
            "trace-" + tenantId,
            "observation-" + canonicalHash.charAt(0),
            idempotencyKey,
            OutcomeSource.DRY_RUN_RESULT,
            ObservedDecisionOutcome.SUCCEEDED,
            OBSERVED_AT,
            EVALUATION_TIME,
            canonicalHash);
    final FeedbackAttributionRecord attribution =
        new FeedbackAttributionRecord(
            uuid("attribution-" + correlation),
            tenantId,
            environment,
            observation.observationId(),
            observation.decisionId(),
            observation.traceId(),
            OBSERVED_AT,
            attributionId,
            "policy-1",
            "v1",
            FeedbackStatus.ATTRIBUTED,
            new BigDecimal("0.80000"),
            canonicalHash,
            FeedbackAttributionErrorCode.NONE);
    final AttributionContributionRecord contribution =
        new AttributionContributionRecord(
            uuid("contribution-" + correlation),
            tenantId,
            environment,
            attributionId,
            AttributionDimension.EVIDENCE_QUALITY,
            new BigDecimal("0.80000"),
            new BigDecimal("0.60000"),
            AttributionImpact.POSITIVE,
            new BigDecimal("0.80000"),
            "MEASUREMENT_POSITIVE",
            "safe-evidence",
            0);
    return new FeedbackPersistenceAggregate(
        observation,
        attribution,
        List.of(contribution),
        List.of(
            reference("audit-" + correlation, tenantId, environment, attributionId, ReferenceType.AUDIT, "audit:event-1"),
            reference(
                "replay-" + correlation,
                tenantId,
                environment,
                attributionId,
                ReferenceType.REPLAY,
                "replay-case:" + uuid("replay-" + correlation)),
            reference(
                "evidence-" + correlation,
                tenantId,
                environment,
                attributionId,
                ReferenceType.EVIDENCE,
                "evidence:safe-evidence")));
  }

  private static AttributionReferenceRecord reference(
      final String id,
      final String tenantId,
      final FeedbackEnvironment environment,
      final String attributionId,
      final ReferenceType type,
      final String value) {
    return new AttributionReferenceRecord(
        uuid(id), tenantId, environment, attributionId, type, value, ReferenceStatus.ACTIVE);
  }

  private static UUID uuid(final String value) {
    return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String hash(final char value) {
    return String.valueOf(value).repeat(64);
  }

  private enum FailureStage {
    CONTRIBUTION,
    REFERENCE
  }

  private static final class FixedPortPostgreSqlContainer
      extends PostgreSQLContainer<FixedPortPostgreSqlContainer> {

    private FixedPortPostgreSqlContainer(final int hostPort) {
      super(DockerImageName.parse("postgres:17"));
      addFixedExposedPort(hostPort, POSTGRESQL_PORT);
    }
  }

  private static final class CommitUnknownTransactionManager implements PlatformTransactionManager {

    @Override
    public TransactionStatus getTransaction(final TransactionDefinition definition) {
      return new SimpleTransactionStatus();
    }

    @Override
    public void commit(final TransactionStatus status) {
      throw new TransactionSystemException("controlled commit outcome unknown");
    }

    @Override
    public void rollback(final TransactionStatus status) {
      // No physical transaction exists in this deterministic boundary-only test double.
    }
  }
}

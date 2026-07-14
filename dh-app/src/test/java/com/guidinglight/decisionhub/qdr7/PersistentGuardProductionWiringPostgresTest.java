package com.guidinglight.decisionhub.qdr7;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.config.DecisionDryRunRuntimeWiringConfig;
import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import com.guidinglight.decisionhub.infra.jdbc.decision.JdbcDecisionAuditRepository;
import com.guidinglight.decisionhub.infra.jdbc.decision.JdbcDecisionReplayQueryRepository;
import com.guidinglight.decisionhub.infra.jdbc.qdr.guard.JdbcGuardCleanupAdapter;
import com.guidinglight.decisionhub.infra.jdbc.qdr.guard.JdbcIdempotencyGuardAdapter;
import com.guidinglight.decisionhub.infra.jdbc.qdr.guard.JdbcRateLimitAdmissionAdapter;
import com.guidinglight.decisionhub.security.nq.NonceReplayGuard;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventStatus;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventType;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionOrchestrator;
import com.guidinglight.decisionhub.usecase.decision.DecisionPersistenceRecords;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQueryRepository;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunCommand;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunContext;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunErrorCode;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunGuardProperties;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunRequestFingerprint;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunResult;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunRuntimeProperties;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunSafeResultProjector;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunService;
import com.guidinglight.decisionhub.usecase.decision.dryrun.PersistentGuardedDecisionDryRunService;
import com.guidinglight.decisionhub.usecase.qdr.DecisionRequestRepository;
import com.guidinglight.decisionhub.usecase.qdr.DecisionRunRepository;
import com.guidinglight.decisionhub.usecase.qdr.QuantDecisionRepository;
import com.guidinglight.decisionhub.usecase.qdr.QuantSignalRepository;
import com.guidinglight.decisionhub.usecase.qdr.gateway.QdrModelGatewayIntegrationPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.GuardCleanupCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.GuardCleanupPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.GuardTransactionBoundary;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyGuardPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyRecordView;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyState;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyTransitionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardHardCeilings;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardIdentity;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionStatus;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Stage-QDR-7 production-equivalent Spring/JDBC persistent guard safety evidence. */
@Testcontainers(disabledWithoutDocker = true)
class PersistentGuardProductionWiringPostgresTest {

  private static final String HASH_A = "a".repeat(64);
  private static final String HASH_B = "b".repeat(64);
  private static final String RESULT_TYPE = "DH_DECISION_OUTPUT";

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("decision_hub")
          .withUsername("decision_hub")
          .withPassword("decision_hub");

  private JdbcTemplate jdbc;

  @BeforeEach
  void resetDatabase() {
    flyway().clean();
    flyway().migrate();
    jdbc = new JdbcTemplate(dataSource());
  }

  @Test
  void completedDuplicateUsesActualSpringJdbcResultReferenceAndSafeProjection() {
    try (AnnotationConfigApplicationContext context = springContext(dataSource())) {
      final DecisionDryRunCommand command = command("completed");
      final String resultId = "result-completed";
      completeGuard(context, command, resultId);

      final DecisionDryRunResult result =
          context.getBean(DecisionDryRunService.class).execute(command);

      assertThat(result.success()).isTrue();
      assertThat(result.status()).isEqualTo(200);
      assertThat(result.snapshot().decisionId()).isEqualTo(resultId);
      assertThat(result.snapshot().traceSummary())
          .containsOnly("result:" + resultId, "idempotency:completed");
      assertThat(result.snapshot().reasons()).containsOnly("SAFE_REASON");
    }
  }

  @Test
  void frozenCeilingsCreateCompleteJdbcPersistentGuardRuntimeWiring() {
    try (AnnotationConfigApplicationContext context =
        springContext(
            dataSource(),
            Map.of(
                "decisionhub.integration1.runtime.guard.rate-window-seconds",
                Integer.toString(PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS),
                "decisionhub.integration1.runtime.guard.rate-limit-value",
                Integer.toString(PersistentGuardHardCeilings.MAX_RATE_QUOTA),
                "decisionhub.integration1.runtime.guard.lease-seconds",
                Integer.toString(PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS),
                "decisionhub.integration1.runtime.guard.idempotency-ttl-seconds",
                Integer.toString(PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS + 1)))) {
      final DecisionDryRunGuardProperties properties =
          context.getBean(DecisionDryRunGuardProperties.class);

      assertThat(properties.rateWindowSeconds())
          .isEqualTo(PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS);
      assertThat(properties.rateLimitValue()).isEqualTo(PersistentGuardHardCeilings.MAX_RATE_QUOTA);
      assertThat(properties.leaseDuration())
          .isEqualTo(Duration.ofSeconds(PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS));
      assertThat(context.getBean(RateLimitAdmissionPort.class))
          .isInstanceOf(JdbcRateLimitAdmissionAdapter.class);
      assertThat(context.getBean(IdempotencyGuardPort.class))
          .isInstanceOf(JdbcIdempotencyGuardAdapter.class);
      assertThat(context.getBean(GuardCleanupPort.class))
          .isInstanceOf(JdbcGuardCleanupAdapter.class);
      assertThat(context.getBean(GuardTransactionBoundary.class)).isNotNull();
      assertThat(context.getBean(NonceReplayGuard.class)).isNotNull();
      assertThat(context.getBean(DecisionDryRunService.class))
          .isInstanceOf(PersistentGuardedDecisionDryRunService.class);
      assertThat(context.getBean(DecisionDryRunRuntimeProperties.class).allowedSources())
          .containsExactly("NQ_DRYRUN");
    }
  }

  @Test
  void valuesAboveFrozenCeilingsFailContextBeforeRuntimeBeansAreCreated() {
    assertContextFailsClosed(
        Map.of(
            "decisionhub.integration1.runtime.guard.rate-window-seconds",
            Integer.toString(PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS + 1)));
    assertContextFailsClosed(
        Map.of(
            "decisionhub.integration1.runtime.guard.rate-limit-value",
            Integer.toString(PersistentGuardHardCeilings.MAX_RATE_QUOTA + 1)));
    assertContextFailsClosed(
        Map.of(
            "decisionhub.integration1.runtime.guard.lease-seconds",
            Integer.toString(PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS + 1)));
  }

  @Test
  void rateCommandMaximumReachesJdbcAndOverflowCannotMutatePostgres() {
    try (AnnotationConfigApplicationContext context = springContext(dataSource())) {
      final RateLimitAdmissionPort adapter = context.getBean(RateLimitAdmissionPort.class);
      final PersistentGuardIdentity identity =
          new PersistentGuardIdentity(
              "test",
              PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
              PersistentGuardIdentity.NQ_DRYRUN_SOURCE,
              "tenant-ceiling");

      assertThat(
              adapter
                  .tryAcquire(
                      new RateLimitAdmissionCommand(
                          identity,
                          PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS,
                          PersistentGuardHardCeilings.MAX_RATE_QUOTA))
                  .status())
          .isEqualTo(RateLimitAdmissionStatus.ACCEPTED);
      final Integer rowsBeforeOverflow =
          jdbc.queryForObject(
              "select count(*) from dh_qdr7_rate_limit_bucket where tenant_id = ?",
              Integer.class,
              "tenant-ceiling");

      assertThatThrownBy(
              () ->
                  adapter.tryAcquire(
                      new RateLimitAdmissionCommand(
                          identity,
                          PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS + 1,
                          PersistentGuardHardCeilings.MAX_RATE_QUOTA)))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(
              () ->
                  adapter.tryAcquire(
                      new RateLimitAdmissionCommand(
                          identity,
                          PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS,
                          PersistentGuardHardCeilings.MAX_RATE_QUOTA + 1)))
          .isInstanceOf(IllegalArgumentException.class);
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from dh_qdr7_rate_limit_bucket where tenant_id = ?",
                  Integer.class,
                  "tenant-ceiling"))
          .isEqualTo(rowsBeforeOverflow);
    }
  }

  @Test
  void actualJdbcDuplicateMapsMissingWrongTenantWrongTypeChecksumAndUnreadableToUnavailable() {
    assertUnavailableAfterDatabaseCorruption(
        "missing",
        jdbc -> {
          dropResultForeignKey(jdbc);
          jdbc.update("delete from dh_decision_output where decision_id = ?", "result-missing");
        });
    assertUnavailableAfterDatabaseCorruption(
        "wrong-tenant",
        jdbc -> {
          dropResultForeignKey(jdbc);
          deleteReplay(jdbc, "tenant-a", "result-wrong-tenant");
          seedReplay(jdbc, "tenant-b", "result-wrong-tenant", "request-wrong-tenant");
        });
    assertUnavailableAfterDatabaseCorruption(
        "wrong-type",
        jdbc -> {
          jdbc.execute(
              "alter table dh_qdr7_idempotency_guard"
                  + " drop constraint chk_dh_qdr7_idempotency_state_fields");
          jdbc.execute(
              "alter table dh_qdr7_idempotency_guard"
                  + " drop constraint chk_dh_qdr7_idempotency_result_type");
          jdbc.update(
              "update dh_qdr7_idempotency_guard set result_type = 'INVALID_RESULT_TYPE'"
                  + " where request_id = ?",
              "request-wrong-type");
        });
    assertUnavailableAfterDatabaseCorruption(
        "checksum",
        jdbc ->
            jdbc.update(
                "update dh_qdr7_idempotency_guard set result_checksum = ? where request_id = ?",
                HASH_B,
                "request-checksum"));
    assertUnavailableAfterDatabaseCorruption(
        "unreadable",
        jdbc ->
            jdbc.update(
                "delete from dh_decision_context_snapshot where decision_id = ?",
                "result-unreadable"));
  }

  @Test
  void productionSpringBoundaryUsesOneDataSourceAndRollsBackEveryCompletionFailure() {
    try (AnnotationConfigApplicationContext context = springContext(dataSource())) {
      final DataSource dataSource = context.getBean(DataSource.class);
      final JdbcTemplate contextJdbc = context.getBean(JdbcTemplate.class);
      final PlatformTransactionManager manager = context.getBean(PlatformTransactionManager.class);
      final GuardTransactionBoundary boundary = context.getBean(GuardTransactionBoundary.class);
      final IdempotencyGuardPort guards = context.getBean(IdempotencyGuardPort.class);
      final DecisionAuditRepository audit = context.getBean(DecisionAuditRepository.class);

      assertThat(manager).isInstanceOf(DataSourceTransactionManager.class);
      assertThat(((DataSourceTransactionManager) manager).getDataSource()).isSameAs(dataSource);
      boundary.required(
          () -> {
            assertThat(DataSourceUtils.getConnection(dataSource))
                .isSameAs(DataSourceUtils.getConnection(contextJdbc.getDataSource()));
            return Boolean.TRUE;
          });

      final IdempotencyRecordView outputFailure =
          startInProgress(boundary, guards, "output-failure");
      seedOutput(contextJdbc, "result-output-failure", "tenant-a", "request-output-failure");
      assertThatThrownBy(
              () ->
                  boundary.required(
                      () -> {
                        audit.saveOutput(
                            output("result-output-failure", "tenant-a", "request-output-failure"));
                        audit.saveAuditEvent(
                            audit("audit-output-failure", "result-output-failure"));
                        complete(guards, outputFailure, HASH_A);
                        return Boolean.TRUE;
                      }))
          .isInstanceOf(RuntimeException.class);
      assertInProgressAndNoAudit(contextJdbc, guards, outputFailure, "audit-output-failure");

      final IdempotencyRecordView auditFailure = startInProgress(boundary, guards, "audit-failure");
      audit.saveAuditEvent(audit("duplicate-audit", "seed-audit"));
      assertThatThrownBy(
              () ->
                  boundary.required(
                      () -> {
                        audit.saveOutput(
                            output("result-audit-failure", "tenant-a", "request-audit-failure"));
                        audit.saveAuditEvent(audit("duplicate-audit", "result-audit-failure"));
                        complete(guards, auditFailure, HASH_A);
                        return Boolean.TRUE;
                      }))
          .isInstanceOf(RuntimeException.class);
      assertThat(
              contextJdbc.queryForObject(
                  "select count(*) from dh_decision_output where decision_id = 'result-audit-failure'",
                  Integer.class))
          .isZero();
      assertInProgressAndNoAudit(contextJdbc, guards, auditFailure, "audit-audit-failure");

      final IdempotencyRecordView checksumFailure =
          startInProgress(boundary, guards, "checksum-failure");
      assertThatThrownBy(
              () ->
                  boundary.required(
                      () -> {
                        audit.saveOutput(
                            output(
                                "result-checksum-failure", "tenant-a", "request-checksum-failure"));
                        audit.saveAuditEvent(
                            audit("audit-checksum-failure", "result-checksum-failure"));
                        complete(guards, checksumFailure, "not-a-checksum");
                        return Boolean.TRUE;
                      }))
          .isInstanceOf(RuntimeException.class);
      assertThat(
              contextJdbc.queryForObject(
                  "select count(*) from dh_decision_output where decision_id = 'result-checksum-failure'",
                  Integer.class))
          .isZero();
      assertInProgressAndNoAudit(contextJdbc, guards, checksumFailure, "audit-checksum-failure");

      final IdempotencyRecordView casFailure = startInProgress(boundary, guards, "cas-failure");
      assertThatThrownBy(
              () ->
                  boundary.required(
                      () -> {
                        audit.saveOutput(
                            output("result-cas-failure", "tenant-a", "request-cas-failure"));
                        audit.saveAuditEvent(audit("audit-cas-failure", "result-cas-failure"));
                        completeWithVersion(
                            guards, casFailure, casFailure.stateVersion() + 1, HASH_A);
                        return Boolean.TRUE;
                      }))
          .isInstanceOf(RuntimeException.class);
      assertThat(
              contextJdbc.queryForObject(
                  "select count(*) from dh_decision_output where decision_id = 'result-cas-failure'",
                  Integer.class))
          .isZero();
      assertInProgressAndNoAudit(contextJdbc, guards, casFailure, "audit-cas-failure");
    }
  }

  @Test
  void realJdbcIdempotencyCommitUnknownDoesNotReadmitOrExecuteBusiness() {
    final AtomicBoolean failed = new AtomicBoolean();
    final DecisionDryRunCommand command = command("commit-unknown");
    final String requestHash = new DecisionDryRunRequestFingerprint().hash(command);
    try (AnnotationConfigApplicationContext uncertain =
        springContext(afterCommitFailureDataSource(dataSource(), failed))) {
      final DecisionDryRunResult result =
          uncertain.getBean(DecisionDryRunService.class).execute(command);
      assertThat(result.success()).isFalse();
      assertThat(result.status()).isEqualTo(503);
      assertThat(result.errorCode()).isEqualTo(DecisionDryRunErrorCode.IDEMPOTENCY_COMMIT_UNKNOWN);
    }

    final PersistentGuardIdentity identity = identity("tenant-a");
    final IdempotencyRecordView reconciled =
        new JdbcIdempotencyGuardAdapter(jdbc).findExact(identity, command.requestId(), requestHash);
    assertThat(reconciled.state()).isEqualTo(IdempotencyState.RECEIVED);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_qdr7_idempotency_guard where request_id = ?",
                Integer.class,
                command.requestId()))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_decision_output where request_id = ?",
                Integer.class,
                command.requestId()))
        .isZero();

    try (AnnotationConfigApplicationContext normal = springContext(dataSource())) {
      final DecisionDryRunResult retry =
          normal.getBean(DecisionDryRunService.class).execute(command);
      assertThat(retry.success()).isFalse();
      assertThat(retry.errorCode()).isEqualTo(DecisionDryRunErrorCode.IDEMPOTENCY_IN_PROGRESS);
    }
  }

  @Test
  void databaseClockIgnoresPlusAndMinusFortyEightHourCallerClocks() {
    try (AnnotationConfigApplicationContext context = springContext(dataSource())) {
      final GuardTransactionBoundary boundary = context.getBean(GuardTransactionBoundary.class);
      final IdempotencyGuardPort guards = context.getBean(IdempotencyGuardPort.class);
      final var limiter =
          context.getBean(
              "decisionDryRunRateLimiter",
              com.guidinglight.decisionhub.security.nq.RateLimiter.class);
      final Instant dbBefore = jdbc.queryForObject("select transaction_timestamp()", Instant.class);

      assertThat(
              limiter
                  .check(
                      "NQ_DRYRUN",
                      "tenant-plus",
                      PersistentDecisionDryRunRateLimiter.ROUTE,
                      dbBefore.plus(Duration.ofHours(48)),
                      "request-rate-plus",
                      "trace-rate-plus")
                  .allowed())
          .isTrue();
      assertThat(
              limiter
                  .check(
                      "NQ_DRYRUN",
                      "tenant-minus",
                      PersistentDecisionDryRunRateLimiter.ROUTE,
                      dbBefore.minus(Duration.ofHours(48)),
                      "request-rate-minus",
                      "trace-rate-minus")
                  .allowed())
          .isTrue();
      final Instant dbAfter = jdbc.queryForObject("select transaction_timestamp()", Instant.class);
      final List<Instant> windows =
          jdbc.query(
              "select window_start from dh_qdr7_rate_limit_bucket"
                  + " where tenant_id in ('tenant-plus','tenant-minus') order by tenant_id",
              (rs, row) -> rs.getTimestamp(1).toInstant());
      assertThat(windows)
          .allSatisfy(window -> assertThat(window).isBetween(dbBefore.minusSeconds(60), dbAfter));

      final DecisionDryRunCommand command = command("clock");
      final String hash = new DecisionDryRunRequestFingerprint().hash(command);
      final IdempotencyRecordView received =
          boundary.required(
              () ->
                  guards
                      .admit(
                          new IdempotencyAdmissionCommand(
                              identity("tenant-a"),
                              command.requestId(),
                              hash,
                              IdempotencyAdmissionCommand.HASH_VERSION,
                              Duration.ofMinutes(10),
                              Duration.ofHours(1)))
                      .record());
      final IdempotencyRecordView lease =
          boundary.required(
              () ->
                  guards.transition(
                      transition(
                          received,
                          IdempotencyState.RECEIVED,
                          received.stateVersion(),
                          null,
                          IdempotencyState.IN_PROGRESS,
                          "worker-clock",
                          UUID.randomUUID(),
                          Duration.ofMinutes(1),
                          null,
                          null,
                          null)));
      final IdempotencyRecordView heartbeat =
          boundary.required(
              () ->
                  guards.transition(
                      transition(
                          lease,
                          IdempotencyState.IN_PROGRESS,
                          lease.stateVersion(),
                          lease.leaseToken(),
                          IdempotencyState.IN_PROGRESS,
                          lease.leaseOwner(),
                          lease.leaseToken(),
                          Duration.ofMinutes(2),
                          null,
                          null,
                          null)));
      assertThat(received.expiresAt())
          .isBetween(dbBefore.plusSeconds(540), dbAfter.plusSeconds(660));
      assertThat(received.retentionUntil())
          .isBetween(dbBefore.plusSeconds(3_540), dbAfter.plusSeconds(3_660));
      assertThat(lease.leaseExpiresAt())
          .isBetween(dbBefore.plusSeconds(50), dbAfter.plusSeconds(70));
      assertThat(heartbeat.leaseExpiresAt())
          .isBetween(dbBefore.plusSeconds(110), dbAfter.plusSeconds(130));

      jdbc.update(
          "update dh_qdr7_idempotency_guard set created_at = transaction_timestamp()-interval '3 hour',"
              + " updated_at = transaction_timestamp()-interval '3 hour',"
              + " lease_expires_at = transaction_timestamp()-interval '1 second',"
              + " expires_at = transaction_timestamp()-interval '2 second',"
              + " retention_until = transaction_timestamp()-interval '1 second' where request_id = ?",
          command.requestId());
      final int expired =
          boundary.required(
              () ->
                  new JdbcGuardCleanupAdapter(context.getBean(JdbcTemplate.class))
                      .cleanupRetainedIdempotency(cleanupCommand("tenant-a")));
      assertThat(expired).isEqualTo(1);
      assertThat(
              new JdbcIdempotencyGuardAdapter(jdbc)
                  .findExact(identity("tenant-a"), command.requestId(), hash)
                  .state())
          .isEqualTo(IdempotencyState.EXPIRED);
    }
  }

  @Test
  void concurrentIdempotencyCleanupUsesSkipLockedAndLeavesLockedOrIneligibleRowsUnprocessed()
      throws Exception {
    try (AnnotationConfigApplicationContext context = springContext(dataSource())) {
      final GuardTransactionBoundary boundary = context.getBean(GuardTransactionBoundary.class);
      final JdbcTemplate contextJdbc = context.getBean(JdbcTemplate.class);
      seedEligibleFailed(contextJdbc, "cleanup-a");
      seedEligibleFailed(contextJdbc, "cleanup-b");
      seedEligibleFailed(contextJdbc, "cleanup-c");
      seedActiveLease(contextJdbc, "cleanup-active");
      seedRetentionFuture(contextJdbc, "cleanup-future");
      final ExecutorService workers = Executors.newFixedThreadPool(2);
      try {
        final List<Callable<Integer>> work =
            List.of(
                () ->
                    boundary.required(
                        () ->
                            new JdbcGuardCleanupAdapter(contextJdbc)
                                .cleanupRetainedIdempotency(cleanupCommand())),
                () ->
                    boundary.required(
                        () ->
                            new JdbcGuardCleanupAdapter(contextJdbc)
                                .cleanupRetainedIdempotency(cleanupCommand())));
        int transitioned = 0;
        for (final var future : workers.invokeAll(work)) {
          transitioned += future.get(10, TimeUnit.SECONDS);
        }
        assertThat(transitioned).isEqualTo(3);
      } finally {
        workers.shutdownNow();
      }
      assertThat(countState(contextJdbc, "EXPIRED")).isEqualTo(3);
      assertThat(state(contextJdbc, "cleanup-active")).isEqualTo("IN_PROGRESS");
      assertThat(state(contextJdbc, "cleanup-future")).isEqualTo("FAILED");
      assertThat(
              boundary.required(
                  () ->
                      new JdbcGuardCleanupAdapter(contextJdbc)
                          .cleanupRetainedIdempotency(cleanupCommand())))
          .isZero();

      seedEligibleFailed(contextJdbc, "cleanup-locked");
      try (Connection locked = dataSource().getConnection()) {
        locked.setAutoCommit(false);
        try (var statement =
            locked.prepareStatement(
                "select guard_id from dh_qdr7_idempotency_guard where request_id = ? for update")) {
          statement.setString(1, "cleanup-locked");
          statement.executeQuery();
          assertThat(
                  boundary.required(
                      () ->
                          new JdbcGuardCleanupAdapter(contextJdbc)
                              .cleanupRetainedIdempotency(cleanupCommand())))
              .isZero();
          assertThat(state(contextJdbc, "cleanup-locked")).isEqualTo("FAILED");
        }
        locked.commit();
      }
      assertThat(
              boundary.required(
                  () ->
                      new JdbcGuardCleanupAdapter(contextJdbc)
                          .cleanupRetainedIdempotency(cleanupCommand())))
          .isEqualTo(1);

      seedEligibleFailed(contextJdbc, "cleanup-rollback");
      assertThatThrownBy(
              () ->
                  boundary.required(
                      () -> {
                        new JdbcGuardCleanupAdapter(contextJdbc)
                            .cleanupRetainedIdempotency(cleanupCommand());
                        throw new IllegalStateException("deterministic cleanup worker rollback");
                      }))
          .isInstanceOf(IllegalStateException.class);
      assertThat(state(contextJdbc, "cleanup-rollback")).isEqualTo("FAILED");
      assertThat(
              boundary.required(
                  () ->
                      new JdbcGuardCleanupAdapter(contextJdbc)
                          .cleanupRetainedIdempotency(cleanupCommand())))
          .isEqualTo(1);
    }
  }

  @Test
  void cleanupIsTenantAndEnvironmentScoped() {
    try (AnnotationConfigApplicationContext context = springContext(dataSource())) {
      final GuardTransactionBoundary boundary = context.getBean(GuardTransactionBoundary.class);
      final JdbcTemplate contextJdbc = context.getBean(JdbcTemplate.class);
      seedEligibleFailed(contextJdbc, "cleanup-target");
      seedEligibleFailed(contextJdbc, "cleanup-other-tenant", "test", "tenant-other");
      seedEligibleFailed(contextJdbc, "cleanup-other-environment", "staging", "tenant-cleanup");

      assertThat(
              boundary.required(
                  () ->
                      new JdbcGuardCleanupAdapter(contextJdbc)
                          .cleanupRetainedIdempotency(cleanupCommand())))
          .isEqualTo(1);
      assertThat(state(contextJdbc, "cleanup-target")).isEqualTo("EXPIRED");
      assertThat(state(contextJdbc, "cleanup-other-tenant")).isEqualTo("FAILED");
      assertThat(state(contextJdbc, "cleanup-other-environment")).isEqualTo("FAILED");
    }
  }

  private void assertUnavailableAfterDatabaseCorruption(
      final String suffix, final java.util.function.Consumer<JdbcTemplate> corruption) {
    resetDatabase();
    try (AnnotationConfigApplicationContext context = springContext(dataSource())) {
      final DecisionDryRunCommand command = command(suffix);
      final String resultId = "result-" + suffix;
      completeGuard(context, command, resultId);
      corruption.accept(jdbc);
      final DecisionDryRunResult result =
          context.getBean(DecisionDryRunService.class).execute(command);
      assertThat(result.success()).isFalse();
      assertThat(result.status()).isEqualTo(503);
      assertThat(result.errorCode())
          .isEqualTo(DecisionDryRunErrorCode.IDEMPOTENCY_RESULT_UNAVAILABLE);
    }
  }

  private void completeGuard(
      final AnnotationConfigApplicationContext context,
      final DecisionDryRunCommand command,
      final String resultId) {
    final GuardTransactionBoundary boundary = context.getBean(GuardTransactionBoundary.class);
    final IdempotencyGuardPort guards = context.getBean(IdempotencyGuardPort.class);
    final String hash = context.getBean(DecisionDryRunRequestFingerprint.class).hash(command);
    seedReplay(jdbc, command.tenantId(), resultId, command.requestId());
    final IdempotencyRecordView received =
        boundary.required(
            () ->
                guards
                    .admit(
                        new IdempotencyAdmissionCommand(
                            identity(command.tenantId()),
                            command.requestId(),
                            hash,
                            IdempotencyAdmissionCommand.HASH_VERSION,
                            Duration.ofMinutes(10),
                            Duration.ofHours(1)))
                    .record());
    final UUID leaseToken = UUID.randomUUID();
    final IdempotencyRecordView lease =
        boundary.required(
            () ->
                guards.transition(
                    transition(
                        received,
                        IdempotencyState.RECEIVED,
                        received.stateVersion(),
                        null,
                        IdempotencyState.IN_PROGRESS,
                        "worker-completed",
                        leaseToken,
                        Duration.ofMinutes(1),
                        null,
                        null,
                        null)));
    final DecisionDryRunSafeResultProjector projector =
        context.getBean(DecisionDryRunSafeResultProjector.class);
    final String checksum = projector.checksum(projector.project(command.tenantId(), resultId));
    boundary.required(
        () -> {
          guards.transition(
              transition(
                  lease,
                  IdempotencyState.IN_PROGRESS,
                  lease.stateVersion(),
                  leaseToken,
                  IdempotencyState.COMPLETED,
                  null,
                  null,
                  null,
                  resultId,
                  checksum,
                  null));
          return Boolean.TRUE;
        });
  }

  private IdempotencyRecordView startInProgress(
      final GuardTransactionBoundary boundary,
      final IdempotencyGuardPort guards,
      final String suffix) {
    final DecisionDryRunCommand command = command(suffix);
    final String hash = new DecisionDryRunRequestFingerprint().hash(command);
    final IdempotencyRecordView received =
        boundary.required(
            () ->
                guards
                    .admit(
                        new IdempotencyAdmissionCommand(
                            identity("tenant-a"),
                            command.requestId(),
                            hash,
                            IdempotencyAdmissionCommand.HASH_VERSION,
                            Duration.ofMinutes(10),
                            Duration.ofHours(1)))
                    .record());
    return boundary.required(
        () ->
            guards.transition(
                transition(
                    received,
                    IdempotencyState.RECEIVED,
                    received.stateVersion(),
                    null,
                    IdempotencyState.IN_PROGRESS,
                    "worker-" + suffix,
                    UUID.randomUUID(),
                    Duration.ofMinutes(1),
                    null,
                    null,
                    null)));
  }

  private static IdempotencyRecordView complete(
      final IdempotencyGuardPort guards,
      final IdempotencyRecordView record,
      final String checksum) {
    return completeWithVersion(guards, record, record.stateVersion(), checksum);
  }

  private static IdempotencyRecordView completeWithVersion(
      final IdempotencyGuardPort guards,
      final IdempotencyRecordView record,
      final long expectedVersion,
      final String checksum) {
    return guards.transition(
        transition(
            record,
            IdempotencyState.IN_PROGRESS,
            expectedVersion,
            record.leaseToken(),
            IdempotencyState.COMPLETED,
            null,
            null,
            null,
            "result-" + record.requestId(),
            checksum,
            null));
  }

  private static IdempotencyTransitionCommand transition(
      final IdempotencyRecordView record,
      final IdempotencyState expectedState,
      final long expectedVersion,
      final UUID expectedToken,
      final IdempotencyState targetState,
      final String newOwner,
      final UUID newToken,
      final Duration leaseDuration,
      final String resultId,
      final String checksum,
      final String errorCode) {
    return new IdempotencyTransitionCommand(
        record.identity(),
        record.requestId(),
        record.requestHash(),
        expectedState,
        expectedVersion,
        expectedToken == null ? null : record.leaseOwner(),
        expectedToken,
        targetState,
        newOwner,
        newToken,
        leaseDuration,
        targetState == IdempotencyState.COMPLETED ? RESULT_TYPE : null,
        resultId,
        checksum,
        errorCode);
  }

  private static DecisionDryRunCommand command(final String suffix) {
    return new DecisionDryRunCommand(
        "request-" + suffix,
        "trace-" + suffix,
        "tenant-a",
        "NQ_DRYRUN",
        "2026-07-12T00:00:00Z",
        "nonce-" + suffix,
        "1",
        true,
        java.util.Set.of("PLACE_ORDER", "CANCEL_ORDER"),
        new DecisionDryRunContext(
            "BTC-USDT",
            "SPOT",
            "1h",
            "strategy-safe",
            "research-safe",
            "context-safe",
            "snapshot-safe",
            Instant.parse("2026-07-12T00:00:00Z"),
            List.of("evidence-safe"),
            128),
        false);
  }

  private static PersistentGuardIdentity identity(final String tenant) {
    return new PersistentGuardIdentity(
        "test",
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
        PersistentGuardIdentity.NQ_DRYRUN_SOURCE,
        tenant);
  }

  private static GuardCleanupCommand cleanupCommand() {
    return cleanupCommand("tenant-cleanup");
  }

  private static GuardCleanupCommand cleanupCommand(final String tenantId) {
    return new GuardCleanupCommand(
        "test",
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
        PersistentGuardIdentity.NQ_DRYRUN_SOURCE,
        tenantId,
        Duration.ofSeconds(1),
        10);
  }

  private static void seedReplay(
      final JdbcTemplate jdbc,
      final String tenant,
      final String decisionId,
      final String requestId) {
    jdbc.update(
        "insert into dh_decision_request"
            + " (decision_id,request_id,trace_id,tenant_id,source,decision_type,subject_json,context_ref,"
            + " requested_at,schema_version,created_at) values (?,?,? ,?,'NQ_DRYRUN','READ_ONLY_RECOMMENDATION',"
            + " '{}'::jsonb,'context-safe',transaction_timestamp(),'1',transaction_timestamp())",
        decisionId,
        requestId,
        "trace-" + decisionId,
        tenant);
    jdbc.update(
        "insert into dh_decision_context_snapshot"
            + " (decision_id,tenant_id,trace_id,context_snapshot_json,evidence_refs_json,created_at)"
            + " values (?,?,?,'{}'::jsonb,'[]'::jsonb,transaction_timestamp())",
        decisionId,
        tenant,
        "trace-" + decisionId);
    jdbc.update(
        "insert into dh_decision_trace_step"
            + " (id,decision_id,tenant_id,trace_id,step_name,step_status,started_at,ended_at,created_at)"
            + " values (?,?,?,?,'POLICY_CHECK','COMPLETED',transaction_timestamp(),transaction_timestamp(),"
            + " transaction_timestamp())",
        "step-" + decisionId,
        decisionId,
        tenant,
        "trace-" + decisionId);
    seedOutput(jdbc, decisionId, tenant, requestId);
    jdbc.update(
        "insert into dh_decision_audit_event"
            + " (id,decision_id,tenant_id,trace_id,event_type,event_status,event_json,created_at)"
            + " values (?,?,?,?,'DECISION_COMPLETED','SUCCESS','{}'::jsonb,transaction_timestamp())",
        "audit-" + decisionId,
        decisionId,
        tenant,
        "trace-" + decisionId);
  }

  private static void seedOutput(
      final JdbcTemplate jdbc,
      final String decisionId,
      final String tenant,
      final String requestId) {
    jdbc.update(
        "insert into dh_decision_output"
            + " (decision_id,tenant_id,trace_id,request_id,decision_type,action,risk_level,policy_status,"
            + " confidence,output_json,created_at) values (?,?,?,?,'READ_ONLY_RECOMMENDATION','NO_TRADE','LOW',"
            + " 'ALLOWED',0.5,'{\"reasonCodes\":[\"SAFE_REASON\"]}'::jsonb,transaction_timestamp())",
        decisionId,
        tenant,
        "trace-" + decisionId,
        requestId);
  }

  private static void deleteReplay(
      final JdbcTemplate jdbc, final String tenant, final String decisionId) {
    jdbc.update(
        "delete from dh_decision_audit_event where tenant_id = ? and decision_id = ?",
        tenant,
        decisionId);
    jdbc.update(
        "delete from dh_decision_trace_step where tenant_id = ? and decision_id = ?",
        tenant,
        decisionId);
    jdbc.update(
        "delete from dh_decision_context_snapshot where tenant_id = ? and decision_id = ?",
        tenant,
        decisionId);
    jdbc.update(
        "delete from dh_decision_output where tenant_id = ? and decision_id = ?",
        tenant,
        decisionId);
    jdbc.update(
        "delete from dh_decision_request where tenant_id = ? and decision_id = ?",
        tenant,
        decisionId);
  }

  private static DecisionPersistenceRecords.OutputRecord output(
      final String decisionId, final String tenant, final String requestId) {
    return new DecisionPersistenceRecords.OutputRecord(
        decisionId,
        tenant,
        "trace-" + decisionId,
        requestId,
        DecisionType.READ_ONLY_RECOMMENDATION,
        DecisionAction.NO_TRADE,
        DecisionRiskLevel.LOW,
        DecisionPolicyStatus.ALLOWED,
        new BigDecimal("0.5"),
        Map.of("reasonCodes", List.of("SAFE_REASON")),
        Instant.parse("2026-07-12T00:00:00Z"));
  }

  private static DecisionPersistenceRecords.AuditEventRecord audit(
      final String id, final String decisionId) {
    return new DecisionPersistenceRecords.AuditEventRecord(
        id,
        decisionId,
        "tenant-a",
        "trace-" + decisionId,
        DecisionAuditEventType.QDR7_IDEMPOTENCY_COMPLETED,
        DecisionAuditEventStatus.SUCCESS,
        Map.of("state", "COMPLETED"),
        null,
        Instant.parse("2026-07-12T00:00:00Z"));
  }

  private static void assertInProgressAndNoAudit(
      final JdbcTemplate jdbc,
      final IdempotencyGuardPort guards,
      final IdempotencyRecordView record,
      final String auditId) {
    assertThat(
            guards.findExact(record.identity(), record.requestId(), record.requestHash()).state())
        .isEqualTo(IdempotencyState.IN_PROGRESS);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_decision_audit_event where id = ?",
                Integer.class,
                auditId))
        .isZero();
  }

  private static void dropResultForeignKey(final JdbcTemplate jdbc) {
    jdbc.execute(
        "alter table dh_qdr7_idempotency_guard drop constraint fk_dh_qdr7_idempotency_result");
  }

  private static void seedEligibleFailed(final JdbcTemplate jdbc, final String requestId) {
    seedEligibleFailed(jdbc, requestId, "test", "tenant-cleanup");
  }

  private static void seedEligibleFailed(
      final JdbcTemplate jdbc,
      final String requestId,
      final String environment,
      final String tenantId) {
    jdbc.update(
        "insert into dh_qdr7_idempotency_guard"
            + " (guard_id,environment,endpoint,source,tenant_id,request_id,request_hash,hash_version,state,"
            + " state_version,stable_error_code,created_at,updated_at,failed_at,expires_at,retention_until)"
            + " values (?,?,?,'NQ_DRYRUN',?,?,?,'QDR7-DRYRUN-CJSON-1','FAILED',0,'SAFE_FAILURE',"
            + " transaction_timestamp()-interval '4 hour',"
            + " transaction_timestamp()-interval '4 hour',transaction_timestamp()-interval '3 hour',"
            + " transaction_timestamp()-interval '2 hour',transaction_timestamp()-interval '1 hour')",
        UUID.randomUUID(),
        environment,
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
        tenantId,
        requestId,
        HASH_A);
  }

  private static void seedActiveLease(final JdbcTemplate jdbc, final String requestId) {
    jdbc.update(
        "insert into dh_qdr7_idempotency_guard"
            + " (guard_id,environment,endpoint,source,tenant_id,request_id,request_hash,hash_version,state,"
            + " state_version,lease_owner,lease_token,created_at,updated_at,lease_expires_at,expires_at,retention_until)"
            + " values (?,'test',?,'NQ_DRYRUN','tenant-cleanup',?,'"
            + HASH_A
            + "',"
            + " 'QDR7-DRYRUN-CJSON-1','IN_PROGRESS',0,'worker-active',?,transaction_timestamp()-interval '4 hour',"
            + " transaction_timestamp()-interval '4 hour',transaction_timestamp()+interval '1 hour',"
            + " transaction_timestamp()-interval '2 hour',"
            + " transaction_timestamp()-interval '1 hour')",
        UUID.randomUUID(),
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
        requestId,
        UUID.randomUUID());
  }

  private static void seedRetentionFuture(final JdbcTemplate jdbc, final String requestId) {
    jdbc.update(
        "insert into dh_qdr7_idempotency_guard"
            + " (guard_id,environment,endpoint,source,tenant_id,request_id,request_hash,hash_version,state,"
            + " state_version,stable_error_code,created_at,updated_at,failed_at,expires_at,retention_until)"
            + " values (?,'test',?,'NQ_DRYRUN','tenant-cleanup',?,'"
            + HASH_A
            + "',"
            + " 'QDR7-DRYRUN-CJSON-1','FAILED',0,'SAFE_FAILURE',transaction_timestamp()-interval '4 hour',"
            + " transaction_timestamp()-interval '4 hour',transaction_timestamp()-interval '3 hour',"
            + " transaction_timestamp()-interval '2 hour',transaction_timestamp()+interval '1 hour')",
        UUID.randomUUID(),
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
        requestId);
  }

  private static int countState(final JdbcTemplate jdbc, final String state) {
    return jdbc.queryForObject(
        "select count(*) from dh_qdr7_idempotency_guard where state = ?", Integer.class, state);
  }

  private static String state(final JdbcTemplate jdbc, final String requestId) {
    return jdbc.queryForObject(
        "select state from dh_qdr7_idempotency_guard where request_id = ?",
        String.class,
        requestId);
  }

  private static DataSource afterCommitFailureDataSource(
      final DataSource delegate, final AtomicBoolean failed) {
    return (DataSource)
        Proxy.newProxyInstance(
            DataSource.class.getClassLoader(),
            new Class<?>[] {DataSource.class},
            (proxy, method, args) -> {
              try {
                final Object value = method.invoke(delegate, args);
                if ("getConnection".equals(method.getName())
                    && value instanceof Connection connection) {
                  return Proxy.newProxyInstance(
                      Connection.class.getClassLoader(),
                      new Class<?>[] {Connection.class},
                      (connectionProxy, connectionMethod, connectionArgs) -> {
                        try {
                          if ("commit".equals(connectionMethod.getName())
                              && !failed.getAndSet(true)) {
                            connection.commit();
                            throw new SQLException("deterministic after-commit connection failure");
                          }
                          return connectionMethod.invoke(connection, connectionArgs);
                        } catch (final InvocationTargetException error) {
                          throw error.getCause();
                        }
                      });
                }
                return value;
              } catch (final InvocationTargetException error) {
                throw error.getCause();
              }
            });
  }

  private static AnnotationConfigApplicationContext springContext(final DataSource dataSource) {
    return springContext(dataSource, Map.of());
  }

  private static AnnotationConfigApplicationContext springContext(
      final DataSource dataSource, final Map<String, Object> overrides) {
    final AnnotationConfigApplicationContext context =
        unrefreshedSpringContext(dataSource, overrides);
    context.refresh();
    return context;
  }

  private static AnnotationConfigApplicationContext unrefreshedSpringContext(
      final DataSource dataSource, final Map<String, Object> overrides) {
    final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.getEnvironment().setActiveProfiles("test");
    final Map<String, Object> properties = new HashMap<>();
    properties.put("decisionhub.integration1.runtime.enabled", "true");
    properties.put("decisionhub.integration1.runtime.guard.environment", "test");
    properties.put("decisionhub.integration1.runtime.guard.rate-window-seconds", "60");
    properties.put("decisionhub.integration1.runtime.guard.rate-limit-value", "20");
    properties.put("decisionhub.integration1.runtime.guard.lease-seconds", "300");
    properties.put("decisionhub.integration1.runtime.guard.idempotency-ttl-seconds", "600");
    properties.put("decisionhub.integration1.runtime.guard.retention-seconds", "3600");
    properties.put("decisionhub.integration1.runtime.allowed-sources", "NQ_DRYRUN");
    properties.put(
        "decisionhub.integration1.runtime.allowed-tenant-source-pairs", "tenant-a:NQ_DRYRUN");
    properties.putAll(overrides);
    context
        .getEnvironment()
        .getPropertySources()
        .addFirst(new MapPropertySource("qdr7-test-properties", properties));
    context.register(DecisionDryRunRuntimeWiringConfig.class, TestDependencies.class);
    context.registerBean(DataSource.class, () -> dataSource);
    context.registerBean(JdbcTemplate.class, () -> new JdbcTemplate(dataSource));
    context.registerBean(
        PlatformTransactionManager.class, () -> new DataSourceTransactionManager(dataSource));
    return context;
  }

  private static void assertContextFailsClosed(final Map<String, Object> overrides) {
    final AnnotationConfigApplicationContext context =
        unrefreshedSpringContext(dataSource(), overrides);
    try {
      assertThatThrownBy(context::refresh).hasRootCauseInstanceOf(IllegalArgumentException.class);
      assertThat(context.getBeanFactory().containsSingleton("decisionDryRunRateLimiter")).isFalse();
      assertThat(context.getBeanFactory().containsSingleton("decisionDryRunService")).isFalse();
    } finally {
      context.close();
    }
  }

  private static Flyway flyway() {
    return Flyway.configure()
        .cleanDisabled(false)
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("filesystem:src/main/resources/db/migration")
        .load();
  }

  private static DriverManagerDataSource dataSource() {
    return new DriverManagerDataSource(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  @Configuration(proxyBeanMethods = false)
  static class TestDependencies {

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    DecisionAuditRepository decisionAuditRepository(
        final JdbcTemplate jdbcTemplate, final ObjectMapper objectMapper) {
      return new JdbcDecisionAuditRepository(jdbcTemplate, objectMapper);
    }

    @Bean
    DecisionReplayQueryRepository decisionReplayQueryRepository(
        final JdbcTemplate jdbcTemplate, final ObjectMapper objectMapper) {
      return new JdbcDecisionReplayQueryRepository(jdbcTemplate, objectMapper);
    }

    @Bean
    DecisionOrchestrator decisionOrchestrator() {
      return mock(DecisionOrchestrator.class);
    }

    @Bean
    DecisionRequestRepository decisionRequestRepository() {
      return mock(DecisionRequestRepository.class);
    }

    @Bean
    DecisionRunRepository decisionRunRepository() {
      return mock(DecisionRunRepository.class);
    }

    @Bean
    QuantSignalRepository quantSignalRepository() {
      return mock(QuantSignalRepository.class);
    }

    @Bean
    QuantDecisionRepository quantDecisionRepository() {
      return mock(QuantDecisionRepository.class);
    }

    @Bean
    QdrModelGatewayIntegrationPort qdrModelGatewayIntegrationPort() {
      return mock(QdrModelGatewayIntegrationPort.class);
    }

    @Bean
    NonceReplayGuard nonceReplayGuard() {
      return mock(NonceReplayGuard.class);
    }
  }
}

package com.guidinglight.decisionhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import com.guidinglight.decisionhub.infra.jdbc.decision.JdbcDecisionAuditRepository;
import com.guidinglight.decisionhub.infra.jdbc.qdr.guard.JdbcGuardCleanupAdapter;
import com.guidinglight.decisionhub.infra.jdbc.qdr.guard.JdbcIdempotencyGuardAdapter;
import com.guidinglight.decisionhub.infra.jdbc.qdr.guard.JdbcRateLimitAdmissionAdapter;
import com.guidinglight.decisionhub.qdr7.PersistentDecisionDryRunRateLimiter;
import com.guidinglight.decisionhub.security.nq.RateLimitResult;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventStatus;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventType;
import com.guidinglight.decisionhub.usecase.decision.DecisionPersistenceRecords;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunGuardProperties;
import com.guidinglight.decisionhub.usecase.qdr.guard.GuardCleanupCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.GuardTransactionBoundary;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionStatus;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyRecordView;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyState;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyTransitionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardIdentity;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardStateException;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardStoreException;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionStatus;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Stage-QDR-7 V12→V14真实PostgreSQL 17/Flyway/并发/CAS/cleanup回归。
 *
 * <p>测试只连接一次性container，不调用HTTP、Provider、NQ、Agent/LangGraph、订单、账户、ledger、Paper或LIVE。
 */
@Testcontainers(disabledWithoutDocker = true)
class V12PersistentRuntimeGuardsFlywayPostgresTest {

  private static final String HASH_A = "a".repeat(64);
  private static final String HASH_B = "b".repeat(64);

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("decision_hub")
          .withUsername("decision_hub")
          .withPassword("decision_hub");

  private JdbcTemplate jdbc;
  private TransactionTemplate transaction;

  @BeforeEach
  void migrateFreshDatabase() {
    flyway(null).clean();
    flyway(null).migrate();
    final DriverManagerDataSource dataSource = dataSource();
    jdbc = new JdbcTemplate(dataSource);
    transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }

  @Test
  void cleanAndPaddedV12FailedRowsMigrateToV14WithoutChangingHistory() {
    assertThat(flyway(null).info().current().getVersion().getVersion()).isEqualTo("14");
    assertThat(tableExists("dh_qdr7_rate_limit_bucket")).isTrue();
    assertThat(tableExists("dh_qdr7_idempotency_guard")).isTrue();

    flyway(null).clean();
    final Flyway v11 = flyway("12");
    v11.migrate();
    final Map<String, Integer> checksumsBefore = checksums(v11.info().applied());
    jdbc = new JdbcTemplate(dataSource());
    jdbc.update(
        "insert into dh_qdr7_idempotency_guard"
            + " (guard_id,environment,endpoint,source,tenant_id,request_id,request_hash,hash_version,state,"
            + "state_version,stable_error_code,completed_at,expires_at,retention_until)"
            + " values (?,'test',?,'NQ_DRYRUN','tenant-upgrade','request-upgrade',?,'QDR7-DRYRUN-CJSON-1',"
            + "'FAILED',1,' SAFE_FAILURE ',transaction_timestamp(),transaction_timestamp()+interval '1 hour',"
            + "transaction_timestamp()+interval '2 hour')",
        UUID.randomUUID(),
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
        HASH_A);

    final Flyway v12 = flyway(null);
    v12.migrate();

    assertThat(v12.info().current().getVersion().getVersion()).isEqualTo("14");
    final Map<String, Integer> checksumsAfter = checksums(v12.info().applied());
    checksumsBefore.forEach(
        (version, checksum) -> assertThat(checksumsAfter.get(version)).isEqualTo(checksum));
    assertThat(
            jdbc.queryForMap(
                "select stable_error_code,completed_at,failed_at,expired_at"
                    + " from dh_qdr7_idempotency_guard where request_id='request-upgrade'"))
        .containsEntry("stable_error_code", "SAFE_FAILURE")
        .containsEntry("completed_at", null)
        .containsEntry("expired_at", null);
    assertThat(
            jdbc.queryForObject(
                "select data_type || ':' || character_maximum_length"
                    + " from information_schema.columns"
                    + " where table_schema='public' and table_name='dh_qdr7_idempotency_guard'"
                    + " and column_name='result_type'",
                String.class))
        .isEqualTo("character varying:32");
  }

  @Test
  void v13DatabaseMigratesToV14AndRejectsOversizedResultType() {
    flyway(null).clean();
    final Flyway v13 = flyway("13");
    v13.migrate();
    assertThat(v13.info().current().getVersion().getVersion()).isEqualTo("13");
    final Map<String, Integer> checksumsBeforeV14 = checksums(v13.info().applied());

    seedDecisionOutput("tenant-v13", "result-v13");
    final Flyway v14 = flyway(null);
    v14.migrate();
    assertThat(v14.info().current().getVersion().getVersion()).isEqualTo("14");
    final Map<String, Integer> checksumsAfterV14 = checksums(v14.info().applied());
    checksumsBeforeV14.forEach(
        (version, checksum) -> assertThat(checksumsAfterV14.get(version)).isEqualTo(checksum));

    flyway(null).clean();
    flyway("13").migrate();
    jdbc = new JdbcTemplate(dataSource());
    seedDecisionOutput("tenant-overflow", "result-overflow");
    jdbc.execute(
        "alter table dh_qdr7_idempotency_guard"
            + " drop constraint chk_dh_qdr7_idempotency_state_fields");
    jdbc.execute(
        "alter table dh_qdr7_idempotency_guard"
            + " drop constraint chk_dh_qdr7_idempotency_result_type");
    jdbc.update(
        "insert into dh_qdr7_idempotency_guard"
            + " (guard_id,environment,endpoint,source,tenant_id,request_id,request_hash,hash_version,state,"
            + " state_version,result_type,result_id,result_checksum,completed_at,expires_at,retention_until)"
            + " values (?,'test',?,'NQ_DRYRUN','tenant-overflow','request-overflow',?,'QDR7-DRYRUN-CJSON-1',"
            + " 'COMPLETED',1,?,'result-overflow',?,transaction_timestamp(),"
            + " transaction_timestamp()+interval '1 hour',transaction_timestamp()+interval '2 hour')",
        UUID.randomUUID(),
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
        HASH_A,
        "x".repeat(33),
        HASH_B);
    assertThatThrownBy(() -> flyway(null).migrate())
        .isInstanceOf(org.flywaydb.core.api.FlywayException.class);
  }

  @Test
  void preV13CompatibilityRejectsBlankFailedErrorCodeWithoutDefaulting() {
    flyway(null).clean();
    flyway("12").migrate();
    jdbc = new JdbcTemplate(dataSource());
    jdbc.execute(
        "alter table dh_qdr7_idempotency_guard"
            + " drop constraint chk_dh_qdr7_idempotency_state_fields");
    jdbc.update(
        "insert into dh_qdr7_idempotency_guard"
            + " (guard_id,environment,endpoint,source,tenant_id,request_id,request_hash,hash_version,state,"
            + " state_version,stable_error_code,completed_at,expires_at,retention_until)"
            + " values (?,'test',?,'NQ_DRYRUN','tenant-invalid','request-invalid-failed',?,'QDR7-DRYRUN-CJSON-1',"
            + " 'FAILED',1,'   ',transaction_timestamp(),transaction_timestamp()+interval '1 hour',"
            + " transaction_timestamp()+interval '2 hour')",
        UUID.randomUUID(),
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
        HASH_A);
    assertThatThrownBy(() -> flyway(null).migrate())
        .isInstanceOf(org.flywaydb.core.api.FlywayException.class);
  }

  @Test
  void fixedWindowAdmissionHasExactConcurrentWinnerCountAndIsolation() throws Exception {
    final PersistentGuardIdentity identity = identity("tenant-rate");
    final int limit = 10;
    final var executor = Executors.newFixedThreadPool(8);
    try {
      final Callable<Boolean> attempt =
          () ->
              transaction.execute(
                  status ->
                      new JdbcRateLimitAdmissionAdapter(jdbc)
                              .tryAcquire(new RateLimitAdmissionCommand(identity, 60, limit))
                              .status()
                          == RateLimitAdmissionStatus.ACCEPTED);
      final var futures = executor.invokeAll(java.util.Collections.nCopies(40, attempt));
      int accepted = 0;
      for (final var future : futures) {
        if (Boolean.TRUE.equals(future.get(10, TimeUnit.SECONDS))) {
          accepted++;
        }
      }
      assertThat(accepted).isEqualTo(limit);
    } finally {
      executor.shutdownNow();
    }

    final var otherTenant =
        transaction.execute(
            status ->
                new JdbcRateLimitAdmissionAdapter(jdbc)
                    .tryAcquire(
                        new RateLimitAdmissionCommand(identity("tenant-other"), 60, limit)));
    final var otherEnvironment =
        transaction.execute(
            status ->
                new JdbcRateLimitAdmissionAdapter(jdbc)
                    .tryAcquire(
                        new RateLimitAdmissionCommand(
                            new PersistentGuardIdentity(
                                "dev",
                                PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
                                PersistentGuardIdentity.NQ_DRYRUN_SOURCE,
                                "tenant-rate"),
                            60,
                            limit)));
    assertThat(otherTenant.status()).isEqualTo(RateLimitAdmissionStatus.ACCEPTED);
    assertThat(otherEnvironment.status()).isEqualTo(RateLimitAdmissionStatus.ACCEPTED);
  }

  @Test
  void concurrentIdempotencyAdmissionHasOneAtomicWinner() throws Exception {
    final Instant now = Instant.now();
    final IdempotencyAdmissionCommand command =
        new IdempotencyAdmissionCommand(
            identity("tenant-concurrent-idem"),
            "request-concurrent",
            HASH_A,
            IdempotencyAdmissionCommand.HASH_VERSION,
            Duration.ofMinutes(10),
            Duration.ofHours(1));
    final var executor = Executors.newFixedThreadPool(8);
    try {
      final Callable<IdempotencyAdmissionStatus> attempt =
          () ->
              transaction.execute(
                  status -> new JdbcIdempotencyGuardAdapter(jdbc).admit(command).status());
      final var futures = executor.invokeAll(java.util.Collections.nCopies(24, attempt));
      int admitted = 0;
      for (final var future : futures) {
        if (future.get(10, TimeUnit.SECONDS) == IdempotencyAdmissionStatus.ADMITTED) {
          admitted++;
        }
      }
      assertThat(admitted).isEqualTo(1);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void schemaRejectsNonCanonicalIdentityHashStateAndCounter() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into dh_qdr7_rate_limit_bucket"
                        + " (environment, endpoint, source, tenant_id, window_start, window_end,"
                        + " window_seconds, limit_value, request_count)"
                        + " values ('test', ?, 'nq_dryrun', 'tenant-a', transaction_timestamp(),"
                        + " transaction_timestamp() + interval '1 minute', 60, 10, 1)",
                    PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into dh_qdr7_idempotency_guard"
                        + " (guard_id, environment, endpoint, source, tenant_id, request_id,"
                        + " request_hash, hash_version, state, state_version, expires_at, retention_until)"
                        + " values (?, 'test', ?, 'NQ_DRYRUN', 'tenant-a', 'request-invalid',"
                        + " 'not-a-hash', 'QDR7-DRYRUN-CJSON-1', 'UNKNOWN', -1,"
                        + " transaction_timestamp() + interval '1 hour',"
                        + " transaction_timestamp() + interval '2 hour')",
                    UUID.randomUUID(),
                    PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }

  @Test
  void v13StateConstraintsRejectMissingTypedTerminalFieldsAndPartialLease() {
    final UUID guard = UUID.randomUUID();
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into dh_qdr7_idempotency_guard"
                        + " (guard_id,environment,endpoint,source,tenant_id,request_id,request_hash,hash_version,state,"
                        + "state_version,result_id,result_checksum,completed_at,expires_at,retention_until)"
                        + " values (?,'test',?,'NQ_DRYRUN','tenant-check','missing-type',?,'QDR7-DRYRUN-CJSON-1',"
                        + "'COMPLETED',1,'missing',?,transaction_timestamp(),transaction_timestamp()+interval '1 hour',"
                        + "transaction_timestamp()+interval '2 hour')",
                    guard,
                    PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
                    HASH_A,
                    HASH_B))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into dh_qdr7_idempotency_guard"
                        + " (guard_id,environment,endpoint,source,tenant_id,request_id,request_hash,hash_version,state,"
                        + "state_version,stable_error_code,expires_at,retention_until)"
                        + " values (?,'test',?,'NQ_DRYRUN','tenant-check','missing-failed-at',?,'QDR7-DRYRUN-CJSON-1',"
                        + "'FAILED',1,'SAFE_FAILURE',transaction_timestamp()+interval '1 hour',"
                        + "transaction_timestamp()+interval '2 hour')",
                    UUID.randomUUID(),
                    PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
                    HASH_A))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into dh_qdr7_idempotency_guard"
                        + " (guard_id,environment,endpoint,source,tenant_id,request_id,request_hash,hash_version,state,"
                        + "state_version,lease_owner,expires_at,retention_until)"
                        + " values (?,'test',?,'NQ_DRYRUN','tenant-check','partial-lease',?,'QDR7-DRYRUN-CJSON-1',"
                        + "'IN_PROGRESS',1,'worker',transaction_timestamp()+interval '1 hour',"
                        + "transaction_timestamp()+interval '2 hour')",
                    UUID.randomUUID(),
                    PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
                    HASH_A))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }

  @Test
  void databaseClockCreatesLifecycleAndCleanupPreservesExpiredIdentityTombstone() {
    final JdbcIdempotencyGuardAdapter adapter = new JdbcIdempotencyGuardAdapter(jdbc);
    final IdempotencyAdmissionCommand command =
        new IdempotencyAdmissionCommand(
            identity("tenant-tombstone"),
            "request-tombstone",
            HASH_A,
            IdempotencyAdmissionCommand.HASH_VERSION,
            Duration.ofSeconds(2),
            Duration.ofSeconds(3));
    final Instant dbBefore = jdbc.queryForObject("select transaction_timestamp()", Instant.class);
    final var admitted = transaction.execute(status -> adapter.admit(command));
    final Instant dbAfter = jdbc.queryForObject("select transaction_timestamp()", Instant.class);
    assertThat(admitted.record().expiresAt())
        .isBetween(dbBefore.plusMillis(1500), dbAfter.plusMillis(2500));
    jdbc.update(
        "update dh_qdr7_idempotency_guard set created_at=transaction_timestamp()-interval '3 second',"
            + " updated_at=transaction_timestamp()-interval '3 second',expires_at=transaction_timestamp()-interval '2 second',"
            + " retention_until=transaction_timestamp()-interval '1 second' where request_id='request-tombstone'");
    final var cleanup = new JdbcGuardCleanupAdapter(jdbc);
    final Integer expired =
        transaction.execute(
            status ->
                cleanup.cleanupRetainedIdempotency(
                    new GuardCleanupCommand(
                        "test",
                        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
                        PersistentGuardIdentity.NQ_DRYRUN_SOURCE,
                        "tenant-tombstone",
                        Duration.ofMillis(1),
                        10)));
    assertThat(expired).isEqualTo(1);
    assertThat(transaction.execute(status -> adapter.admit(command)).status())
        .isEqualTo(IdempotencyAdmissionStatus.EXPIRED);
    final var conflict =
        new IdempotencyAdmissionCommand(
            identity("tenant-tombstone"),
            "request-tombstone",
            HASH_B,
            IdempotencyAdmissionCommand.HASH_VERSION,
            Duration.ofSeconds(2),
            Duration.ofSeconds(3));
    assertThat(transaction.execute(status -> adapter.admit(conflict)).status())
        .isEqualTo(IdempotencyAdmissionStatus.CONFLICT);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_qdr7_idempotency_guard where request_id='request-tombstone'",
                Integer.class))
        .isEqualTo(1);
  }

  @Test
  void cleanupUsesDatabaseGraceAndConcurrentAdaptersDoNotCrossCurrentWindow() throws Exception {
    jdbc.update(
        "insert into dh_qdr7_rate_limit_bucket(environment,endpoint,source,tenant_id,window_start,window_end,"
            + "window_seconds,limit_value,request_count) values"
            + "('test',?,'NQ_DRYRUN','cleanup-rate',transaction_timestamp()-interval '120 second',transaction_timestamp()-interval '60 second',60,10,1),"
            + "('test',?,'NQ_DRYRUN','cleanup-rate',date_trunc('minute',transaction_timestamp()),date_trunc('minute',transaction_timestamp())+interval '60 second',60,10,1),"
            + "('test',?,'NQ_DRYRUN','cleanup-rate',transaction_timestamp()+interval '60 second',transaction_timestamp()+interval '120 second',60,10,1)",
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT);
    final GuardCleanupCommand command =
        new GuardCleanupCommand(
            "test",
            PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
            PersistentGuardIdentity.NQ_DRYRUN_SOURCE,
            "cleanup-rate",
            Duration.ofSeconds(1),
            10);
    final var pool = Executors.newFixedThreadPool(2);
    try {
      final var calls =
          java.util.List.<Callable<Integer>>of(
              () ->
                  transaction.execute(
                      s -> new JdbcGuardCleanupAdapter(jdbc).cleanupExpiredRateBuckets(command)),
              () ->
                  transaction.execute(
                      s -> new JdbcGuardCleanupAdapter(jdbc).cleanupExpiredRateBuckets(command)));
      int total = 0;
      for (final var result : pool.invokeAll(calls)) total += result.get(10, TimeUnit.SECONDS);
      assertThat(total).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_qdr7_rate_limit_bucket where tenant_id = 'cleanup-rate'",
                Integer.class))
        .isEqualTo(2);
  }

  @Test
  void idempotencyAdmissionCasLeaseCompletionAndDuplicateSemanticsArePersistent() throws Exception {
    final JdbcIdempotencyGuardAdapter adapter = new JdbcIdempotencyGuardAdapter(jdbc);
    final Instant now = Instant.now();
    final IdempotencyAdmissionCommand command =
        new IdempotencyAdmissionCommand(
            identity("tenant-idem"),
            "request-1",
            HASH_A,
            IdempotencyAdmissionCommand.HASH_VERSION,
            Duration.ofMinutes(10),
            Duration.ofHours(1));
    final var admitted = transaction.execute(status -> adapter.admit(command));
    assertThat(admitted.status()).isEqualTo(IdempotencyAdmissionStatus.ADMITTED);

    final UUID lease = UUID.randomUUID();
    final IdempotencyRecordView inProgress =
        transaction.execute(
            status ->
                adapter.transition(
                    transition(
                        admitted.record(),
                        IdempotencyState.RECEIVED,
                        IdempotencyState.IN_PROGRESS,
                        null,
                        lease,
                        now.plusSeconds(30),
                        null,
                        null,
                        null)));
    assertThat(inProgress.state()).isEqualTo(IdempotencyState.IN_PROGRESS);

    final IdempotencyRecordView heartbeat =
        transaction.execute(
            status ->
                adapter.transition(
                    transition(
                        inProgress,
                        IdempotencyState.IN_PROGRESS,
                        IdempotencyState.IN_PROGRESS,
                        lease,
                        lease,
                        now.plusSeconds(60),
                        null,
                        null,
                        null)));
    seedDecisionOutput("tenant-idem", "result-1");
    final IdempotencyRecordView completed =
        transaction.execute(
            status ->
                adapter.transition(
                    transition(
                        heartbeat,
                        IdempotencyState.IN_PROGRESS,
                        IdempotencyState.COMPLETED,
                        lease,
                        null,
                        null,
                        "result-1",
                        HASH_B,
                        null)));
    assertThat(completed.state()).isEqualTo(IdempotencyState.COMPLETED);

    final var duplicate = transaction.execute(status -> adapter.admit(command));
    assertThat(duplicate.status()).isEqualTo(IdempotencyAdmissionStatus.COMPLETED);
    assertThat(duplicate.record().resultId()).isEqualTo("result-1");

    final var conflict =
        transaction.execute(
            status ->
                adapter.admit(
                    new IdempotencyAdmissionCommand(
                        identity("tenant-idem"),
                        "request-1",
                        HASH_B,
                        IdempotencyAdmissionCommand.HASH_VERSION,
                        Duration.ofMinutes(10),
                        Duration.ofHours(1))));
    assertThat(conflict.status()).isEqualTo(IdempotencyAdmissionStatus.CONFLICT);
    assertThatThrownBy(
            () ->
                transaction.execute(
                    status ->
                        adapter.transition(
                            transition(
                                completed,
                                IdempotencyState.COMPLETED,
                                IdempotencyState.FAILED,
                                null,
                                null,
                                null,
                                null,
                                null,
                                "MUST_NOT_OVERWRITE"))))
        .isInstanceOf(PersistentGuardStateException.class);
  }

  @Test
  void expiredLeaseRecoveryAndBoundedCleanupNeverDeleteActiveLease() throws Exception {
    final JdbcIdempotencyGuardAdapter adapter = new JdbcIdempotencyGuardAdapter(jdbc);
    final Instant now = Instant.now();
    final IdempotencyAdmissionCommand command =
        new IdempotencyAdmissionCommand(
            identity("tenant-recovery"),
            "request-recovery",
            HASH_A,
            IdempotencyAdmissionCommand.HASH_VERSION,
            Duration.ofMinutes(10),
            Duration.ofHours(1));
    final var admitted = transaction.execute(status -> adapter.admit(command));
    final UUID firstLease = UUID.randomUUID();
    final IdempotencyRecordView active =
        transaction.execute(
            status ->
                adapter.transition(
                    transition(
                        admitted.record(),
                        IdempotencyState.RECEIVED,
                        IdempotencyState.IN_PROGRESS,
                        null,
                        firstLease,
                        Instant.now().plusMillis(250),
                        null,
                        null,
                        null)));
    Thread.sleep(350);
    final UUID recoveredLease = UUID.randomUUID();
    final IdempotencyRecordView recovered =
        transaction.execute(
            status ->
                adapter.transition(
                    transition(
                        active,
                        IdempotencyState.IN_PROGRESS,
                        IdempotencyState.IN_PROGRESS,
                        null,
                        recoveredLease,
                        Instant.now().plusSeconds(30),
                        null,
                        null,
                        null)));
    assertThat(recovered.leaseToken()).isEqualTo(recoveredLease);

    insertExpiredCleanupRows();
    final JdbcGuardCleanupAdapter cleanup = new JdbcGuardCleanupAdapter(jdbc);
    final GuardCleanupCommand cleanupCommand =
        new GuardCleanupCommand(
            "test",
            PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
            PersistentGuardIdentity.NQ_DRYRUN_SOURCE,
            "tenant-cleanup",
            Duration.ofMillis(1),
            1);
    final Integer cleanedRate =
        transaction.execute(status -> cleanup.cleanupExpiredRateBuckets(cleanupCommand));
    final Integer cleanedIdempotency =
        transaction.execute(status -> cleanup.cleanupRetainedIdempotency(cleanupCommand));
    assertThat(cleanedRate).isEqualTo(1);
    assertThat(cleanedIdempotency).isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_qdr7_idempotency_guard where request_id = ?",
                Integer.class,
                "request-recovery"))
        .isEqualTo(1);
  }

  @Test
  void transactionRollbackLeavesNoAcceptedBucket() {
    final JdbcRateLimitAdmissionAdapter adapter = new JdbcRateLimitAdmissionAdapter(jdbc);
    assertThatThrownBy(
            () ->
                transaction.execute(
                    status -> {
                      assertThat(
                              adapter
                                  .tryAcquire(
                                      new RateLimitAdmissionCommand(
                                          identity("tenant-rollback"), 60, 5))
                                  .status())
                          .isEqualTo(RateLimitAdmissionStatus.ACCEPTED);
                      throw new IllegalStateException("force rollback");
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_qdr7_rate_limit_bucket where tenant_id = ?",
                Integer.class,
                "tenant-rollback"))
        .isZero();
  }

  @Test
  void realJdbcAdmissionAndAuditFailuresRollbackRateAndIdempotency() {
    final JdbcDecisionAuditRepository audit =
        new JdbcDecisionAuditRepository(jdbc, new ObjectMapper());
    audit.saveAuditEvent(audit("duplicate-audit", "seed-decision", "tenant-atomic"));
    assertThatThrownBy(
            () ->
                transaction.execute(
                    status -> {
                      assertThat(
                              new JdbcRateLimitAdmissionAdapter(jdbc)
                                  .tryAcquire(
                                      new RateLimitAdmissionCommand(
                                          identity("tenant-rate-audit-rollback"), 60, 5))
                                  .status())
                          .isEqualTo(RateLimitAdmissionStatus.ACCEPTED);
                      audit.saveAuditEvent(
                          audit("duplicate-audit", "rate-decision", "tenant-rate-audit-rollback"));
                      return true;
                    }))
        .isInstanceOf(RuntimeException.class);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_qdr7_rate_limit_bucket where tenant_id='tenant-rate-audit-rollback'",
                Integer.class))
        .isZero();

    final IdempotencyAdmissionCommand command =
        new IdempotencyAdmissionCommand(
            identity("tenant-idem-audit-rollback"),
            "request-idem-audit-rollback",
            HASH_A,
            IdempotencyAdmissionCommand.HASH_VERSION,
            Duration.ofMinutes(10),
            Duration.ofHours(1));
    assertThatThrownBy(
            () ->
                transaction.execute(
                    status -> {
                      assertThat(new JdbcIdempotencyGuardAdapter(jdbc).admit(command).status())
                          .isEqualTo(IdempotencyAdmissionStatus.ADMITTED);
                      audit.saveAuditEvent(
                          audit("duplicate-audit", "idem-decision", "tenant-idem-audit-rollback"));
                      return true;
                    }))
        .isInstanceOf(RuntimeException.class);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_qdr7_idempotency_guard where request_id='request-idem-audit-rollback'",
                Integer.class))
        .isZero();
  }

  @Test
  void realJdbcOutputAuditAndTerminalCasCommitOrRollbackAtomically() {
    final JdbcDecisionAuditRepository repository =
        new JdbcDecisionAuditRepository(jdbc, new ObjectMapper());
    final JdbcIdempotencyGuardAdapter adapter = new JdbcIdempotencyGuardAdapter(jdbc);
    final IdempotencyAdmissionCommand command =
        new IdempotencyAdmissionCommand(
            identity("tenant-completion"),
            "request-completion",
            HASH_A,
            IdempotencyAdmissionCommand.HASH_VERSION,
            Duration.ofMinutes(10),
            Duration.ofHours(1));
    final var admitted = transaction.execute(status -> adapter.admit(command));
    final UUID lease = UUID.randomUUID();
    final var progress =
        transaction.execute(
            status ->
                adapter.transition(
                    transition(
                        admitted.record(),
                        IdempotencyState.RECEIVED,
                        IdempotencyState.IN_PROGRESS,
                        null,
                        lease,
                        Instant.now().plusSeconds(30),
                        null,
                        null,
                        null)));
    transaction.executeWithoutResult(
        status -> {
          repository.saveOutput(output("result-atomic", "tenant-completion", "request-completion"));
          repository.saveAuditEvent(audit("audit-atomic", "result-atomic", "tenant-completion"));
          adapter.transition(
              transition(
                  progress,
                  IdempotencyState.IN_PROGRESS,
                  IdempotencyState.COMPLETED,
                  lease,
                  null,
                  null,
                  "result-atomic",
                  HASH_B,
                  null));
        });
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_decision_output where decision_id='result-atomic'",
                Integer.class))
        .isEqualTo(1);
    assertThat(adapter.admit(command).status()).isEqualTo(IdempotencyAdmissionStatus.COMPLETED);

    final IdempotencyAdmissionCommand rollbackCommand =
        new IdempotencyAdmissionCommand(
            identity("tenant-completion"),
            "request-completion-rollback",
            HASH_A,
            IdempotencyAdmissionCommand.HASH_VERSION,
            Duration.ofMinutes(10),
            Duration.ofHours(1));
    final var rollbackAdmitted = transaction.execute(status -> adapter.admit(rollbackCommand));
    final UUID rollbackLease = UUID.randomUUID();
    final var rollbackProgress =
        transaction.execute(
            status ->
                adapter.transition(
                    transition(
                        rollbackAdmitted.record(),
                        IdempotencyState.RECEIVED,
                        IdempotencyState.IN_PROGRESS,
                        null,
                        rollbackLease,
                        Instant.now().plusSeconds(30),
                        null,
                        null,
                        null)));
    assertThatThrownBy(
            () ->
                transaction.executeWithoutResult(
                    status -> {
                      repository.saveOutput(
                          output(
                              "result-rollback",
                              "tenant-completion",
                              "request-completion-rollback"));
                      repository.saveAuditEvent(
                          audit("audit-atomic", "result-rollback", "tenant-completion"));
                      adapter.transition(
                          transition(
                              rollbackProgress,
                              IdempotencyState.IN_PROGRESS,
                              IdempotencyState.COMPLETED,
                              rollbackLease,
                              null,
                              null,
                              "result-rollback",
                              HASH_B,
                              null));
                    }))
        .isInstanceOf(RuntimeException.class);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_decision_output where decision_id='result-rollback'",
                Integer.class))
        .isZero();
    assertThat(
            adapter
                .findExact(identity("tenant-completion"), "request-completion-rollback", HASH_A)
                .state())
        .isEqualTo(IdempotencyState.IN_PROGRESS);
  }

  @Test
  void completionRejectsMissingAndWrongTenantResultReferences() {
    final JdbcIdempotencyGuardAdapter adapter = new JdbcIdempotencyGuardAdapter(jdbc);
    final IdempotencyAdmissionCommand command =
        new IdempotencyAdmissionCommand(
            identity("tenant-result-ref"),
            "request-result-ref",
            HASH_A,
            IdempotencyAdmissionCommand.HASH_VERSION,
            Duration.ofMinutes(10),
            Duration.ofHours(1));
    final var admitted = transaction.execute(status -> adapter.admit(command));
    final UUID lease = UUID.randomUUID();
    final var progress =
        transaction.execute(
            status ->
                adapter.transition(
                    transition(
                        admitted.record(),
                        IdempotencyState.RECEIVED,
                        IdempotencyState.IN_PROGRESS,
                        null,
                        lease,
                        Instant.now().plusSeconds(30),
                        null,
                        null,
                        null)));
    assertThatThrownBy(
            () ->
                transaction.executeWithoutResult(
                    status ->
                        adapter.transition(
                            transition(
                                progress,
                                IdempotencyState.IN_PROGRESS,
                                IdempotencyState.COMPLETED,
                                lease,
                                null,
                                null,
                                "missing-result",
                                HASH_B,
                                null))))
        .isInstanceOf(PersistentGuardStoreException.class);
    seedDecisionOutput("other-tenant", "wrong-tenant-result");
    assertThatThrownBy(
            () ->
                transaction.executeWithoutResult(
                    status ->
                        adapter.transition(
                            transition(
                                progress,
                                IdempotencyState.IN_PROGRESS,
                                IdempotencyState.COMPLETED,
                                lease,
                                null,
                                null,
                                "wrong-tenant-result",
                                HASH_B,
                                null))))
        .isInstanceOf(PersistentGuardStoreException.class);
    assertThat(
            adapter.findExact(identity("tenant-result-ref"), "request-result-ref", HASH_A).state())
        .isEqualTo(IdempotencyState.IN_PROGRESS);
  }

  @Test
  void afterCommitConnectionFailureIsCommitUnknownAndDoesNotReadmit() {
    final AtomicBoolean failFirstCommit = new AtomicBoolean();
    final DataSource uncertain = afterCommitFailureDataSource(dataSource(), failFirstCommit);
    final JdbcTemplate uncertainJdbc = new JdbcTemplate(uncertain);
    final TransactionTemplate uncertainTransaction =
        new TransactionTemplate(new DataSourceTransactionManager(uncertain));
    final GuardTransactionBoundary boundary =
        new GuardTransactionBoundary() {
          @Override
          public <T> T required(final java.util.function.Supplier<T> action) {
            return uncertainTransaction.execute(status -> action.get());
          }
        };
    final var limiter =
        new PersistentDecisionDryRunRateLimiter(
            new JdbcRateLimitAdmissionAdapter(uncertainJdbc),
            boundary,
            new JdbcDecisionAuditRepository(uncertainJdbc, new ObjectMapper()),
            new DecisionDryRunGuardProperties(
                true,
                "test",
                3600,
                1,
                Duration.ofSeconds(5),
                Duration.ofMinutes(10),
                Duration.ofHours(1)),
            Clock.systemUTC());
    final RateLimitResult uncertainResult =
        limiter.check(
            "NQ_DRYRUN",
            "tenant-commit-unknown",
            PersistentDecisionDryRunRateLimiter.ROUTE,
            Instant.EPOCH,
            "request-commit-unknown",
            "trace-commit-unknown");
    assertThat(uncertainResult.reason()).isEqualTo(RateLimitResult.REASON_COMMIT_UNKNOWN);
    assertThat(
            jdbc.queryForObject(
                "select request_count from dh_qdr7_rate_limit_bucket where tenant_id='tenant-commit-unknown'",
                Integer.class))
        .isEqualTo(1);
    final var reconciledDuplicate =
        transaction.execute(
            status ->
                new JdbcRateLimitAdmissionAdapter(jdbc)
                    .tryAcquire(
                        new RateLimitAdmissionCommand(identity("tenant-commit-unknown"), 3600, 1)));
    assertThat(reconciledDuplicate.status()).isEqualTo(RateLimitAdmissionStatus.RATE_LIMITED);
    assertThat(
            jdbc.queryForObject(
                "select request_count from dh_qdr7_rate_limit_bucket where tenant_id='tenant-commit-unknown'",
                Integer.class))
        .isEqualTo(1);
  }

  private static DecisionPersistenceRecords.AuditEventRecord audit(
      final String id, final String decisionId, final String tenant) {
    return new DecisionPersistenceRecords.AuditEventRecord(
        id,
        decisionId,
        tenant,
        "trace-atomic",
        DecisionAuditEventType.QDR7_IDEMPOTENCY_COMPLETED,
        DecisionAuditEventStatus.SUCCESS,
        Map.of("state", "safe"),
        null,
        Instant.now());
  }

  private static DecisionPersistenceRecords.OutputRecord output(
      final String decisionId, final String tenant, final String requestId) {
    return new DecisionPersistenceRecords.OutputRecord(
        decisionId,
        tenant,
        "trace-atomic",
        requestId,
        DecisionType.READ_ONLY_RECOMMENDATION,
        DecisionAction.NO_TRADE,
        DecisionRiskLevel.LOW,
        DecisionPolicyStatus.ALLOWED,
        new BigDecimal("0.5"),
        Map.of(),
        Instant.now());
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

  private static IdempotencyTransitionCommand transition(
      final IdempotencyRecordView record,
      final IdempotencyState expected,
      final IdempotencyState target,
      final UUID expectedLease,
      final UUID newLease,
      final Instant leaseExpiry,
      final String resultId,
      final String checksum,
      final String errorCode) {
    return new IdempotencyTransitionCommand(
        record.identity(),
        record.requestId(),
        record.requestHash(),
        expected,
        record.stateVersion(),
        expectedLease == null ? null : record.leaseOwner(),
        expectedLease,
        target,
        newLease == null ? null : "worker-test",
        newLease,
        leaseExpiry == null
            ? null
            : Duration.ofMillis(
                Math.max(1L, Duration.between(Instant.now(), leaseExpiry).toMillis())),
        target == IdempotencyState.COMPLETED ? "DH_DECISION_OUTPUT" : null,
        resultId,
        checksum,
        errorCode);
  }

  private void seedDecisionOutput(final String tenant, final String decisionId) {
    jdbc.update(
        "insert into dh_decision_output"
            + " (decision_id, tenant_id, trace_id, request_id, decision_type, action, risk_level,"
            + " policy_status, confidence, output_json, created_at)"
            + " values (?, ?, 'trace-test', ?, 'READ_ONLY_RECOMMENDATION', 'NO_TRADE', 'LOW',"
            + " 'ALLOWED', 0.5, '{}'::jsonb, transaction_timestamp())",
        decisionId,
        tenant,
        decisionId);
  }

  private void insertExpiredCleanupRows() {
    jdbc.update(
        "insert into dh_qdr7_rate_limit_bucket"
            + " (environment, endpoint, source, tenant_id, window_start, window_end, window_seconds,"
            + " limit_value, request_count, created_at, updated_at)"
            + " values ('test', ?, 'NQ_DRYRUN', 'tenant-cleanup', transaction_timestamp() - interval '2 hour',"
            + " transaction_timestamp() - interval '1 hour', 3600, 10, 1,"
            + " transaction_timestamp() - interval '2 hour', transaction_timestamp() - interval '2 hour')",
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT);
    jdbc.update(
        "insert into dh_qdr7_idempotency_guard"
            + " (guard_id, environment, endpoint, source, tenant_id, request_id, request_hash,"
            + " hash_version, state, state_version, created_at, updated_at, expired_at, expires_at,"
            + " retention_until) values (?, 'test', ?, 'NQ_DRYRUN', 'tenant-cleanup', 'request-cleanup',"
            + " ?, 'QDR7-DRYRUN-CJSON-1', 'EXPIRED', 1, transaction_timestamp() - interval '3 hour',"
            + " transaction_timestamp() - interval '2 hour', transaction_timestamp() - interval '2 hour',"
            + " transaction_timestamp() - interval '2 hour', transaction_timestamp() - interval '1 hour')",
        UUID.randomUUID(),
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
        HASH_A);
  }

  private boolean tableExists(final String table) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "select exists(select 1 from information_schema.tables"
                + " where table_schema = 'public' and table_name = ?)",
            Boolean.class,
            table));
  }

  private static Map<String, Integer> checksums(final MigrationInfo[] migrations) {
    return Arrays.stream(migrations)
        .filter(
            info ->
                info.getVersion() != null && info.getVersion().getVersion().matches("[1-9]|1[0-3]"))
        .collect(
            Collectors.toMap(info -> info.getVersion().getVersion(), MigrationInfo::getChecksum));
  }

  private static PersistentGuardIdentity identity(final String tenant) {
    return new PersistentGuardIdentity(
        "test",
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
        PersistentGuardIdentity.NQ_DRYRUN_SOURCE,
        tenant);
  }

  private static Flyway flyway(final String target) {
    final var configuration =
        Flyway.configure()
            .cleanDisabled(false)
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .locations("filesystem:src/main/resources/db/migration");
    if (target != null) {
      configuration.target(target);
    }
    return configuration.load();
  }

  private static DriverManagerDataSource dataSource() {
    return new DriverManagerDataSource(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }
}

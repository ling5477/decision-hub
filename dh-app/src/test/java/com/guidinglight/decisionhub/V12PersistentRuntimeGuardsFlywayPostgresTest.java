package com.guidinglight.decisionhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guidinglight.decisionhub.infra.jdbc.qdr.guard.JdbcGuardCleanupAdapter;
import com.guidinglight.decisionhub.infra.jdbc.qdr.guard.JdbcIdempotencyGuardAdapter;
import com.guidinglight.decisionhub.infra.jdbc.qdr.guard.JdbcRateLimitAdmissionAdapter;
import com.guidinglight.decisionhub.usecase.qdr.guard.GuardCleanupCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionStatus;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyRecordView;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyState;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyTransitionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardIdentity;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardStateException;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionStatus;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
 * Stage-QDR-7 V12真实PostgreSQL 17/Flyway/并发/CAS/cleanup回归。
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
  void cleanAndExistingV11DatabasesMigrateToV12WithoutChangingHistory() {
    assertThat(flyway(null).info().current().getVersion().getVersion()).isEqualTo("12");
    assertThat(tableExists("dh_qdr7_rate_limit_bucket")).isTrue();
    assertThat(tableExists("dh_qdr7_idempotency_guard")).isTrue();

    flyway(null).clean();
    final Flyway v11 = flyway("11");
    v11.migrate();
    final Map<String, Integer> checksumsBefore = checksums(v11.info().applied());
    seedDecisionOutput("tenant-upgrade", "result-upgrade");

    final Flyway v12 = flyway(null);
    v12.migrate();

    assertThat(v12.info().current().getVersion().getVersion()).isEqualTo("12");
    final Map<String, Integer> checksumsAfter = checksums(v12.info().applied());
    checksumsBefore.forEach(
        (version, checksum) -> assertThat(checksumsAfter.get(version)).isEqualTo(checksum));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_decision_output where tenant_id = 'tenant-upgrade'",
                Integer.class))
        .isEqualTo(1);
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
                    .tryAcquire(new RateLimitAdmissionCommand(identity("tenant-other"), 60, limit)));
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
            now.plusSeconds(600),
            now.plusSeconds(3600));
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
  void idempotencyAdmissionCasLeaseCompletionAndDuplicateSemanticsArePersistent()
      throws Exception {
    final JdbcIdempotencyGuardAdapter adapter = new JdbcIdempotencyGuardAdapter(jdbc);
    final Instant now = Instant.now();
    final IdempotencyAdmissionCommand command =
        new IdempotencyAdmissionCommand(
            identity("tenant-idem"),
            "request-1",
            HASH_A,
            IdempotencyAdmissionCommand.HASH_VERSION,
            now.plusSeconds(600),
            now.plusSeconds(3600));
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
                        now.plusSeconds(600),
                        now.plusSeconds(3600))));
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
            now.plusSeconds(600),
            now.plusSeconds(3600));
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
            Instant.now(),
            1);
    final Integer cleanedRate =
        transaction.execute(status -> cleanup.cleanupExpiredRateBuckets(cleanupCommand));
    final Integer cleanedIdempotency =
        transaction.execute(status -> cleanup.cleanupRetainedIdempotency(cleanupCommand));
    assertThat(cleanedRate).isEqualTo(1);
    assertThat(cleanedIdempotency).isEqualTo(1);
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
                                      new RateLimitAdmissionCommand(identity("tenant-rollback"), 60, 5))
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
        expectedLease,
        target,
        newLease,
        leaseExpiry,
        resultId,
        checksum,
        errorCode,
        Instant.now());
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
            + " hash_version, state, state_version, created_at, updated_at, completed_at, expires_at,"
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
        .filter(info -> info.getVersion() != null && info.getVersion().getVersion().matches("[1-9]|1[01]"))
        .collect(
            Collectors.toMap(
                info -> info.getVersion().getVersion(), MigrationInfo::getChecksum));
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

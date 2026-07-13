package com.guidinglight.decisionhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Stage-QDR-7 B2：验证beforeEachMigrate callback与V13的真实PostgreSQL事务耦合。
 *
 * <p>所有数据库均为一次性PostgreSQL 17 Testcontainers。测试不连接生产库、不调用HTTP、Provider、NQ、
 * Agent/LangGraph、Paper或LIVE；V13故障和statement-timeout仅在临时migration fixture中注入。
 */
@Testcontainers(disabledWithoutDocker = true)
class V13TransactionalCompatibilityCallbackFlywayPostgresTest {

  private static final String GUARD_TABLE = "public.dh_qdr7_idempotency_guard";
  private static final String STATE_CHECK = "chk_dh_qdr7_idempotency_state_fields";
  private static final String HASH = "a".repeat(64);
  private static final Path MIGRATION_ROOT =
      Path.of("src", "main", "resources", "db", "migration").toAbsolutePath().normalize();

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("decision_hub")
          .withUsername("decision_hub")
          .withPassword("decision_hub");

  private JdbcTemplate jdbc;

  @BeforeEach
  void resetDatabase() {
    flyway(null, MIGRATION_ROOT).clean();
    jdbc = new JdbcTemplate(dataSource());
  }

  @Test
  void callbackReplacementAndFrozenTimeoutContractAreSourceGuarded() throws IOException {
    final Path oldCallback = MIGRATION_ROOT.resolve("beforeMigrate__qdr7_v13_compatibility.sql");
    final Path callback = MIGRATION_ROOT.resolve("beforeEachMigrate__qdr7_v13_compatibility.sql");
    final String source = Files.readString(callback);

    assertThat(Files.exists(oldCallback)).isFalse();
    assertThat(Files.exists(callback)).isTrue();
    assertThat(source)
        .contains("set local lock_timeout = '5s';")
        .contains("set local statement_timeout = '60s';")
        .contains("pg_get_constraintdef(con.oid, false)")
        .contains("order by guard_id")
        .contains("limit 1000")
        .contains("offline governance required");
    assertThat(source.indexOf("qdr7 compatibility rejected invalid FAILED stable_error_code"))
        .isLessThan(source.indexOf("alter table public.dh_qdr7_idempotency_guard"));
    long versionedMigrationCount = 0;
    try (DirectoryStream<Path> migrations = Files.newDirectoryStream(MIGRATION_ROOT)) {
      for (Path migration : migrations) {
        if (migration.getFileName().toString().matches("V(1[0-4]|[1-9])__.*\\.sql")) {
          versionedMigrationCount++;
        }
      }
    }
    assertThat(versionedMigrationCount).isEqualTo(14);
  }

  @Test
  void freshV1ToV14AndV11ToV14RemainFlywayCompatible() {
    flyway(null, MIGRATION_ROOT).migrate();
    assertThat(currentSuccessfulVersion()).isEqualTo("14");
    assertThat(jdbc.queryForObject("show server_version", String.class)).startsWith("17.");

    flyway(null, MIGRATION_ROOT).clean();
    flyway("11", MIGRATION_ROOT).migrate();
    assertThat(currentSuccessfulVersion()).isEqualTo("11");
    flyway(null, MIGRATION_ROOT).migrate();
    assertThat(currentSuccessfulVersion()).isEqualTo("14");
  }

  @Test
  void v12CleanAndPaddedFailedRowsMigrateToV14WithBoundedRepair() {
    migrateToV12();
    seedFailed("padded", " SAFE_FAILURE ");

    flyway(null, MIGRATION_ROOT).migrate();

    assertThat(currentSuccessfulVersion()).isEqualTo("14");
    assertThat(stableErrorCode("padded")).isEqualTo("SAFE_FAILURE");
    assertThat(columnType("result_type")).isEqualTo("character varying:32");
    assertThat(constraintDefinition()).contains("failed_at IS NOT NULL");
  }

  @Test
  void repairCeilingAllowsExactlyOneThousandRowsAndRejectsOneThousandOneBeforeWrite() {
    migrateToV12();
    seedPaddedFailedRows(1000, "within-ceiling");

    flyway(null, MIGRATION_ROOT).migrate();

    assertThat(currentSuccessfulVersion()).isEqualTo("14");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_qdr7_idempotency_guard"
                    + " where request_id like 'within-ceiling-%' and stable_error_code = 'SAFE_FAILURE'",
                Integer.class))
        .isEqualTo(1000);

    flyway(null, MIGRATION_ROOT).clean();
    migrateToV12();
    final String v12Constraint = constraintDefinition();
    seedPaddedFailedRows(1001, "over-ceiling");

    final FlywayException failure =
        assertThrows(FlywayException.class, () -> flyway(null, MIGRATION_ROOT).migrate());

    assertThat(messages(failure)).contains("repair ceiling exceeded");
    assertThat(currentSuccessfulVersion()).isEqualTo("12");
    assertThat(constraintDefinition()).isEqualTo(v12Constraint);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_qdr7_idempotency_guard"
                    + " where request_id like 'over-ceiling-%' and stable_error_code = ' SAFE_FAILURE '",
                Integer.class))
        .isEqualTo(1001);
  }

  @Test
  void blankLegacyDataFailsBeforeCallbackDdlOrDml() {
    migrateToV12();
    final String v12Constraint = constraintDefinition();
    jdbc.execute("alter table " + GUARD_TABLE + " drop constraint " + STATE_CHECK);
    seedFailed("blank", "   ");
    jdbc.execute(
        "alter table "
            + GUARD_TABLE
            + " add constraint "
            + STATE_CHECK
            + " "
            + v12Constraint
            + " not valid");
    // 仅为模拟历史损坏记录：保留冻结definition与validated标记，使实际callback能进入blank
    // precheck；该catalog修改只发生在一次性Testcontainers，不属于生产migration路径。
    jdbc.update(
        "update pg_constraint set convalidated = true"
            + " where conrelid = 'public.dh_qdr7_idempotency_guard'::regclass and conname = ?",
        STATE_CHECK);

    final FlywayException failure =
        assertThrows(FlywayException.class, () -> flyway(null, MIGRATION_ROOT).migrate());

    assertThat(messages(failure)).contains("rejected invalid FAILED stable_error_code");
    assertThat(currentSuccessfulVersion()).isEqualTo("12");
    assertThat(stableErrorCode("blank")).isEqualTo("   ");
    assertThat(constraintDefinition()).isEqualTo(v12Constraint);
    assertThat(hasColumn("lease_owner")).isFalse();
  }

  @Test
  void missingOrDriftedV12CheckFailsClosedBeforeCompatibilityMutation() {
    migrateToV12();
    jdbc.execute("alter table " + GUARD_TABLE + " drop constraint " + STATE_CHECK);

    final FlywayException missing =
        assertThrows(FlywayException.class, () -> flyway(null, MIGRATION_ROOT).migrate());

    assertThat(messages(missing)).contains("V12 state CHECK fingerprint drift");
    assertThat(currentSuccessfulVersion()).isEqualTo("12");
    assertThat(hasColumn("lease_owner")).isFalse();

    flyway(null, MIGRATION_ROOT).clean();
    migrateToV12();
    jdbc.execute("alter table " + GUARD_TABLE + " drop constraint " + STATE_CHECK);
    jdbc.execute(
        "alter table " + GUARD_TABLE + " add constraint " + STATE_CHECK + " check (state is not null)");

    final FlywayException drifted =
        assertThrows(FlywayException.class, () -> flyway(null, MIGRATION_ROOT).migrate());

    assertThat(messages(drifted)).contains("V12 state CHECK fingerprint drift");
    assertThat(currentSuccessfulVersion()).isEqualTo("12");
    assertThat(constraintDefinition()).isEqualTo("CHECK ((state IS NOT NULL))");
  }

  @Test
  void injectedV13FailureRollsBackCallbackDdlAndRepairThenRetriesSafely(@TempDir Path tempDir)
      throws IOException {
    migrateToV12();
    seedFailed("retry", " SAFE_FAILURE ");
    final String v12Constraint = constraintDefinition();
    final Path failingFixture =
        migrationFixture(tempDir, "do $$ begin raise exception 'qdr7 injected V13 failure'; end $$;", false);

    final FlywayException failure =
        assertThrows(FlywayException.class, () -> flyway(null, failingFixture).migrate());

    assertThat(messages(failure)).contains("qdr7 injected V13 failure");
    assertThat(currentSuccessfulVersion()).isEqualTo("12");
    assertThat(stableErrorCode("retry")).isEqualTo(" SAFE_FAILURE ");
    assertThat(constraintDefinition()).isEqualTo(v12Constraint);
    assertThat(hasColumn("lease_owner")).isFalse();
    assertThat(successfulHistoryRows("13")).isZero();

    flyway(null, MIGRATION_ROOT).migrate();

    assertThat(currentSuccessfulVersion()).isEqualTo("14");
    assertThat(stableErrorCode("retry")).isEqualTo("SAFE_FAILURE");
    assertThat(successfulHistoryRows("13")).isEqualTo(1);
  }

  @Test
  void lockTimeoutAfterFiveSecondsRollsBackCallbackAndV13() throws Exception {
    migrateToV12();
    seedFailed("lock", " SAFE_FAILURE ");
    final String v12Constraint = constraintDefinition();

    try (Connection lockedConnection = dataSource().getConnection();
        Statement lockStatement = lockedConnection.createStatement()) {
      lockedConnection.setAutoCommit(false);
      lockStatement.execute("lock table " + GUARD_TABLE + " in access share mode");

      final Throwable failure = migrateInSeparateThreadExpectingFailure(MIGRATION_ROOT, 12);

      assertThat(messages(failure)).contains("lock timeout");
      assertThat(currentSuccessfulVersion()).isEqualTo("12");
      assertThat(stableErrorCode("lock")).isEqualTo(" SAFE_FAILURE ");
      assertThat(constraintDefinition()).isEqualTo(v12Constraint);
      assertThat(hasColumn("lease_owner")).isFalse();
    }
  }

  @Test
  @Timeout(value = 75, unit = TimeUnit.SECONDS)
  void statementTimeoutAfterSixtySecondsRollsBackCallbackAndV13(@TempDir Path tempDir)
      throws IOException {
    migrateToV12();
    seedFailed("statement", " SAFE_FAILURE ");
    final String v12Constraint = constraintDefinition();
    final Path slowFixture = migrationFixture(tempDir, "select pg_sleep(61);", false);

    final FlywayException failure =
        assertThrows(FlywayException.class, () -> flyway(null, slowFixture).migrate());

    assertThat(messages(failure)).contains("statement timeout");
    assertThat(currentSuccessfulVersion()).isEqualTo("12");
    assertThat(stableErrorCode("statement")).isEqualTo(" SAFE_FAILURE ");
    assertThat(constraintDefinition()).isEqualTo(v12Constraint);
    assertThat(hasColumn("lease_owner")).isFalse();
  }

  @Test
  void completedV13AndV14EnvironmentsDoNotAccessLockedGuardTable(@TempDir Path tempDir)
      throws Exception {
    migrateTo("13");
    assertCompletedEnvironmentNoOp(tempDir.resolve("v13"), "13");

    flyway(null, MIGRATION_ROOT).clean();
    flyway(null, MIGRATION_ROOT).migrate();
    assertCompletedEnvironmentNoOp(tempDir.resolve("v14"), null);
  }

  @Test
  void v14RejectsThirtyThreeCharactersWithoutTruncationAndRetriesAtThirtyTwo() {
    migrateTo("13");
    seedDecisionOutput("tenant-overflow", "result-overflow");
    jdbc.execute("alter table " + GUARD_TABLE + " drop constraint " + STATE_CHECK);
    jdbc.execute("alter table " + GUARD_TABLE + " drop constraint chk_dh_qdr7_idempotency_result_type");
    final String overflow = "x".repeat(33);
    jdbc.update(
        "insert into dh_qdr7_idempotency_guard"
            + " (guard_id,environment,endpoint,source,tenant_id,request_id,request_hash,hash_version,state,"
            + " state_version,result_type,result_id,result_checksum,completed_at,expires_at,retention_until)"
            + " values (?,'test','/api/ai/decision-dry-runs','NQ_DRYRUN','tenant-overflow',"
            + " 'request-overflow',?,'QDR7-DRYRUN-CJSON-1','COMPLETED',1,?,'result-overflow',?,"
            + " transaction_timestamp(),transaction_timestamp()+interval '1 hour',"
            + " transaction_timestamp()+interval '2 hour')",
        UUID.randomUUID(),
        HASH,
        overflow,
        "b".repeat(64));

    final FlywayException failure =
        assertThrows(FlywayException.class, () -> flyway(null, MIGRATION_ROOT).migrate());

    assertThat(messages(failure)).contains("result_type longer than frozen varchar(32)");
    assertThat(currentSuccessfulVersion()).isEqualTo("13");
    assertThat(columnType("result_type")).isEqualTo("character varying:64");
    assertThat(resultType("request-overflow")).isEqualTo(overflow);
    assertThat(successfulHistoryRows("14")).isZero();

    jdbc.update(
        "update dh_qdr7_idempotency_guard set result_type = ? where request_id = ?",
        "x".repeat(32),
        "request-overflow");
    flyway(null, MIGRATION_ROOT).migrate();

    assertThat(currentSuccessfulVersion()).isEqualTo("14");
    assertThat(columnType("result_type")).isEqualTo("character varying:32");
  }

  private void migrateToV12() {
    migrateTo("12");
    assertThat(currentSuccessfulVersion()).isEqualTo("12");
  }

  private void migrateTo(final String target) {
    flyway(target, MIGRATION_ROOT).migrate();
  }

  private void seedPaddedFailedRows(final int count, final String prefix) {
    for (int index = 0; index < count; index++) {
      seedFailed(prefix + "-" + index, " SAFE_FAILURE ");
    }
  }

  private void seedFailed(final String suffix, final String errorCode) {
    jdbc.update(
        "insert into dh_qdr7_idempotency_guard"
            + " (guard_id,environment,endpoint,source,tenant_id,request_id,request_hash,hash_version,state,"
            + " state_version,stable_error_code,completed_at,expires_at,retention_until)"
            + " values (?,'test','/api/ai/decision-dry-runs','NQ_DRYRUN',? ,?,?,'QDR7-DRYRUN-CJSON-1',"
            + " 'FAILED',1,?,transaction_timestamp(),transaction_timestamp()+interval '1 hour',"
            + " transaction_timestamp()+interval '2 hour')",
        UUID.randomUUID(),
        "tenant-" + suffix,
        suffix,
        HASH,
        errorCode);
  }

  private void seedDecisionOutput(final String tenantId, final String decisionId) {
    jdbc.update(
        "insert into dh_decision_output"
            + " (decision_id,tenant_id,trace_id,request_id,decision_type,action,risk_level,policy_status,"
            + " confidence,output_json,created_at)"
            + " values (?,?,'trace-overflow',?,'READ_ONLY_RECOMMENDATION','NO_TRADE','LOW','ALLOWED',"
            + " 0.5,'{}'::jsonb,transaction_timestamp())",
        decisionId,
        tenantId,
        decisionId);
  }

  private void assertCompletedEnvironmentNoOp(final Path fixtureRoot, final String target)
      throws Exception {
    final Path fixture = migrationFixture(fixtureRoot, null, true);
    try (Connection lockedConnection = dataSource().getConnection();
        Statement lockStatement = lockedConnection.createStatement()) {
      lockedConnection.setAutoCommit(false);
      lockStatement.execute("lock table " + GUARD_TABLE + " in access exclusive mode");
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      try {
        final Future<?> migration = executor.submit(() -> flyway(target, fixture).migrate());
        migration.get(3, TimeUnit.SECONDS);
      } finally {
        executor.shutdownNow();
      }
    }
  }

  private Throwable migrateInSeparateThreadExpectingFailure(final Path location, final int timeoutSeconds)
      throws InterruptedException {
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      final Future<?> migration = executor.submit(() -> flyway(null, location).migrate());
      try {
        migration.get(timeoutSeconds, TimeUnit.SECONDS);
        throw new AssertionError("migration unexpectedly succeeded");
      } catch (ExecutionException failure) {
        return failure.getCause();
      } catch (java.util.concurrent.TimeoutException timeout) {
        throw new AssertionError("migration did not fail within timeout", timeout);
      }
    } finally {
      executor.shutdownNow();
    }
  }

  private Path migrationFixture(final Path root, final String v13Suffix, final boolean probe)
      throws IOException {
    Files.createDirectories(root);
    try (DirectoryStream<Path> migrations = Files.newDirectoryStream(MIGRATION_ROOT)) {
      for (Path migration : migrations) {
        Files.copy(migration, root.resolve(migration.getFileName()));
      }
    }
    if (v13Suffix != null) {
      final Path v13 = root.resolve("V13__qdr7_persistent_guard_safety_fix.sql");
      Files.writeString(v13, Files.readString(v13) + System.lineSeparator() + v13Suffix);
    }
    if (probe) {
      Files.writeString(root.resolve("R__qdr7_callback_no_op_probe.sql"), "select 1;");
    }
    return root;
  }

  private String currentSuccessfulVersion() {
    return jdbc.queryForObject(
        "select version from flyway_schema_history where success = true"
            + " order by installed_rank desc limit 1",
        String.class);
  }

  private int successfulHistoryRows(final String version) {
    return jdbc.queryForObject(
        "select count(*) from flyway_schema_history where version = ? and success = true",
        Integer.class,
        version);
  }

  private String stableErrorCode(final String requestId) {
    return jdbc.queryForObject(
        "select stable_error_code from dh_qdr7_idempotency_guard where request_id = ?",
        String.class,
        requestId);
  }

  private String resultType(final String requestId) {
    return jdbc.queryForObject(
        "select result_type from dh_qdr7_idempotency_guard where request_id = ?",
        String.class,
        requestId);
  }

  private String constraintDefinition() {
    return jdbc.queryForObject(
        "select pg_get_constraintdef(oid, false) from pg_constraint"
            + " where conrelid = 'public.dh_qdr7_idempotency_guard'::regclass and conname = ?",
        String.class,
        STATE_CHECK);
  }

  private String columnType(final String columnName) {
    return jdbc.queryForObject(
        "select data_type || ':' || character_maximum_length from information_schema.columns"
            + " where table_schema = 'public' and table_name = 'dh_qdr7_idempotency_guard'"
            + " and column_name = ?",
        String.class,
        columnName);
  }

  private boolean hasColumn(final String columnName) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "select exists(select 1 from information_schema.columns"
                + " where table_schema = 'public' and table_name = 'dh_qdr7_idempotency_guard'"
                + " and column_name = ?)",
            Boolean.class,
            columnName));
  }

  private static String messages(final Throwable throwable) {
    final StringBuilder messages = new StringBuilder();
    Throwable current = throwable;
    while (current != null) {
      if (current.getMessage() != null) {
        messages.append(current.getMessage()).append('\n');
      }
      current = current.getCause();
    }
    return messages.toString();
  }

  private static Flyway flyway(final String target, final Path location) {
    final var configuration =
        Flyway.configure()
            .cleanDisabled(false)
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .locations("filesystem:" + location.toString().replace('\\', '/'));
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

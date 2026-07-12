package com.guidinglight.decisionhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionEvidence;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.infra.jdbc.qdr.JdbcCanonicalReplaySnapshotRepository;
import com.guidinglight.decisionhub.infra.jdbc.qdr.ReplayInputSnapshotAssemblyService;
import com.guidinglight.decisionhub.infra.jdbc.qdr.model.JdbcModelGatewayCallRepository;
import com.guidinglight.decisionhub.infra.jdbc.qdr.model.JdbcPromptVersionRepository;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceCorrelation;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidencePolicy;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceRef;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceAggregateService;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQueryRepository;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallRecord;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionRecord;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionRecord;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.RedactionStatus;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelQueryPort;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunDetailView;
import com.guidinglight.decisionhub.usecase.qdr.replay.EvaluationCaseRepository;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionVerdictRepository;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayCaseRecord;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayCaseRepository;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotAssembler;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotAssemblyException;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotAssemblyRequest;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotHash;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotHasher;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotConflictException;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotIdentity;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotRecord;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotSources;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotVersionVector;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotWriteCommand;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.ReplayInputSnapshot;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * V10 canonical replay snapshot 的真实 PostgreSQL/Flyway schema 回归测试。
 *
 * <p>测试使用一次性 PostgreSQL 17 Testcontainers，覆盖 clean migration、V1-V9 upgrade、tenant FK、 immutable
 * trigger、payload/version constraints、tenant-bound JDBC 与 transaction rollback。测试不访问 生产数据库，不调用
 * HTTP/provider/NQ/Agent/LangGraph，也不实现 assembler、canonicalizer 或 replay。
 */
@Testcontainers(disabledWithoutDocker = true)
class V10CanonicalReplaySnapshotFlywayPostgresTest {

    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);

    @Container
  static PostgreSQLContainer<?> pg =
      new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("decision_hub")
            .withUsername("decision_hub")
            .withPassword("decision_hub");

    @Test
    void cleanDatabaseMigratesFromV1ThroughV11() throws Exception {
        final Flyway flyway = resetDatabase();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("11");
        try (Connection connection = connection()) {
      assertThat(
              singleInt(
                            connection,
                            "select count(*) from information_schema.tables"
                                    + " where table_schema='public'"
                                    + " and table_name='qdr_canonical_replay_snapshot'"))
                    .isEqualTo(1);
        }
    }

    @Test
    void existingV1ToV9SchemaUpgradesThroughV11WithoutChangingHistory() throws Exception {
        cleanDatabase();
        final Flyway v9 = flyway("9");
        v9.migrate();
        assertThat(v9.info().current().getVersion().getVersion()).isEqualTo("9");

        final Flyway v11 = flyway("11");
        v11.migrate();

        assertThat(v11.info().current().getVersion().getVersion()).isEqualTo("11");
        try (Connection connection = connection()) {
      assertThat(
              singleInt(
                            connection,
                            "select count(*) from flyway_schema_history"
                                    + " where success and version::integer between 1 and 9"))
                    .isEqualTo(9);
        }
    }

  @Test
  void existingV1ToV10DataUpgradesToV11WithMetadataCommentsOnly() throws Exception {
    cleanDatabase();
    final Flyway v10 = flyway("10");
    v10.migrate();
    try (Connection connection = connection(); Statement statement = connection.createStatement()) {
      insertSources(statement);
      statement.executeUpdate(
          snapshotInsert(
              "00000000-0000-0000-0000-000000000010",
              "tenant-a",
              "snapshot-v10-upgrade",
              "DECISION-1",
              "00000000-0000-0000-0000-000000000004",
              4096,
              contextJson("context-v10-upgrade")));
    }

    final Flyway v11 = flyway("11");
    v11.migrate();

    assertThat(v11.info().current().getVersion().getVersion()).isEqualTo("11");
    try (Connection connection = connection()) {
      assertThat(singleInt(connection, "select count(*) from qdr_canonical_replay_snapshot"))
          .isEqualTo(1);
      assertThat(
              singleInt(
                  connection,
                  "select count(*) from pg_constraint"
                      + " where conname like '%qdr_canonical_snapshot%'"
                      + " and obj_description(oid, 'pg_constraint') is not null"))
          .isEqualTo(21);
      assertThat(
              singleInt(
                  connection,
                  "select count(*) from pg_class where relkind='i'"
                      + " and relname like 'idx_qdr_canonical_snapshot_%'"
                      + " and obj_description(oid, 'pg_class') is not null"))
          .isEqualTo(4);
    }
  }

    @Test
    void safeStructuredSnapshotInsertsAndUpdateTriggerRejectsMutation() throws Exception {
        resetDatabase();
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
            insertSources(statement);
      statement.executeUpdate(
          snapshotInsert(
                    "00000000-0000-0000-0000-000000000010",
                    "tenant-a",
                    "snapshot-1",
                    "DECISION-1",
                    "00000000-0000-0000-0000-000000000004",
                    4096,
                    contextJson("context-1")));

            assertThat(singleInt(connection, "select count(*) from qdr_canonical_replay_snapshot"))
                    .isEqualTo(1);
      assertThatThrownBy(
              () ->
                  statement.executeUpdate(
                            "update qdr_canonical_replay_snapshot set payload_bytes=payload_bytes+1"
                                    + " where tenant_id='tenant-a' and snapshot_id='snapshot-1'"))
                    .isInstanceOf(SQLException.class)
                    .extracting(error -> ((SQLException) error).getSQLState())
                    .isEqualTo("55000");
        }
    }

  @Test
  void tenantBoundJdbcInsertExactReadsIdempotencyAndIdentityIsolationWork() throws Exception {
    resetDatabase();
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
      insertSources(statement);
    }
    final JdbcTemplate jdbcTemplate = jdbcTemplate();
    final JdbcCanonicalReplaySnapshotRepository snapshots =
        new JdbcCanonicalReplaySnapshotRepository(jdbcTemplate, new ObjectMapper());
    final CanonicalReplaySnapshotWriteCommand record = snapshotCommand(HASH_A);

    final CanonicalReplaySnapshotRecord persisted = snapshots.insert("tenant-a", record);
    assertThat(persisted.identity()).isEqualTo(record.identity());
    assertThat(persisted.versionVector()).isEqualTo(record.versionVector());
    assertThat(persisted.canonicalInputHash()).isEqualTo(record.canonicalInputHash());
    assertThat(persisted.evidenceRefs().getFirst().evidence().summary()).isEqualTo("V5");
    assertThat(persisted.createdAt()).isNotEqualTo(record.sourceCapturedAt());
    assertThat(persisted.identity().evaluationCaseRowId()).isNull();
    assertThat(persisted.identity().regressionVerdictRowId()).isNull();
    assertThat(persisted.createdAt())
        .isEqualTo(
            jdbcTemplate.queryForObject(
                    "select created_at from qdr_canonical_replay_snapshot"
                        + " where tenant_id=? and snapshot_id=?",
                    java.sql.Timestamp.class,
                    "tenant-a",
                    "snapshot-jdbc")
                .toInstant());
    assertThat(snapshots.findByTenantAndSnapshotId("tenant-a", "snapshot-jdbc"))
        .contains(persisted);
    assertThat(
            snapshots.findByTenantAndIdentity("tenant-a", record.identity(), "QDR6-REPLAY-INPUT-1"))
        .contains(persisted);
    assertThat(snapshots.findByTenantAndSnapshotId("tenant-b", "snapshot-jdbc")).isEmpty();
    assertThat(snapshots.insert("tenant-a", record)).isEqualTo(persisted);
    assertThatThrownBy(() -> snapshots.insert("tenant-a", snapshotCommand(HASH_B)))
        .isInstanceOf(CanonicalReplaySnapshotConflictException.class);

    final JdbcPromptVersionRepository prompts = new JdbcPromptVersionRepository(jdbcTemplate);
    assertThat(
            prompts.findByTenantAndPromptVersionId(
                "tenant-a", uuid("00000000-0000-0000-0000-000000000004")))
        .isPresent();
    assertThat(
            prompts.findByTenantAndPromptVersionId(
                "tenant-b", uuid("00000000-0000-0000-0000-000000000004")))
        .isEmpty();

    final JdbcModelGatewayCallRepository calls = new JdbcModelGatewayCallRepository(jdbcTemplate);
    assertThat(
            calls.findByTenantAndDecisionRunAndModelCallRef(
                "tenant-a", uuid("00000000-0000-0000-0000-000000000002"), "call-1"))
        .isPresent();
    assertThat(
            calls.findByTenantAndDecisionRunAndModelCallRef(
                "tenant-a", uuid("00000000-0000-0000-0000-000000000099"), "call-1"))
        .isEmpty();
    assertThat(
            calls.findByTenantAndDecisionRunAndModelCallRef(
                "tenant-a", uuid("00000000-0000-0000-0000-000000000002"), "call-other"))
        .isEmpty();
    assertThat(
            calls.findByTenantAndDecisionRunAndModelCallRef(
                "tenant-b", uuid("00000000-0000-0000-0000-000000000002"), "call-1"))
        .isEmpty();
  }

  @Test
  void jdbcInsertOmitsDatabaseGeneratedCreatedAt() throws Exception {
    final Field insertField = JdbcCanonicalReplaySnapshotRepository.class.getDeclaredField("INSERT");
    insertField.setAccessible(true);
    final String insertSql = (String) insertField.get(null);

    assertThat(insertSql.toLowerCase(java.util.Locale.ROOT)).doesNotContain("created_at");
    assertThat(
            java.util.Arrays.stream(CanonicalReplaySnapshotWriteCommand.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase(java.util.Locale.ROOT)))
        .noneMatch(name -> name.equals("createdat"));
  }

  @Test
  void v9ReplayInputHashAndStructuredSummaryDriftFailClosed() throws Exception {
    assertSourceMutationRejected(
        "update qdr_replay_input_ref set ref_id='input-other'"
            + " where tenant_id='tenant-a' and id='00000000-0000-0000-0000-000000000020'");
    assertSourceMutationRejected(
        "update qdr_replay_input_ref set content_hash='" + HASH_B + "',"
            + " input_ref=jsonb_set(input_ref, '{contentHash}', to_jsonb('" + HASH_B + "'::text))"
            + " where tenant_id='tenant-a' and id='00000000-0000-0000-0000-000000000020'");
    assertSourceMutationRejected(
        "update qdr_expected_decision_summary set confidence_band='HIGH'"
            + " where tenant_id='tenant-a' and id='00000000-0000-0000-0000-000000000021'");
    assertSourceMutationRejected(
        "update qdr_expected_decision_summary"
            + " set summary_json=jsonb_set(summary_json, '{actionLabel}', '\"NO_TRADE\"'::jsonb)"
            + " where tenant_id='tenant-a' and id='00000000-0000-0000-0000-000000000021'");
  }

  @Test
  void v9ProjectionDoesNotFallbackToCrossTenantInput() throws Exception {
    resetDatabase();
    try (Connection connection = connection(); Statement statement = connection.createStatement()) {
      insertSources(statement);
      statement.executeUpdate(
          "insert into qdr_replay_input_ref("
              + "id,tenant_id,case_id,source_decision_id,source_request_id,trace_id,request_id,"
              + "policy_version,model_gateway_version_ref,ref_type,ref_id,input_ref,content_hash,"
              + "created_at,updated_at) values ("
              + "'00000000-0000-0000-0000-000000000120','tenant-b','case-1','decision-1',"
              + "'request-1','trace-1','request-1','policy-1','gateway-1','SAFE_INPUT','input-1',"
              + "'{\"refType\":\"SAFE_INPUT\",\"refId\":\"input-1\",\"contentHash\":\""
              + HASH_B
              + "\"}'::jsonb,'"
              + HASH_B
              + "',now(),now())");
      statement.executeUpdate(
          "update qdr_replay_input_ref set content_hash='" + HASH_B + "',"
              + " input_ref=jsonb_set(input_ref, '{contentHash}', to_jsonb('" + HASH_B + "'::text))"
              + " where tenant_id='tenant-b' and id='00000000-0000-0000-0000-000000000120'");
    }
    final JdbcCanonicalReplaySnapshotRepository snapshots =
        new JdbcCanonicalReplaySnapshotRepository(jdbcTemplate(), new ObjectMapper());
    final CanonicalReplaySnapshotWriteCommand base = snapshotCommand(HASH_A);
    final CanonicalReplaySnapshotWriteCommand crossTenantProjection =
        new CanonicalReplaySnapshotWriteCommand(
            base.id(), base.identity(), base.source(), base.decisionType(), base.sourceCapturedAt(),
            base.subject(), base.contextSnapshot(), base.evidenceRefs(),
            new ReplayInputRef("SAFE_INPUT", "input-1", HASH_B), base.expectedDecisionSummary(),
            base.versionVector(), HASH_B, base.expectedSummaryHash(), base.providerSummaryHash(),
            base.canonicalInputHash(), base.payloadBytes());

    assertThatThrownBy(() -> snapshots.insert("tenant-a", crossTenantProjection))
        .isInstanceOf(CanonicalReplaySnapshotPersistenceException.class)
        .hasMessageContaining("exact projection mismatch");
  }

  @Test
  void surroundingTransactionRollbackRemovesSnapshotInsert() throws Exception {
    resetDatabase();
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
      insertSources(statement);
    }
    final DriverManagerDataSource dataSource = dataSource();
    final JdbcCanonicalReplaySnapshotRepository snapshots =
        new JdbcCanonicalReplaySnapshotRepository(new JdbcTemplate(dataSource), new ObjectMapper());
    final TransactionTemplate transaction =
        new TransactionTemplate(new DataSourceTransactionManager(dataSource));

    transaction.executeWithoutResult(
        status -> {
          snapshots.insert("tenant-a", snapshotCommand(HASH_A));
          status.setRollbackOnly();
        });

    assertThat(snapshots.findByTenantAndSnapshotId("tenant-a", "snapshot-jdbc")).isEmpty();
  }

  @Test
  void v5V6V8V9IdentityDriftAndJdbcFailureRemainStructuredFailClosed() throws Exception {
    assertSourceMutationRejected(
        "update dh_decision_request set trace_id='trace-other'"
            + " where tenant_id='tenant-a' and decision_id='decision-1'");
    assertSourceMutationRejected(
        "update decision_request set request_id='request-other'"
            + " where tenant_id='tenant-a'"
            + " and id='00000000-0000-0000-0000-000000000001'");
    assertSourceMutationRejected(
        "update qdr_model_gateway_call set model_call_ref='call-other'"
            + " where tenant_id='tenant-a'"
            + " and id='00000000-0000-0000-0000-000000000008'");
    assertSourceMutationRejected(
        "update qdr_replay_case set trace_id='trace-other'"
            + " where tenant_id='tenant-a'"
            + " and id='00000000-0000-0000-0000-000000000022'");

    resetDatabase();
    final JdbcCanonicalReplaySnapshotRepository snapshots =
        new JdbcCanonicalReplaySnapshotRepository(jdbcTemplate(), new ObjectMapper());
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
      statement.execute("drop table qdr_canonical_replay_snapshot");
    }
    assertThatThrownBy(() -> snapshots.findByTenantAndSnapshotId("tenant-a", "snapshot-jdbc"))
        .isInstanceOf(CanonicalReplaySnapshotPersistenceException.class)
        .hasMessageContaining("find canonical replay snapshot failed");
  }

  private static void assertSourceMutationRejected(final String mutation) throws Exception {
    resetDatabase();
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
      insertSources(statement);
    }
    final JdbcCanonicalReplaySnapshotRepository snapshots =
        new JdbcCanonicalReplaySnapshotRepository(jdbcTemplate(), new ObjectMapper());
    snapshots.insert("tenant-a", snapshotCommand(HASH_A));
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(mutation);
    }
    assertThatThrownBy(() -> snapshots.findByTenantAndSnapshotId("tenant-a", "snapshot-jdbc"))
        .isInstanceOf(CanonicalReplaySnapshotPersistenceException.class)
        .hasMessageContaining("exact");
  }

    @Test
    void requiredVersionDuplicateIdentityAndOrphanIdentityFailClosed() throws Exception {
        resetDatabase();
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
            insertSources(statement);

      assertSqlRejected(
          statement,
          snapshotInsert(
                    "00000000-0000-0000-0000-000000000011",
                    "tenant-a",
                    "snapshot-null-version",
                    null,
                    "00000000-0000-0000-0000-000000000004",
                    4096,
                    contextJson("context-null-version")));

      statement.executeUpdate(
          snapshotInsert(
                    "00000000-0000-0000-0000-000000000012",
                    "tenant-a",
                    "snapshot-duplicate",
                    "DECISION-1",
                    "00000000-0000-0000-0000-000000000004",
                    4096,
                    contextJson("context-duplicate")));
      assertSqlRejected(
          statement,
          snapshotInsert(
                    "00000000-0000-0000-0000-000000000013",
                    "tenant-a",
                    "snapshot-duplicate",
                    "DECISION-1",
                    "00000000-0000-0000-0000-000000000004",
                    4096,
                    contextJson("context-duplicate")));

      assertSqlRejected(
          statement,
          snapshotInsert(
                    "00000000-0000-0000-0000-000000000014",
                    "tenant-a",
                    "snapshot-orphan",
                    "DECISION-1",
                    "00000000-0000-0000-0000-000000000099",
                    4096,
                    contextJson("context-orphan")));

      assertSqlRejected(
          statement,
          snapshotInsert(
                    "00000000-0000-0000-0000-000000000015",
                    "tenant-b",
                    "snapshot-cross-tenant",
                    "DECISION-1",
                    "00000000-0000-0000-0000-000000000004",
                    4096,
                    contextJson("context-cross-tenant")));
        }
    }

    @Test
    void totalAndPerFieldPayloadLimitsRejectOversizeRows() throws Exception {
        resetDatabase();
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
            insertSources(statement);

      assertSqlRejected(
          statement,
          snapshotInsert(
                    "00000000-0000-0000-0000-000000000016",
                    "tenant-a",
                    "snapshot-total-oversize",
                    "DECISION-1",
                    "00000000-0000-0000-0000-000000000004",
                    262145,
                    contextJson("context-total-oversize")));

      assertSqlRejected(
          statement,
          snapshotInsert(
                    "00000000-0000-0000-0000-000000000017",
                    "tenant-a",
                    "snapshot-context-oversize",
                    "DECISION-1",
                    "00000000-0000-0000-0000-000000000004",
                    200000,
                    contextJson("x".repeat(131073))));

      assertSqlRejected(
          statement,
          snapshotInsertWithPayloads(
                    "00000000-0000-0000-0000-000000000018",
                    "tenant-a",
                    "snapshot-evidence-oversize",
                    "DECISION-1",
                    "00000000-0000-0000-0000-000000000004",
                    200000,
                    contextJson("context-evidence-oversize"),
                    "[{\"refId\":\"" + "x".repeat(65537) + "\"}]",
                    expectedSummaryJson("MEDIUM")));

      assertSqlRejected(
          statement,
          snapshotInsertWithPayloads(
                    "00000000-0000-0000-0000-000000000019",
                    "tenant-a",
                    "snapshot-summary-oversize",
                    "DECISION-1",
                    "00000000-0000-0000-0000-000000000004",
                    200000,
                    contextJson("context-summary-oversize"),
                    evidenceRefsJson(),
                    expectedSummaryJson("x".repeat(32769))));
        }
    }

    @Test
    void failedV10MigrationRollsBackEarlierAlterStatements() throws Exception {
        cleanDatabase();
        flyway("9").migrate();
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
            statement.execute("create table qdr_canonical_replay_snapshot(marker integer)");
        }

        assertThatThrownBy(() -> flyway("11").migrate()).isInstanceOf(FlywayException.class);

        try (Connection connection = connection()) {
      assertThat(
              singleInt(
                            connection,
                            "select count(*) from pg_constraint"
                                    + " where conname='ux_dh_decision_request_tenant_decision'"))
                    .isZero();
      assertThat(
              singleInt(
                            connection,
                            "select count(*) from pg_constraint"
                                    + " where conname='ux_qdr_model_gateway_call_tenant_wide_identity'"))
                    .isZero();
        }
    }

  @Test
  void p3ServiceUsesRealRepeatableReadAndPersistsAfterHashWithExactReadBack()
      throws Exception {
    resetDatabase();
    try (Connection connection = connection(); Statement statement = connection.createStatement()) {
      insertSources(statement);
    }
    final DriverManagerDataSource dataSource = dataSource();
    final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    final JdbcCanonicalReplaySnapshotRepository delegate =
        new JdbcCanonicalReplaySnapshotRepository(jdbcTemplate, new ObjectMapper());
    final AtomicReference<String> isolation = new AtomicReference<>();
    final CanonicalReplaySnapshotPersistencePort observing =
        observingPort(
            delegate,
            () -> isolation.set(jdbcTemplate.queryForObject("show transaction_isolation", String.class)),
            false);
    final CanonicalReplaySnapshotWriteCommand command = snapshotCommand(HASH_B);
    final ReplayInputSnapshotAssemblyService service =
        p3Service(observing, new DataSourceTransactionManager(dataSource), command);

    final CanonicalReplaySnapshotRecord persisted =
        service.assembleHashAndPersist(p3Request(command));
    final CanonicalReplaySnapshotRecord duplicate =
        service.assembleHashAndPersist(p3Request(command));
    final CanonicalReplaySnapshotWriteCommand conflictingCommand = snapshotCommand("c".repeat(64));
    final ReplayInputSnapshotAssemblyService conflictingService =
        p3Service(observing, new DataSourceTransactionManager(dataSource), conflictingCommand);

    assertThat(isolation.get()).isEqualTo("repeatable read");
    assertThat(persisted).isEqualTo(duplicate);
    assertThatThrownBy(
            () -> conflictingService.assembleHashAndPersist(p3Request(conflictingCommand)))
        .isInstanceOf(CanonicalReplaySnapshotConflictException.class);
    assertThat(persisted.canonicalInputHash()).isEqualTo(HASH_B);
    assertThat(persisted.createdAt())
        .isEqualTo(
            jdbcTemplate
                .queryForObject(
                    "select created_at from qdr_canonical_replay_snapshot"
                        + " where tenant_id=? and snapshot_id=?",
                    java.sql.Timestamp.class,
                    "tenant-a",
                    "snapshot-jdbc")
                .toInstant());
    assertThat(delegate.findByTenantAndSnapshotId("tenant-b", "snapshot-jdbc")).isEmpty();
  }

  @Test
  void p3FailureAfterInsertRollsBackEntirePostgresTransaction() throws Exception {
    resetDatabase();
    try (Connection connection = connection(); Statement statement = connection.createStatement()) {
      insertSources(statement);
    }
    final DriverManagerDataSource dataSource = dataSource();
    final JdbcCanonicalReplaySnapshotRepository delegate =
        new JdbcCanonicalReplaySnapshotRepository(new JdbcTemplate(dataSource), new ObjectMapper());
    final CanonicalReplaySnapshotWriteCommand command = snapshotCommand(HASH_B);
    final ReplayInputSnapshotAssemblyService service =
        p3Service(
            observingPort(delegate, () -> {}, true),
            new DataSourceTransactionManager(dataSource),
            command);

    assertThatThrownBy(() -> service.assembleHashAndPersist(p3Request(command)))
        .isInstanceOf(CanonicalReplaySnapshotAssemblyException.class);
    assertThat(delegate.findByTenantAndSnapshotId("tenant-a", "snapshot-jdbc")).isEmpty();
  }

  @Test
  void p3TransactionManagerMissingFailsFastAndSourceDriftPreventsInsert() {
    final CanonicalReplaySnapshotPersistencePort persistence =
        mock(CanonicalReplaySnapshotPersistencePort.class);
    final CanonicalReplaySnapshotWriteCommand command = snapshotCommand(HASH_B);
    assertThatThrownBy(() -> p3Service(persistence, null, command))
        .isInstanceOf(CanonicalReplaySnapshotAssemblyException.class)
        .extracting(error -> ((CanonicalReplaySnapshotAssemblyException) error).code())
        .isEqualTo(CanonicalReplaySnapshotAssemblyException.Code.TRANSACTION_MANAGER_REQUIRED);

    final ReplayInputSnapshot first = mock(ReplayInputSnapshot.class);
    final ReplayInputSnapshot changed = mock(ReplayInputSnapshot.class);
    final ReplayInputSnapshotAssemblyService service =
        p3Service(
            persistence,
            new DataSourceTransactionManager(dataSource()),
            command,
            first,
            changed);
    assertThatThrownBy(() -> service.assembleHashAndPersist(p3Request(command)))
        .isInstanceOf(CanonicalReplaySnapshotAssemblyException.class)
        .extracting(error -> ((CanonicalReplaySnapshotAssemblyException) error).code())
        .isEqualTo(CanonicalReplaySnapshotAssemblyException.Code.SOURCE_CHANGED);
    org.mockito.Mockito.verifyNoInteractions(persistence);
  }

  private static ReplayInputSnapshotAssemblyService p3Service(
      final CanonicalReplaySnapshotPersistencePort persistence,
      final org.springframework.transaction.PlatformTransactionManager transactionManager,
      final CanonicalReplaySnapshotWriteCommand command,
      final ReplayInputSnapshot... assemblySnapshots) {
    final DecisionReplayQueryRepository replayQuery = mock(DecisionReplayQueryRepository.class);
    final DecisionReadModelQueryPort readModel = mock(DecisionReadModelQueryPort.class);
    final PromptVersionPersistencePort prompt = mock(PromptVersionPersistencePort.class);
    final ModelVersionPersistencePort model = mock(ModelVersionPersistencePort.class);
    final ModelGatewayCallPersistencePort gateway = mock(ModelGatewayCallPersistencePort.class);
    final ReplayCaseRepository replayCase = mock(ReplayCaseRepository.class);
    final EvaluationCaseRepository evaluation = mock(EvaluationCaseRepository.class);
    final RegressionVerdictRepository verdict = mock(RegressionVerdictRepository.class);
    final DecisionEvidenceAggregateService aggregateService = mock(DecisionEvidenceAggregateService.class);
    final CanonicalReplaySnapshotAssembler assembler = mock(CanonicalReplaySnapshotAssembler.class);
    final CanonicalReplaySnapshotHasher hasher = mock(CanonicalReplaySnapshotHasher.class);
    final ReplayInputSnapshot snapshot =
        assemblySnapshots.length == 0 ? mock(ReplayInputSnapshot.class) : assemblySnapshots[0];

    when(replayQuery.findReplay(any())).thenReturn(mock(com.guidinglight.decisionhub.domain.decision.DecisionReplayView.class));
    when(readModel.findDecisionRunDetail(any())).thenReturn(Optional.of(mock(DecisionRunDetailView.class)));
    when(prompt.findByTenantAndPromptVersionId(any(), any()))
        .thenReturn(Optional.of(mock(PromptVersionRecord.class)));
    when(model.findByTenantAndModelVersionId(any(), any()))
        .thenReturn(Optional.of(mock(ModelVersionRecord.class)));
    when(gateway.findByTenantAndDecisionRunAndModelCallRef(any(), any(), any()))
        .thenReturn(Optional.of(mock(ModelGatewayCallRecord.class)));
    when(replayCase.findById(any(), any())).thenReturn(Optional.of(mock(ReplayCaseRecord.class)));
    when(aggregateService.aggregate(any(), any())).thenReturn(mock(DecisionEvidenceAggregate.class));
    if (assemblySnapshots.length > 1) {
      when(assembler.assemble(any(), any(CanonicalReplaySnapshotSources.class)))
          .thenReturn(assemblySnapshots[0], assemblySnapshots[1]);
    } else {
      when(assembler.assemble(any(), any(CanonicalReplaySnapshotSources.class))).thenReturn(snapshot);
    }
    when(hasher.hash(any()))
        .thenReturn(new CanonicalReplaySnapshotHash("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8), HASH_B));
    when(assembler.toWriteCommand(any(), any(), any(), any())).thenReturn(command);

    return new ReplayInputSnapshotAssemblyService(
        replayQuery,
        readModel,
        prompt,
        model,
        gateway,
        replayCase,
        evaluation,
        verdict,
        aggregateService,
        persistence,
        assembler,
        hasher,
        transactionManager);
  }

  private static CanonicalReplaySnapshotAssemblyRequest p3Request(
      final CanonicalReplaySnapshotWriteCommand command) {
    final CanonicalReplaySnapshotIdentity id = command.identity();
    return new CanonicalReplaySnapshotAssemblyRequest(
        command.id(),
        id.snapshotId(),
        id.tenantId(),
        id.correlation().traceId(),
        id.correlation().requestId(),
        id.correlation().decisionId(),
        id.decisionRequestId(),
        id.decisionRunId(),
        id.modelCallId(),
        id.modelCallRef(),
        id.promptVersionId(),
        id.modelVersionId(),
        id.replayCaseRowId(),
        id.replayCaseId(),
        id.evaluationCaseRowId(),
        id.evaluationCaseId(),
        id.regressionVerdictRowId(),
        id.regressionVerdictId(),
        null,
        DecisionEvidencePolicy.CORE_DECISION,
        command.versionVector());
  }

  private static CanonicalReplaySnapshotPersistencePort observingPort(
      final CanonicalReplaySnapshotPersistencePort delegate,
      final Runnable beforeInsert,
      final boolean failAfterInsert) {
    return new CanonicalReplaySnapshotPersistencePort() {
      @Override
      public CanonicalReplaySnapshotRecord insert(
          final String tenantId, final CanonicalReplaySnapshotWriteCommand command) {
        beforeInsert.run();
        final CanonicalReplaySnapshotRecord inserted = delegate.insert(tenantId, command);
        if (failAfterInsert) {
          throw new CanonicalReplaySnapshotAssemblyException(
              CanonicalReplaySnapshotAssemblyException.Code.PERSISTENCE_MISMATCH,
              "synthetic post-insert failure");
        }
        return inserted;
      }

      @Override
      public Optional<CanonicalReplaySnapshotRecord> findByTenantAndSnapshotId(
          final String tenantId, final String snapshotId) {
        return delegate.findByTenantAndSnapshotId(tenantId, snapshotId);
      }

      @Override
      public Optional<CanonicalReplaySnapshotRecord> findByTenantAndIdentity(
          final String tenantId,
          final CanonicalReplaySnapshotIdentity identity,
          final String snapshotSchemaVersion) {
        return delegate.findByTenantAndIdentity(tenantId, identity, snapshotSchemaVersion);
      }
    };
  }

  private static CanonicalReplaySnapshotWriteCommand snapshotCommand(
      final String canonicalInputHash) {
    final Instant capturedAt = Instant.parse("2026-07-11T00:00:00Z");
    final DecisionEvidenceCorrelation correlation =
        new DecisionEvidenceCorrelation("tenant-a", "trace-1", "request-1", "decision-1");
    final CanonicalReplaySnapshotIdentity identity =
        new CanonicalReplaySnapshotIdentity(
            correlation,
            "snapshot-jdbc",
            uuid("00000000-0000-0000-0000-000000000001"),
            uuid("00000000-0000-0000-0000-000000000002"),
            uuid("00000000-0000-0000-0000-000000000008"),
            "call-1",
            uuid("00000000-0000-0000-0000-000000000004"),
            uuid("00000000-0000-0000-0000-000000000007"),
            uuid("00000000-0000-0000-0000-000000000022"),
            "case-1",
            null,
            null,
            null,
            null);
    final DecisionEvidenceRef evidence =
        new DecisionEvidenceRef(
                new DecisionEvidence("evidence-1", "REQUEST", "safe request summary"),
            correlation,
            DecisionEvidencePolicy.EvidenceType.REQUEST,
            "evidence-1",
            HASH_A,
            "V5",
            true,
            RedactionStatus.REDACTED);
    return new CanonicalReplaySnapshotWriteCommand(
        uuid("00000000-0000-0000-0000-000000000030"),
        identity,
        "TEST_SOURCE",
        "READ_ONLY_RECOMMENDATION",
        capturedAt,
        new DecisionSubject("BTC-USDT", "SPOT", "1h", null, null),
        new DecisionContextSnapshot("context-1", capturedAt, List.of("evidence-1")),
        List.of(evidence),
        new ReplayInputRef("SAFE_INPUT", "input-1", HASH_A),
        new ExpectedDecisionSummary(
            "READ_ONLY_RECOMMENDATION",
            "OBSERVE",
            "MEDIUM",
            RiskLevel.LOW,
            List.of("evidence-1"),
            List.of("PLACE_ORDER")),
        new CanonicalReplaySnapshotVersionVector(
            "QDR6-REPLAY-INPUT-1",
            "DECISION-1",
            "QDR6-CONTEXT-1",
            "policy-1",
            "evaluation-policy-1",
            "prompt-1",
            HASH_A,
            "model-1",
            HASH_A,
            "gateway-1",
            "QDR6-CJSON-1",
            "QDR6-MOCK-REPLAY-1",
            "SHA-256"),
        HASH_A,
        HASH_A,
        null,
        canonicalInputHash,
        4096);
  }

  private static JdbcTemplate jdbcTemplate() {
    return new JdbcTemplate(dataSource());
  }

  private static DriverManagerDataSource dataSource() {
    final DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.postgresql.Driver");
    dataSource.setUrl(pg.getJdbcUrl());
    dataSource.setUsername(pg.getUsername());
    dataSource.setPassword(pg.getPassword());
    return dataSource;
  }

  private static UUID uuid(final String value) {
    return UUID.fromString(value);
  }

    private static Flyway resetDatabase() {
        cleanDatabase();
        final Flyway flyway = flyway("11");
        flyway.migrate();
        return flyway;
    }

    private static void cleanDatabase() {
        flyway(null).clean();
    }

    private static Flyway flyway(final String target) {
    final var configuration =
        Flyway.configure()
                .cleanDisabled(false)
                .dataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())
                .locations("filesystem:src/main/resources/db/migration");
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
    }

    private static int singleInt(final Connection connection, final String sql) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getInt(1);
        }
    }

    private static void assertSqlRejected(final Statement statement, final String sql) {
        assertThatThrownBy(() -> statement.executeUpdate(sql)).isInstanceOf(SQLException.class);
    }

    private static void insertSources(final Statement statement) throws SQLException {
    statement.executeUpdate(
        """
                insert into dh_decision_request(
                  decision_id, request_id, trace_id, tenant_id, source, decision_type,
                  subject_json, context_ref, requested_at, schema_version, created_at
                ) values (
                  'decision-1', 'request-1', 'trace-1', 'tenant-a', 'TEST_SOURCE',
                  'READ_ONLY_RECOMMENDATION',
                  '{"symbol":"BTC-USDT","market":"SPOT","timeframe":"1h"}'::jsonb,
                  'context-1', '2026-07-11T00:00:00Z', 'DECISION-1', '2026-07-11T00:00:01Z'
                )
                """);
    statement.executeUpdate(
        """
                insert into decision_request(
                  id, request_key, request_type, source_system, source_ref_id, tenant_id,
                  trace_id, request_id, input_payload_json, context_payload_json,
                  status, created_at, updated_at
                ) values (
                  '00000000-0000-0000-0000-000000000001', 'request-key-1',
                  'QUANT_DECISION_REVIEW', 'TEST_SOURCE', 'source-ref-1', 'tenant-a',
                  'trace-1', 'request-1', '{}'::jsonb, '{}'::jsonb,
                  'ACCEPTED', '2026-07-11T00:00:00Z', '2026-07-11T00:00:00Z'
                )
                """);
    statement.executeUpdate(
        """
                insert into decision_run(
                  id, decision_request_id, run_no, status, orchestrator_key, model_provider,
                  model_name, started_at, finished_at, latency_ms, created_at
                ) values (
                  '00000000-0000-0000-0000-000000000002',
                  '00000000-0000-0000-0000-000000000001', 1, 'SUCCEEDED',
                  'deterministic-mock', null, null,
                  '2026-07-11T00:00:00Z', '2026-07-11T00:00:01Z', 1000,
                  '2026-07-11T00:00:00Z'
                )
                """);
    statement.executeUpdate(
        """
                insert into qdr_prompt_template(
                  id, tenant_id, template_key, display_name, current_version_id,
                  status, created_at, updated_at
                ) values (
                  '00000000-0000-0000-0000-000000000003', 'tenant-a', 'template-1',
                  'Template 1', null, 'ACTIVE', '2026-07-11T00:00:00Z', '2026-07-11T00:00:00Z'
                )
                """);
    statement.executeUpdate(
        """
                insert into qdr_prompt_version(
                  id, tenant_id, prompt_template_id, version, render_policy_key,
                  template_ref, template_hash, redacted_summary, status, checksum,
                  created_at, created_by
                ) values (
                  '00000000-0000-0000-0000-000000000004', 'tenant-a',
                  '00000000-0000-0000-0000-000000000003', 'prompt-1', 'render-policy-1',
                  'template-ref-1', '%s', 'safe template metadata', 'ACTIVE', '%s',
                  '2026-07-11T00:00:00Z', 'test'
                )
                """
            .formatted(HASH_A, HASH_A));
    statement.executeUpdate(
        """
                insert into qdr_model_profile(
                  id, tenant_id, provider_profile_id, provider_kind, provider_key,
                  model_key, display_name, capability_summary, context_window_tokens,
                  max_output_tokens, profile_status, trust_policy_ref, created_at, updated_at
                ) values (
                  '00000000-0000-0000-0000-000000000005', 'tenant-a',
                  '00000000-0000-0000-0000-000000000006', 'MOCK', 'mock-provider',
                  'model-key-1', 'Model 1', 'safe capability metadata', 4096, 512,
                  'ENABLED', 'trust-policy-1', '2026-07-11T00:00:00Z', '2026-07-11T00:00:00Z'
                )
                """);
    statement.executeUpdate(
        """
                insert into qdr_model_version(
                  id, tenant_id, model_profile_id, model_name, model_version,
                  capability_summary, version_status, checksum, created_at
                ) values (
                  '00000000-0000-0000-0000-000000000007', 'tenant-a',
                  '00000000-0000-0000-0000-000000000005', 'mock-model', 'model-1',
                  'safe model metadata', 'ACTIVE', '%s', '2026-07-11T00:00:00Z'
                )
                """
            .formatted(HASH_A));
    statement.executeUpdate(
        """
                insert into qdr_model_gateway_call(
                  id, tenant_id, trace_id, request_id, decision_run_id, prompt_version_id,
                  model_version_id, provider_profile_id, provider_kind, provider_identity_ref,
                  status, failure_code, trust_decision, provider_trust_decision_ref,
                  model_call_ref, budget_summary, input_characters, rendered_prompt_characters,
                  output_characters, estimated_tokens, memory_entries, redacted_input_summary,
                  redacted_output_summary, input_hash, output_hash, audit_ref, trace_ref, created_at
                ) values (
                  '00000000-0000-0000-0000-000000000008', 'tenant-a', 'trace-1', 'request-1',
                  '00000000-0000-0000-0000-000000000002',
                  '00000000-0000-0000-0000-000000000004',
                  '00000000-0000-0000-0000-000000000007',
                  '00000000-0000-0000-0000-000000000006', 'MOCK', 'provider-ref-1',
                  'SUCCEEDED', null, 'ALLOWED', 'trust-decision-ref-1', 'call-1',
                  'safe budget metadata', 10, 10, 10, 10, 1,
                  'safe input metadata', 'safe output metadata', '%s', '%s',
                  'audit-ref-1', 'trace-ref-1', '2026-07-11T00:00:00Z'
                )
                """
            .formatted(HASH_A, HASH_B));
    statement.executeUpdate(
        """
                insert into qdr_replay_input_ref(
                  id, tenant_id, case_id, source_decision_id, source_request_id,
                  trace_id, request_id, policy_version, model_gateway_version_ref,
                  ref_type, ref_id, input_ref, content_hash, created_at, updated_at
                ) values (
                  '00000000-0000-0000-0000-000000000020', 'tenant-a', 'case-1',
                  'decision-1', 'request-1', 'trace-1', 'request-1', 'policy-1', 'gateway-1',
                  'SAFE_INPUT', 'input-1',
                  '{"refType":"SAFE_INPUT","refId":"input-1","contentHash":"%s"}'::jsonb,
                  '%s', '2026-07-11T00:00:00Z', '2026-07-11T00:00:00Z'
                )
                """
            .formatted(HASH_A, HASH_A));
    statement.executeUpdate(
        """
                insert into qdr_expected_decision_summary(
                  id, tenant_id, case_id, source_decision_id, source_request_id,
                  trace_id, request_id, policy_version, model_gateway_version_ref,
                  input_ref_id, output_ref_id, summary_role, decision_type, action_label,
                  confidence_band, risk_level, summary_json, required_evidence_refs_json,
                  forbidden_actions_json, summary_hash, created_at, updated_at
                ) values (
                  '00000000-0000-0000-0000-000000000021', 'tenant-a', 'case-1',
                  'decision-1', 'request-1', 'trace-1', 'request-1', 'policy-1', 'gateway-1',
                  '00000000-0000-0000-0000-000000000020', null, 'EXPECTED',
                  'READ_ONLY_RECOMMENDATION', 'OBSERVE', 'MEDIUM', 'LOW',
                  '{"decisionType":"READ_ONLY_RECOMMENDATION","actionLabel":"OBSERVE",\
                    "confidenceBand":"MEDIUM","riskLevel":"LOW",\
                    "requiredEvidenceRefs":["evidence-1"],\
                    "forbiddenActions":["PLACE_ORDER"]}'::jsonb,
                  '["evidence-1"]'::jsonb, '["PLACE_ORDER"]'::jsonb, '%s',
                  '2026-07-11T00:00:00Z', '2026-07-11T00:00:00Z'
                )
                """
            .formatted(HASH_A));
    statement.executeUpdate(
        """
                insert into qdr_replay_case(
                  id, tenant_id, case_id, source_decision_id, source_request_id,
                  trace_id, request_id, policy_version, model_gateway_version_ref,
                  input_ref_id, expected_summary_id, expected_summary_hash, case_checksum,
                  created_at, updated_at
                ) values (
                  '00000000-0000-0000-0000-000000000022', 'tenant-a', 'case-1',
                  'decision-1', 'request-1', 'trace-1', 'request-1', 'policy-1', 'gateway-1',
                  '00000000-0000-0000-0000-000000000020',
                  '00000000-0000-0000-0000-000000000021', '%s', '%s',
                  '2026-07-11T00:00:00Z', '2026-07-11T00:00:00Z'
                )
                """
            .formatted(HASH_A, HASH_B));
    }

    private static String snapshotInsert(
            final String id,
            final String tenantId,
            final String snapshotId,
            final String decisionSchemaVersion,
            final String promptVersionId,
            final int payloadBytes,
            final String contextJson) {
        return snapshotInsertWithPayloads(
                id,
                tenantId,
                snapshotId,
                decisionSchemaVersion,
                promptVersionId,
                payloadBytes,
                contextJson,
                evidenceRefsJson(),
                expectedSummaryJson("MEDIUM"));
    }

    private static String snapshotInsertWithPayloads(
            final String id,
            final String tenantId,
            final String snapshotId,
            final String decisionSchemaVersion,
            final String promptVersionId,
            final int payloadBytes,
            final String contextJson,
            final String evidenceRefsJson,
            final String expectedSummaryJson) {
        final String decisionVersionSql =
                decisionSchemaVersion == null ? "null" : "'" + decisionSchemaVersion + "'";
        return """
                insert into qdr_canonical_replay_snapshot(
                  id, tenant_id, snapshot_id, decision_id, decision_request_id, decision_run_id,
                  trace_id, request_id, source, decision_type, source_captured_at,
                  model_call_id, model_call_ref, prompt_version_id, model_version_id,
                  replay_case_row_id, replay_case_id,
                  subject_json, context_payload_json, evidence_refs_json,
                  replay_input_ref_json, expected_decision_summary_json,
                  snapshot_schema_version, decision_schema_version, context_schema_version,
                  policy_version, evaluation_policy_version, prompt_version_ref,
                  prompt_version_checksum, model_version_ref, model_version_checksum,
                  model_gateway_version_ref, canonicalization_version, replay_executor_version,
                  hash_algorithm_version, replay_input_hash, expected_summary_hash,
                  provider_summary_hash, canonical_input_hash, payload_bytes
                ) values (
                  '%s', '%s', '%s', 'decision-1',
                  '00000000-0000-0000-0000-000000000001',
                  '00000000-0000-0000-0000-000000000002',
                  'trace-1', 'request-1', 'TEST_SOURCE', 'READ_ONLY_RECOMMENDATION',
                  '2026-07-11T00:00:00Z',
                  '00000000-0000-0000-0000-000000000008', 'call-1', '%s',
                  '00000000-0000-0000-0000-000000000007',
                  '00000000-0000-0000-0000-000000000022', 'case-1',
                  '{"symbol":"BTC-USDT","market":"SPOT","timeframe":"1h"}'::jsonb,
                  '%s'::jsonb,
                  '%s'::jsonb,
                  '{"refType":"SAFE_INPUT","refId":"input-1","contentHash":"%s"}'::jsonb,
                  '%s'::jsonb,
                  'QDR6-REPLAY-INPUT-1', %s, 'QDR6-CONTEXT-1',
                  'policy-1', 'evaluation-policy-1', 'prompt-1', '%s',
                  'model-1', '%s', 'gateway-1', 'QDR6-CJSON-1',
                  'QDR6-MOCK-REPLAY-1', 'SHA-256', '%s', '%s', null, '%s', %d
                )
                """
        .formatted(
                id,
                tenantId,
                snapshotId,
                promptVersionId,
                contextJson.replace("'", "''"),
                evidenceRefsJson.replace("'", "''"),
                HASH_A,
                expectedSummaryJson.replace("'", "''"),
                decisionVersionSql,
                HASH_A,
                HASH_A,
                HASH_A,
                HASH_A,
                HASH_A,
                payloadBytes);
    }

    private static String contextJson(final String snapshotId) {
        return "{\"snapshotId\":\""
                + snapshotId
                + "\",\"capturedAt\":\"2026-07-11T00:00:00Z\",\"evidenceRefs\":[\"evidence-1\"]}";
    }

    private static String evidenceRefsJson() {
        return "[{\"evidenceType\":\"REQUEST\",\"refId\":\"evidence-1\","
                + "\"sourceType\":\"V5\",\"mandatory\":true,"
                + "\"redactionStatus\":\"REDACTED\"}]";
    }

    private static String expectedSummaryJson(final String confidenceBand) {
        return "{\"decisionType\":\"READ_ONLY_RECOMMENDATION\","
                + "\"actionLabel\":\"OBSERVE\",\"confidenceBand\":\""
                + confidenceBand
                + "\",\"riskLevel\":\"LOW\",\"requiredEvidenceRefs\":[\"evidence-1\"],"
                + "\"forbiddenActions\":[\"PLACE_ORDER\"]}";
    }
}

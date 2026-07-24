package com.guidinglight.decisionhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Stage-QDR-9 V15 clean/upgrade/schema constraints 的真实 PostgreSQL 17 回归。
 *
 * <p>测试只连接一次性 Testcontainers database，不调用 HTTP、Provider、NQ、Agent/LangGraph、Paper 或 LIVE。
 */
@Testcontainers(disabledWithoutDocker = true)
class V15Qdr9FeedbackPersistenceFlywayPostgresTest {

  private static final String HASH_A = "a".repeat(64);
  private static final String HASH_B = "b".repeat(64);
  private static final String HASH_C = "c".repeat(64);
  private static final Instant OBSERVED_AT = Instant.parse("2026-07-21T09:59:00Z");
  private static final Instant EVALUATION_TIME = Instant.parse("2026-07-21T10:00:00Z");
  private static final List<String> TABLES =
      List.of(
          "qdr_feedback_outcome_observation",
          "qdr_feedback_attribution",
          "qdr_feedback_attribution_contribution",
          "qdr_feedback_attribution_reference");

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("decision_hub")
          .withUsername("decision_hub")
          .withPassword("decision_hub");

  private JdbcTemplate jdbc;

  @BeforeEach
  void migrateFreshDatabase() {
    flyway(null).clean();
    flyway(null).migrate();
    jdbc = new JdbcTemplate(dataSource());
  }

  @Test
  void cleanMigrationCreatesFrozenTablesColumnsConstraintsIndexesAndComments() {
    assertThat(flyway(null).info().current().getVersion().getVersion()).isEqualTo("15");
    assertThat(existingTables()).containsExactlyInAnyOrderElementsOf(TABLES);
    assertThat(columnCount("qdr_feedback_outcome_observation")).isEqualTo(13);
    assertThat(columnCount("qdr_feedback_attribution")).isEqualTo(15);
    assertThat(columnCount("qdr_feedback_attribution_contribution")).isEqualTo(13);
    assertThat(columnCount("qdr_feedback_attribution_reference")).isEqualTo(8);
    assertThat(jsonbColumnCount()).isZero();
    assertThat(timestampTypes())
        .allMatch(type -> type.equals("timestamp with time zone"));
    assertThat(createdAtDefaults())
        .allMatch(value -> value.contains("transaction_timestamp()"));
    assertThat(constraintNames())
        .contains(
            "pk_qdr_feedback_outcome_observation",
            "ux_qdr_feedback_observation_scope",
            "ux_qdr_feedback_observation_idempotency",
            "fk_qdr_feedback_attribution_observation",
            "ux_qdr_feedback_contribution_dimension",
            "ux_qdr_feedback_contribution_order",
            "fk_qdr_feedback_contribution_attribution",
            "ux_qdr_feedback_reference_identity",
            "fk_qdr_feedback_reference_attribution");
    assertThat(indexNames())
        .contains(
            "idx_qdr_feedback_observation_decision",
            "idx_qdr_feedback_observation_trace",
            "idx_qdr_feedback_observation_time",
            "idx_qdr_feedback_attribution_observation",
            "idx_qdr_feedback_attribution_policy",
            "idx_qdr_feedback_attribution_status",
            "idx_qdr_feedback_attribution_time",
            "idx_qdr_feedback_reference_hold");
    assertThat(tableComments()).allMatch(comment -> comment != null && !comment.isBlank());
  }

  @Test
  void v14UpgradePreservesAllExistingSchemaAndDataAndAppliesV15Once() {
    flyway(null).clean();
    final Flyway v14 = flyway("14");
    v14.migrate();
    jdbc = new JdbcTemplate(dataSource());
    assertThat(v14.info().current().getVersion().getVersion()).isEqualTo("14");
    assertThat(existingTables()).isEmpty();
    final List<Map<String, Object>> schemaBefore = existingSchemaSignature();
    final Map<String, Integer> checksumsBefore = checksums(v14.info().applied());

    jdbc.update(
        "insert into dh_nq_feedback_events"
            + " (id,tenant_id,run_id,candidate_id,trace_id,source,event_type,positive,status,"
            + "payload_json,occurred_at,received_at,event_id,schema_version,validation_status)"
            + " values (?,?,?,?,?,?,?,?,?,cast(? as jsonb),?,?,?,?,?)",
        "legacy-feedback-1",
        "tenant-legacy",
        "run-legacy",
        "candidate-legacy",
        "trace-legacy",
        "PAPER",
        "LEGACY_FIXTURE",
        true,
        "RECEIVED",
        "{}",
        java.sql.Timestamp.from(OBSERVED_AT),
        java.sql.Timestamp.from(EVALUATION_TIME),
        "event-legacy-1",
        "1.0.0",
        "VALID");

    final Flyway v15 = flyway(null);
    v15.migrate();

    assertThat(v15.info().current().getVersion().getVersion()).isEqualTo("15");
    assertThat(existingSchemaSignature()).isEqualTo(schemaBefore);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dh_nq_feedback_events where id='legacy-feedback-1'",
                Integer.class))
        .isEqualTo(1);
    for (String table : TABLES) {
      assertThat(
              jdbc.queryForObject("select count(*) from " + table, Integer.class))
          .isZero();
    }
    final Map<String, Integer> checksumsAfter = checksums(v15.info().applied());
    checksumsBefore.forEach(
        (version, checksum) ->
            assertThat(checksumsAfter.get(version)).isEqualTo(checksum));
    assertThat(successfulMigrationCount("15")).isEqualTo(1);
    v15.migrate();
    assertThat(successfulMigrationCount("15")).isEqualTo(1);
  }

  @Test
  void databaseEnforcesScopeIdempotencyFieldAndChildUniquenessContracts() {
    insertObservation("tenant-a", "DEV", "observation-1", HASH_A);
    insertAttribution(
        "tenant-a",
        "DEV",
        "observation-1",
        HASH_B,
        new BigDecimal("0.75000"));

    assertThatThrownBy(
            () ->
                insertObservation(
                    "tenant-a", "DEV", "observation-other", HASH_A))
        .isInstanceOf(RuntimeException.class);
    insertObservation("tenant-b", "DEV", "observation-2", HASH_A);
    insertObservation("tenant-a", "TEST", "observation-3", HASH_A);
    assertThat(observationCountByKey(HASH_A)).isEqualTo(3);

    assertThatThrownBy(
            () ->
                insertObservation("tenant-a", "DEV", "observation-1", HASH_C))
        .isInstanceOf(RuntimeException.class);
    assertThatThrownBy(
            () ->
                insertObservation("tenant-invalid", "PROD", "observation-4", HASH_C))
        .isInstanceOf(RuntimeException.class);
    assertThatThrownBy(
            () ->
                insertObservation(null, "DEV", "observation-5", HASH_C))
        .isInstanceOf(RuntimeException.class);
    assertThatThrownBy(
            () ->
                insertObservation(
                    "x".repeat(129), "DEV", "observation-6", HASH_C))
        .isInstanceOf(RuntimeException.class);

    assertThatThrownBy(
            () ->
                insertAttribution(
                    "tenant-a",
                    "DEV",
                    "observation-1",
                    HASH_B,
                    new BigDecimal("0.50000")))
        .isInstanceOf(RuntimeException.class);
    assertThatThrownBy(
            () ->
                insertAttribution(
                    "tenant-a",
                    "DEV",
                    "observation-1",
                    HASH_C,
                    new BigDecimal("1.00001")))
        .isInstanceOf(RuntimeException.class);

    insertContribution(
        AttributionDimensionValue.EVIDENCE_QUALITY, 0, UUID.randomUUID());
    assertThatThrownBy(
            () ->
                insertContribution(
                    AttributionDimensionValue.RISK_DISCIPLINE,
                    0,
                    UUID.randomUUID()))
        .isInstanceOf(RuntimeException.class);
    insertReference(UUID.randomUUID(), "audit:audit-event-1");
    assertThatThrownBy(
            () -> insertReference(UUID.randomUUID(), "audit:audit-event-1"))
        .isInstanceOf(RuntimeException.class);
  }

  private void insertObservation(
      final String tenantId,
      final String environment,
      final String observationId,
      final String idempotencyKey) {
    jdbc.update(
        "insert into qdr_feedback_outcome_observation"
            + " (id,tenant_id,environment,decision_id,trace_id,observation_id,idempotency_key,"
            + "outcome_source,outcome_status,observed_at,evaluation_time,canonical_hash)"
            + " values (?,?,?,?,?,?,?,?,?,?,?,?)",
        UUID.randomUUID(),
        tenantId,
        environment,
        "decision-" + observationId,
        "trace-" + observationId,
        observationId,
        idempotencyKey,
        "DRY_RUN_RESULT",
        "SUCCEEDED",
        java.sql.Timestamp.from(OBSERVED_AT),
        java.sql.Timestamp.from(EVALUATION_TIME),
        HASH_A);
  }

  private void insertAttribution(
      final String tenantId,
      final String environment,
      final String observationId,
      final String attributionId,
      final BigDecimal confidence) {
    jdbc.update(
        "insert into qdr_feedback_attribution"
            + " (id,tenant_id,environment,observation_id,decision_id,trace_id,observed_at,"
            + "attribution_id,policy_id,policy_version,attribution_status,confidence,"
            + "canonical_hash,error_code)"
            + " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        UUID.randomUUID(),
        tenantId,
        environment,
        observationId,
        "decision-" + observationId,
        "trace-" + observationId,
        java.sql.Timestamp.from(OBSERVED_AT),
        attributionId,
        "policy-1",
        "v1",
        "ATTRIBUTED",
        confidence,
        HASH_A,
        "NONE");
  }

  private void insertContribution(
      final AttributionDimensionValue dimension,
      final int sortOrder,
      final UUID id) {
    jdbc.update(
        "insert into qdr_feedback_attribution_contribution"
            + " (id,tenant_id,environment,attribution_id,dimension,measurement,contribution,"
            + "impact,confidence,reason_code,evidence_ref,sort_order)"
            + " values (?,?,?,?,?,?,?,?,?,?,?,?)",
        id,
        "tenant-a",
        "DEV",
        HASH_B,
        dimension.name(),
        new BigDecimal("0.80000"),
        new BigDecimal("0.60000"),
        "POSITIVE",
        new BigDecimal("0.80000"),
        "MEASUREMENT_POSITIVE",
        "SAFE:evidence-1",
        sortOrder);
  }

  private void insertReference(final UUID id, final String referenceValue) {
    jdbc.update(
        "insert into qdr_feedback_attribution_reference"
            + " (id,tenant_id,environment,attribution_id,reference_type,reference_value,"
            + "reference_status) values (?,?,?,?,?,?,?)",
        id,
        "tenant-a",
        "DEV",
        HASH_B,
        "AUDIT",
        referenceValue,
        "ACTIVE");
  }

  private List<String> existingTables() {
    return jdbc.queryForList(
        "select table_name from information_schema.tables"
            + " where table_schema='public' and table_name like 'qdr_feedback_%'"
            + " order by table_name",
        String.class);
  }

  private int columnCount(final String table) {
    return jdbc.queryForObject(
        "select count(*) from information_schema.columns"
            + " where table_schema='public' and table_name=?",
        Integer.class,
        table);
  }

  private int jsonbColumnCount() {
    return jdbc.queryForObject(
        "select count(*) from information_schema.columns"
            + " where table_schema='public' and table_name in (?,?,?,?) and data_type='jsonb'",
        Integer.class,
        TABLES.toArray());
  }

  private List<String> timestampTypes() {
    return jdbc.queryForList(
        "select data_type from information_schema.columns"
            + " where table_schema='public' and table_name in (?,?,?,?)"
            + " and column_name in ('observed_at','evaluation_time','created_at')",
        String.class,
        TABLES.toArray());
  }

  private List<String> createdAtDefaults() {
    return jdbc.queryForList(
        "select column_default from information_schema.columns"
            + " where table_schema='public' and table_name in (?,?,?,?)"
            + " and column_name='created_at'",
        String.class,
        TABLES.toArray());
  }

  private List<String> constraintNames() {
    return jdbc.queryForList(
        "select conname from pg_constraint"
            + " where conrelid = any(array["
            + TABLES.stream().map(table -> "'public." + table + "'::regclass").collect(Collectors.joining(","))
            + "]) order by conname",
        String.class);
  }

  private List<String> indexNames() {
    return jdbc.queryForList(
        "select indexname from pg_indexes"
            + " where schemaname='public' and tablename in (?,?,?,?) order by indexname",
        String.class,
        TABLES.toArray());
  }

  private List<String> tableComments() {
    return jdbc.queryForList(
        "select obj_description(c.oid) from pg_class c"
            + " join pg_namespace n on n.oid=c.relnamespace"
            + " where n.nspname='public' and c.relname in (?,?,?,?) order by c.relname",
        String.class,
        TABLES.toArray());
  }

  private List<Map<String, Object>> existingSchemaSignature() {
    return jdbc.queryForList(
        "select table_name,column_name,data_type,character_maximum_length,is_nullable,"
            + "column_default,ordinal_position from information_schema.columns"
            + " where table_schema='public' and table_name <> 'flyway_schema_history'"
            + " and table_name not like 'qdr_feedback_%'"
            + " order by table_name,ordinal_position");
  }

  private int observationCountByKey(final String idempotencyKey) {
    return jdbc.queryForObject(
        "select count(*) from qdr_feedback_outcome_observation where idempotency_key=?",
        Integer.class,
        idempotencyKey);
  }

  private int successfulMigrationCount(final String version) {
    return jdbc.queryForObject(
        "select count(*) from flyway_schema_history where version=? and success=true",
        Integer.class,
        version);
  }

  private static Map<String, Integer> checksums(final MigrationInfo[] migrations) {
    return java.util.Arrays.stream(migrations)
        .filter(info -> info.getVersion() != null)
        .collect(
            Collectors.toMap(
                info -> info.getVersion().getVersion(),
                MigrationInfo::getChecksum,
                (left, right) -> right));
  }

  private static Flyway flyway(final String target) {
    final var configuration =
        Flyway.configure().cleanDisabled(false).dataSource(dataSource());
    if (target != null) {
      configuration.target(target);
    }
    return configuration.load();
  }

  private static DataSource dataSource() {
    return new DriverManagerDataSource(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private enum AttributionDimensionValue {
    EVIDENCE_QUALITY,
    RISK_DISCIPLINE
  }
}

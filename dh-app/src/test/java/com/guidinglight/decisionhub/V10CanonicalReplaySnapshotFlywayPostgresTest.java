package com.guidinglight.decisionhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * V10 canonical replay snapshot 的真实 PostgreSQL/Flyway schema 回归测试。
 *
 * <p>测试使用一次性 PostgreSQL 17 Testcontainers，覆盖 clean migration、V1-V9 upgrade、tenant FK、
 * immutable trigger、payload/version constraints 与 migration transaction rollback。测试不访问生产数据库，
 * 不调用 HTTP/provider/NQ/Agent/LangGraph，也不实现 JDBC adapter 或 replay。
 */
@Testcontainers(disabledWithoutDocker = true)
class V10CanonicalReplaySnapshotFlywayPostgresTest {

    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("decision_hub")
            .withUsername("decision_hub")
            .withPassword("decision_hub");

    @Test
    void cleanDatabaseMigratesFromV1ThroughV10() throws Exception {
        final Flyway flyway = resetDatabase();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("10");
        try (Connection connection = connection()) {
            assertThat(singleInt(
                            connection,
                            "select count(*) from information_schema.tables"
                                    + " where table_schema='public'"
                                    + " and table_name='qdr_canonical_replay_snapshot'"))
                    .isEqualTo(1);
        }
    }

    @Test
    void existingV1ToV9SchemaUpgradesToV10WithoutChangingHistory() throws Exception {
        cleanDatabase();
        final Flyway v9 = flyway("9");
        v9.migrate();
        assertThat(v9.info().current().getVersion().getVersion()).isEqualTo("9");

        final Flyway v10 = flyway(null);
        v10.migrate();

        assertThat(v10.info().current().getVersion().getVersion()).isEqualTo("10");
        try (Connection connection = connection()) {
            assertThat(singleInt(
                            connection,
                            "select count(*) from flyway_schema_history"
                                    + " where success and version::integer between 1 and 9"))
                    .isEqualTo(9);
        }
    }

    @Test
    void safeStructuredSnapshotInsertsAndUpdateTriggerRejectsMutation() throws Exception {
        resetDatabase();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            insertSources(statement);
            statement.executeUpdate(snapshotInsert(
                    "00000000-0000-0000-0000-000000000010",
                    "tenant-a",
                    "snapshot-1",
                    "DECISION-1",
                    "00000000-0000-0000-0000-000000000004",
                    4096,
                    contextJson("context-1")));

            assertThat(singleInt(connection, "select count(*) from qdr_canonical_replay_snapshot"))
                    .isEqualTo(1);
            assertThatThrownBy(() -> statement.executeUpdate(
                            "update qdr_canonical_replay_snapshot set payload_bytes=payload_bytes+1"
                                    + " where tenant_id='tenant-a' and snapshot_id='snapshot-1'"))
                    .isInstanceOf(SQLException.class)
                    .extracting(error -> ((SQLException) error).getSQLState())
                    .isEqualTo("55000");
        }
    }

    @Test
    void requiredVersionDuplicateIdentityAndOrphanIdentityFailClosed() throws Exception {
        resetDatabase();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            insertSources(statement);

            assertSqlRejected(statement, snapshotInsert(
                    "00000000-0000-0000-0000-000000000011",
                    "tenant-a",
                    "snapshot-null-version",
                    null,
                    "00000000-0000-0000-0000-000000000004",
                    4096,
                    contextJson("context-null-version")));

            statement.executeUpdate(snapshotInsert(
                    "00000000-0000-0000-0000-000000000012",
                    "tenant-a",
                    "snapshot-duplicate",
                    "DECISION-1",
                    "00000000-0000-0000-0000-000000000004",
                    4096,
                    contextJson("context-duplicate")));
            assertSqlRejected(statement, snapshotInsert(
                    "00000000-0000-0000-0000-000000000013",
                    "tenant-a",
                    "snapshot-duplicate",
                    "DECISION-1",
                    "00000000-0000-0000-0000-000000000004",
                    4096,
                    contextJson("context-duplicate")));

            assertSqlRejected(statement, snapshotInsert(
                    "00000000-0000-0000-0000-000000000014",
                    "tenant-a",
                    "snapshot-orphan",
                    "DECISION-1",
                    "00000000-0000-0000-0000-000000000099",
                    4096,
                    contextJson("context-orphan")));

            assertSqlRejected(statement, snapshotInsert(
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
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            insertSources(statement);

            assertSqlRejected(statement, snapshotInsert(
                    "00000000-0000-0000-0000-000000000016",
                    "tenant-a",
                    "snapshot-total-oversize",
                    "DECISION-1",
                    "00000000-0000-0000-0000-000000000004",
                    262145,
                    contextJson("context-total-oversize")));

            assertSqlRejected(statement, snapshotInsert(
                    "00000000-0000-0000-0000-000000000017",
                    "tenant-a",
                    "snapshot-context-oversize",
                    "DECISION-1",
                    "00000000-0000-0000-0000-000000000004",
                    200000,
                    contextJson("x".repeat(131073))));

            assertSqlRejected(statement, snapshotInsertWithPayloads(
                    "00000000-0000-0000-0000-000000000018",
                    "tenant-a",
                    "snapshot-evidence-oversize",
                    "DECISION-1",
                    "00000000-0000-0000-0000-000000000004",
                    200000,
                    contextJson("context-evidence-oversize"),
                    "[{\"refId\":\"" + "x".repeat(65537) + "\"}]",
                    expectedSummaryJson("MEDIUM")));

            assertSqlRejected(statement, snapshotInsertWithPayloads(
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
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("create table qdr_canonical_replay_snapshot(marker integer)");
        }

        assertThatThrownBy(() -> flyway(null).migrate()).isInstanceOf(FlywayException.class);

        try (Connection connection = connection()) {
            assertThat(singleInt(
                            connection,
                            "select count(*) from pg_constraint"
                                    + " where conname='ux_dh_decision_request_tenant_decision'"))
                    .isZero();
            assertThat(singleInt(
                            connection,
                            "select count(*) from pg_constraint"
                                    + " where conname='ux_qdr_model_gateway_call_tenant_wide_identity'"))
                    .isZero();
        }
    }

    private static Flyway resetDatabase() {
        cleanDatabase();
        final Flyway flyway = flyway(null);
        flyway.migrate();
        return flyway;
    }

    private static void cleanDatabase() {
        flyway(null).clean();
    }

    private static Flyway flyway(final String target) {
        final var configuration = Flyway.configure()
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
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getInt(1);
        }
    }

    private static void assertSqlRejected(final Statement statement, final String sql) {
        assertThatThrownBy(() -> statement.executeUpdate(sql)).isInstanceOf(SQLException.class);
    }

    private static void insertSources(final Statement statement) throws SQLException {
        statement.executeUpdate("""
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
        statement.executeUpdate("""
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
        statement.executeUpdate("""
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
        statement.executeUpdate("""
                insert into qdr_prompt_template(
                  id, tenant_id, template_key, display_name, current_version_id,
                  status, created_at, updated_at
                ) values (
                  '00000000-0000-0000-0000-000000000003', 'tenant-a', 'template-1',
                  'Template 1', null, 'ACTIVE', '2026-07-11T00:00:00Z', '2026-07-11T00:00:00Z'
                )
                """);
        statement.executeUpdate("""
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
                """.formatted(HASH_A, HASH_A));
        statement.executeUpdate("""
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
        statement.executeUpdate("""
                insert into qdr_model_version(
                  id, tenant_id, model_profile_id, model_name, model_version,
                  capability_summary, version_status, checksum, created_at
                ) values (
                  '00000000-0000-0000-0000-000000000007', 'tenant-a',
                  '00000000-0000-0000-0000-000000000005', 'mock-model', 'model-1',
                  'safe model metadata', 'ACTIVE', '%s', '2026-07-11T00:00:00Z'
                )
                """.formatted(HASH_A));
        statement.executeUpdate("""
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
                """.formatted(HASH_A, HASH_B));
        statement.executeUpdate("""
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
                """.formatted(HASH_A, HASH_A));
        statement.executeUpdate("""
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
                  '{"decisionType":"READ_ONLY_RECOMMENDATION","actionLabel":"OBSERVE"}'::jsonb,
                  '["evidence-1"]'::jsonb, '["PLACE_ORDER"]'::jsonb, '%s',
                  '2026-07-11T00:00:00Z', '2026-07-11T00:00:00Z'
                )
                """.formatted(HASH_A));
        statement.executeUpdate("""
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
                """.formatted(HASH_A, HASH_B));
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
                """.formatted(
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

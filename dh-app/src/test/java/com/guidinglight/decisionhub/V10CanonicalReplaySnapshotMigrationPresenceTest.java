package com.guidinglight.decisionhub;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** V10 canonical replay snapshot migration 的文件级顺序、安全和边界回归测试。 */
class V10CanonicalReplaySnapshotMigrationPresenceTest {

    private static final Pattern FORBIDDEN_STORAGE_COLUMN = Pattern.compile(
            "(?m)^\\s*(raw_prompt|raw_provider_response|credential|api_key|api_secret|token|passphrase)"
                    + "\\s+(uuid|varchar|text|jsonb|bytea)",
            Pattern.CASE_INSENSITIVE);

    private static String body;
    private static String lowerBody;

    @BeforeAll
    static void load() throws IOException {
        final Path migration = Path.of(
                        "src",
                        "main",
                        "resources",
                        "db",
                        "migration",
                        "V10__qdr6_canonical_replay_snapshot.sql")
                .toAbsolutePath()
                .normalize();
        final Path v9 = migration.resolveSibling("V9__qdr_replay_evaluation_baseline.sql");
        assertTrue(Files.exists(v9), "V9 must remain present before V10");
        assertTrue(Files.exists(migration), "V10 migration file must exist: " + migration);
        body = Files.readString(migration, StandardCharsets.UTF_8);
        lowerBody = body.toLowerCase(Locale.ROOT);
    }

    @Test
    void declaresFrozenTableCompositeIdentityAndIndexes() {
        assertAll(
                () -> assertTrue(body.contains("create table qdr_canonical_replay_snapshot")),
                () -> assertTrue(body.contains("unique (tenant_id, snapshot_id)")),
                () -> assertTrue(body.contains("ux_qdr_canonical_snapshot_aggregate_identity")),
                () -> assertTrue(body.contains("foreign key (tenant_id, decision_id)")),
                () -> assertTrue(body.contains("foreign key (tenant_id, decision_request_id)")),
                () -> assertTrue(body.contains("foreign key (decision_run_id, decision_request_id)")),
                () -> assertTrue(body.contains("foreign key (tenant_id, model_call_id")),
                () -> assertTrue(body.contains("idx_qdr_canonical_snapshot_tenant_run")),
                () -> assertTrue(body.contains("idx_qdr_canonical_snapshot_tenant_model_call")));
    }

    @Test
    void declaresVersionPayloadAndImmutableConstraints() {
        assertAll(
                () -> assertTrue(body.contains("QDR6-REPLAY-INPUT-1")),
                () -> assertTrue(body.contains("QDR6-CONTEXT-1")),
                () -> assertTrue(body.contains("QDR6-CJSON-1")),
                () -> assertTrue(body.contains("QDR6-MOCK-REPLAY-1")),
                () -> assertTrue(body.contains("SHA-256")),
                () -> assertTrue(body.contains("payload_bytes between 1 and 262144")),
                () -> assertTrue(body.contains("octet_length(context_payload_json::text) <= 131072")),
                () -> assertTrue(body.contains("octet_length(evidence_refs_json::text) <= 65536")),
                () -> assertTrue(
                        body.contains("octet_length(expected_decision_summary_json::text) <= 32768")),
                () -> assertTrue(body.contains("before update on qdr_canonical_replay_snapshot")),
                () -> assertTrue(body.contains("errcode = '55000'")));
    }

    @Test
    void hasCommentsAndNoUnsafeOrRuntimeBoundaryLeak() {
        assertAll(
                () -> assertTrue(body.contains("comment on table qdr_canonical_replay_snapshot")),
                () -> assertTrue(body.contains("comment on trigger")),
                () -> assertFalse(FORBIDDEN_STORAGE_COLUMN.matcher(body).find()),
                () -> assertFalse(lowerBody.contains("create unique index concurrently")),
                () -> assertFalse(lowerBody.contains("requestmapping")),
                () -> assertFalse(lowerBody.contains("controller")),
                () -> assertFalse(lowerBody.contains("webclient")),
                () -> assertFalse(lowerBody.contains("resttemplate")),
                () -> assertFalse(lowerBody.contains("httpclient")),
                () -> assertFalse(lowerBody.contains("autogen")),
                () -> assertFalse(lowerBody.contains("crewai")));
    }
}

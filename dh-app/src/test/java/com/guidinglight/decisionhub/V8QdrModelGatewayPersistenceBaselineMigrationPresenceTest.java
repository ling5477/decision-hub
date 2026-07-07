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

/**
 * stage-qdr-3 B3：校验 V8 QDR model gateway persistence baseline migration。
 *
 * <p>本测试只做文件级审查，不启动数据库；用于防止 V8 缺表、缺约束、缺索引、缺注释或误引入 raw storage 字段。
 */
class V8QdrModelGatewayPersistenceBaselineMigrationPresenceTest {

    private static final Pattern FORBIDDEN_STORAGE_COLUMN =
            Pattern.compile(
                    "(?m)^\\s*(raw_prompt|raw_provider_response|credential|api_key|api_secret|token|passphrase)\\s+"
                            + "(uuid|varchar|text|jsonb|bytea)",
                    Pattern.CASE_INSENSITIVE);

    private static String body;
    private static String lowerBody;

    @BeforeAll
    static void load() throws IOException {
        final Path migration =
                Path.of(
                                "src",
                                "main",
                                "resources",
                                "db",
                                "migration",
                                "V8__qdr_model_gateway_persistence_baseline.sql")
                        .toAbsolutePath()
                        .normalize();
        assertTrue(Files.exists(migration), "V8 migration file must exist: " + migration);
        body = Files.readString(migration, StandardCharsets.UTF_8);
        lowerBody = body.toLowerCase(Locale.ROOT);
    }

    @Test
    void v8ContainsExpectedTables() {
        assertAll(
                () -> assertTrue(body.contains("create table if not exists qdr_prompt_template")),
                () -> assertTrue(body.contains("create table if not exists qdr_prompt_version")),
                () -> assertTrue(body.contains("create table if not exists qdr_model_profile")),
                () -> assertTrue(body.contains("create table if not exists qdr_model_version")),
                () -> assertTrue(body.contains("create table if not exists qdr_model_gateway_call")));
    }

    @Test
    void v8ContainsTenantBoundRequiredFields() {
        assertAll(
                () -> assertTrue(body.contains("tenant_id varchar not null")),
                () -> assertTrue(body.contains("created_at timestamptz not null")),
                () -> assertTrue(body.contains("trace_id varchar not null")),
                () -> assertTrue(body.contains("request_id varchar not null")),
                () -> assertTrue(body.contains("decision_run_id uuid not null")),
                () -> assertTrue(body.contains("prompt_version_id uuid not null")),
                () -> assertTrue(body.contains("model_version_id uuid not null")),
                () -> assertTrue(body.contains("provider_profile_id uuid not null")));
    }

    @Test
    void v8ContainsFkUniqueAndTenantIndexes() {
        assertAll(
                () -> assertTrue(body.contains("foreign key (prompt_template_id) references qdr_prompt_template(id)")),
                () -> assertTrue(body.contains("foreign key (model_profile_id) references qdr_model_profile(id)")),
                () -> assertTrue(body.contains("foreign key (decision_run_id) references decision_run(id)")),
                () -> assertTrue(body.contains("foreign key (prompt_version_id) references qdr_prompt_version(id)")),
                () -> assertTrue(body.contains("foreign key (model_version_id) references qdr_model_version(id)")),
                () -> assertTrue(body.contains("unique (tenant_id, template_key)")),
                () -> assertTrue(body.contains("unique (tenant_id, prompt_template_id, version)")),
                () -> assertTrue(body.contains("unique (tenant_id, model_key)")),
                () -> assertTrue(body.contains("unique (tenant_id, model_call_ref)")),
                () -> assertTrue(body.contains("idx_qdr_model_gateway_call_tenant_decision_run")),
                () -> assertTrue(body.contains("idx_qdr_model_gateway_call_trace_id")),
                () -> assertTrue(body.contains("idx_qdr_model_gateway_call_request_id")));
    }

    @Test
    void v8ContainsCheckConstraints() {
        assertAll(
                () -> assertTrue(body.contains("chk_qdr_prompt_version_checksum")),
                () -> assertTrue(body.contains("chk_qdr_model_profile_provider_kind")),
                () -> assertTrue(body.contains("chk_qdr_model_profile_status")),
                () -> assertTrue(body.contains("chk_qdr_model_version_checksum")),
                () -> assertTrue(body.contains("chk_qdr_model_gateway_call_status")),
                () -> assertTrue(body.contains("chk_qdr_model_gateway_call_failure_code")),
                () -> assertTrue(body.contains("chk_qdr_model_gateway_call_failure_shape")),
                () -> assertTrue(body.contains("chk_qdr_model_gateway_call_hashes")),
                () -> assertTrue(body.contains("chk_qdr_model_gateway_call_budget_non_negative")),
                () -> assertTrue(body.contains("'MOCK'")),
                () -> assertTrue(body.contains("'LOCAL_PLANNED'")),
                () -> assertTrue(body.contains("'SUCCEEDED'")),
                () -> assertTrue(body.contains("'FAILED'")),
                () -> assertTrue(body.contains("'ALLOWED'")),
                () -> assertTrue(body.contains("'DENIED'")));
    }

    @Test
    void v8ForbidsRawPromptRawProviderResponseAndCredentialStorageColumns() {
        assertAll(
                () -> assertFalse(lowerBody.contains("raw_prompt")),
                () -> assertFalse(lowerBody.contains("raw_provider_response")),
                () -> assertFalse(
                        FORBIDDEN_STORAGE_COLUMN.matcher(body).find(),
                        "V8 must not declare raw prompt/provider response or credential storage columns"),
                () -> assertTrue(body.contains("template_ref varchar not null")),
                () -> assertTrue(body.contains("template_hash varchar(64) not null")),
                () -> assertTrue(body.contains("redacted_input_summary text not null")),
                () -> assertTrue(body.contains("redacted_output_summary text")),
                () -> assertTrue(body.contains("input_hash varchar(64) not null")),
                () -> assertTrue(body.contains("output_hash varchar(64)")));
    }

    @Test
    void v8CommentsStateRawAndCredentialStorageBoundary() {
        assertAll(
                () -> assertTrue(lowerBody.contains("raw prompt")),
                () -> assertTrue(lowerBody.contains("raw provider response")),
                () -> assertTrue(lowerBody.contains("credential")),
                () -> assertTrue(lowerBody.contains("不保存 raw prompt")),
                () -> assertTrue(lowerBody.contains("不保存 provider credential")),
                () -> assertTrue(lowerBody.contains("不代表 real provider 已接入")));
    }

    @Test
    void v8DoesNotContainDestructiveHistoricalDdlOrUnexpectedNextMigration() {
        final Path v9 =
                Path.of("src", "main", "resources", "db", "migration", "V9__unexpected.sql")
                        .toAbsolutePath()
                        .normalize();

        assertAll(
                () -> assertFalse(lowerBody.contains("drop table")),
                () -> assertFalse(lowerBody.contains("truncate table")),
                () -> assertFalse(lowerBody.contains("delete from")),
                () -> assertFalse(lowerBody.contains("alter table decision_")),
                () -> assertFalse(Files.exists(v9), "stage-qdr-3 B3 must not create V9 migration"));
    }
}

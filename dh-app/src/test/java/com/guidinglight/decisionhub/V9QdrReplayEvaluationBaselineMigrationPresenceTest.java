package com.guidinglight.decisionhub;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * stage-qdr-4 B2：校验 V9 QDR replay/evaluation persistence baseline migration。
 *
 * <p>本测试只做文件级审查；用于防止 V9 缺表、缺 tenant-bound 约束、缺安全 COMMENT 或误引入 raw storage /
 * executable trading action 字段。
 */
class V9QdrReplayEvaluationBaselineMigrationPresenceTest {

    private static final Pattern FORBIDDEN_STORAGE_COLUMN =
            Pattern.compile(
                    "(?m)^\\s*(raw_prompt|raw_provider_response|credential|api_key|api_secret|token|passphrase)"
                            + "\\s+(uuid|varchar|text|jsonb|bytea)",
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
                                "V9__qdr_replay_evaluation_baseline.sql")
                        .toAbsolutePath()
                        .normalize();
        assertTrue(Files.exists(migration), "V9 migration file must exist: " + migration);
        body = Files.readString(migration, StandardCharsets.UTF_8);
        lowerBody = body.toLowerCase(Locale.ROOT);
    }

    @Test
    void v9CreatesSevenReplayEvaluationTables() {
        assertAll(
                () -> assertTrue(body.contains("create table if not exists qdr_replay_case")),
                () -> assertTrue(body.contains("create table if not exists qdr_evaluation_case")),
                () -> assertTrue(body.contains("create table if not exists qdr_expected_decision_summary")),
                () -> assertTrue(body.contains("create table if not exists qdr_regression_verdict")),
                () -> assertTrue(body.contains("create table if not exists qdr_regression_finding")),
                () -> assertTrue(body.contains("create table if not exists qdr_replay_input_ref")),
                () -> assertTrue(body.contains("create table if not exists qdr_replay_output_ref")));
    }

    @Test
    void v9ContainsTenantBoundRequiredColumnsAndIndexes() {
        for (String table :
                List.of(
                        "qdr_replay_case",
                        "qdr_evaluation_case",
                        "qdr_expected_decision_summary",
                        "qdr_regression_verdict",
                        "qdr_regression_finding",
                        "qdr_replay_input_ref",
                        "qdr_replay_output_ref")) {
            assertTrue(body.contains("comment on table " + table), table + " must have COMMENT");
            assertTrue(body.contains("comment on column " + table + ".tenant_id"), table + " tenant COMMENT");
        }
        assertAll(
                () -> assertTrue(body.contains("tenant_id varchar(128) not null")),
                () -> assertTrue(body.contains("created_at timestamptz not null")),
                () -> assertTrue(body.contains("updated_at timestamptz not null")),
                () -> assertTrue(body.contains("unique (tenant_id, id)")),
                () -> assertTrue(body.contains("unique (tenant_id, case_id)")),
                () -> assertTrue(body.contains("unique (tenant_id, evaluation_id)")),
                () -> assertTrue(body.contains("unique (tenant_id, verdict_id)")),
                () -> assertTrue(body.contains("idx_qdr_replay_case_tenant_source_request")),
                () -> assertTrue(body.contains("idx_qdr_evaluation_case_tenant_source_decision")),
                () -> assertTrue(body.contains("idx_qdr_regression_verdict_tenant_created")),
                () -> assertTrue(body.contains("idx_qdr_regression_finding_tenant_created")));
    }

    @Test
    void v9ContainsReplayEvaluationSafetyConstraints() {
        assertAll(
                () -> assertTrue(body.contains("chk_qdr_evaluation_case_verdict")),
                () -> assertTrue(body.contains("chk_qdr_regression_verdict_status")),
                () -> assertTrue(body.contains("chk_qdr_regression_finding_verdict")),
                () -> assertTrue(body.contains("'PASS'")),
                () -> assertTrue(body.contains("'FAIL'")),
                () -> assertTrue(body.contains("'WARN'")),
                () -> assertTrue(body.contains("'SKIPPED'")),
                () -> assertTrue(body.contains("'INFO'")),
                () -> assertTrue(body.contains("'ERROR'")),
                () -> assertTrue(body.contains("'BLOCKER'")),
                () -> assertTrue(body.contains("action_label not in")),
                () -> assertTrue(body.contains("'BUY'")),
                () -> assertTrue(body.contains("'SELL'")),
                () -> assertTrue(body.contains("'MARKET_ORDER'")),
                () -> assertTrue(body.contains("'PLACE_ORDER'")),
                () -> assertTrue(body.contains("'CANCEL_ORDER'")),
                () -> assertTrue(body.contains("'MUTATE_NQ_STATE'")),
                () -> assertTrue(body.contains("~ '^[0-9a-f]{64}$'")));
    }

    @Test
    void v9ContainsTenantBoundFkWithoutEvaluationVerdictCycle() {
        final String evaluationBlock =
                body.substring(
                        body.indexOf("create table if not exists qdr_evaluation_case"),
                        body.indexOf("create table if not exists qdr_regression_verdict"));
        assertAll(
                () -> assertTrue(body.contains("foreign key (tenant_id, input_ref_id)")),
                () -> assertTrue(body.contains("foreign key (tenant_id, output_ref_id)")),
                () -> assertTrue(body.contains("foreign key (tenant_id, expected_summary_id)")),
                () -> assertTrue(body.contains("foreign key (tenant_id, actual_summary_id)")),
                () -> assertTrue(body.contains("fk_qdr_regression_verdict_evaluation_case")),
                () -> assertTrue(body.contains("fk_qdr_regression_finding_verdict")),
                () -> assertFalse(
                        evaluationBlock.contains("references qdr_regression_verdict"),
                        "evaluation case must not reverse-FK to verdict and create a cycle"));
    }

    @Test
    void v9ForbidsRawPromptProviderResponseCredentialStorageColumns() {
        assertAll(
                () ->
                        assertFalse(
                                FORBIDDEN_STORAGE_COLUMN.matcher(body).find(),
                                "V9 must not declare raw prompt/provider response or credential columns"),
                () -> assertTrue(body.contains("not (input_ref ?| array[")),
                () -> assertTrue(body.contains("not (output_ref ?| array[")),
                () -> assertTrue(body.contains("not (summary_json ?| array[")));
    }

    @Test
    void v9CommentsStateNotExecutableTradingSignalAndNoRawStorage() {
        assertAll(
                () -> assertTrue(lowerBody.contains("not executable trading signal")),
                () -> assertTrue(lowerBody.contains("no raw prompt")),
                () -> assertTrue(lowerBody.contains("no raw provider response")),
                () -> assertTrue(lowerBody.contains("no credential")),
                () -> assertTrue(lowerBody.contains("tenant-bound")),
                () -> assertTrue(lowerBody.contains("不代表 real provider 已接入")),
                () -> assertTrue(lowerBody.contains("不代表 real http 或 provider 已接入")));
    }

    @Test
    void v9DoesNotContainDestructiveDdlOrApiControllerBoundaryLeak() {
        assertAll(
                () -> assertFalse(lowerBody.contains("drop table")),
                () -> assertFalse(lowerBody.contains("truncate table")),
                () -> assertFalse(lowerBody.contains("delete from")),
                () -> assertFalse(lowerBody.contains("requestmapping")),
                () -> assertFalse(lowerBody.contains("controller")),
                () -> assertFalse(lowerBody.contains("webclient")),
                () -> assertFalse(lowerBody.contains("resttemplate")),
                () -> assertFalse(lowerBody.contains("httpclient")),
                () -> assertFalse(lowerBody.contains("autogen")),
                () -> assertFalse(lowerBody.contains("crewai")));
    }
}

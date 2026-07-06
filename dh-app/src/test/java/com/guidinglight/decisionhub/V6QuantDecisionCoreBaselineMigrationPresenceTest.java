package com.guidinglight.decisionhub;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * stage-qdr-1：校验 V6 Decision Core 主线表 migration。
 *
 * <p>本测试只做文件级审查，不启动数据库；用于防止主线缺表、缺索引、缺禁止 action 约束或误引入交易表。
 */
class V6QuantDecisionCoreBaselineMigrationPresenceTest {

    private static String body;
    private static String lowerBody;

    @BeforeAll
    static void load() throws IOException {
        final Path migration =
                Path.of("src", "main", "resources", "db", "migration",
                                "V6__qdr_decision_core_baseline.sql")
                        .toAbsolutePath()
                        .normalize();
        assertTrue(Files.exists(migration), "V6 migration file must exist: " + migration);
        body = Files.readString(migration, StandardCharsets.UTF_8);
        lowerBody = body.toLowerCase(java.util.Locale.ROOT);
    }

    @Test
    void v6ContainsDecisionCoreMainlineTables() {
        assertAll(
                () -> assertTrue(body.contains("create table if not exists decision_request"),
                        "decision_request table missing"),
                () -> assertTrue(body.contains("create table if not exists decision_run"),
                        "decision_run table missing"),
                () -> assertTrue(body.contains("create table if not exists quant_signal"),
                        "quant_signal table missing"),
                () -> assertTrue(body.contains("create table if not exists quant_decision"),
                        "quant_decision table missing"));
    }

    @Test
    void v6ContainsRequiredConstraintsAndIndexes() {
        assertAll(
                () -> assertTrue(body.contains("unique (tenant_id, request_key)"),
                        "tenant/request_key unique constraint missing"),
                () -> assertTrue(body.contains("unique (decision_request_id, run_no)"),
                        "run_no unique constraint missing"),
                () -> assertTrue(body.contains("idx_decision_request_tenant_created"),
                        "decision_request tenant index missing"),
                () -> assertTrue(body.contains("idx_decision_request_trace"),
                        "decision_request trace index missing"),
                () -> assertTrue(body.contains("idx_decision_request_request"),
                        "decision_request request_id index missing"),
                () -> assertTrue(body.contains("idx_quant_signal_source_received"),
                        "quant_signal source/received index missing"),
                () -> assertTrue(body.contains("idx_quant_signal_symbol_timeframe"),
                        "quant_signal symbol/timeframe index missing"),
                () -> assertTrue(body.contains("idx_quant_decision_run"),
                        "quant_decision run index missing"));
    }

    @Test
    void v6ContainsReadonlyActionAndHumanApprovalConstraints() {
        assertAll(
                () -> assertTrue(body.contains("'OBSERVE'"), "OBSERVE action missing"),
                () -> assertTrue(body.contains("'NO_TRADE'"), "NO_TRADE action missing"),
                () -> assertTrue(body.contains("'LONG_BIAS'"), "LONG_BIAS action missing"),
                () -> assertTrue(body.contains("'SHORT_BIAS'"), "SHORT_BIAS action missing"),
                () -> assertTrue(body.contains("'NEEDS_REVIEW'"), "NEEDS_REVIEW action missing"),
                () -> assertTrue(body.contains("'REJECTED'"), "REJECTED action missing"),
                () -> assertTrue(body.contains("action not in ('BUY', 'SELL', 'PLACE_ORDER', 'CANCEL_ORDER')"),
                        "executable action deny check missing"),
                () -> assertTrue(body.contains("human_approval_status varchar(32) not null default 'NOT_REQUIRED'"),
                        "human approval default missing"),
                () -> assertTrue(body.contains("'APPROVED'"), "approval enum value missing"));
    }

    @Test
    void v6ContainsColumnCommentsForEveryDecisionCoreField() {
        final Map<String, List<String>> requiredColumns =
                Map.of(
                        "decision_request",
                        List.of(
                                "id",
                                "request_key",
                                "request_type",
                                "source_system",
                                "source_ref_id",
                                "tenant_id",
                                "trace_id",
                                "request_id",
                                "input_payload_json",
                                "context_payload_json",
                                "status",
                                "created_at",
                                "updated_at"),
                        "decision_run",
                        List.of(
                                "id",
                                "decision_request_id",
                                "run_no",
                                "status",
                                "orchestrator_key",
                                "model_provider",
                                "model_name",
                                "started_at",
                                "finished_at",
                                "latency_ms",
                                "error_code",
                                "error_message",
                                "created_at"),
                        "quant_signal",
                        List.of(
                                "id",
                                "decision_request_id",
                                "source_system",
                                "symbol",
                                "exchange",
                                "timeframe",
                                "signal_type",
                                "signal_payload_json",
                                "strategy_id",
                                "strategy_version",
                                "dataset_version",
                                "received_at",
                                "created_at"),
                        "quant_decision",
                        List.of(
                                "id",
                                "quant_signal_id",
                                "decision_run_id",
                                "action",
                                "confidence_score",
                                "risk_level",
                                "rationale",
                                "constraints_json",
                                "human_approval_status",
                                "created_at"));
        requiredColumns.forEach(
                (table, columns) ->
                        columns.forEach(
                                column ->
                                        assertTrue(
                                                lowerBody.contains(
                                                        "comment on column " + table + "." + column + " is"),
                                                table + "." + column + " comment missing")));
    }

    @Test
    void v6DoesNotCreateTradingOrNqOwnedTables() {
        assertAll(
                () -> assertFalse(lowerBody.contains("create table if not exists dh_order"),
                        "V6 must not create order tables"),
                () -> assertFalse(lowerBody.contains("create table if not exists dh_trade"),
                        "V6 must not create trade tables"),
                () -> assertFalse(lowerBody.contains("create table if not exists dh_fill"),
                        "V6 must not create fill tables"),
                () -> assertFalse(lowerBody.contains("create table if not exists dh_position"),
                        "V6 must not create position tables"),
                () -> assertFalse(lowerBody.contains("create table if not exists dh_live_"),
                        "V6 must not create live tables"),
                () -> assertFalse(lowerBody.contains("create table if not exists nq_"),
                        "V6 must not create NQ-owned tables"));
    }
}

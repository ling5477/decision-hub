package com.guidinglight.decisionhub;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * stage-qdr-2 B3：校验 V7 Human Approval Packet migration。
 *
 * <p>本测试只做文件级审查，不启动数据库；用于防止 approval schema 缺字段、缺约束、缺索引或误引入破坏性 DDL。
 */
class V7HumanApprovalPacketMigrationPresenceTest {

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
                                "V7__human_approval_packet.sql")
                        .toAbsolutePath()
                        .normalize();
        assertTrue(Files.exists(migration), "V7 migration file must exist: " + migration);
        body = Files.readString(migration, StandardCharsets.UTF_8);
        lowerBody = body.toLowerCase(Locale.ROOT);
    }

    @Test
    void v7ContainsHumanApprovalPacketTableAndRequiredFields() {
        assertAll(
                () -> assertTrue(body.contains("create table if not exists human_approval_packet")),
                () -> assertTrue(body.contains("id uuid primary key")),
                () -> assertTrue(body.contains("decision_run_id uuid not null")),
                () -> assertTrue(body.contains("tenant_id varchar not null")),
                () -> assertTrue(body.contains("trace_id varchar not null")),
                () -> assertTrue(body.contains("request_id varchar not null")),
                () -> assertTrue(body.contains("approval_key varchar not null")),
                () -> assertTrue(body.contains("approval_type varchar not null")),
                () -> assertTrue(body.contains("approval_status varchar not null")),
                () -> assertTrue(body.contains("risk_level varchar not null")),
                () -> assertTrue(body.contains("decision_action varchar not null")),
                () -> assertTrue(body.contains("confidence_score numeric(5,4)")),
                () -> assertTrue(body.contains("checklist_json jsonb not null")),
                () -> assertTrue(body.contains("evidence_refs_json jsonb")));
    }

    @Test
    void v7ContainsFkUniqueChecksAndIndexes() {
        assertAll(
                () -> assertTrue(body.contains("unique (tenant_id, approval_key)")),
                () -> assertTrue(body.contains("foreign key (decision_run_id) references decision_run(id)")),
                () -> assertTrue(body.contains("chk_human_approval_packet_status")),
                () -> assertTrue(body.contains("chk_human_approval_packet_type")),
                () -> assertTrue(body.contains("chk_human_approval_packet_decision_action")),
                () -> assertTrue(body.contains("chk_human_approval_packet_risk_level")),
                () -> assertTrue(body.contains("chk_human_approval_packet_confidence")),
                () -> assertTrue(body.contains("idx_human_approval_packet_tenant_created_at")),
                () -> assertTrue(body.contains("idx_human_approval_packet_decision_run_id")),
                () -> assertTrue(body.contains("idx_human_approval_packet_status")),
                () -> assertTrue(body.contains("idx_human_approval_packet_trace_id")),
                () -> assertTrue(body.contains("idx_human_approval_packet_request_id")));
    }

    @Test
    void v7ContainsAllowedStateAndActionConstraints() {
        assertAll(
                () -> assertTrue(body.contains("'PENDING'")),
                () -> assertTrue(body.contains("'APPROVED'")),
                () -> assertTrue(body.contains("'REJECTED'")),
                () -> assertTrue(body.contains("'NEEDS_REVIEW'")),
                () -> assertTrue(body.contains("'EXPIRED'")),
                () -> assertTrue(body.contains("'QUANT_DECISION_REVIEW'")),
                () -> assertTrue(body.contains("'RISK_REVIEW'")),
                () -> assertTrue(body.contains("'STRATEGY_RELEASE_REVIEW'")),
                () -> assertTrue(body.contains("'ANOMALY_REVIEW'")),
                () -> assertTrue(body.contains("'OBSERVE'")),
                () -> assertTrue(body.contains("'NO_TRADE'")),
                () -> assertTrue(body.contains("'LONG_BIAS'")),
                () -> assertTrue(body.contains("'SHORT_BIAS'")));
    }

    @Test
    void v7ExplicitlyDeniesExecutableActionsAndDestructiveDdl() {
        assertAll(
                () -> assertTrue(body.contains("'BUY'")),
                () -> assertTrue(body.contains("'SELL'")),
                () -> assertTrue(body.contains("'PLACE_ORDER'")),
                () -> assertTrue(body.contains("'CANCEL_ORDER'")),
                () -> assertTrue(body.contains("'MARKET_ORDER'")),
                () -> assertTrue(body.contains("'LIMIT_ORDER'")),
                () -> assertFalse(lowerBody.contains("drop table")),
                () -> assertFalse(lowerBody.contains("alter table")),
                () -> assertFalse(lowerBody.contains("truncate table")),
                () -> assertFalse(lowerBody.contains("delete from")));
    }

    @Test
    void v7CommentsStateApprovalIsEvidenceNotTradingAuthorization() {
        assertAll(
                () -> assertTrue(lowerBody.contains("dh 内部人工审查证据表")),
                () -> assertTrue(lowerBody.contains("不是交易授权表")),
                () -> assertTrue(lowerBody.contains("approved 不等于 buy")),
                () -> assertTrue(lowerBody.contains("rejected 不等于 sell")),
                () -> assertTrue(lowerBody.contains("不得保存 credential")),
                () -> assertTrue(lowerBody.contains("raw provider response")),
                () -> assertTrue(lowerBody.contains("raw prompt")));
    }
}

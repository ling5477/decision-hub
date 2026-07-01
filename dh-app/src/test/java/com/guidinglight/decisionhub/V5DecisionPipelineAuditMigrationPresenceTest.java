package com.guidinglight.decisionhub;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * K3：校验 V5 decision audit / snapshot / trace migration 的关键 SQL 片段。
 *
 * <p>本测试只做文件级审查，不启动数据库；用于防止 K3 schema 缺表、缺索引、缺 JSONB/timestamptz 或误引入交易执行表。
 */
class V5DecisionPipelineAuditMigrationPresenceTest {

  private static Path migration;
  private static String body;
  private static String lowerBody;

  @BeforeAll
  static void load() throws IOException {
    migration =
        Path.of("src", "main", "resources", "db", "migration", "V5__dh_decision_pipeline_audit.sql")
            .toAbsolutePath()
            .normalize();
    assertTrue(Files.exists(migration), "V5 migration file must exist: " + migration);
    body = Files.readString(migration, StandardCharsets.UTF_8);
    lowerBody = body.toLowerCase(java.util.Locale.ROOT);
  }

  @Test
  void v5_containsSixK3AuditTables() {
    assertAll(
        () -> assertTrue(body.contains("create table if not exists dh_decision_request"),
            "dh_decision_request table missing"),
        () -> assertTrue(body.contains("create table if not exists dh_decision_context_snapshot"),
            "dh_decision_context_snapshot table missing"),
        () -> assertTrue(body.contains("create table if not exists dh_decision_trace_step"),
            "dh_decision_trace_step table missing"),
        () -> assertTrue(body.contains("create table if not exists dh_decision_provider_call_log"),
            "dh_decision_provider_call_log table missing"),
        () -> assertTrue(body.contains("create table if not exists dh_decision_output"),
            "dh_decision_output table missing"),
        () -> assertTrue(body.contains("create table if not exists dh_decision_audit_event"),
            "dh_decision_audit_event table missing"));
  }

  @Test
  void v5_containsTraceTenantDecisionIndexesAndComments() {
    assertAll(
        () -> assertTrue(body.contains("idx_dh_decision_request_tenant_created"),
            "request tenant index missing"),
        () -> assertTrue(body.contains("idx_dh_decision_context_snapshot_trace"),
            "snapshot trace index missing"),
        () -> assertTrue(body.contains("idx_dh_decision_trace_step_decision_id"),
            "trace step decision index missing"),
        () -> assertTrue(body.contains("idx_dh_decision_provider_call_trace"),
            "provider trace index missing"),
        () -> assertTrue(body.contains("idx_dh_decision_output_request"),
            "output request index missing"),
        () -> assertTrue(body.contains("idx_dh_decision_audit_event_tenant_created"),
            "audit event tenant index missing"),
        () -> assertTrue(body.contains("comment on table dh_decision_request"),
            "request table comment missing"),
        () -> assertTrue(body.contains("comment on column dh_decision_output.output_json"),
            "output_json comment missing"));
  }

  @Test
  void v5_usesJsonbAndTimestamptzForSnapshotsTraceAndAudit() {
    assertAll(
        () -> assertTrue(body.contains("subject_json jsonb"),
            "request subject_json must use jsonb"),
        () -> assertTrue(body.contains("context_snapshot_json jsonb"),
            "context snapshot must use jsonb"),
        () -> assertTrue(body.contains("evidence_refs_json jsonb"),
            "evidence refs must use jsonb"),
        () -> assertTrue(body.contains("signal_json jsonb"),
            "provider signal summary must use jsonb"),
        () -> assertTrue(body.contains("output_json jsonb"),
            "decision output must use jsonb"),
        () -> assertTrue(body.contains("event_json jsonb"),
            "audit event must use jsonb"),
        () -> assertTrue(body.contains("requested_at timestamptz"),
            "request timestamp must use timestamptz"),
        () -> assertTrue(body.contains("created_at timestamptz"),
            "created timestamp must use timestamptz"));
  }

  @Test
  void v5_containsReadOnlySafetyConstraints() {
    assertAll(
        () -> assertTrue(body.contains("READ_ONLY_RECOMMENDATION"),
            "decision type read-only constraint missing"),
        () -> assertTrue(body.contains("ABSTAIN"),
            "ABSTAIN action constraint missing"),
        () -> assertTrue(body.contains("NO_TRADE"),
            "NO_TRADE action constraint missing"),
        () -> assertTrue(body.contains("LONG_BIAS"),
            "LONG_BIAS action constraint missing"),
        () -> assertTrue(body.contains("SHORT_BIAS"),
            "SHORT_BIAS action constraint missing"),
        () -> assertTrue(body.contains("latency_ms >= 0"),
            "provider latency check missing"),
        () -> assertTrue(body.contains("confidence >= 0 and confidence <= 1"),
            "confidence range check missing"));
  }

  @Test
  void v5_doesNotCreateTradingExecutionOrNqRuntimeTables() {
    assertAll(
        () -> assertFalse(lowerBody.contains("create table if not exists dh_order"),
            "V5 must not create order tables"),
        () -> assertFalse(lowerBody.contains("create table if not exists dh_trade"),
            "V5 must not create trade tables"),
        () -> assertFalse(lowerBody.contains("create table if not exists dh_fill"),
            "V5 must not create fill tables"),
        () -> assertFalse(lowerBody.contains("create table if not exists dh_position"),
            "V5 must not create position tables"),
        () -> assertFalse(lowerBody.contains("create table if not exists dh_live_"),
            "V5 must not create live execution tables"),
        () -> assertFalse(lowerBody.contains("create table if not exists nq_"),
            "V5 must not create NQ-owned tables"));
  }
}

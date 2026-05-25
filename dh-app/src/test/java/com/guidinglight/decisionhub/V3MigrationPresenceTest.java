package com.guidinglight.decisionhub;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Stage2-PoC-B5：校验 V3 迁移脚本存在且包含关键 SQL 片段。
 *
 * <p>本地默认跳过 Docker / Testcontainers，因此用文件层 SQL 片段断言兜底，确保 V3 schema 设计与本批工单一致。
 */
class V3MigrationPresenceTest {

  private static Path migration;
  private static String body;

  @BeforeAll
  static void load() throws IOException {
    migration =
        Path.of("src", "main", "resources", "db", "migration", "V3__stage2_poc_tools.sql")
            .toAbsolutePath()
            .normalize();
    assertTrue(Files.exists(migration), "V3 migration file must exist: " + migration);
    body = Files.readString(migration, StandardCharsets.UTF_8);
  }

  @Test
  void v3_contains_four_new_tables() {
    assertAll(
        () -> assertTrue(body.contains("create table if not exists dh_forecast_artifacts"),
            "dh_forecast_artifacts table missing"),
        () -> assertTrue(body.contains("create table if not exists dh_external_market_snapshots"),
            "dh_external_market_snapshots table missing"),
        () -> assertTrue(body.contains("create table if not exists dh_reflection_entries"),
            "dh_reflection_entries table missing"),
        () -> assertTrue(body.contains("create table if not exists dh_checkpoint_entries"),
            "dh_checkpoint_entries table missing"));
  }

  @Test
  void v3_contains_research_runs_alters() {
    assertAll(
        () -> assertTrue(
            body.contains("alter table dh_research_runs add column if not exists regime"),
            "dh_research_runs.regime ALTER missing"),
        () -> assertTrue(
            body.contains("alter table dh_research_runs add column if not exists planner_strategy"),
            "dh_research_runs.planner_strategy ALTER missing"));
  }

  @Test
  void v3_contains_nq_feedback_alters_and_event_id_uniqueness() {
    assertAll(
        () -> assertTrue(
            body.contains("alter table dh_nq_feedback_events add column if not exists event_id"),
            "dh_nq_feedback_events.event_id ALTER missing"),
        () -> assertTrue(
            body.contains("alter table dh_nq_feedback_events add column if not exists schema_version"),
            "dh_nq_feedback_events.schema_version ALTER missing"),
        () -> assertTrue(
            body.contains("alter table dh_nq_feedback_events add column if not exists validation_status"),
            "dh_nq_feedback_events.validation_status ALTER missing"),
        () -> assertTrue(
            body.contains("alter table dh_nq_feedback_events add column if not exists source_job_id"),
            "dh_nq_feedback_events.source_job_id ALTER missing"),
        () -> assertTrue(
            body.contains("ux_dh_nq_feedback_events_event_id"),
            "unique constraint on event_id missing"));
  }

  @Test
  void v3_preserves_jsonb_and_payload_columns() {
    assertAll(
        () -> assertTrue(body.contains("raw_payload_json jsonb"),
            "raw_payload_json column type must be jsonb"),
        () -> assertTrue(body.contains("payload_json jsonb"),
            "payload_json column type must be jsonb"),
        () -> assertTrue(body.contains("snapshot_json jsonb"),
            "snapshot_json column type must be jsonb"),
        () -> assertTrue(body.contains("comment on table"),
            "table comments missing"),
        () -> assertTrue(body.contains("comment on column"),
            "column comments missing"));
  }

  @Test
  void v3_contains_no_trade_or_order_or_live_tables() {
    final String lower = body.toLowerCase();
    assertAll(
        () -> assertTrue(!lower.contains("dh_orders"),
            "V3 must not introduce orders tables"),
        () -> assertTrue(!lower.contains("dh_trades"),
            "V3 must not introduce trades tables"),
        () -> assertTrue(!lower.contains("dh_fills"),
            "V3 must not introduce fills tables"),
        () -> assertTrue(!lower.contains("dh_positions"),
            "V3 must not introduce positions tables"),
        () -> assertTrue(!lower.contains("dh_live_"),
            "V3 must not introduce live-execution tables"));
  }
}

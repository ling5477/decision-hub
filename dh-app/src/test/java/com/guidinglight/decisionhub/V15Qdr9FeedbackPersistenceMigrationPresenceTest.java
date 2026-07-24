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

/** Stage-QDR-9 V15 migration 与 stable machine contract 的文件级安全回归。 */
class V15Qdr9FeedbackPersistenceMigrationPresenceTest {

  private static final Pattern FORBIDDEN_STORAGE_COLUMN =
      Pattern.compile(
          "(?m)^\\s*(raw_prompt|raw_provider_response|credential|authorization|api_key|"
              + "api_secret|password|token|canonical_value|payload_json)\\s+"
              + "(uuid|varchar|text|jsonb|bytea)",
          Pattern.CASE_INSENSITIVE);

  private static String body;
  private static String lowerBody;
  private static String machineContract;

  @BeforeAll
  static void loadContracts() throws IOException {
    final Path migration =
        Path.of(
                "src",
                "main",
                "resources",
                "db",
                "migration",
                "V15__qdr9_structured_feedback_persistence.sql")
            .toAbsolutePath()
            .normalize();
    final Path v14 =
        migration.resolveSibling("V14__qdr7_persistent_guard_schema_alignment.sql");
    final Path contract =
        Path.of(
                "..",
                "config",
                "qdr9-feedback",
                "qdr9-feedback-contract.yml")
            .toAbsolutePath()
            .normalize();

    assertTrue(Files.exists(v14), "V14 must remain present before V15");
    assertTrue(Files.exists(migration), "V15 migration must exist: " + migration);
    assertTrue(Files.exists(contract), "QDR9 machine contract must exist: " + contract);
    body = Files.readString(migration, StandardCharsets.UTF_8);
    lowerBody = body.toLowerCase(Locale.ROOT);
    machineContract = Files.readString(contract, StandardCharsets.UTF_8);
  }

  @Test
  void createsExactlyTheFrozenFourTableAggregate() {
    assertAll(
        () -> assertTrue(body.contains("create table qdr_feedback_outcome_observation")),
        () -> assertTrue(body.contains("create table qdr_feedback_attribution (")),
        () ->
            assertTrue(
                body.contains("create table qdr_feedback_attribution_contribution")),
        () ->
            assertTrue(
                body.contains("create table qdr_feedback_attribution_reference")),
        () -> assertFalse(lowerBody.contains("create table if not exists")),
        () -> assertFalse(lowerBody.contains("create table dh_nq_feedback_events")));
  }

  @Test
  void declaresFrozenTenantEnvironmentConstraintsAndIndexes() {
    assertAll(
        () ->
            assertTrue(
                body.contains(
                    "unique (tenant_id, environment, idempotency_key)")),
        () ->
            assertTrue(
                body.contains(
                    "unique (tenant_id, environment, attribution_id)")),
        () -> assertTrue(body.contains("environment in ('DEV', 'TEST')")),
        () -> assertTrue(body.contains("confidence between 0 and 1")),
        () -> assertTrue(body.contains("sort_order >= 0 and sort_order < 32")),
        () -> assertTrue(body.contains("idx_qdr_feedback_observation_decision")),
        () -> assertTrue(body.contains("idx_qdr_feedback_observation_trace")),
        () -> assertTrue(body.contains("idx_qdr_feedback_attribution_policy")),
        () -> assertTrue(body.contains("idx_qdr_feedback_attribution_status")),
        () -> assertTrue(body.contains("idx_qdr_feedback_reference_hold")));
  }

  @Test
  void containsNoJsonbRawPayloadOrHistoricalMutation() {
    assertAll(
        () -> assertFalse(FORBIDDEN_STORAGE_COLUMN.matcher(body).find()),
        () -> assertFalse(lowerBody.contains(" jsonb")),
        () -> assertFalse(lowerBody.contains("alter table dh_nq_feedback_events")),
        () -> assertFalse(lowerBody.contains("insert into dh_nq_feedback_events")),
        () -> assertFalse(lowerBody.contains("update dh_nq_feedback_events")),
        () -> assertFalse(lowerBody.contains("delete from")),
        () -> assertFalse(lowerBody.contains("drop table")),
        () -> assertTrue(body.contains("comment on table qdr_feedback_outcome_observation")),
        () -> assertTrue(body.contains("comment on table qdr_feedback_attribution")),
        () ->
            assertTrue(
                body.contains(
                    "comment on table qdr_feedback_attribution_contribution")),
        () ->
            assertTrue(
                body.contains(
                    "comment on table qdr_feedback_attribution_reference")));
  }

  @Test
  void machineContractFreezesVersionLimitsAndSafetyBoundary() {
    assertAll(
        () ->
            assertTrue(
                machineContract.contains(
                    "schemaVersion: DH-QDR9-FEEDBACK-PERSISTENCE-1")),
        () -> assertTrue(machineContract.contains("version: 15")),
        () -> assertTrue(machineContract.contains("identifier: 128")),
        () -> assertTrue(machineContract.contains("referenceValue: 256")),
        () -> assertTrue(machineContract.contains("contributions: 32")),
        () -> assertTrue(machineContract.contains("jsonbInAggregate: forbidden")),
        () -> assertTrue(machineContract.contains("rawPayloadStorage: forbidden")),
        () -> assertTrue(machineContract.contains("jdbcImplementationInB1: forbidden")),
        () -> assertTrue(machineContract.contains("automaticLearning: forbidden")));
  }
}

package com.guidinglight.decisionhub.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Stage2-PoC-B1：JsonSchema 存在性 + 基本结构校验。
 *
 * <p>逐个检查 contracts/json-schema/ 下 16 个 schema 文件： ① 文件存在；② 可被 Jackson 解析为合法 JSON；③ 顶层包含 {@code $schema}
 * / {@code $id} / {@code title}；④ {@code additionalProperties} 为 {@code false}；⑤ 不出现 placeOrder /
 * submitOrder / executeOrder / bypassRisk / forceExecute 字样。
 *
 * <p>运行目录：dh-domain 模块 basedir；通过 {@code ../contracts/json-schema/} 定位文件。
 */
class JsonSchemaPresenceTest {

  private static final List<String> EXPECTED =
      List.of(
          "nq-feedback-envelope.schema.json",
          "nq-feedback-paper-run-created.schema.json",
          "nq-feedback-paper-run-started.schema.json",
          "nq-feedback-paper-run-stopped.schema.json",
          "nq-feedback-paper-run-daily-report-generated.schema.json",
          "nq-feedback-paper-run-alert-raised.schema.json",
          "nq-feedback-paper-run-recovery-event-recorded.schema.json",
          "nq-feedback-paper-run-stability-check-completed.schema.json",
          "nq-feedback-backtest-result-ready.schema.json",
          "dh-backtest-request.schema.json",
          "dh-backtest-request-accepted.schema.json",
          "dh-backtest-result-snapshot.schema.json",
          "forecast-artifact.schema.json",
          "external-market-snapshot.schema.json",
          "reflection-entry.schema.json",
          "checkpoint-entry.schema.json");

  private static final List<String> FORBIDDEN_TRADING_TOKENS =
      List.of("placeOrder", "submitOrder", "executeOrder", "bypassRisk", "forceExecute");

  private static Path schemaDir() {
    return Path.of("..", "contracts", "json-schema").toAbsolutePath().normalize();
  }

  @Test
  void all16SchemasPresent() {
    assertEquals(16, EXPECTED.size());
    final Path dir = schemaDir();
    for (String name : EXPECTED) {
      final Path p = dir.resolve(name);
      assertTrue(Files.exists(p), "missing schema: " + p);
    }
  }

  @Test
  void allSchemasAreParseableAndHaveCoreMetadata() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final Path dir = schemaDir();
    for (String name : EXPECTED) {
      final Path p = dir.resolve(name);
      final JsonNode root = mapper.readTree(p.toFile());
      assertNotNull(root.get("$schema"), name + " missing $schema");
      assertNotNull(root.get("$id"), name + " missing $id");
      assertNotNull(root.get("title"), name + " missing title");
      assertTrue(root.has("type"), name + " missing type");
    }
  }

  @Test
  void allSchemasForbidAdditionalProperties() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final Path dir = schemaDir();
    for (String name : EXPECTED) {
      final Path p = dir.resolve(name);
      final JsonNode root = mapper.readTree(p.toFile());
      final JsonNode addProps = root.get("additionalProperties");
      assertNotNull(addProps, name + " missing additionalProperties");
      assertFalse(addProps.asBoolean(true), name + " must set additionalProperties=false");
    }
  }

  @Test
  void noSchemaCarriesTradingExecutionSemantics() throws Exception {
    final Path dir = schemaDir();
    for (String name : EXPECTED) {
      final String body = Files.readString(dir.resolve(name));
      for (String token : FORBIDDEN_TRADING_TOKENS) {
        assertFalse(
            body.contains(token), "schema " + name + " must not contain forbidden token: " + token);
      }
    }
  }

  @Test
  void envelopeEnumMatchesEightDomainEventTypes() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode root =
        mapper.readTree(schemaDir().resolve("nq-feedback-envelope.schema.json").toFile());
    final JsonNode enumNode = root.path("properties").path("eventType").path("enum");
    assertTrue(enumNode.isArray(), "eventType.enum must be array");
    assertEquals(8, enumNode.size(), "eventType.enum must declare 8 values");
  }
}

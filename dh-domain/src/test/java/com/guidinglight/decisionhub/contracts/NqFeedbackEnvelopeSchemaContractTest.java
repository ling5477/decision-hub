package com.guidinglight.decisionhub.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Stage3-B1 Contract Alignment：NqFeedbackEnvelope JSON Schema 与 dh-domain 枚举 / 字段一致性。
 *
 * <p>检查项：
 *
 * <ul>
 *   <li>schema 文件存在且可解析；
 *   <li>required 字段完整：10 个核心字段全部声明；
 *   <li>additionalProperties = false；
 *   <li>eventType.enum 与 {@link NqFeedbackEventType} 8 个枚举值一一对应；
 *   <li>sourceSystem 限制为常量 {@code "nexus-quant"}；
 *   <li>schemaVersion 走 semver 正则；
 *   <li>不出现交易执行语义关键字（placeOrder / submitOrder / executeOrder / bypassRisk /
 *       forceExecute）；
 *   <li>不出现 /orders /trades /live 危险路径片段。
 * </ul>
 *
 * <p>本测试不修改任何 schema，仅校验 Stage2-PoC-B1 + Stage3-B1 描述补充后契约口径。
 */
class NqFeedbackEnvelopeSchemaContractTest {

  private static final List<String> REQUIRED_FIELDS =
      List.of(
          "eventId",
          "eventType",
          "occurredAt",
          "sourceSystem",
          "sourceJobId",
          "traceId",
          "requestId",
          "correlationId",
          "schemaVersion",
          "payloadJson");

  private static final List<String> FORBIDDEN_TOKENS =
      List.of(
          "placeOrder",
          "submitOrder",
          "executeOrder",
          "bypassRisk",
          "forceExecute",
          "/orders",
          "/trades",
          "/live");

  private static Path schemaPath() {
    return Path.of("..", "contracts", "json-schema", "nq-feedback-envelope.schema.json")
        .toAbsolutePath()
        .normalize();
  }

  @Test
  void schemaFileIsPresentAndParseable() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode root = mapper.readTree(schemaPath().toFile());
    assertNotNull(root, "envelope schema must be parseable");
    assertNotNull(root.get("$id"), "envelope schema must have $id");
    assertEquals("NqFeedbackEnvelope", root.path("title").asText(), "title must match");
  }

  @Test
  void requiredFieldsCoverAllTenEnvelopeFields() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode required = mapper.readTree(schemaPath().toFile()).path("required");
    assertTrue(required.isArray(), "required must be array");
    final Set<String> declared = new HashSet<>();
    required.forEach(n -> declared.add(n.asText()));
    for (String field : REQUIRED_FIELDS) {
      assertTrue(declared.contains(field), "envelope required must include: " + field);
    }
    assertEquals(
        REQUIRED_FIELDS.size(),
        declared.size(),
        "envelope required must be exactly the declared 10 fields");
  }

  @Test
  void additionalPropertiesIsFalse() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode root = mapper.readTree(schemaPath().toFile());
    assertTrue(root.has("additionalProperties"));
    assertFalse(
        root.path("additionalProperties").asBoolean(true),
        "envelope must reject additional properties");
  }

  @Test
  void eventTypeEnumMatchesDomainEnumOneToOne() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode enumNode =
        mapper
            .readTree(schemaPath().toFile())
            .path("properties")
            .path("eventType")
            .path("enum");
    assertTrue(enumNode.isArray(), "eventType.enum must be array");
    final Set<String> schemaValues = new HashSet<>();
    enumNode.forEach(n -> schemaValues.add(n.asText()));

    final Set<String> domainValues = new HashSet<>();
    for (NqFeedbackEventType t : NqFeedbackEventType.values()) {
      domainValues.add(t.name());
    }
    assertEquals(
        domainValues,
        schemaValues,
        "envelope eventType.enum must equal NqFeedbackEventType (Stage3-B1: 8 values)");
    assertEquals(8, schemaValues.size(), "Stage3-B1 不允许新增事件类型，必须保持 8 种");
  }

  @Test
  void sourceSystemIsConstantNexusQuant() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode source =
        mapper.readTree(schemaPath().toFile()).path("properties").path("sourceSystem");
    assertEquals(
        "nexus-quant",
        source.path("const").asText(),
        "sourceSystem must be const 'nexus-quant'");
  }

  @Test
  void schemaVersionUsesSemverPattern() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode sv =
        mapper.readTree(schemaPath().toFile()).path("properties").path("schemaVersion");
    final String pattern = sv.path("pattern").asText();
    assertTrue(
        pattern.contains("[0-9]+") && pattern.contains("\\."),
        "schemaVersion must enforce numeric.numeric.numeric semver");
    // 校验当前规划版本能通过 pattern
    assertTrue("1.0.0".matches(pattern), "1.0.0 must match semver pattern");
    assertFalse("1.0".matches(pattern), "1.0 must not match semver pattern");
  }

  @Test
  void envelopeSchemaHasNoTradingExecutionSemantics() throws Exception {
    final String body = java.nio.file.Files.readString(schemaPath());
    for (String token : FORBIDDEN_TOKENS) {
      assertFalse(
          body.contains(token),
          "envelope schema must not contain forbidden token: " + token);
    }
  }
}

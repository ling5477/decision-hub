package com.guidinglight.decisionhub.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.backtest.BacktestVerdict;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Stage3-B1 Contract Alignment：DhBacktestResultSnapshot JSON Schema 与 dh-domain 枚举 / 字段一致性。
 *
 * <p>检查项：
 *
 * <ul>
 *   <li>schema 文件存在且可解析；
 *   <li>required 9 字段完整（含 verdict / rawPayloadJson）；
 *   <li>additionalProperties = false；
 *   <li>verdict.enum 与 {@link BacktestVerdict} 3 值一一对应；
 *   <li>winRate range [0,1]；
 *   <li>不出现交易执行语义关键字与 /orders /trades /live。
 * </ul>
 */
class BacktestResultSnapshotSchemaContractTest {

  private static final List<String> REQUIRED_FIELDS =
      List.of(
          "resultId",
          "requestId",
          "traceId",
          "candidateId",
          "periodStart",
          "periodEnd",
          "verdict",
          "recordedAt",
          "rawPayloadJson");

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
    return Path.of("..", "contracts", "json-schema", "dh-backtest-result-snapshot.schema.json")
        .toAbsolutePath()
        .normalize();
  }

  @Test
  void schemaFileIsPresentAndParseable() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode root = mapper.readTree(schemaPath().toFile());
    assertNotNull(root, "snapshot schema must be parseable");
    assertNotNull(root.get("$id"));
    assertEquals("DhBacktestResultSnapshot", root.path("title").asText());
  }

  @Test
  void requiredFieldsCoverNineCoreFields() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode required = mapper.readTree(schemaPath().toFile()).path("required");
    assertTrue(required.isArray());
    final Set<String> declared = new HashSet<>();
    required.forEach(n -> declared.add(n.asText()));
    for (String field : REQUIRED_FIELDS) {
      assertTrue(declared.contains(field), "snapshot required missing: " + field);
    }
    assertEquals(
        REQUIRED_FIELDS.size(),
        declared.size(),
        "snapshot required must be exactly the declared 9 fields");
  }

  @Test
  void additionalPropertiesIsFalse() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode root = mapper.readTree(schemaPath().toFile());
    assertTrue(root.has("additionalProperties"));
    assertFalse(root.path("additionalProperties").asBoolean(true));
  }

  @Test
  void verdictEnumMatchesDomainEnumOneToOne() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode verdictEnum =
        mapper
            .readTree(schemaPath().toFile())
            .path("properties")
            .path("verdict")
            .path("enum");
    assertTrue(verdictEnum.isArray());
    final Set<String> schemaValues = new HashSet<>();
    verdictEnum.forEach(n -> schemaValues.add(n.asText()));
    final Set<String> domainValues = new HashSet<>();
    for (BacktestVerdict v : BacktestVerdict.values()) {
      domainValues.add(v.name());
    }
    assertEquals(
        domainValues,
        schemaValues,
        "snapshot.verdict.enum must equal BacktestVerdict values");
    assertEquals(3, schemaValues.size(), "BacktestVerdict 必须保持 3 个值");
  }

  @Test
  void winRateRangeIsZeroToOne() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode winRate =
        mapper
            .readTree(schemaPath().toFile())
            .path("properties")
            .path("winRate");
    assertEquals(0.0, winRate.path("minimum").asDouble(-1.0));
    assertEquals(1.0, winRate.path("maximum").asDouble(-1.0));
  }

  @Test
  void snapshotSchemaHasNoTradingExecutionSemantics() throws Exception {
    final String body = java.nio.file.Files.readString(schemaPath());
    for (String token : FORBIDDEN_TOKENS) {
      assertFalse(
          body.contains(token),
          "snapshot schema must not contain forbidden token: " + token);
    }
  }
}

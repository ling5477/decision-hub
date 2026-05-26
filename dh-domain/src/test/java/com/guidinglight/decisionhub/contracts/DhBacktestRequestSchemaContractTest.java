package com.guidinglight.decisionhub.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.backtest.BacktestFrequency;
import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequestStatus;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Stage3-B1 Contract Alignment：DhBacktestRequest JSON Schema 与 dh-domain 枚举 / 字段一致性。
 *
 * <p>检查项：
 *
 * <ul>
 *   <li>schema 文件存在且可解析；
 *   <li>required 14 字段完整（含 status / frequency / symbols / capital 等核心字段）；
 *   <li>additionalProperties = false；
 *   <li>status.enum 与 {@link DhBacktestRequestStatus} 6 值一一对应；
 *   <li>frequency.enum 与 {@link BacktestFrequency} 3 值一一对应；
 *   <li>initialCapital exclusiveMinimum=0；symbols.minItems=1；
 *   <li>不出现交易执行语义关键字与 /orders /trades /live。
 * </ul>
 */
class DhBacktestRequestSchemaContractTest {

  private static final List<String> REQUIRED_FIELDS =
      List.of(
          "requestId",
          "traceId",
          "candidateId",
          "strategyName",
          "strategyVersion",
          "strategyParametersJson",
          "startDate",
          "endDate",
          "initialCapital",
          "symbols",
          "frequency",
          "requestedBy",
          "requestedAt",
          "status");

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
    return Path.of("..", "contracts", "json-schema", "dh-backtest-request.schema.json")
        .toAbsolutePath()
        .normalize();
  }

  @Test
  void schemaFileIsPresentAndParseable() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode root = mapper.readTree(schemaPath().toFile());
    assertNotNull(root, "DhBacktestRequest schema must be parseable");
    assertNotNull(root.get("$id"));
    assertEquals("DhBacktestRequest", root.path("title").asText());
  }

  @Test
  void requiredFieldsCoverAllFourteenCoreFields() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode required = mapper.readTree(schemaPath().toFile()).path("required");
    assertTrue(required.isArray());
    final Set<String> declared = new HashSet<>();
    required.forEach(n -> declared.add(n.asText()));
    for (String field : REQUIRED_FIELDS) {
      assertTrue(declared.contains(field), "DhBacktestRequest required missing: " + field);
    }
    assertEquals(
        REQUIRED_FIELDS.size(),
        declared.size(),
        "DhBacktestRequest required must be exactly the declared 14 fields");
  }

  @Test
  void additionalPropertiesIsFalse() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode root = mapper.readTree(schemaPath().toFile());
    assertTrue(root.has("additionalProperties"));
    assertFalse(root.path("additionalProperties").asBoolean(true));
  }

  @Test
  void statusEnumMatchesDomainEnumOneToOne() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode statusEnum =
        mapper
            .readTree(schemaPath().toFile())
            .path("properties")
            .path("status")
            .path("enum");
    assertTrue(statusEnum.isArray());
    final Set<String> schemaValues = new HashSet<>();
    statusEnum.forEach(n -> schemaValues.add(n.asText()));
    final Set<String> domainValues = new HashSet<>();
    for (DhBacktestRequestStatus s : DhBacktestRequestStatus.values()) {
      domainValues.add(s.name());
    }
    assertEquals(
        domainValues,
        schemaValues,
        "DhBacktestRequest.status.enum must equal DhBacktestRequestStatus values");
    assertEquals(6, schemaValues.size(), "DhBacktestRequestStatus 必须保持 6 个值");
  }

  @Test
  void frequencyEnumMatchesDomainEnumOneToOne() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode freqEnum =
        mapper
            .readTree(schemaPath().toFile())
            .path("properties")
            .path("frequency")
            .path("enum");
    assertTrue(freqEnum.isArray());
    final Set<String> schemaValues = new HashSet<>();
    freqEnum.forEach(n -> schemaValues.add(n.asText()));
    final Set<String> domainValues = new HashSet<>();
    for (BacktestFrequency f : BacktestFrequency.values()) {
      domainValues.add(f.name());
    }
    assertEquals(
        domainValues,
        schemaValues,
        "DhBacktestRequest.frequency.enum must equal BacktestFrequency values");
    assertEquals(3, schemaValues.size(), "BacktestFrequency 必须保持 3 个值");
  }

  @Test
  void initialCapitalExclusiveMinimumZeroAndSymbolsMinItemsOne() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode root = mapper.readTree(schemaPath().toFile());
    final JsonNode capital = root.path("properties").path("initialCapital");
    assertEquals(
        0,
        capital.path("exclusiveMinimum").asInt(-1),
        "initialCapital must enforce exclusiveMinimum = 0");
    final JsonNode symbols = root.path("properties").path("symbols");
    assertEquals(
        1,
        symbols.path("minItems").asInt(-1),
        "symbols must enforce minItems = 1 (no full-market scan)");
  }

  @Test
  void backtestRequestSchemaHasNoTradingExecutionSemantics() throws Exception {
    final String body = java.nio.file.Files.readString(schemaPath());
    for (String token : FORBIDDEN_TOKENS) {
      assertFalse(
          body.contains(token),
          "DhBacktestRequest schema must not contain forbidden token: " + token);
    }
  }
}

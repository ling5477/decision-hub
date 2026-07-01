package com.guidinglight.decisionhub.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** GateK K1 contract test for the DH decision request JSON schema. */
class DecisionRequestSchemaContractTest {

  private static final List<String> REQUIRED_FIELDS =
      List.of(
          "requestId",
          "traceId",
          "tenantId",
          "source",
          "decisionType",
          "subject",
          "requestedAt",
          "schemaVersion");

  private static final List<String> FORBIDDEN_REQUEST_FIELDS =
      List.of(
          "orderId",
          "accountId",
          "apiKey",
          "apiSecret",
          "passphrase",
          "leverage",
          "quantity",
          "price",
          "side",
          "venueCredential",
          "brokerCredential");

  private static Path schemaPath() {
    return Path.of("..", "contracts", "json-schema", "dh-decision-request.schema.json")
        .toAbsolutePath()
        .normalize();
  }

  @Test
  void schemaFileIsPresentAndParseable() throws Exception {
    final JsonNode root = new ObjectMapper().readTree(schemaPath().toFile());
    assertNotNull(root, "DhDecisionRequest schema must be parseable");
    assertEquals("DhDecisionRequest", root.path("title").asText());
    assertEquals("1.0.0", root.path("properties").path("schemaVersion").path("const").asText());
  }

  @Test
  void requiredFieldsCoverReadOnlyRequestContract() throws Exception {
    final JsonNode required = new ObjectMapper().readTree(schemaPath().toFile()).path("required");
    assertTrue(required.isArray());
    final Set<String> declared = new HashSet<>();
    required.forEach(n -> declared.add(n.asText()));
    assertEquals(Set.copyOf(REQUIRED_FIELDS), declared);
  }

  @Test
  void additionalPropertiesIsFalseOnRootSubjectAndContextSnapshot() throws Exception {
    final JsonNode root = new ObjectMapper().readTree(schemaPath().toFile());
    assertFalse(root.path("additionalProperties").asBoolean(true));
    assertFalse(root.path("properties").path("subject").path("additionalProperties").asBoolean(true));
    assertFalse(
        root.path("properties").path("contextSnapshot").path("additionalProperties").asBoolean(true));
  }

  @Test
  void decisionTypeEnumMatchesDomainEnumOneToOne() throws Exception {
    final JsonNode enumNode =
        new ObjectMapper()
            .readTree(schemaPath().toFile())
            .path("properties")
            .path("decisionType")
            .path("enum");
    final Set<String> schemaValues = new HashSet<>();
    enumNode.forEach(n -> schemaValues.add(n.asText()));
    final Set<String> domainValues = new HashSet<>();
    for (DecisionType type : DecisionType.values()) {
      domainValues.add(type.name());
    }
    assertEquals(domainValues, schemaValues);
    assertEquals(Set.of("READ_ONLY_RECOMMENDATION"), schemaValues);
  }

  @Test
  void requestSchemaHasNoCredentialOrExecutionIntentFields() throws Exception {
    final String body = Files.readString(schemaPath());
    for (String field : FORBIDDEN_REQUEST_FIELDS) {
      assertFalse(body.contains(field), "request schema must not contain field: " + field);
    }
  }
}

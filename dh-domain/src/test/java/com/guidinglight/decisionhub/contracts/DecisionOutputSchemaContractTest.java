package com.guidinglight.decisionhub.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import com.guidinglight.decisionhub.domain.decision.ForbiddenAction;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** GateK K1 contract test for the DH decision output JSON schema. */
class DecisionOutputSchemaContractTest {

  private static final List<String> REQUIRED_FIELDS =
      List.of(
          "requestId",
          "traceId",
          "tenantId",
          "decisionType",
          "action",
          "status",
          "riskLevel",
          "policyStatus",
          "providerStatus",
          "forbiddenActions",
          "createdAt",
          "schemaVersion");

  private static Path schemaPath() {
    return Path.of("..", "contracts", "json-schema", "dh-decision-output.schema.json")
        .toAbsolutePath()
        .normalize();
  }

  @Test
  void schemaFileIsPresentAndParseable() throws Exception {
    final JsonNode root = new ObjectMapper().readTree(schemaPath().toFile());
    assertNotNull(root, "DhDecisionOutput schema must be parseable");
    assertEquals("DhDecisionOutput", root.path("title").asText());
    assertEquals("1.0.0", root.path("properties").path("schemaVersion").path("const").asText());
  }

  @Test
  void requiredFieldsCoverStructuredOutputContract() throws Exception {
    final JsonNode required = new ObjectMapper().readTree(schemaPath().toFile()).path("required");
    assertTrue(required.isArray());
    final Set<String> declared = new HashSet<>();
    required.forEach(n -> declared.add(n.asText()));
    assertEquals(Set.copyOf(REQUIRED_FIELDS), declared);
  }

  @Test
  void additionalPropertiesIsFalse() throws Exception {
    final JsonNode root = new ObjectMapper().readTree(schemaPath().toFile());
    assertTrue(root.has("additionalProperties"));
    assertFalse(root.path("additionalProperties").asBoolean(true));
  }

  @Test
  void decisionTypeEnumOnlyAllowsReadOnlyRecommendation() throws Exception {
    final Set<String> schemaValues = enumValues("decisionType");
    assertEquals(Set.of(DecisionType.READ_ONLY_RECOMMENDATION.name()), schemaValues);
  }

  @Test
  void actionEnumMatchesDomainAndExcludesExecutionCommands() throws Exception {
    final Set<String> schemaValues = enumValues("action");
    final Set<String> domainValues = new HashSet<>();
    for (DecisionAction action : DecisionAction.values()) {
      domainValues.add(action.name());
    }
    assertEquals(domainValues, schemaValues);
    assertEquals(
        Set.of("ABSTAIN", "OBSERVE", "NO_TRADE", "LONG_BIAS", "SHORT_BIAS"), schemaValues);
    assertFalse(schemaValues.contains("BUY"));
    assertFalse(schemaValues.contains("SELL"));
    assertFalse(schemaValues.contains("PLACE_ORDER"));
    assertFalse(schemaValues.contains("CANCEL_ORDER"));
    assertFalse(schemaValues.contains("MARKET_ORDER"));
    assertFalse(schemaValues.contains("LIMIT_ORDER"));
  }

  @Test
  void supportingEnumsMatchDomainOneToOne() throws Exception {
    assertEquals(enumNames(DecisionStatus.values()), enumValues("status"));
    assertEquals(enumNames(DecisionRiskLevel.values()), enumValues("riskLevel"));
    assertEquals(enumNames(DecisionPolicyStatus.values()), enumValues("policyStatus"));
    assertEquals(enumNames(ProviderSignalStatus.values()), enumValues("providerStatus"));
  }

  @Test
  void forbiddenActionsAreRequiredAndContainMandatorySet() throws Exception {
    final JsonNode root = new ObjectMapper().readTree(schemaPath().toFile());
    final JsonNode required = root.path("required");
    final Set<String> requiredFields = new HashSet<>();
    required.forEach(n -> requiredFields.add(n.asText()));
    assertTrue(requiredFields.contains("forbiddenActions"));

    final Set<String> schemaValues = forbiddenActionValues(root);
    assertEquals(enumNames(ForbiddenAction.values()), schemaValues);
    assertTrue(schemaValues.contains("PLACE_ORDER"));
    assertTrue(schemaValues.contains("CANCEL_ORDER"));
    assertTrue(schemaValues.contains("MUTATE_NQ_STATE"));
    assertTrue(schemaValues.contains("READ_NQ_DB"));
    assertTrue(schemaValues.contains("WRITE_NQ_DB"));
  }

  private static Set<String> enumValues(final String property) throws Exception {
    final JsonNode enumNode =
        new ObjectMapper()
            .readTree(schemaPath().toFile())
            .path("properties")
            .path(property)
            .path("enum");
    final Set<String> values = new HashSet<>();
    enumNode.forEach(n -> values.add(n.asText()));
    return values;
  }

  private static Set<String> forbiddenActionValues(final JsonNode root) {
    final JsonNode enumNode =
        root.path("properties").path("forbiddenActions").path("items").path("enum");
    final Set<String> values = new HashSet<>();
    enumNode.forEach(n -> values.add(n.asText()));
    return values;
  }

  private static Set<String> enumNames(final Enum<?>[] values) {
    final Set<String> names = new HashSet<>();
    for (Enum<?> value : values) {
      names.add(value.name());
    }
    return names;
  }
}

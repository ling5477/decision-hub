package com.guidinglight.decisionhub.contracts;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** GateK K1 schema guard against executable instructions and credential fields. */
class DecisionNoTradingInstructionContractTest {

  private static final List<String> FORBIDDEN_REQUEST_TOKENS =
      List.of(
          "orderId",
          "accountId",
          "apiKey",
          "apiSecret",
          "passphrase",
          "quantity",
          "price",
          "side");

  private static final List<String> FORBIDDEN_OUTPUT_PROPERTIES =
      List.of("command", "orderCommand", "executionCommand", "freeTextFinalOutput", "rawText");

  @Test
  void decisionRequestSchemaContainsNoCredentialOrExecutionFields() throws Exception {
    final String body = Files.readString(schema("dh-decision-request.schema.json"));
    for (String token : FORBIDDEN_REQUEST_TOKENS) {
      assertFalse(body.contains(token), "request schema must not contain token: " + token);
    }
  }

  @Test
  void decisionOutputSchemaContainsNoFreeTextOrExecutionCommandProperties() throws Exception {
    final String body = Files.readString(schema("dh-decision-output.schema.json"));
    for (String token : FORBIDDEN_OUTPUT_PROPERTIES) {
      assertFalse(body.contains(token), "output schema must not contain token: " + token);
    }
  }

  private static Path schema(final String filename) {
    return Path.of("..", "contracts", "json-schema", filename).toAbsolutePath().normalize();
  }
}

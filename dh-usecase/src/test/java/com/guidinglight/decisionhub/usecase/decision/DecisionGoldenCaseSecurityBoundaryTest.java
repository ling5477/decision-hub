package com.guidinglight.decisionhub.usecase.decision;

import static com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.all;
import static com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.requiredObject;
import static com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.requiredText;
import static com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.stringList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.GoldenCase;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * K7 golden case 安全边界测试。
 *
 * <p>该测试只扫描 fixture 内容，不启动外部 runtime。它允许 `expectedDecision.forbiddenActions`
 * 保存固定禁止清单，但禁止这些词作为 output action、字段名或 runtime 接入配置出现。
 */
final class DecisionGoldenCaseSecurityBoundaryTest {

    private static final Set<String> FORBIDDEN_FIELD_NAMES =
            Set.of(
                    "accountId",
                    "apiKey",
                    "apiSecret",
                    "passphrase",
                    "quantity",
                    "price",
                    "side",
                    "orderId",
                    "venueCredential",
                    "brokerCredential",
                    "privateKey",
                    "token");
    private static final Set<String> FORBIDDEN_OUTPUT_ACTIONS =
            Set.of("BUY", "SELL", "PLACE_ORDER", "CANCEL_ORDER", "MARKET_ORDER", "LIMIT_ORDER");
    private static final Set<String> FIXED_FORBIDDEN_ACTIONS =
            Set.of("PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE", "READ_NQ_DB", "WRITE_NQ_DB");
    private static final Set<String> FORBIDDEN_RUNTIME_TOKENS =
            Set.of(
                    "RealClient",
                    "LangGraph",
                    "OpenAI",
                    "Claude",
                    "Gemini",
                    "WebClient",
                    "RestTemplate",
                    "HttpClient",
                    "https://",
                    "http://",
                    "127.0.0.1",
                    "localhost");

    @Test
    void goldenCasesContainNoSensitiveOrExecutionFields() {
        for (GoldenCase goldenCase : all()) {
            assertNoForbiddenFieldNames(goldenCase.root(), goldenCase.caseId());
            assertNoForbiddenRuntimeTokens(goldenCase.root().toString(), goldenCase.caseId());
            assertTrue(requiredObject(goldenCase.root(), "forbiddenAssertions").path("noCredentialFields").asBoolean());
            assertTrue(requiredObject(goldenCase.root(), "forbiddenAssertions").path("noExecutionIntent").asBoolean());
        }
    }

    @Test
    void forbiddenActionsAreFixedButNeverUsedAsOutputAction() {
        for (GoldenCase goldenCase : all()) {
            final JsonNode expected = goldenCase.expectedDecision();

            assertFalse(FORBIDDEN_OUTPUT_ACTIONS.contains(requiredText(expected, "action")), goldenCase.caseId());
            assertEquals(FIXED_FORBIDDEN_ACTIONS, Set.copyOf(stringList(expected.path("forbiddenActions"))));
            assertNoForbiddenOutputActionValueOutsideForbiddenActions(
                    goldenCase.root(), "", goldenCase.caseId());
        }
    }

    @Test
    void securityBoundaryKeepsRuntimeAndLiveDisabled() {
        for (GoldenCase goldenCase : all()) {
            final JsonNode boundary = requiredObject(goldenCase.root(), "securityBoundary");

            assertTrue(boundary.path("readOnly").asBoolean(), goldenCase.caseId());
            assertEquals("NOT_STARTED", requiredText(boundary, "nqRuntime"), goldenCase.caseId());
            assertEquals("FORBIDDEN", requiredText(boundary, "externalRuntime"), goldenCase.caseId());
            assertEquals("FORBIDDEN", requiredText(boundary, "providerRuntime"), goldenCase.caseId());
            assertEquals("DISABLED", requiredText(boundary, "liveState"), goldenCase.caseId());
        }
    }

    private static void assertNoForbiddenFieldNames(final JsonNode node, final String caseId) {
        if (node.isObject()) {
            final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> field = fields.next();
                assertFalse(FORBIDDEN_FIELD_NAMES.contains(field.getKey()), caseId + ":" + field.getKey());
                assertNoForbiddenFieldNames(field.getValue(), caseId);
            }
            return;
        }
        if (node.isArray()) {
            node.forEach(item -> assertNoForbiddenFieldNames(item, caseId));
        }
    }

    private static void assertNoForbiddenOutputActionValueOutsideForbiddenActions(
            final JsonNode node, final String currentField, final String caseId) {
        if (node.isObject()) {
            final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> field = fields.next();
                assertNoForbiddenOutputActionValueOutsideForbiddenActions(
                        field.getValue(), field.getKey(), caseId);
            }
            return;
        }
        if (node.isArray()) {
            node.forEach(item -> assertNoForbiddenOutputActionValueOutsideForbiddenActions(item, currentField, caseId));
            return;
        }
        if (node.isTextual()
                && FORBIDDEN_OUTPUT_ACTIONS.contains(node.asText())
                && !"forbiddenActions".equals(currentField)) {
            throw new AssertionError(caseId + " uses forbidden output action outside forbiddenActions: " + node.asText());
        }
    }

    private static void assertNoForbiddenRuntimeTokens(final String json, final String caseId) {
        for (String token : FORBIDDEN_RUNTIME_TOKENS) {
            assertFalse(json.contains(token), caseId + " must not contain runtime token " + token);
        }
    }
}

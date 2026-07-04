package com.guidinglight.decisionhub.usecase.decision.integration1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.ForbiddenAction;
import com.guidinglight.decisionhub.usecase.decision.integration1.support.DhDryRunTestSupportEntry;
import com.guidinglight.decisionhub.usecase.decision.integration1.support.DhDryRunTestSupportEntry.Exchange;
import com.guidinglight.decisionhub.usecase.decision.integration1.support.DhDryRunTestSupportEntry.InMemoryNonceStore;
import com.guidinglight.decisionhub.usecase.decision.integration1.support.DhDryRunTestSupportEntry.Result;
import com.guidinglight.decisionhub.usecase.decision.integration1.support.DhDryRunTestSupportEntry.Step;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * IMP3 joint mock contract fixture tests。
 *
 * <p>本测试只消费 `src/test/resources` 中的 mock-only fixture family，并复用 IMP1 的 test-support
 * validation chain。它不创建 runtime endpoint、不修改生产 schema、不接真实 HTTP、不接真实 provider，也不把
 * `NQ_DRYRUN` 放入生产 allowlist。
 */
final class DhIntegration1JointMockContractFixtureTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Instant NOW = Instant.parse("2026-07-04T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String RESOURCE = "/nq-dh/integration1/joint_mock_contract_fixtures.json";
    private static final Set<String> REQUEST_FAMILIES =
            Set.of(
                    "valid_read_only_recommendation_request",
                    "missing_signature_request",
                    "invalid_signature_request",
                    "timestamp_skew_request",
                    "nonce_replay_request",
                    "source_denied_request",
                    "tenant_mismatch_request",
                    "forbidden_credential_field_request",
                    "forbidden_order_account_field_request",
                    "forbidden_execution_intent_request");
    private static final Set<String> RESPONSE_FAMILIES =
            Set.of(
                    "valid_abstain_response",
                    "valid_observe_response",
                    "valid_no_trade_response",
                    "readonly_long_bias_response",
                    "readonly_short_bias_response",
                    "fail_closed_provider_timeout_response",
                    "fail_closed_risk_blocked_response",
                    "fail_closed_internal_error_response");
    private static final Set<String> READONLY_ACTIONS =
            Set.of("ABSTAIN", "OBSERVE", "NO_TRADE", "LONG_BIAS", "SHORT_BIAS");
    private static final Set<String> DOC_ONLY_ALIAS_FIELDS =
            Set.of("dryRun", "decisionId", "confidence", "traceSummary", "replayRef", "auditRef",
                    "X-NQ-DH-Schema-Version");

    @Test
    void fixtureSetContainsAllRequiredFamiliesAndStaysMockOnly() throws Exception {
        final JsonNode root = fixtures();

        assertTrue(root.path("mockOnly").asBoolean());
        assertEquals("NO_RUNTIME", root.path("runtimeState").asText());
        assertEquals(REQUEST_FAMILIES, familyNames("request"));
        assertEquals(RESPONSE_FAMILIES, familyNames("response"));
        for (JsonNode fixture : families()) {
            assertFixturePayloadHasNoRealUrlCredentialOrExecutionMaterial(fixture);
        }
    }

    @Test
    void requestFixturesParseAndValidateThroughMockOnlyChain() throws Exception {
        final DhDryRunTestSupportEntry entry = defaultEntry();

        final Result valid = entry.validate(signedExchange(fixture("valid_read_only_recommendation_request")));
        assertTrue(valid.accepted());
        assertEquals(200, valid.statusCode());
        assertTrue(valid.steps().contains(Step.STRUCTURED_DECISION_OUTPUT_ASSEMBLY_BOUNDARY));

        assertRejected(entry, "missing_signature_request", "MISSING_CANONICAL_HEADER");
        assertRejected(entry, "invalid_signature_request", "SIGNATURE_INVALID");
        assertRejected(entry, "timestamp_skew_request", "TIMESTAMP_INVALID");
        assertRejected(entry, "source_denied_request", "SOURCE_DENIED");
        assertRejected(entry, "tenant_mismatch_request", "HEADER_BINDING_MISMATCH");

        final Exchange replay = signedExchange(fixture("nonce_replay_request"));
        assertTrue(entry.validate(replay).accepted());
        final Result replayResult = entry.validate(replay);
        assertEquals("NONCE_REPLAY", replayResult.errorCode());
        assertFalse(replayResult.accepted());
    }

    @Test
    void forbiddenRequestFamiliesFailClosedAfterSyntheticMutation() throws Exception {
        for (Map.Entry<String, Mutation> entry : forbiddenMutations().entrySet()) {
            final JsonNode fixture = fixture(entry.getKey());
            final ObjectNode payload = ((ObjectNode) fixture.path("request").deepCopy());
            payload.put(entry.getValue().fieldName(), entry.getValue().fieldValue());
            final Result result = defaultEntry().validate(signedExchangeWithPayload(fixture, payload));

            assertFalse(result.accepted());
            assertEquals("FORBIDDEN_FIELD", result.errorCode());
            assertTrue(result.steps().contains(Step.FORBIDDEN_FIELDS_VALIDATION));
            assertEquals(DecisionAction.ABSTAIN, result.output().getAction());
        }
    }

    @Test
    void responseFixturesStayReadonlyAndFailClosedWhereRequired() throws Exception {
        for (JsonNode fixture : families()) {
            if (!"response".equals(fixture.path("fixtureKind").asText())) {
                continue;
            }
            final JsonNode response = fixture.path("response");

            assertRequiredResponseShape(response);
            assertTrue(READONLY_ACTIONS.contains(response.path("action").asText()));
            assertFalse(Set.of("BUY", "SELL").contains(response.path("action").asText()));
            assertEquals(ForbiddenAction.mandatorySet(), forbiddenActions(response));

            if (fixture.path("expectedOutcome").asText().contains("FAIL_CLOSED")) {
                assertEquals("ABSTAIN", response.path("action").asText());
                assertTrue(Set.of("ABSTAINED", "BLOCKED").contains(response.path("status").asText()));
            }
        }
    }

    @Test
    void runtimeEndpointControllerRealProviderAgentAndLangGraphAreStillAbsent() throws Exception {
        for (String token : List.of(
                "NqDhDryRunController",
                "NqDhDryRunClient",
                "RealNqDhDryRunClient",
                "NqDhDryRunAgent",
                "NqDhLangGraphDryRun",
                "Integration1DryRunController",
                "Integration1LangGraphDryRun",
                "@PostMapping(\"/api/nq-dh",
                "@RequestMapping(\"/api/nq-dh")) {
            assertNoProductionToken(token, List.of("dh-api/src/main/java", "dh-app/src/main/java"));
        }
    }

    @Test
    void schemaAliasesRemainNotRequiredByCurrentSchema() throws Exception {
        final JsonNode requestSchema = readSchema("dh-decision-request.schema.json");
        final JsonNode outputSchema = readSchema("dh-decision-output.schema.json");

        for (String alias : DOC_ONLY_ALIAS_FIELDS) {
            assertFalse(requiredFields(requestSchema).contains(alias), alias + " must not be request required");
            assertFalse(propertyNames(requestSchema).contains(alias), alias + " must not be request property");
            assertFalse(requiredFields(outputSchema).contains(alias), alias + " must not be output required");
            assertFalse(propertyNames(outputSchema).contains(alias), alias + " must not be output property");
        }
    }

    private static void assertRejected(
            final DhDryRunTestSupportEntry entry, final String family, final String errorCode) throws Exception {
        final Result result = entry.validate(signedExchange(fixture(family)));

        assertFalse(result.accepted());
        assertEquals(errorCode, result.errorCode());
        assertTrue(result.steps().contains(Step.FAIL_CLOSED_RESPONSE_NORMALIZATION));
        assertEquals(DecisionAction.ABSTAIN, result.output().getAction());
    }

    private static DhDryRunTestSupportEntry defaultEntry() {
        return new DhDryRunTestSupportEntry(
                request -> com.guidinglight.decisionhub.domain.decision.DecisionOutput.observation(
                        request.getRequestId(),
                        request.getTraceId(),
                        request.getTenantId(),
                        DecisionAction.NO_TRADE,
                        com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel.LOW,
                        List.of("JOINT_MOCK_VALIDATION"),
                        request.getContextSnapshot().evidenceRefs(),
                        NOW),
                new InMemoryNonceStore(),
                CLOCK);
    }

    private static Exchange signedExchange(final JsonNode fixture) throws JsonProcessingException {
        return signedExchangeWithPayload(fixture, (ObjectNode) fixture.path("request").deepCopy());
    }

    private static Exchange signedExchangeWithPayload(final JsonNode fixture, final ObjectNode payload)
            throws JsonProcessingException {
        final String body = MAPPER.writeValueAsString(payload);
        final Map<String, String> headers = headers(fixture.path("headers"));
        if (headers.containsKey(DhDryRunTestSupportEntry.HEADER_SIGNATURE)
                && !"fixture-invalid-signature".equals(headers.get(DhDryRunTestSupportEntry.HEADER_SIGNATURE))) {
            headers.put(
                    DhDryRunTestSupportEntry.HEADER_SIGNATURE,
                    DhDryRunTestSupportEntry.sign(headers, body, DhDryRunTestSupportEntry.TEST_SECRET));
        }
        return new Exchange(headers, body, payload);
    }

    private static Map<String, String> headers(final JsonNode headerNode) {
        final Map<String, String> headers = new LinkedHashMap<>();
        headerNode.fields().forEachRemaining(entry -> headers.put(entry.getKey(), entry.getValue().asText()));
        return headers;
    }

    private static Set<String> familyNames(final String kind) throws Exception {
        final Set<String> names = new HashSet<>();
        for (JsonNode fixture : families()) {
            if (kind.equals(fixture.path("fixtureKind").asText())) {
                names.add(fixture.path("fixtureFamily").asText());
            }
        }
        return names;
    }

    private static List<JsonNode> families() throws Exception {
        final List<JsonNode> families = new ArrayList<>();
        fixtures().path("families").forEach(families::add);
        return families;
    }

    private static JsonNode fixture(final String family) throws Exception {
        for (JsonNode fixture : families()) {
            if (family.equals(fixture.path("fixtureFamily").asText())) {
                return fixture;
            }
        }
        throw new IllegalArgumentException("missing fixture family: " + family);
    }

    private static JsonNode fixtures() throws IOException {
        try (InputStream input = DhIntegration1JointMockContractFixtureTest.class.getResourceAsStream(RESOURCE)) {
            assertNotNull(input, "missing fixture resource " + RESOURCE);
            return MAPPER.readTree(input);
        }
    }

    private static void assertRequiredResponseShape(final JsonNode response) {
        for (String field : List.of(
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
                "schemaVersion")) {
            assertFalse(response.path(field).isMissingNode(), "response must contain " + field);
        }
        assertEquals("READ_ONLY_RECOMMENDATION", response.path("decisionType").asText());
        assertEquals("1.0.0", response.path("schemaVersion").asText());
    }

    private static Set<ForbiddenAction> forbiddenActions(final JsonNode response) {
        final Set<ForbiddenAction> actions = new HashSet<>();
        response.path("forbiddenActions").forEach(node -> actions.add(ForbiddenAction.valueOf(node.asText())));
        return actions;
    }

    private static void assertFixturePayloadHasNoRealUrlCredentialOrExecutionMaterial(final JsonNode fixture) {
        final String payloadText = (fixture.path("request").toString() + fixture.path("response").toString())
                .toLowerCase(Locale.ROOT);
        for (String token : List.of(
                "http://",
                "https://",
                "apikey",
                "apisecret",
                "passphrase",
                "cookie",
                "privatekey",
                "accountid",
                "orderid",
                "quantity",
                "price",
                "leverage",
                "placeorder",
                "cancelorder",
                "paperrunstart",
                "liverunstart",
                "mutaterisk",
                "mutateledger")) {
            assertFalse(payloadText.contains(token), "fixture payload must not contain " + token);
        }
        assertFalse(payloadText.contains("\"buy\""));
        assertFalse(payloadText.contains("\"sell\""));
    }

    private static Map<String, Mutation> forbiddenMutations() {
        final Map<String, Mutation> mutations = new LinkedHashMap<>();
        mutations.put("forbidden_credential_field_request", new Mutation("apiSecret", "synthetic-rejected"));
        mutations.put("forbidden_order_account_field_request", new Mutation("orderId", "synthetic-rejected"));
        mutations.put("forbidden_execution_intent_request", new Mutation("intent", "BUY"));
        return mutations;
    }

    private static JsonNode readSchema(final String fileName) throws IOException {
        for (Path candidate : List.of(
                Path.of("contracts", "json-schema", fileName),
                Path.of("..", "contracts", "json-schema", fileName))) {
            final Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.exists(normalized)) {
                return MAPPER.readTree(normalized.toFile());
            }
        }
        throw new IOException("schema not found: " + fileName);
    }

    private static Set<String> requiredFields(final JsonNode schema) {
        final Set<String> fields = new HashSet<>();
        schema.path("required").forEach(node -> fields.add(node.asText()));
        return fields;
    }

    private static Set<String> propertyNames(final JsonNode schema) {
        final Set<String> fields = new HashSet<>();
        schema.path("properties").fieldNames().forEachRemaining(fields::add);
        return fields;
    }

    private static void assertNoProductionToken(final String token, final List<String> roots) throws IOException {
        for (Path root : existingRoots(roots)) {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(Files::isRegularFile)
                        .filter(DhIntegration1JointMockContractFixtureTest::isJavaFile)
                        .toList()) {
                    assertFalse(
                            Files.readString(file).contains(token),
                            "production Java source must not contain token " + token + " in " + file);
                }
            }
        }
    }

    private static List<Path> existingRoots(final List<String> roots) {
        final List<Path> existing = new ArrayList<>();
        for (String root : roots) {
            for (Path candidate : List.of(Path.of(root), Path.of("..", root))) {
                final Path normalized = candidate.toAbsolutePath().normalize();
                if (Files.isDirectory(normalized)) {
                    existing.add(normalized);
                    break;
                }
            }
        }
        return existing;
    }

    private static boolean isJavaFile(final Path path) {
        return path.getFileName().toString().endsWith(".java");
    }

    private record Mutation(String fieldName, String fieldValue) {
    }
}

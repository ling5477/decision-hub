package com.guidinglight.decisionhub.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.ForbiddenAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Integration-1 contract gap guard。
 *
 * <p>本测试跟随 `NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-IMPLEMENTATION` 更新：允许 DH 内部出现受限
 * `POST /api/ai/decision-dry-runs` 入站 endpoint、dev/test `NQ_DRYRUN` source 与 endpoint-local error
 * taxonomy；仍禁止把这些内容写入 formal schema、production allowlist、NQ client、真实 provider、真实 HTTP 或
 * LIVE 路径。
 */
class DecisionContractGapGuardTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Set<String> DOC_ONLY_ALIAS_FIELDS =
            Set.of(
                    "dryRun",
                    "decisionId",
                    "confidence",
                    "traceSummary",
                    "replayRef",
                    "auditRef",
                    "X-NQ-DH-Schema-Version");

    private static final Set<String> FORBIDDEN_EXECUTION_PROPERTY_NAMES =
            Set.of("buy", "sell", "quantity", "price", "leverage", "order", "account", "credential", "mutation");

    private static final Set<String> CANONICAL_ERROR_MAPPING_CANDIDATES =
            Set.of(
                    "SIGNATURE_INVALID",
                    "TIMESTAMP_INVALID",
                    "TIMESTAMP_OUT_OF_WINDOW",
                    "NONCE_REPLAY",
                    "TENANT_MISMATCH",
                    "SOURCE_DENIED",
                    "PAYLOAD_TOO_LARGE",
                    "RATE_LIMITED",
                    "MEMORY_LIMIT_EXCEEDED",
                    "POLICY_DENIED",
                    "PROVIDER_DISABLED",
                    "PROVIDER_TIMEOUT",
                    "BUDGET_EXCEEDED",
                    "UNKNOWN_ERROR");

    private static final List<Path> MAIN_SOURCE_ROOTS =
            List.of(
                    Path.of("src", "main", "java"),
                    Path.of("..", "dh-security", "src", "main", "java"),
                    Path.of("..", "dh-usecase", "src", "main", "java"),
                    Path.of("..", "dh-api", "src", "main", "java"),
                    Path.of("..", "dh-app", "src", "main", "java"),
                    Path.of("..", "dh-infra", "src", "main", "java"));

    private static final List<Path> RUNTIME_ENDPOINT_SOURCE_ROOTS =
            List.of(
                    Path.of("..", "dh-api", "src", "main", "java"),
                    Path.of("..", "dh-app", "src", "main", "java"));

    private static final List<String> LIMITED_RUNTIME_ENDPOINT_ALLOWED_PATH_PARTS =
            List.of(
                    "/dh-api/src/main/java/com/guidinglight/decisionhub/api/decision/DecisionDryRun",
                    "/dh-api/src/main/java/com/guidinglight/decisionhub/api/security/DhApiAuthenticationFilter.java",
                    "/dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfig.java",
                    "/dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/HmacNqDryRunAuthenticator.java",
                    "/dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NqDryRunAuthRequest.java",
                    "/dh-security/src/main/java/com/guidinglight/decisionhub/security/nq/NqDryRunAuthResult.java",
                    "/dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DecisionDryRun",
                    "/dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun/DefaultDecisionDryRunService.java",
                    "/dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/");

    @Test
    void sourceNqDryrunRemainsReviewGatedAndIsNotProductionAllowlisted() throws Exception {
        final JsonNode sourceProperty = schema("dh-decision-request.schema.json").path("properties").path("source");

        assertFalse(enumValues(sourceProperty).contains("NQ_DRYRUN"));
        assertFalse(sourceProperty.path("const").asText("").equals("NQ_DRYRUN"));
        assertJavaSourceTokenOnlyInAllowedLimitedRuntimeFiles("NQ_DRYRUN", MAIN_SOURCE_ROOTS);
        assertProductionProfileKeepsLimitedRuntimeDisabled();
    }

    @Test
    void limitedDryRunEndpointRemainsInboundOnlyAndDoesNotAddNqRuntimeClient() throws Exception {
        assertJavaSourceContains(
                Path.of("..", "dh-api", "src", "main", "java", "com", "guidinglight", "decisionhub", "api",
                        "decision", "DecisionDryRunController.java"),
                "@PostMapping(\"/decision-dry-runs\")");
        assertNoJavaSourceToken("NqDhDryRunClient", RUNTIME_ENDPOINT_SOURCE_ROOTS);
        assertNoJavaSourceToken("RealNqDryRun", RUNTIME_ENDPOINT_SOURCE_ROOTS);
        assertNoJavaSourceToken("NqDhLangGraphDryRun", RUNTIME_ENDPOINT_SOURCE_ROOTS);
        assertNoJavaSourceToken("@RequestMapping(\"/api/nq-dh", RUNTIME_ENDPOINT_SOURCE_ROOTS);
        assertNoJavaSourceToken("@PostMapping(\"/api/nq-dh", RUNTIME_ENDPOINT_SOURCE_ROOTS);
    }

    @Test
    void docOnlyAliasFieldsAreNotSchemaRequiredOrProperties() throws Exception {
        final JsonNode requestSchema = schema("dh-decision-request.schema.json");
        final JsonNode outputSchema = schema("dh-decision-output.schema.json");

        for (String alias : DOC_ONLY_ALIAS_FIELDS) {
            assertFalse(requiredFields(requestSchema).contains(alias), alias + " must not be request required");
            assertFalse(propertyNames(requestSchema).contains(alias), alias + " must not be request property");
            assertFalse(requiredFields(outputSchema).contains(alias), alias + " must not be output required");
            assertFalse(propertyNames(outputSchema).contains(alias), alias + " must not be output property");
        }
    }

    @Test
    void actionAndForbiddenActionContractsRemainReadonlyAndFixed() {
        final Set<String> decisionActions = enumNames(DecisionAction.values());
        final Set<String> forbiddenActions = enumNames(ForbiddenAction.values());

        assertEquals(Set.of("ABSTAIN", "OBSERVE", "NO_TRADE", "LONG_BIAS", "SHORT_BIAS"), decisionActions);
        assertFalse(decisionActions.contains("BUY"));
        assertFalse(decisionActions.contains("SELL"));

        assertEquals(
                Set.of("PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE", "READ_NQ_DB", "WRITE_NQ_DB"),
                forbiddenActions);
    }

    @Test
    void schemaPropertiesContainNoExecutableTradingFields() throws Exception {
        final Set<String> propertyNames = new HashSet<>();
        collectPropertyNames(schema("dh-decision-request.schema.json"), propertyNames);
        collectPropertyNames(schema("dh-decision-output.schema.json"), propertyNames);

        for (String property : propertyNames) {
            final String normalized = normalize(property);
            assertFalse(
                    FORBIDDEN_EXECUTION_PROPERTY_NAMES.contains(normalized),
                    "schema property must not become execution field: " + property);
        }
        assertFalse(enumValues(schema("dh-decision-output.schema.json").path("properties").path("action")).contains("BUY"));
        assertFalse(enumValues(schema("dh-decision-output.schema.json").path("properties").path("action")).contains("SELL"));
    }

    @Test
    void canonicalErrorNamesAreImplementedInEndpointLocalEnum() throws Exception {
        final Path errorCodeFile =
                Path.of("..", "dh-usecase", "src", "main", "java", "com", "guidinglight", "decisionhub", "usecase",
                                "decision", "dryrun", "DecisionDryRunErrorCode.java")
                        .toAbsolutePath()
                        .normalize();
        final String errorCodeSource = Files.readString(errorCodeFile);
        for (String errorName : CANONICAL_ERROR_MAPPING_CANDIDATES) {
            assertTrue(
                    errorCodeSource.contains(errorName),
                    errorName + " must be present in endpoint-local DecisionDryRunErrorCode");
        }
    }

    private static JsonNode schema(final String fileName) throws IOException {
        return MAPPER.readTree(
                Path.of("..", "contracts", "json-schema", fileName).toAbsolutePath().normalize().toFile());
    }

    private static Set<String> requiredFields(final JsonNode root) {
        final Set<String> values = new HashSet<>();
        root.path("required").forEach(node -> values.add(node.asText()));
        return values;
    }

    private static Set<String> propertyNames(final JsonNode root) {
        final Set<String> values = new HashSet<>();
        root.path("properties").fieldNames().forEachRemaining(values::add);
        return values;
    }

    private static Set<String> enumValues(final JsonNode propertyNode) {
        final Set<String> values = new HashSet<>();
        propertyNode.path("enum").forEach(node -> values.add(node.asText()));
        return values;
    }

    private static Set<String> enumNames(final Enum<?>[] values) {
        final Set<String> names = new HashSet<>();
        for (Enum<?> value : values) {
            names.add(value.name());
        }
        return names;
    }

    private static void collectPropertyNames(final JsonNode node, final Set<String> names) {
        if (node == null || node.isMissingNode()) {
            return;
        }
        final JsonNode properties = node.path("properties");
        if (properties.isObject()) {
            properties.fieldNames().forEachRemaining(names::add);
            properties.forEach(child -> collectPropertyNames(child, names));
        }
        if (node.path("items").isObject()) {
            collectPropertyNames(node.path("items"), names);
        }
    }

    private static void assertNoJavaSourceToken(final String token, final List<Path> roots) throws IOException {
        for (Path root : roots) {
            final Path normalizedRoot = root.toAbsolutePath().normalize();
            if (!Files.exists(normalizedRoot)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(normalizedRoot)) {
                for (Path file : files.filter(Files::isRegularFile).filter(DecisionContractGapGuardTest::isJavaFile).toList()) {
                    final String content = Files.readString(file);
                    assertFalse(content.contains(token), "production Java source must not contain " + token + " in " + file);
                }
            }
        }
    }

    private static void assertJavaSourceTokenOnlyInAllowedLimitedRuntimeFiles(final String token, final List<Path> roots)
            throws IOException {
        for (Path root : roots) {
            final Path normalizedRoot = root.toAbsolutePath().normalize();
            if (!Files.exists(normalizedRoot)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(normalizedRoot)) {
                for (Path file : files.filter(Files::isRegularFile).filter(DecisionContractGapGuardTest::isJavaFile).toList()) {
                    final String content = Files.readString(file);
                    if (content.contains(token)) {
                        assertTrue(
                                isAllowedLimitedRuntimeEndpointFile(file),
                                "production Java source may contain "
                                        + token
                                        + " only in limited dry-run endpoint files, found in "
                                        + file);
                    }
                }
            }
        }
    }

    private static void assertJavaSourceContains(final Path path, final String token) throws IOException {
        final Path normalized = path.toAbsolutePath().normalize();
        assertTrue(Files.exists(normalized), "expected Java source does not exist: " + normalized);
        assertTrue(Files.readString(normalized).contains(token), "expected source token not found: " + token);
    }

    private static void assertProductionProfileKeepsLimitedRuntimeDisabled() throws IOException {
        final Path prodConfig =
                Path.of("..", "dh-app", "src", "main", "resources", "application-prod.yml")
                        .toAbsolutePath()
                        .normalize();
        final String content = Files.readString(prodConfig);
        assertTrue(content.contains("enabled: false"), "prod profile must keep limited runtime endpoint disabled");
        assertTrue(content.contains("production-enabled: false"), "prod profile must not permit runtime enablement");
        assertTrue(content.contains("kill-switch-enabled: true"), "prod profile must keep kill switch active");
        assertTrue(content.contains("allowed-sources: \"\""), "prod profile must not allowlist NQ_DRYRUN");
    }

    private static boolean isAllowedLimitedRuntimeEndpointFile(final Path file) {
        final String normalized = file.toAbsolutePath().normalize().toString().replace('\\', '/');
        return LIMITED_RUNTIME_ENDPOINT_ALLOWED_PATH_PARTS.stream().anyMatch(normalized::contains);
    }

    private static boolean isJavaFile(final Path path) {
        return path.getFileName().toString().endsWith(".java");
    }

    private static String normalize(final String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}

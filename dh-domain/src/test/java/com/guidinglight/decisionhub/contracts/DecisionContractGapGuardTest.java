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
 * IMP0 contract gap guard：只验证当前 DH dry-run 相关 gap 仍保持 test-support / review-gated。
 *
 * <p>本测试不得成为 runtime 授权。若未来要落地 source allowlist、canonical error enum、wire-level alias 或
 * dry-run endpoint，必须先删除或改写本测试，并经过独立 contract / API / security review。
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
                    "AUTH_FAILED",
                    "CONTRACT_INVALID",
                    "TIMESTAMP_SKEW",
                    "NONCE_REPLAY",
                    "SOURCE_DENIED",
                    "INTERNAL_FAIL_CLOSED");

    private static final List<Path> MAIN_SOURCE_ROOTS =
            List.of(
                    Path.of("src", "main", "java"),
                    Path.of("..", "dh-usecase", "src", "main", "java"),
                    Path.of("..", "dh-api", "src", "main", "java"),
                    Path.of("..", "dh-app", "src", "main", "java"),
                    Path.of("..", "dh-infra", "src", "main", "java"));

    private static final List<Path> RUNTIME_ENDPOINT_SOURCE_ROOTS =
            List.of(
                    Path.of("..", "dh-api", "src", "main", "java"),
                    Path.of("..", "dh-app", "src", "main", "java"));

    @Test
    void sourceNqDryrunRemainsReviewGatedAndIsNotProductionAllowlisted() throws Exception {
        final JsonNode sourceProperty = schema("dh-decision-request.schema.json").path("properties").path("source");

        assertFalse(enumValues(sourceProperty).contains("NQ_DRYRUN"));
        assertFalse(sourceProperty.path("const").asText("").equals("NQ_DRYRUN"));
        assertNoJavaSourceToken("NQ_DRYRUN", MAIN_SOURCE_ROOTS);
    }

    @Test
    void dryRunEndpointShapeRemainsNoRuntimeEndpoint() throws Exception {
        assertNoJavaSourceToken("NQ_DRYRUN", RUNTIME_ENDPOINT_SOURCE_ROOTS);
        assertNoJavaSourceToken("dry-run", RUNTIME_ENDPOINT_SOURCE_ROOTS);
        assertNoJavaSourceToken("dryrun", RUNTIME_ENDPOINT_SOURCE_ROOTS);
        assertNoJavaSourceToken("dryRun", RUNTIME_ENDPOINT_SOURCE_ROOTS);
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
    void canonicalErrorNamesRemainMappingCandidatesNotProductionEnums() throws Exception {
        for (String errorName : CANONICAL_ERROR_MAPPING_CANDIDATES) {
            assertNoJavaSourceToken(errorName, MAIN_SOURCE_ROOTS);
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

    private static boolean isJavaFile(final Path path) {
        return path.getFileName().toString().endsWith(".java");
    }

    private static String normalize(final String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}

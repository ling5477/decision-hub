package com.guidinglight.decisionhub.usecase.decision.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * K7 golden case fixture loader。
 *
 * <p>测试只在本地读取 `golden_cases/decision/*.json`，并把 `input` 转成 K1 domain request。该 loader 不写文件、
 * 不调用 provider、HTTP、NQ、LLM 或 LangGraph，用于保证 K7 eval baseline 是确定性 fixture。
 */
public final class DecisionGoldenCaseFixtures {

    /**
     * K7 要求固定存在的全部 golden case 文件。
     */
    public static final List<String> REQUIRED_FILENAMES =
            List.of(
                    "valid_no_trade.json",
                    "policy_blocked.json",
                    "provider_timeout_abstain.json",
                    "provider_budget_exceeded_abstain.json",
                    "high_risk_abstain.json",
                    "no_evidence_abstain.json",
                    "forbidden_action_rejected.json",
                    "mock_nq_valid_dryrun.json",
                    "mock_nq_provider_blocked.json",
                    "mock_nq_no_live_trade_guard.json",
                    "replay_found_trace.json",
                    "replay_tenant_mismatch_blocked.json");

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path FIXTURE_ROOT =
            Path.of("..", "golden_cases", "decision").toAbsolutePath().normalize();

    private DecisionGoldenCaseFixtures() {
    }

    /**
     * 读取 K7 全部 golden case，按文件名稳定排序。
     *
     * @return golden case 列表。
     */
    public static List<GoldenCase> all() {
        try (var files = Files.list(FIXTURE_ROOT)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(DecisionGoldenCaseFixtures::readCase)
                    .toList();
        } catch (final IOException error) {
            throw new AssertionError("failed to list K7 golden cases", error);
        }
    }

    /**
     * 按 `caseId` 查找 golden case。
     *
     * @param caseId fixture 内的 caseId。
     * @return 对应 golden case。
     */
    public static GoldenCase byCaseId(final String caseId) {
        return all().stream()
                .filter(candidate -> candidate.caseId().equals(caseId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing golden case: " + caseId));
    }

    /**
     * 把 golden case `input` 转成 K1 `DecisionRequest`。
     *
     * @param input `dh-decision-request` schema 形状的 JSON 节点。
     * @return domain request。
     */
    public static DecisionRequest requestFrom(final JsonNode input) {
        final JsonNode subject = requiredObject(input, "subject");
        final JsonNode snapshot = requiredObject(input, "contextSnapshot");
        return DecisionRequest.readOnlyRecommendation(
                requiredText(input, "requestId"),
                requiredText(input, "traceId"),
                requiredText(input, "tenantId"),
                requiredText(input, "source"),
                new DecisionSubject(
                        requiredText(subject, "symbol"),
                        requiredText(subject, "market"),
                        requiredText(subject, "timeframe"),
                        nullableText(subject, "strategyRef"),
                        nullableText(subject, "researchRef")),
                nullableText(input, "contextRef"),
                new DecisionContextSnapshot(
                        requiredText(snapshot, "snapshotId"),
                        Instant.parse(requiredText(snapshot, "capturedAt")),
                        stringList(snapshot.path("evidenceRefs"))),
                Instant.parse(requiredText(input, "requestedAt")));
    }

    /**
     * 断言实际 output 与 golden case `expectedDecision` 完全一致。
     *
     * @param caseId caseId，用于失败信息定位。
     * @param expected `expectedDecision` JSON。
     * @param output 实际 K1 structured output。
     */
    public static void assertDecisionMatches(
            final String caseId, final JsonNode expected, final DecisionOutput output) {
        assertEquals(requiredText(expected, "requestId"), output.getRequestId(), caseId);
        assertEquals(requiredText(expected, "traceId"), output.getTraceId(), caseId);
        assertEquals(requiredText(expected, "tenantId"), output.getTenantId(), caseId);
        assertEquals(requiredText(expected, "decisionType"), output.getDecisionType().name(), caseId);
        assertEquals(requiredText(expected, "action"), output.getAction().name(), caseId);
        assertEquals(requiredText(expected, "status"), output.getStatus().name(), caseId);
        assertEquals(requiredText(expected, "riskLevel"), output.getRiskLevel().name(), caseId);
        assertEquals(requiredText(expected, "policyStatus"), output.getPolicyStatus().name(), caseId);
        assertEquals(requiredText(expected, "providerStatus"), output.getProviderStatus().name(), caseId);
        assertEquals(
                new HashSet<>(stringList(expected.path("forbiddenActions"))),
                output.getForbiddenActions().stream().map(Enum::name).collect(Collectors.toSet()),
                caseId);
        assertEquals(stringList(expected.path("reasonCodes")), output.getReasonCodes(), caseId);
        assertEquals(stringList(expected.path("evidenceRefs")), output.getEvidenceRefs(), caseId);
        assertEquals(Instant.parse(requiredText(expected, "createdAt")), output.getCreatedAt(), caseId);
        assertEquals(requiredText(expected, "schemaVersion"), output.getSchemaVersion(), caseId);
    }

    /**
     * 返回 JSON array 中的文本值列表。
     *
     * @param node array 节点。
     * @return 字符串列表；缺失时为空。
     */
    public static List<String> stringList(final JsonNode node) {
        final List<String> values = new ArrayList<>();
        if (node == null || node.isMissingNode() || node.isNull()) {
            return values;
        }
        assertTrue(node.isArray(), "expected array node");
        node.forEach(item -> values.add(item.asText()));
        return List.copyOf(values);
    }

    /**
     * 返回 JSON object 的字段名集合。
     *
     * @param node object 节点。
     * @return 字段名集合。
     */
    public static Set<String> fieldNames(final JsonNode node) {
        final Set<String> names = new HashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    /**
     * 读取 schema JSON。
     *
     * @param filename `contracts/json-schema` 下的 schema 文件名。
     * @return schema JSON。
     */
    public static JsonNode schema(final String filename) {
        try {
            return MAPPER.readTree(Path.of("..", "contracts", "json-schema", filename).toFile());
        } catch (final IOException error) {
            throw new AssertionError("failed to read schema: " + filename, error);
        }
    }

    /**
     * 读取文本字段；字段缺失或为空时让测试失败。
     */
    public static String requiredText(final JsonNode node, final String field) {
        final JsonNode value = node.path(field);
        assertTrue(value.isTextual(), "missing text field: " + field);
        assertTrue(!value.asText().isBlank(), "blank text field: " + field);
        return value.asText();
    }

    /**
     * 读取可空文本字段。
     */
    public static String nullableText(final JsonNode node, final String field) {
        final JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        assertTrue(value.isTextual(), "expected nullable text field: " + field);
        return value.asText();
    }

    /**
     * 返回必填 object 节点；字段缺失时让测试失败。
     */
    public static JsonNode requiredObject(final JsonNode node, final String field) {
        final JsonNode value = node.path(field);
        assertTrue(value.isObject(), "missing object field: " + field);
        return value;
    }

    private static GoldenCase readCase(final Path path) {
        try {
            return new GoldenCase(path.getFileName().toString(), MAPPER.readTree(path.toFile()));
        } catch (final IOException error) {
            throw new AssertionError("failed to parse golden case: " + path, error);
        }
    }

    /**
     * 单个 K7 golden case。
     *
     * @param filename 文件名。
     * @param root fixture root JSON。
     */
    public record GoldenCase(String filename, JsonNode root) {

        /**
         * 返回 caseId。
         */
        public String caseId() {
            return requiredText(root, "caseId");
        }

        /**
         * 返回 input 节点。
         */
        public JsonNode input() {
            return requiredObject(root, "input");
        }

        /**
         * 返回 expectedDecision 节点。
         */
        public JsonNode expectedDecision() {
            return requiredObject(root, "expectedDecision");
        }

        /**
         * 返回可选 expectedReplay 节点。
         */
        public JsonNode expectedReplay() {
            return root.path("expectedReplay");
        }
    }
}

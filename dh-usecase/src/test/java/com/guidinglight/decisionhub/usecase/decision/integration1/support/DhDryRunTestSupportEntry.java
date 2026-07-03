package com.guidinglight.decisionhub.usecase.decision.integration1.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import com.guidinglight.decisionhub.domain.decision.ForbiddenAction;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import com.guidinglight.decisionhub.usecase.decision.DecisionOrchestrator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.StreamSupport;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * IMP1 test-only dry-run entry harness。
 *
 * <p>该入口只在 DH 测试范围内模拟未来 dry-run entry 的 validation chain：它不注册 Spring bean、不暴露
 * Controller、不读取真实 secret、不访问 NQ、不发送 HTTP、不连接真实 provider。所有失败统一转换为结构化
 * fail-closed output，便于后续 IMP2/NQ stub recorder 复用同一安全语义。
 */
public final class DhDryRunTestSupportEntry {

    public static final String HEADER_SOURCE = "X-NQ-DH-Source";
    public static final String HEADER_TENANT_ID = "X-NQ-DH-Tenant-Id";
    public static final String HEADER_REQUEST_ID = "X-NQ-DH-Request-Id";
    public static final String HEADER_TRACE_ID = "X-NQ-DH-Trace-Id";
    public static final String HEADER_TIMESTAMP = "X-NQ-DH-Timestamp";
    public static final String HEADER_NONCE = "X-NQ-DH-Nonce";
    public static final String HEADER_SIGNATURE = "X-NQ-DH-Signature";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    public static final String TEST_SOURCE = "NQ_MOCK";
    public static final String REVIEW_GATED_SOURCE = "NQ_DRYRUN";
    public static final String TEST_SECRET = "imp1-test-only-secret";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_PAYLOAD_BYTES = 64 * 1024;
    private static final long TIMESTAMP_WINDOW_SECONDS = 300L;
    private static final Set<String> REQUIRED_HEADERS =
            Set.of(
                    HEADER_SOURCE,
                    HEADER_TENANT_ID,
                    HEADER_REQUEST_ID,
                    HEADER_TRACE_ID,
                    HEADER_TIMESTAMP,
                    HEADER_NONCE,
                    HEADER_SIGNATURE,
                    HEADER_CONTENT_TYPE);
    private static final Set<String> FORBIDDEN_TOKENS =
            Set.of(
                    "apikey",
                    "apisecret",
                    "secret",
                    "token",
                    "cookie",
                    "credential",
                    "passphrase",
                    "privatekey",
                    "account",
                    "accountid",
                    "order",
                    "orderid",
                    "quantity",
                    "leverage",
                    "placeorder",
                    "cancelorder",
                    "buy",
                    "sell");
    private static final Set<DecisionAction> READONLY_ACTIONS =
            Set.of(
                    DecisionAction.ABSTAIN,
                    DecisionAction.OBSERVE,
                    DecisionAction.NO_TRADE,
                    DecisionAction.LONG_BIAS,
                    DecisionAction.SHORT_BIAS);

    private final DecisionOrchestrator orchestrator;
    private final NonceStore nonceStore;
    private final Clock clock;

    /**
     * 创建 IMP1 test-only entry。
     *
     * @param orchestrator mock-only DecisionOrchestrator；测试可注入 provider guard 场景
     * @param nonceStore   内存 nonce store；用于 replay fail-closed
     * @param clock        固定测试时钟；避免 timestamp 测试依赖系统时间
     */
    public DhDryRunTestSupportEntry(
            final DecisionOrchestrator orchestrator, final NonceStore nonceStore, final Clock clock) {
        this.orchestrator = Objects.requireNonNull(orchestrator, "orchestrator");
        this.nonceStore = Objects.requireNonNull(nonceStore, "nonceStore");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 执行 test-support dry-run validation chain。
     *
     * @param exchange test-only exchange；body 与 headers 均由测试构造
     * @return 结构化 validation result；失败时也包含 fail-closed DecisionOutput
     */
    public Result validate(final Exchange exchange) {
        final List<Step> steps = new ArrayList<>();
        try {
            steps.add(Step.PAYLOAD_SIZE_GATE);
            if (exchange.body().getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
                return reject(exchange, steps, 413, "PAYLOAD_TOO_LARGE");
            }

            steps.add(Step.CANONICAL_HEADER_PRESENCE);
            for (String required : REQUIRED_HEADERS) {
                if (isBlank(exchange.headers().get(required))) {
                    return reject(exchange, steps, missingHeaderStatus(required), "MISSING_CANONICAL_HEADER");
                }
            }
            if (!"application/json".equalsIgnoreCase(exchange.headers().get(HEADER_CONTENT_TYPE))) {
                return reject(exchange, steps, 400, "BAD_CONTENT_TYPE");
            }

            steps.add(Step.REQUEST_TRACE_TENANT_BINDING);
            if (!matches(exchange, HEADER_TENANT_ID, "tenantId")
                    || !matches(exchange, HEADER_REQUEST_ID, "requestId")
                    || !matches(exchange, HEADER_TRACE_ID, "traceId")
                    || !matches(exchange, HEADER_SOURCE, "source")) {
                return reject(exchange, steps, 403, "HEADER_BINDING_MISMATCH");
            }

            steps.add(Step.SOURCE_ALLOWLIST_GUARD);
            if (!TEST_SOURCE.equals(exchange.headers().get(HEADER_SOURCE))) {
                return reject(exchange, steps, 403, "SOURCE_DENIED");
            }

            steps.add(Step.TIMESTAMP_UTC_Z_VALIDATION);
            if (!validUtcZTimestamp(exchange.headers().get(HEADER_TIMESTAMP))) {
                return reject(exchange, steps, 401, "TIMESTAMP_INVALID");
            }

            steps.add(Step.NONCE_REPLAY_FAIL_CLOSED);
            if (!nonceStore.registerIfAbsent(
                    exchange.headers().get(HEADER_SOURCE),
                    exchange.headers().get(HEADER_TENANT_ID),
                    exchange.headers().get(HEADER_REQUEST_ID),
                    exchange.headers().get(HEADER_NONCE))) {
                return reject(exchange, steps, 409, "NONCE_REPLAY");
            }

            steps.add(Step.HMAC_VALUE_BASED_SIGNATURE_VALIDATION);
            if (!constantTimeEquals(expectedSignature(exchange), exchange.headers().get(HEADER_SIGNATURE))) {
                return reject(exchange, steps, 401, "SIGNATURE_INVALID");
            }

            steps.add(Step.SCHEMA_CONTRACT_SHAPE_VALIDATION);
            final List<String> schemaErrors = schemaErrors(exchange.payload());
            if (!schemaErrors.isEmpty()) {
                return reject(exchange, steps, 400, "CONTRACT_INVALID");
            }

            steps.add(Step.FORBIDDEN_FIELDS_VALIDATION);
            final List<String> forbidden = forbiddenHits(exchange.payload());
            if (!forbidden.isEmpty()) {
                return reject(exchange, steps, 400, "FORBIDDEN_FIELD");
            }

            steps.add(Step.DECISION_ORCHESTRATOR_MOCK_ONLY_BOUNDARY);
            final DecisionOutput output;
            try {
                output = orchestrator.decide(toDecisionRequest(exchange.payload()));
            } catch (final RuntimeException error) {
                return reject(exchange, steps, 500, "INTERNAL_FAIL_CLOSED");
            }

            steps.add(Step.PROVIDER_GUARD_MOCK_ONLY_BOUNDARY);
            if (output.getProviderStatus() == ProviderSignalStatus.SUCCESS) {
                return reject(exchange, steps, 500, "REAL_PROVIDER_FORBIDDEN");
            }

            steps.add(Step.AUDIT_TRACE_REPLAY_SAFE_SUMMARY_BOUNDARY);
            final SafeSummary summary = safeSummary(exchange, output, null);
            if (containsUnsafeSummary(summary)) {
                return reject(exchange, steps, 500, "UNSAFE_SUMMARY");
            }

            steps.add(Step.STRUCTURED_DECISION_OUTPUT_ASSEMBLY_BOUNDARY);
            if (!structuredReadOnly(output)) {
                return reject(exchange, steps, 500, "STRUCTURED_OUTPUT_INVALID");
            }

            steps.add(Step.FAIL_CLOSED_RESPONSE_NORMALIZATION);
            return Result.accepted(List.copyOf(steps), output, summary);
        } catch (final RuntimeException error) {
            return reject(exchange, steps, 500, "INTERNAL_FAIL_CLOSED");
        }
    }

    /**
     * 构造已签名的 test-only exchange。
     *
     * @param source  header/source 值；`NQ_DRYRUN` 仍会被 allowlist guard 拒绝
     * @param payload payload JSON 节点
     * @param nonce   nonce 值
     * @param now     固定测试时间
     * @return 可直接提交给 entry 的 exchange
     */
    public static Exchange signedExchange(
            final String source, final JsonNode payload, final String nonce, final Instant now) {
        final String body = toJson(payload);
        final Map<String, String> headers = new LinkedHashMap<>();
        headers.put(HEADER_SOURCE, source);
        headers.put(HEADER_TENANT_ID, payload.path("tenantId").asText());
        headers.put(HEADER_REQUEST_ID, payload.path("requestId").asText());
        headers.put(HEADER_TRACE_ID, payload.path("traceId").asText());
        headers.put(HEADER_TIMESTAMP, now.toString());
        headers.put(HEADER_NONCE, nonce);
        headers.put(HEADER_CONTENT_TYPE, "application/json");
        headers.put(HEADER_SIGNATURE, sign(headers, body, TEST_SECRET));
        return new Exchange(headers, body, payload);
    }

    /**
     * 返回一份最小合法 mock-only dry-run payload。
     */
    public static JsonNode validPayload() {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", "imp1-dryrun-request-1");
        payload.put("traceId", "trace-imp1-dryrun-request-1");
        payload.put("tenantId", "tenant-imp1");
        payload.put("source", TEST_SOURCE);
        payload.put("decisionType", "READ_ONLY_RECOMMENDATION");
        payload.put(
                "subject",
                Map.of(
                        "symbol", "BTC-USDT",
                        "market", "CRYPTO_SPOT",
                        "timeframe", "1h",
                        "strategyRef", "imp1-readonly-strategy"));
        payload.put("contextRef", "mock-nq://imp1/dryrun/context");
        payload.put(
                "contextSnapshot",
                Map.of(
                        "snapshotId", "snapshot-imp1",
                        "capturedAt", "2026-07-03T00:00:00Z",
                        "evidenceRefs", List.of("evidence://imp1/market-regime")));
        payload.put("requestedAt", "2026-07-03T00:00:00Z");
        return MAPPER.valueToTree(payload);
    }

    /**
     * 使用 value-based signatureMaterial 生成 HMAC；header name 不进入签名材料。
     */
    public static String sign(final Map<String, String> headers, final String body, final String secret) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return hex(mac.doFinal(signatureMaterial(headers, body).getBytes(StandardCharsets.UTF_8)));
        } catch (final java.security.InvalidKeyException | NoSuchAlgorithmException error) {
            throw new IllegalStateException("test-only HMAC init failed", error);
        }
    }

    private Result reject(
            final Exchange exchange, final List<Step> steps, final int statusCode, final String errorCode) {
        if (!steps.contains(Step.FAIL_CLOSED_RESPONSE_NORMALIZATION)) {
            steps.add(Step.FAIL_CLOSED_RESPONSE_NORMALIZATION);
        }
        final DecisionOutput output =
                DecisionOutput.abstainForRisk(
                        safeHeader(exchange, HEADER_REQUEST_ID, "unknown-request"),
                        safeHeader(exchange, HEADER_TRACE_ID, "unknown-trace"),
                        safeHeader(exchange, HEADER_TENANT_ID, "unknown-tenant"),
                        DecisionRiskLevel.UNKNOWN,
                        List.of(errorCode),
                        List.of(),
                        clock.instant());
        return Result.rejected(statusCode, errorCode, List.copyOf(steps), output, safeSummary(exchange, output, errorCode));
    }

    private static boolean matches(final Exchange exchange, final String header, final String field) {
        return exchange.headers().get(header).equals(exchange.payload().path(field).asText());
    }

    private boolean validUtcZTimestamp(final String timestamp) {
        if (isBlank(timestamp) || !timestamp.endsWith("Z")) {
            return false;
        }
        try {
            final Instant parsed = Instant.parse(timestamp);
            return Math.abs(clock.instant().getEpochSecond() - parsed.getEpochSecond())
                    <= TIMESTAMP_WINDOW_SECONDS;
        } catch (final RuntimeException ignored) {
            return false;
        }
    }

    private static String expectedSignature(final Exchange exchange) {
        return sign(exchange.headers(), exchange.body(), TEST_SECRET);
    }

    private static String signatureMaterial(final Map<String, String> headers, final String body) {
        return String.join(
                "\n",
                headers.getOrDefault(HEADER_SOURCE, ""),
                headers.getOrDefault(HEADER_TENANT_ID, ""),
                headers.getOrDefault(HEADER_REQUEST_ID, ""),
                headers.getOrDefault(HEADER_TRACE_ID, ""),
                headers.getOrDefault(HEADER_TIMESTAMP, ""),
                headers.getOrDefault(HEADER_NONCE, ""),
                sha256Hex(body));
    }

    private static List<String> schemaErrors(final JsonNode payload) {
        final List<String> errors = new ArrayList<>();
        for (String field : List.of("requestId", "traceId", "tenantId", "source", "decisionType")) {
            if (payload.path(field).isMissingNode() || payload.path(field).asText("").isBlank()) {
                errors.add("missing " + field);
            }
        }
        if (!"READ_ONLY_RECOMMENDATION".equals(payload.path("decisionType").asText())) {
            errors.add("decisionType must be READ_ONLY_RECOMMENDATION");
        }
        final JsonNode subject = payload.path("subject");
        if (!subject.isObject()) {
            errors.add("subject must be object");
        }
        for (String field : List.of("symbol", "market", "timeframe", "strategyRef")) {
            if (subject.path(field).isMissingNode() || subject.path(field).asText("").isBlank()) {
                errors.add("missing subject." + field);
            }
        }
        final JsonNode contextSnapshot = payload.path("contextSnapshot");
        if (!contextSnapshot.isObject()) {
            errors.add("contextSnapshot must be object");
        }
        final JsonNode evidenceRefs = contextSnapshot.path("evidenceRefs");
        if (!evidenceRefs.isArray() || evidenceRefs.isEmpty()) {
            errors.add("contextSnapshot.evidenceRefs must not be empty");
        }
        return errors;
    }

    private static List<String> forbiddenHits(final JsonNode node) {
        final List<String> hits = new ArrayList<>();
        scanForbidden(node, "$", hits);
        return hits;
    }

    private static void scanForbidden(final JsonNode node, final String path, final List<String> hits) {
        if (node == null || node.isMissingNode()) {
            return;
        }
        if (node.isObject()) {
            final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> entry = fields.next();
                if (forbiddenToken(entry.getKey())) {
                    hits.add(path + "." + entry.getKey());
                }
                scanForbidden(entry.getValue(), path + "." + entry.getKey(), hits);
            }
            return;
        }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                scanForbidden(node.get(i), path + "[" + i + "]", hits);
            }
            return;
        }
        if (node.isTextual() && forbiddenToken(node.asText())) {
            hits.add(path + " (value)");
        }
    }

    private static boolean forbiddenToken(final String value) {
        final String normalized = normalize(value);
        return FORBIDDEN_TOKENS.stream().anyMatch(normalized::contains);
    }

    private static DecisionRequest toDecisionRequest(final JsonNode payload) {
        final JsonNode subject = payload.path("subject");
        final JsonNode context = payload.path("contextSnapshot");
        final List<String> evidenceRefs =
                StreamSupport.stream(context.path("evidenceRefs").spliterator(), false)
                        .map(JsonNode::asText)
                        .toList();
        return DecisionRequest.readOnlyRecommendation(
                payload.path("requestId").asText(),
                payload.path("traceId").asText(),
                payload.path("tenantId").asText(),
                payload.path("source").asText(),
                new DecisionSubject(
                        subject.path("symbol").asText(),
                        subject.path("market").asText(),
                        subject.path("timeframe").asText(),
                        subject.path("strategyRef").asText(),
                        subject.path("researchRef").isMissingNode()
                                ? null
                                : subject.path("researchRef").asText()),
                payload.path("contextRef").asText("mock-nq://imp1/dryrun/context"),
                new DecisionContextSnapshot(
                        context.path("snapshotId").asText(),
                        Instant.parse(context.path("capturedAt").asText()),
                        evidenceRefs),
                Instant.parse(payload.path("requestedAt").asText()));
    }

    private static SafeSummary safeSummary(
            final Exchange exchange, final DecisionOutput output, final String errorCode) {
        return new SafeSummary(
                safeHeader(exchange, HEADER_REQUEST_ID, output.getRequestId()),
                safeHeader(exchange, HEADER_TRACE_ID, output.getTraceId()),
                safeHeader(exchange, HEADER_TENANT_ID, output.getTenantId()),
                output.getAction().name(),
                output.getProviderStatus().name(),
                errorCode == null ? "OK" : errorCode,
                output.getReasonCodes());
    }

    private static boolean structuredReadOnly(final DecisionOutput output) {
        return output.getDecisionType() == DecisionType.READ_ONLY_RECOMMENDATION
                && READONLY_ACTIONS.contains(output.getAction())
                && output.getForbiddenActions().containsAll(ForbiddenAction.mandatorySet())
                && Set.of(
                        DecisionStatus.ABSTAINED,
                        DecisionStatus.OBSERVATION_ONLY,
                        DecisionStatus.DIRECTIONAL_BIAS,
                        DecisionStatus.BLOCKED,
                        DecisionStatus.INVALID)
                .contains(output.getStatus())
                && Set.of(
                        DecisionPolicyStatus.ALLOWED,
                        DecisionPolicyStatus.DENIED,
                        DecisionPolicyStatus.REVIEW_REQUIRED,
                        DecisionPolicyStatus.INVALID,
                        DecisionPolicyStatus.BLOCKED)
                .contains(output.getPolicyStatus());
    }

    private static boolean containsUnsafeSummary(final SafeSummary summary) {
        final String text = summary.conciseText().toLowerCase(Locale.ROOT);
        return text.contains("secret")
                || text.contains("credential")
                || text.contains("signature")
                || text.contains("apikey")
                || text.contains("api_key")
                || text.contains("passphrase")
                || text.contains("raw");
    }

    private static int missingHeaderStatus(final String header) {
        if (HEADER_SIGNATURE.equals(header) || HEADER_TIMESTAMP.equals(header) || HEADER_NONCE.equals(header)) {
            return 401;
        }
        if (HEADER_SOURCE.equals(header) || HEADER_TENANT_ID.equals(header)) {
            return 403;
        }
        return 400;
    }

    private static String safeHeader(final Exchange exchange, final String header, final String fallback) {
        final String value = exchange.headers().get(header);
        return isBlank(value) ? fallback : value;
    }

    private static boolean constantTimeEquals(final String expected, final String actual) {
        if (actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(final String value) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return hex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
    }

    private static String hex(final byte[] bytes) {
        final StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >> 4) & 0xF, 16));
            builder.append(Character.forDigit(value & 0xF, 16));
        }
        return builder.toString();
    }

    private static String toJson(final JsonNode payload) {
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (final JsonProcessingException error) {
            throw new IllegalStateException("test-only payload serialization failed", error);
        }
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private static String normalize(final String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /**
     * validation chain 的显式步骤，用于测试校验顺序。
     */
    public enum Step {
        PAYLOAD_SIZE_GATE,
        CANONICAL_HEADER_PRESENCE,
        REQUEST_TRACE_TENANT_BINDING,
        SOURCE_ALLOWLIST_GUARD,
        TIMESTAMP_UTC_Z_VALIDATION,
        NONCE_REPLAY_FAIL_CLOSED,
        HMAC_VALUE_BASED_SIGNATURE_VALIDATION,
        SCHEMA_CONTRACT_SHAPE_VALIDATION,
        FORBIDDEN_FIELDS_VALIDATION,
        DECISION_ORCHESTRATOR_MOCK_ONLY_BOUNDARY,
        PROVIDER_GUARD_MOCK_ONLY_BOUNDARY,
        AUDIT_TRACE_REPLAY_SAFE_SUMMARY_BOUNDARY,
        STRUCTURED_DECISION_OUTPUT_ASSEMBLY_BOUNDARY,
        FAIL_CLOSED_RESPONSE_NORMALIZATION
    }

    /**
     * test-only exchange，不代表生产 API envelope。
     */
    public record Exchange(Map<String, String> headers, String body, JsonNode payload) {
        public Exchange {
            headers = Map.copyOf(Objects.requireNonNull(headers, "headers"));
            body = Objects.requireNonNull(body, "body");
            payload = Objects.requireNonNull(payload, "payload");
        }
    }

    /**
     * validation result；拒绝结果也必须带结构化 fail-closed output。
     */
    public record Result(
            boolean accepted,
            int statusCode,
            String errorCode,
            List<Step> steps,
            DecisionOutput output,
            SafeSummary safeSummary) {
        public static Result accepted(
                final List<Step> steps, final DecisionOutput output, final SafeSummary safeSummary) {
            return new Result(true, 200, "OK", steps, output, safeSummary);
        }

        public static Result rejected(
                final int statusCode,
                final String errorCode,
                final List<Step> steps,
                final DecisionOutput output,
                final SafeSummary safeSummary) {
            return new Result(false, statusCode, errorCode, steps, output, safeSummary);
        }
    }

    /**
     * audit / trace / replay 只允许保存的安全摘要，不包含 body、signature 或 secret。
     */
    public record SafeSummary(
            String requestId,
            String traceId,
            String tenantId,
            String action,
            String providerStatus,
            String errorCode,
            List<String> reasonCodes) {
        public SafeSummary {
            reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
        }

        public String conciseText() {
            return String.join(
                    "|",
                    requestId,
                    traceId,
                    tenantId,
                    action,
                    providerStatus,
                    errorCode,
                    String.join(",", reasonCodes));
        }
    }

    /**
     * test-only nonce store；实现只保证单进程内 replay 检测。
     */
    public interface NonceStore {
        boolean registerIfAbsent(String source, String tenantId, String requestId, String nonce);
    }

    /**
     * 纯内存 nonce store，不做持久化，不代表生产 replay guard。
     */
    public static final class InMemoryNonceStore implements NonceStore {
        private final Set<String> seen = new LinkedHashSet<>();

        @Override
        public boolean registerIfAbsent(
                final String source, final String tenantId, final String requestId, final String nonce) {
            return seen.add(String.join("|", source, tenantId, requestId, nonce));
        }
    }
}

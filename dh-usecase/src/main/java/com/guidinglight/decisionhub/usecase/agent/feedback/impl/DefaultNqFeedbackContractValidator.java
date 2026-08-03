package com.guidinglight.decisionhub.usecase.agent.feedback.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunRepository;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionCommand;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionErrorCode;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackContractValidator;
import com.guidinglight.decisionhub.usecase.agent.feedback.ValidationResult;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Stage2-PoC-B2：默认 envelope 契约校验。
 *
 * <p>校验顺序与 docs/current/STAGE2_POC_WORK_ORDER.md §Batch 2.4 一致：
 *
 * <ol>
 *   <li>必填字段非空（command 已在 {@link IngestionCommand} 构造时强制非空，本层只做空串/格式补充）。
 *   <li>{@code sourceSystem} 必须等于 {@code "nexus-quant"}。
 *   <li>{@code rawEventType} 映射到 {@code NqFeedbackEventType} 枚举；失败 -> UNKNOWN_EVENT_TYPE。
 *   <li>{@code schemaVersion} >= "1.0.0"；失败 -> INVALID_SCHEMA。
 *   <li>{@code traceId} 在 {@link ResearchRunRepository#find(String)} 命中；不命中 -> UNKNOWN_TRACE。
 *   <li>递归扫描 Integration-0 冻结的 forbidden-field / forbidden-capability；命中即拒绝且不入库。
 *   <li>按 eventType 校验 {@code payloadJson} 必填字段；缺失 -> INVALID_SCHEMA。
 * </ol>
 */
public final class DefaultNqFeedbackContractValidator implements NqFeedbackContractValidator {

  private static final String EXPECTED_SOURCE_SYSTEM = NqFeedbackEnvelope.SOURCE_SYSTEM_NEXUS_QUANT;
  private static final String MIN_SCHEMA_VERSION = "1.0.0";

  /** Integration-0 frozen contract 的敏感字段黑名单；归一化后精确匹配字段名。 */
  private static final List<String> FORBIDDEN_FIELDS =
      List.of(
          "apiKey",
          "apiSecret",
          "secret",
          "token",
          "cookie",
          "passphrase",
          "privateKey",
          "mnemonic",
          "walletPrivateKey",
          "exchangeCredential",
          "accountCredential",
          "rawRequest",
          "rawResponse",
          "fullPrompt",
          "fullContext",
          "rawPrompt",
          "rawContext",
          "signatureRawMaterial",
          "authorization",
          "databaseConnectionString",
          "password",
          "twoFactorSecret",
          "recoveryCode");

  /** Integration-0 frozen contract 的禁止能力黑名单；归一化后精确匹配字段名或字符串值。 */
  private static final List<String> FORBIDDEN_CAPABILITIES =
      List.of(
          "placeOrder",
          "cancelOrder",
          "mutateOrderStatus",
          "mutateStrategyStatus",
          "startPaperRun",
          "stopPaperRun",
          "mutateRiskState",
          "readCredential",
          "writeNqDb",
          "triggerLive",
          "agentDirectTrade",
          "feedbackDirectExecute",
          "orderCommand",
          "liveExecution");

  private static final Map<NqFeedbackEventType, List<String>> REQUIRED_PAYLOAD_FIELDS =
      Map.of(
          NqFeedbackEventType.PAPER_RUN_CREATED,
              List.of(
                  "paperRunId", "candidateId", "strategyName", "requestedBy", "createdAt",
                  "rawPayloadJson"),
          NqFeedbackEventType.PAPER_RUN_STARTED,
              List.of("paperRunId", "startedAt", "mode", "rawPayloadJson"),
          NqFeedbackEventType.PAPER_RUN_STOPPED,
              List.of("paperRunId", "stoppedAt", "reason", "rawPayloadJson"),
          NqFeedbackEventType.PAPER_RUN_DAILY_REPORT_GENERATED,
              List.of("paperRunId", "reportId", "reportDate", "rawPayloadJson"),
          NqFeedbackEventType.PAPER_RUN_ALERT_RAISED,
              List.of(
                  "paperRunId", "alertId", "alertLevel", "alertCode", "message", "raisedAt",
                  "rawPayloadJson"),
          NqFeedbackEventType.PAPER_RUN_RECOVERY_EVENT_RECORDED,
              List.of(
                  "paperRunId", "recoveryEventId", "recoveryReason", "recoveredAt",
                  "rawPayloadJson"),
          NqFeedbackEventType.PAPER_RUN_STABILITY_CHECK_COMPLETED,
              List.of(
                  "paperRunId", "checkId", "result", "summary", "completedAt", "rawPayloadJson"),
          NqFeedbackEventType.BACKTEST_RESULT_READY,
              List.of(
                  "backtestId", "requestId", "candidateId", "periodStart", "periodEnd", "verdict",
                  "readyAt", "rawPayloadJson"));

  private final ResearchRunRepository researchRunRepository;
  private final ObjectMapper objectMapper;

  public DefaultNqFeedbackContractValidator(
      final ResearchRunRepository researchRunRepository, final ObjectMapper objectMapper) {
    this.researchRunRepository = researchRunRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public ValidationResult validate(final IngestionCommand command) {
    if (isBlank(command.getEventId())
        || isBlank(command.getRawEventType())
        || isBlank(command.getSourceSystem())
        || isBlank(command.getSourceJobId())
        || isBlank(command.getTraceId())
        || isBlank(command.getRequestId())
        || isBlank(command.getCorrelationId())
        || isBlank(command.getSchemaVersion())
        || command.getPayloadJson() == null) {
      return ValidationResult.fail(
          IngestionErrorCode.INVALID_SCHEMA, "envelope required field is blank");
    }
    if (!EXPECTED_SOURCE_SYSTEM.equals(command.getSourceSystem())) {
      return ValidationResult.fail(
          IngestionErrorCode.INVALID_SCHEMA,
          "sourceSystem must be '" + EXPECTED_SOURCE_SYSTEM + "'");
    }

    final NqFeedbackEventType eventType;
    try {
      eventType = NqFeedbackEventType.valueOf(command.getRawEventType());
    } catch (IllegalArgumentException ex) {
      return ValidationResult.fail(
          IngestionErrorCode.UNKNOWN_EVENT_TYPE,
          "unknown eventType: " + command.getRawEventType());
    }

    if (compareSemver(command.getSchemaVersion(), MIN_SCHEMA_VERSION) < 0) {
      return ValidationResult.fail(
          IngestionErrorCode.INVALID_SCHEMA,
          "schemaVersion must be >= " + MIN_SCHEMA_VERSION
              + " (was: " + command.getSchemaVersion() + ")");
    }

    if (researchRunRepository.find(command.getTraceId()).isEmpty()) {
      return ValidationResult.fail(
          IngestionErrorCode.UNKNOWN_TRACE,
          "traceId not found in ResearchRunRepository: " + command.getTraceId());
    }

    final JsonNode payload = parsePayload(command.getPayloadJson());
    if (payload == null) {
      return ValidationResult.fail(
          IngestionErrorCode.INVALID_SCHEMA, "payload missing required field for " + eventType + ": <root>");
    }

    final List<String> forbiddenFields = scanForbiddenFields(payload);
    if (!forbiddenFields.isEmpty()) {
      return ValidationResult.fail(
          IngestionErrorCode.FORBIDDEN_FIELD,
          "payload contains forbidden field: " + forbiddenFields.get(0));
    }

    final List<String> forbiddenCapabilities = scanForbiddenCapabilities(payload);
    if (!forbiddenCapabilities.isEmpty()) {
      return ValidationResult.fail(
          IngestionErrorCode.FORBIDDEN_CAPABILITY,
          "payload contains forbidden capability: " + forbiddenCapabilities.get(0));
    }

    final String missingField = checkPayloadFields(eventType, payload);
    if (missingField != null) {
      return ValidationResult.fail(
          IngestionErrorCode.INVALID_SCHEMA,
          "payload missing required field for " + eventType + ": " + missingField);
    }

    final NqFeedbackEnvelope envelope =
        NqFeedbackEnvelope.of(
            command.getEventId(),
            eventType,
            command.getOccurredAt(),
            command.getSourceSystem(),
            command.getSourceJobId(),
            command.getTraceId(),
            command.getRequestId(),
            command.getCorrelationId(),
            command.getSchemaVersion(),
            command.getPayloadJson(),
            command.getReceivedAt());
    return ValidationResult.ok(envelope);
  }

  /**
   * 返回缺失的字段名；全部命中返回 {@code null}。
   *
   * <p>解析失败（非合法 JSON 或非对象）返回 {@code "<root>"} 作为统一错误指示。
   */
  private String checkPayloadFields(final NqFeedbackEventType eventType, final JsonNode node) {
    final List<String> required = REQUIRED_PAYLOAD_FIELDS.get(eventType);
    if (required == null) {
      return null;
    }
    if (node == null || !node.isObject()) {
      return "<root>";
    }
    for (String field : required) {
      final JsonNode v = node.get(field);
      if (v == null || v.isNull()) {
        return field;
      }
      if (v.isTextual() && v.asText().isBlank()) {
        return field;
      }
    }
    return null;
  }

  /** 解析唯一 payload 根对象；失败、非对象、重复字段或尾随 token 返回 {@code null}。 */
  private JsonNode parsePayload(final String payloadJson) {
    return parseStrictSingleRootObject(objectMapper, payloadJson);
  }

  /**
   * 解析完整 feedback payload，并要求输入只含一个 JSON object root。
   *
   * <p>Why：{@link ObjectMapper#readTree(String)} 默认允许首个 root 后仍有 token。这里在 parser 层启用重复字段检测，
   * 读取首个 tree 后再显式推进到 EOF，确保 validator 与 ingestion canonical comparison 对同一完整输入给出一致结论。
   */
  static JsonNode parseStrictSingleRootObject(
      final ObjectMapper mapper, final String payloadJson) {
    try {
      final ObjectMapper checkedMapper = java.util.Objects.requireNonNull(mapper, "mapper");
      try (JsonParser parser = checkedMapper.createParser(payloadJson)) {
        parser.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        final JsonNode node = checkedMapper.readTree(parser);
        if (node == null || !node.isObject()) {
          return null;
        }
        return parser.nextToken() == null ? node : null;
      }
    } catch (Exception ex) {
      return null;
    }
  }

  /**
   * 递归扫描 forbidden fields，返回字段路径而不返回字段值。
   *
   * <p>{@code rawPayloadJson} 是当前 NQ feedback contract 的必填字符串；若其内容本身是 JSON，则继续按 JSON
   * 解析并扫描字段名，避免把含 credential/secret 字段的原始 payload 字符串落库。
   */
  private List<String> scanForbiddenFields(final JsonNode node) {
    final List<String> hits = new ArrayList<>();
    scanFieldNames(node, "$", hits);
    return hits;
  }

  /** 递归扫描 forbidden capabilities；字段名和字符串值都参与匹配。 */
  private List<String> scanForbiddenCapabilities(final JsonNode node) {
    final List<String> hits = new ArrayList<>();
    scanCapabilities(node, "$", hits);
    return hits;
  }

  private void scanFieldNames(final JsonNode node, final String path, final List<String> hits) {
    if (node == null) {
      return;
    }
    if (node.isObject()) {
      final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        final Map.Entry<String, JsonNode> entry = fields.next();
        if (matchesToken(entry.getKey(), FORBIDDEN_FIELDS)) {
          hits.add(path + "." + entry.getKey());
        }
        scanFieldNames(entry.getValue(), path + "." + entry.getKey(), hits);
      }
      return;
    }
    if (node.isArray()) {
      for (int i = 0; i < node.size(); i++) {
        scanFieldNames(node.get(i), path + "[" + i + "]", hits);
      }
      return;
    }
    if (node.isTextual()) {
      parseEmbeddedJson(node.asText()).ifPresent(embedded -> scanFieldNames(embedded, path + "(json)", hits));
    }
  }

  private void scanCapabilities(final JsonNode node, final String path, final List<String> hits) {
    if (node == null) {
      return;
    }
    if (node.isObject()) {
      final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        final Map.Entry<String, JsonNode> entry = fields.next();
        matchCapabilityToken(entry.getKey(), path + "." + entry.getKey() + " (field)", hits);
        scanCapabilities(entry.getValue(), path + "." + entry.getKey(), hits);
      }
      return;
    }
    if (node.isArray()) {
      for (int i = 0; i < node.size(); i++) {
        scanCapabilities(node.get(i), path + "[" + i + "]", hits);
      }
      return;
    }
    if (node.isTextual()) {
      matchCapabilityToken(node.asText(), path + " (value)", hits);
      parseEmbeddedJson(node.asText()).ifPresent(embedded -> scanCapabilities(embedded, path + "(json)", hits));
    }
  }

  private void matchCapabilityToken(
      final String candidate, final String where, final List<String> hits) {
    for (String capability : FORBIDDEN_CAPABILITIES) {
      if (normalize(candidate).equals(normalize(capability))) {
        hits.add(where + " -> " + capability);
      }
    }
  }

  private java.util.Optional<JsonNode> parseEmbeddedJson(final String raw) {
    if (raw == null) {
      return java.util.Optional.empty();
    }
    final String trimmed = raw.trim();
    if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
      return java.util.Optional.empty();
    }
    try {
      return java.util.Optional.of(objectMapper.readTree(trimmed));
    } catch (Exception ex) {
      return java.util.Optional.empty();
    }
  }

  private static boolean matchesToken(final String candidate, final List<String> tokens) {
    final String normalized = normalize(candidate);
    return tokens.stream().anyMatch(token -> normalized.equals(normalize(token)));
  }

  /** 与 INT0 test-only validator 保持一致：大小写无关，忽略下划线/连字符等非字母数字分隔符。 */
  private static String normalize(final String token) {
    return token == null ? "" : token.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
  }

  /** 简单 semver 比较：x.y.z 三段，每段数字。非法版本视为 -1。 */
  private static int compareSemver(final String a, final String b) {
    final int[] av = parseSemver(a);
    final int[] bv = parseSemver(b);
    if (av == null) {
      return -1;
    }
    if (bv == null) {
      return 1;
    }
    for (int i = 0; i < 3; i++) {
      if (av[i] != bv[i]) {
        return Integer.compare(av[i], bv[i]);
      }
    }
    return 0;
  }

  private static int[] parseSemver(final String v) {
    if (v == null) {
      return null;
    }
    final String[] parts = v.split("\\.");
    if (parts.length != 3) {
      return null;
    }
    final int[] out = new int[3];
    for (int i = 0; i < 3; i++) {
      try {
        out[i] = Integer.parseInt(parts[i]);
      } catch (NumberFormatException ex) {
        return null;
      }
      if (out[i] < 0) {
        return null;
      }
    }
    return out;
  }

  private static boolean isBlank(final String s) {
    return s == null || s.isBlank();
  }
}

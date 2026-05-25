package com.guidinglight.decisionhub.usecase.agent.feedback.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunRepository;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionCommand;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionErrorCode;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackContractValidator;
import com.guidinglight.decisionhub.usecase.agent.feedback.ValidationResult;
import java.util.List;
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
 *   <li>按 eventType 校验 {@code payloadJson} 必填字段；缺失 -> INVALID_SCHEMA。
 * </ol>
 */
public final class DefaultNqFeedbackContractValidator implements NqFeedbackContractValidator {

  private static final String EXPECTED_SOURCE_SYSTEM = NqFeedbackEnvelope.SOURCE_SYSTEM_NEXUS_QUANT;
  private static final String MIN_SCHEMA_VERSION = "1.0.0";

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

    final String missingField = checkPayloadFields(eventType, command.getPayloadJson());
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
  private String checkPayloadFields(final NqFeedbackEventType eventType, final String payloadJson) {
    final List<String> required = REQUIRED_PAYLOAD_FIELDS.get(eventType);
    if (required == null) {
      return null;
    }
    final JsonNode node;
    try {
      node = objectMapper.readTree(payloadJson);
    } catch (Exception ex) {
      return "<root>";
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

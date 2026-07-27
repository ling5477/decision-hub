package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * K3 持久化 port 使用的内部记录模型。
 *
 * <p>这些 record 只在 usecase port 与 infra adapter 之间传递脱敏后的审计数据；不暴露数据库 Entity，不包含
 * credential、token、secret、raw provider response 或 NQ DB 内容。
 */
public final class DecisionPersistenceRecords {

  private DecisionPersistenceRecords() {}

  /** dh_decision_request 写入记录。 */
  public record RequestRecord(
      String decisionId,
      String requestId,
      String traceId,
      String tenantId,
      String source,
      DecisionType decisionType,
      Map<String, Object> subjectJson,
      String contextRef,
      Instant requestedAt,
      String schemaVersion,
      Instant createdAt) {

    /** 校验核心追踪字段，避免不可审计记录落库。 */
    public RequestRecord {
      decisionId = requireText(decisionId, "decisionId");
      requestId = requireText(requestId, "requestId");
      traceId = requireText(traceId, "traceId");
      tenantId = requireText(tenantId, "tenantId");
      source = requireText(source, "source");
      decisionType = Objects.requireNonNull(decisionType, "decisionType");
      subjectJson = subjectJson == null ? Map.of() : Map.copyOf(subjectJson);
      requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
      schemaVersion = requireText(schemaVersion, "schemaVersion");
      createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }
  }

  /** dh_decision_context_snapshot 写入记录。 */
  public record ContextSnapshotRecord(
      String decisionId,
      String tenantId,
      String traceId,
      Map<String, Object> contextSnapshotJson,
      List<String> evidenceRefsJson,
      Instant createdAt) {

    /** 校验 snapshot 归属字段；snapshot 内容允许为空 JSON，但归属不可为空。 */
    public ContextSnapshotRecord {
      decisionId = requireText(decisionId, "decisionId");
      tenantId = requireText(tenantId, "tenantId");
      traceId = requireText(traceId, "traceId");
      contextSnapshotJson =
          contextSnapshotJson == null ? Map.of() : Map.copyOf(contextSnapshotJson);
      evidenceRefsJson = evidenceRefsJson == null ? List.of() : List.copyOf(evidenceRefsJson);
      createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }
  }

  /** dh_decision_trace_step 写入记录。 */
  public record TraceStepRecord(
      String id,
      String decisionId,
      String tenantId,
      String traceId,
      DecisionTraceStepName stepName,
      DecisionTraceStepStatus stepStatus,
      Instant startedAt,
      Instant endedAt,
      String errorCode,
      String errorMessage,
      Instant createdAt) {

    /** 校验 trace step 的链路归属和时间。 */
    public TraceStepRecord {
      id = requireText(id, "id");
      decisionId = requireText(decisionId, "decisionId");
      tenantId = requireText(tenantId, "tenantId");
      traceId = requireText(traceId, "traceId");
      stepName = Objects.requireNonNull(stepName, "stepName");
      stepStatus = Objects.requireNonNull(stepStatus, "stepStatus");
      startedAt = Objects.requireNonNull(startedAt, "startedAt");
      createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }
  }

  /** dh_decision_provider_call_log 写入记录。 */
  public record ProviderCallRecord(
      String id,
      String decisionId,
      String tenantId,
      String traceId,
      String providerName,
      ProviderSignalStatus providerStatus,
      long latencyMs,
      Map<String, Object> signalJson,
      String errorCode,
      Instant createdAt) {

    /** 校验 provider call summary；providerName 只能是 mock/disabled 等安全名称。 */
    public ProviderCallRecord {
      id = requireText(id, "id");
      decisionId = requireText(decisionId, "decisionId");
      tenantId = requireText(tenantId, "tenantId");
      traceId = requireText(traceId, "traceId");
      providerName = requireText(providerName, "providerName");
      providerStatus = Objects.requireNonNull(providerStatus, "providerStatus");
      if (latencyMs < 0) {
        throw new IllegalArgumentException("latencyMs must not be negative");
      }
      signalJson = signalJson == null ? Map.of() : Map.copyOf(signalJson);
      createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }
  }

  /** dh_decision_output 写入记录。 */
  public record OutputRecord(
      String decisionId,
      String tenantId,
      String traceId,
      String requestId,
      DecisionType decisionType,
      DecisionAction action,
      DecisionRiskLevel riskLevel,
      DecisionPolicyStatus policyStatus,
      BigDecimal confidence,
      Map<String, Object> outputJson,
      Instant createdAt) {

    /** 校验 output 归属和只读合同字段。 */
    public OutputRecord {
      decisionId = requireText(decisionId, "decisionId");
      tenantId = requireText(tenantId, "tenantId");
      traceId = requireText(traceId, "traceId");
      requestId = requireText(requestId, "requestId");
      decisionType = Objects.requireNonNull(decisionType, "decisionType");
      action = Objects.requireNonNull(action, "action");
      riskLevel = Objects.requireNonNull(riskLevel, "riskLevel");
      policyStatus = Objects.requireNonNull(policyStatus, "policyStatus");
      confidence = Objects.requireNonNull(confidence, "confidence");
      outputJson = Map.copyOf(Objects.requireNonNull(outputJson, "outputJson"));
      createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }
  }

  /** dh_decision_audit_event 写入记录。 */
  public record AuditEventRecord(
      String id,
      String decisionId,
      String tenantId,
      String traceId,
      FeedbackEnvironment environment,
      DecisionAuditEventType eventType,
      DecisionAuditEventStatus eventStatus,
      Map<String, Object> eventJson,
      String errorCode,
      Instant createdAt) {

    /** 校验 audit event 归属字段。 */
    public AuditEventRecord {
      id = requireText(id, "id");
      decisionId = requireText(decisionId, "decisionId");
      tenantId = requireText(tenantId, "tenantId");
      traceId = requireText(traceId, "traceId");
      eventType = Objects.requireNonNull(eventType, "eventType");
      eventStatus = Objects.requireNonNull(eventStatus, "eventStatus");
      eventJson = withEnvironment(eventJson, environment);
      createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Preserves non-feedback audit callers without manufacturing an environment. */
    public AuditEventRecord(
        final String id,
        final String decisionId,
        final String tenantId,
        final String traceId,
        final DecisionAuditEventType eventType,
        final DecisionAuditEventStatus eventStatus,
        final Map<String, Object> eventJson,
        final String errorCode,
        final Instant createdAt) {
      this(
          id,
          decisionId,
          tenantId,
          traceId,
          null,
          eventType,
          eventStatus,
          eventJson,
          errorCode,
          createdAt);
    }
  }

  private static Map<String, Object> withEnvironment(
      final Map<String, Object> eventJson, final FeedbackEnvironment environment) {
    final Map<String, Object> payload = new java.util.LinkedHashMap<>();
    if (eventJson != null) {
      payload.putAll(eventJson);
    }
    if (environment != null) {
      payload.put("environment", environment.name());
    }
    return Map.copyOf(payload);
  }

  private static String requireText(final String value, final String field) {
    final String checked = Objects.requireNonNull(value, field).trim();
    if (checked.isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return checked;
  }
}

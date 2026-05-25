package com.guidinglight.decisionhub.usecase.agent.feedback;

import java.time.Instant;
import java.util.Objects;

/**
 * Stage2-PoC-B2：NQ feedback ingestion 入参命令。
 *
 * <p>承载从 HTTP 边界进入用例层时所需的 envelope 全部字段。 {@code rawEventType} 是 HTTP 边界仍未映射的原始字符串；服务在 §Batch 2.4 第 1
 * 步将其映射到 {@code NqFeedbackEventType} 枚举，映射失败则返回 {@link IngestionErrorCode#UNKNOWN_EVENT_TYPE}。
 *
 * <p>所有跨服务 ID（traceId / requestId / correlationId / sourceJobId）必须独立传入， 不允许在用例层互相替换。
 */
public final class IngestionCommand {

  private final String tenantId;
  private final String eventId;
  private final String rawEventType;
  private final Instant occurredAt;
  private final String sourceSystem;
  private final String sourceJobId;
  private final String traceId;
  private final String requestId;
  private final String correlationId;
  private final String schemaVersion;
  private final String payloadJson;
  private final Instant receivedAt;

  private IngestionCommand(
      final String tenantId,
      final String eventId,
      final String rawEventType,
      final Instant occurredAt,
      final String sourceSystem,
      final String sourceJobId,
      final String traceId,
      final String requestId,
      final String correlationId,
      final String schemaVersion,
      final String payloadJson,
      final Instant receivedAt) {
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    this.eventId = Objects.requireNonNull(eventId, "eventId");
    this.rawEventType = Objects.requireNonNull(rawEventType, "rawEventType");
    this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
    this.sourceSystem = Objects.requireNonNull(sourceSystem, "sourceSystem");
    this.sourceJobId = Objects.requireNonNull(sourceJobId, "sourceJobId");
    this.traceId = Objects.requireNonNull(traceId, "traceId");
    this.requestId = Objects.requireNonNull(requestId, "requestId");
    this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
    this.schemaVersion = Objects.requireNonNull(schemaVersion, "schemaVersion");
    this.payloadJson = Objects.requireNonNull(payloadJson, "payloadJson");
    this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt");
  }

  /** 工厂方法。 */
  public static IngestionCommand of(
      final String tenantId,
      final String eventId,
      final String rawEventType,
      final Instant occurredAt,
      final String sourceSystem,
      final String sourceJobId,
      final String traceId,
      final String requestId,
      final String correlationId,
      final String schemaVersion,
      final String payloadJson,
      final Instant receivedAt) {
    return new IngestionCommand(
        tenantId,
        eventId,
        rawEventType,
        occurredAt,
        sourceSystem,
        sourceJobId,
        traceId,
        requestId,
        correlationId,
        schemaVersion,
        payloadJson,
        receivedAt);
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getEventId() {
    return eventId;
  }

  public String getRawEventType() {
    return rawEventType;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public String getSourceSystem() {
    return sourceSystem;
  }

  public String getSourceJobId() {
    return sourceJobId;
  }

  public String getTraceId() {
    return traceId;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public String getSchemaVersion() {
    return schemaVersion;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }
}

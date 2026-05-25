package com.guidinglight.decisionhub.api.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Stage2-PoC-B2：NQ -> DH 正式 envelope HTTP 请求体。
 *
 * <p>字段与 contracts/json-schema/nq-feedback-envelope.schema.json 一一对应。
 *
 * <p>{@code eventType} 在 HTTP 边界保留为原始字符串；服务层 ({@code
 * com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackContractValidator})
 * 负责映射到 {@code NqFeedbackEventType} 枚举，映射失败返回 400 UNKNOWN_EVENT_TYPE。
 *
 * <p>{@code payloadJson} 保留为原始 JSON 字符串，不在 HTTP 层反序列化为强类型 payload， 由 service / handler
 * 自行解析；这保证了 raw payload 永远不丢失。
 */
public final class NqFeedbackEnvelopeRequest {

  @NotBlank(message = "eventId must not be blank")
  private String eventId;

  @NotBlank(message = "eventType must not be blank")
  private String eventType;

  @NotNull(message = "occurredAt must not be null")
  private Instant occurredAt;

  @NotBlank(message = "sourceSystem must not be blank")
  private String sourceSystem;

  @NotBlank(message = "sourceJobId must not be blank")
  private String sourceJobId;

  @NotBlank(message = "traceId must not be blank")
  private String traceId;

  @NotBlank(message = "requestId must not be blank")
  private String requestId;

  @NotBlank(message = "correlationId must not be blank")
  private String correlationId;

  @NotBlank(message = "schemaVersion must not be blank")
  private String schemaVersion;

  @NotBlank(message = "payloadJson must not be blank")
  private String payloadJson;

  public String getEventId() {
    return eventId;
  }

  public void setEventId(final String eventId) {
    this.eventId = eventId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(final String eventType) {
    this.eventType = eventType;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(final Instant occurredAt) {
    this.occurredAt = occurredAt;
  }

  public String getSourceSystem() {
    return sourceSystem;
  }

  public void setSourceSystem(final String sourceSystem) {
    this.sourceSystem = sourceSystem;
  }

  public String getSourceJobId() {
    return sourceJobId;
  }

  public void setSourceJobId(final String sourceJobId) {
    this.sourceJobId = sourceJobId;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(final String traceId) {
    this.traceId = traceId;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(final String requestId) {
    this.requestId = requestId;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(final String correlationId) {
    this.correlationId = correlationId;
  }

  public String getSchemaVersion() {
    return schemaVersion;
  }

  public void setSchemaVersion(final String schemaVersion) {
    this.schemaVersion = schemaVersion;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public void setPayloadJson(final String payloadJson) {
    this.payloadJson = payloadJson;
  }
}

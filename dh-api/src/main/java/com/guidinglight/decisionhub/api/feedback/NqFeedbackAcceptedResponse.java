package com.guidinglight.decisionhub.api.feedback;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Stage2-PoC-B2：成功接收响应（HTTP 202）。
 *
 * <p>字段：
 *
 * <ul>
 *   <li>{@code eventId}：NQ 侧事件 ID（与请求 envelope 一致）。
 *   <li>{@code status}：始终为 {@code "RECEIVED"}；首次与重放均回相同字符串（按 "原 status" 语义）。
 *   <li>{@code outcome}：辅助字段，区分 {@code "ACCEPTED"} 与 {@code "DUPLICATE"}；客户端可忽略。
 *   <li>{@code traceId}、{@code correlationId}：原样回显，方便上游对账。
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class NqFeedbackAcceptedResponse {

  private String eventId;
  private String status;
  private String outcome;
  private String traceId;
  private String correlationId;

  public NqFeedbackAcceptedResponse() {}

  public NqFeedbackAcceptedResponse(
      final String eventId,
      final String status,
      final String outcome,
      final String traceId,
      final String correlationId) {
    this.eventId = eventId;
    this.status = status;
    this.outcome = outcome;
    this.traceId = traceId;
    this.correlationId = correlationId;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(final String eventId) {
    this.eventId = eventId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public String getOutcome() {
    return outcome;
  }

  public void setOutcome(final String outcome) {
    this.outcome = outcome;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(final String traceId) {
    this.traceId = traceId;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(final String correlationId) {
    this.correlationId = correlationId;
  }
}

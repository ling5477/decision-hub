package com.guidinglight.decisionhub.api.feedback;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Stage2-PoC-B2：拒绝响应（HTTP 400）。
 *
 * <p>字段：
 *
 * <ul>
 *   <li>{@code error}：始终为 {@code "INVALID_REQUEST"}；上层日志兜底。
 *   <li>{@code errorCode}：{@code UNKNOWN_EVENT_TYPE} / {@code INVALID_SCHEMA} / {@code UNKNOWN_TRACE}
 *       三选一（详见 docs/current/STAGE2_POC_WORK_ORDER.md §Batch 2.4）。
 *   <li>{@code message}：人类可读的失败说明（不允许携带堆栈或敏感信息）。
 *   <li>{@code traceId}、{@code correlationId}、{@code eventId}：原样回显，便于排查。
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class NqFeedbackErrorResponse {

  private String error;
  private String errorCode;
  private String message;
  private String eventId;
  private String traceId;
  private String correlationId;

  public NqFeedbackErrorResponse() {}

  public NqFeedbackErrorResponse(
      final String error,
      final String errorCode,
      final String message,
      final String eventId,
      final String traceId,
      final String correlationId) {
    this.error = error;
    this.errorCode = errorCode;
    this.message = message;
    this.eventId = eventId;
    this.traceId = traceId;
    this.correlationId = correlationId;
  }

  public String getError() {
    return error;
  }

  public void setError(final String error) {
    this.error = error;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(final String errorCode) {
    this.errorCode = errorCode;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(final String eventId) {
    this.eventId = eventId;
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

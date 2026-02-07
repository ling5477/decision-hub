package com.guidinglight.decisionhub.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiResponse<T> {

  private boolean success;
  private String code;
  private String message;
  private T data;

  private String traceId;
  private Object detail;
  private Long ts;

  private ApiResponse() {
    this.ts = System.currentTimeMillis();
  }

  public static <T> ApiResponse<T> ok(final T data) {
    return ok(data, null, null);
  }

  public static <T> ApiResponse<T> ok(final T data, final String traceId) {
    return ok(data, traceId, null);
  }

  public static <T> ApiResponse<T> ok(final T data, final String traceId, final Object detail) {
    ApiResponse<T> r = new ApiResponse<>();
    r.success = true;
    r.code = "OK";
    r.message = "OK";
    r.data = data;
    r.traceId = traceId;
    r.detail = detail;
    return r;
  }

  public static <T> ApiResponse<T> fail(final String code, final String message, final String traceId, final Object detail) {
    ApiResponse<T> r = new ApiResponse<>();
    r.success = false;
    r.code = code;
    r.message = message;
    r.traceId = traceId;
    r.detail = detail;
    return r;
  }

  public boolean isSuccess() { return success; }
  public String getCode() { return code; }
  public String getMessage() { return message; }
  public T getData() { return data; }
  public String getTraceId() { return traceId; }
  public Object getDetail() { return detail; }
  public Long getTs() { return ts; }
}

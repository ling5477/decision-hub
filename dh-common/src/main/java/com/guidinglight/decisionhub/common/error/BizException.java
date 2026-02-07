package com.guidinglight.decisionhub.common.error;

public class BizException extends RuntimeException {

  private final int httpStatus;
  private final String code;
  private final Object detail;
  private final String traceId;

  public BizException(final ErrorCode ec, final String message, final Object detail, final String traceId) {
    super(message);
    this.httpStatus = ec.httpStatus();
    this.code = ec.code();
    this.detail = detail;
    this.traceId = traceId;
  }

  public BizException(final ErrorCode ec) {
    this(ec, ec.message(), null, null);
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  public String getCode() {
    return code;
  }

  public Object getDetail() {
    return detail;
  }

  public String getTraceId() {
    return traceId;
  }
}

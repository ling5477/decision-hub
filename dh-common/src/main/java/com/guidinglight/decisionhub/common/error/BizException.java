package com.guidinglight.decisionhub.common.error;

public class BizException extends RuntimeException {

  private final int httpStatus;
  private final String code;
  private final Object detail;
  private final String traceId;

  public BizException(ErrorCode ec) {
    this(ec, ec.message(), null, null);
  }

  public BizException(ErrorCode ec, String overrideMessage) {
    this(ec, overrideMessage, null, null);
  }

  public BizException(ErrorCode ec, String overrideMessage, Object detail) {
    this(ec, overrideMessage, detail, null);
  }

  public BizException(ErrorCode ec, String overrideMessage, Object detail, String traceId) {
    super(overrideMessage);
    this.httpStatus = ec.httpStatus();
    this.code = ec.code();
    this.detail = detail;
    this.traceId = traceId;
  }

  public BizException(int httpStatus, String code, String message, Object detail, String traceId) {
    super(message);
    this.httpStatus = httpStatus;
    this.code = code;
    this.detail = detail;
    this.traceId = traceId;
  }

  public int getHttpStatus() { return httpStatus; }
  public String getCode() { return code; }
  public Object getDetail() { return detail; }
  public String getTraceId() { return traceId; }
}

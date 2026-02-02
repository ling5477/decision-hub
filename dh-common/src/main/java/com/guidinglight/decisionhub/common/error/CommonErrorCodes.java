package com.guidinglight.decisionhub.common.error;

public enum CommonErrorCodes implements ErrorCode {

  BAD_REQUEST("DH-COMMON-400", "Bad request", 400),
  UNAUTHORIZED("DH-COMMON-401", "Unauthorized", 401),
  FORBIDDEN("DH-COMMON-403", "Forbidden", 403),
  NOT_FOUND("DH-COMMON-404", "Not found", 404),
  CONFLICT("DH-COMMON-409", "Conflict", 409),
  UNPROCESSABLE("DH-COMMON-422", "Unprocessable entity", 422),
  INTERNAL_ERROR("DH-COMMON-500", "Internal error", 500);

  private final String code;
  private final String message;
  private final int httpStatus;

  CommonErrorCodes(String code, String message, int httpStatus) {
    this.code = code;
    this.message = message;
    this.httpStatus = httpStatus;
  }

  @Override
  public String code() {
    return code;
  }

  @Override
  public String message() {
    return message;
  }

  @Override
  public int httpStatus() {
    return httpStatus;
  }
}

package com.guidinglight.decisionhub.common.error;

public interface ErrorCode {
  String code();
  String message();
  int httpStatus();
}

package com.guidinglight.decisionhub.domain.qdr.feedback;

/** verified feedback execution scope 无法安全建立时的本地校验异常。 */
public final class FeedbackExecutionScopeException extends IllegalArgumentException {

  /** 创建不暴露请求材料的 scope validation failure。 */
  public FeedbackExecutionScopeException(final String message) {
    super(message);
  }

  /** 创建带本地解析 cause 的 scope validation failure。 */
  public FeedbackExecutionScopeException(final String message, final Throwable cause) {
    super(message, cause);
  }
}

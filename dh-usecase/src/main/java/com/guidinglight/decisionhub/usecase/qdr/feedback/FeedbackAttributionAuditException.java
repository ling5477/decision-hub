package com.guidinglight.decisionhub.usecase.qdr.feedback;

/** Audit port 失败或返回不一致安全引用时的 fail-closed 异常。 */
public final class FeedbackAttributionAuditException extends RuntimeException {

  /** 创建已脱敏的审计失败。 */
  public FeedbackAttributionAuditException(final String message) {
    super(message);
  }

  /** 创建已脱敏的审计失败并保留内部 cause，不向结果暴露 cause。 */
  public FeedbackAttributionAuditException(final String message, final Throwable cause) {
    super(message, cause);
  }
}

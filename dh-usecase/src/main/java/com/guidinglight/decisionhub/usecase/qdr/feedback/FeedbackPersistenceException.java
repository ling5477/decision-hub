package com.guidinglight.decisionhub.usecase.qdr.feedback;

import java.util.Objects;

/** Feedback persistence 失败的稳定异常；消息不得包含 SQL、连接信息或原始 payload。 */
public final class FeedbackPersistenceException extends RuntimeException {

  private final FeedbackPersistenceErrorCode errorCode;

  /** 创建已分类、已脱敏的 persistence 异常。 */
  public FeedbackPersistenceException(
      final FeedbackPersistenceErrorCode errorCode, final String safeMessage) {
    super(requireSafeMessage(safeMessage));
    this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
  }

  /** 创建已分类、已脱敏且保留内部 cause 的 persistence 异常。 */
  public FeedbackPersistenceException(
      final FeedbackPersistenceErrorCode errorCode,
      final String safeMessage,
      final Throwable cause) {
    super(requireSafeMessage(safeMessage), Objects.requireNonNull(cause, "cause"));
    this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
  }

  /** 返回稳定错误分类。 */
  public FeedbackPersistenceErrorCode errorCode() {
    return errorCode;
  }

  private static String requireSafeMessage(final String value) {
    final String checked = Objects.requireNonNull(value, "safeMessage").trim();
    if (checked.isEmpty() || checked.length() > 256) {
      throw new IllegalArgumentException("safeMessage must be within [1,256]");
    }
    return checked;
  }
}

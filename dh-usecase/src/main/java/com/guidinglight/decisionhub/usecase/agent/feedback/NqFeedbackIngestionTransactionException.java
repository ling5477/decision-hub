package com.guidinglight.decisionhub.usecase.agent.feedback;

import java.util.Objects;

/** Feedback ingest persistence/transaction 的内部稳定失败分类；不会进入 wire contract。 */
public final class NqFeedbackIngestionTransactionException extends RuntimeException {

  /** 本任务冻结的两种内部分类。 */
  public enum ErrorCode {
    /** 事务前置、执行、回滚或数据完整性失败。 */
    PERSISTENCE_FAILURE,
    /** action 已完成，但 commit 结果无法证明。 */
    COMMIT_OUTCOME_UNKNOWN
  }

  private final ErrorCode errorCode;

  /** 创建无底层 cause 的脱敏异常。 */
  public NqFeedbackIngestionTransactionException(
      final ErrorCode errorCode, final String safeMessage) {
    super(requireSafeMessage(safeMessage));
    this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
  }

  /** 创建保留底层 cause 的脱敏异常。 */
  public NqFeedbackIngestionTransactionException(
      final ErrorCode errorCode, final String safeMessage, final Throwable cause) {
    super(requireSafeMessage(safeMessage), Objects.requireNonNull(cause, "cause"));
    this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
  }

  /** 返回内部稳定错误分类。 */
  public ErrorCode errorCode() {
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

package com.guidinglight.decisionhub.usecase.decision.dryrun;

/**
 * limited dry-run usecase 的统一结果。
 *
 * <p>成功时只携带 read-only snapshot；失败时只携带 canonical error envelope 所需字段。所有失败均为
 * fail-closed，不得由调用方 fallback 为成功 decision。
 *
 * @param success 是否成功。
 * @param status HTTP 状态建议。
 * @param errorCode canonical error code；成功时为 null。
 * @param message 脱敏错误摘要；成功时为 null。
 * @param requestId requestId，如安全上下文允许。
 * @param traceId traceId，如安全上下文允许。
 * @param auditRef audit reference，如 audit 已成功写入。
 * @param snapshot 成功响应 snapshot。
 */
public record DecisionDryRunResult(
    boolean success,
    int status,
    DecisionDryRunErrorCode errorCode,
    String message,
    String requestId,
    String traceId,
    String auditRef,
    DecisionDryRunSnapshot snapshot) {

  /**
   * 构造成功结果。
   *
   * @param snapshot 成功 snapshot。
   * @return 成功结果。
   */
  public static DecisionDryRunResult success(final DecisionDryRunSnapshot snapshot) {
    return new DecisionDryRunResult(
        true, 200, null, null, null, null, snapshot == null ? null : snapshot.auditRef(), snapshot);
  }

  /**
   * 构造 fail-closed 结果。
   *
   * @param status HTTP 状态建议。
   * @param errorCode canonical error code。
   * @param message 脱敏错误摘要。
   * @param requestId request id。
   * @param traceId trace id。
   * @param auditRef audit ref；audit 写失败时可为空。
   * @return 失败结果。
   */
  public static DecisionDryRunResult rejected(
      final int status,
      final DecisionDryRunErrorCode errorCode,
      final String message,
      final String requestId,
      final String traceId,
      final String auditRef) {
    return new DecisionDryRunResult(
        false, status, errorCode, message, requestId, traceId, auditRef, null);
  }
}

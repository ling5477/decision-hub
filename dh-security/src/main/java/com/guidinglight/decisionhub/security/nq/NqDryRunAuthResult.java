package com.guidinglight.decisionhub.security.nq;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackExecutionScope;

/**
 * Integration-1 limited dry-run HMAC / replay gate 结果。
 *
 * <p>errorCode 使用 runtime API work order 冻结的 canonical taxonomy；reason 只保留安全摘要，不包含签名、
 * 密钥、nonce 明文以外的请求体、原始 body 或任何 credential。
 *
 * @param allowed 是否放行。
 * @param status HTTP 状态建议。
 * @param errorCode canonical error code；放行时为 OK。
 * @param reason 可审计安全摘要。
 */
public record NqDryRunAuthResult(
    boolean allowed,
    int status,
    String errorCode,
    String reason,
    FeedbackExecutionScope executionScope) {

  /** 返回携带唯一 verified root scope 的通过结果。 */
  public static NqDryRunAuthResult success(final FeedbackExecutionScope executionScope) {
    if (executionScope == null) {
      throw new IllegalArgumentException("verified execution scope is required");
    }
    return new NqDryRunAuthResult(true, 200, "OK", "OK", executionScope);
  }

  /**
   * 返回 fail-closed 结果。
   *
   * @param status HTTP 状态建议。
   * @param errorCode canonical error code。
   * @param reason 可审计摘要，不得含敏感值。
   * @return 失败结果。
   */
  public static NqDryRunAuthResult rejected(
      final int status, final String errorCode, final String reason) {
    return new NqDryRunAuthResult(false, status, errorCode, reason, null);
  }
}

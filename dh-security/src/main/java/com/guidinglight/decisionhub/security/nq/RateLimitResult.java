package com.guidinglight.decisionhub.security.nq;

/**
 * 入站限流结果（DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-3）。
 *
 * <p>独立于 {@link NqFeedbackAuthResult}：限流是认证之前的独立闸门，超限不得伪装成 HMAC 失败。
 *
 * <p>字段：
 *
 * <ul>
 *   <li>{@code allowed}：是否放行；false -> controller 映射 HTTP 429。
 *   <li>{@code reason}：失败类型，超限固定为 {@link #REASON_RATE_LIMITED}；<b>只含类型，不含阈值 / 窗口 / 计数 / 密钥</b>。
 *   <li>{@code retryAfterSeconds}：可选粗粒度重试提示（供审计 / 日志）；为避免泄露精确窗口，<b>controller 默认不在响应头暴露</b>。
 *   <li>{@code auditCode}：审计码，超限为 {@link #AUDIT_RATE_LIMITED}，用于可观测记录证明 RATE_LIMITED 分支被触发。
 * </ul>
 */
public record RateLimitResult(
    boolean allowed, String reason, Integer retryAfterSeconds, String auditCode) {

  /** 放行 reason。 */
  public static final String REASON_OK = "OK";

  /** 超限 reason / errorCode（对外只暴露类型，不暴露阈值细节）。 */
  public static final String REASON_RATE_LIMITED = "RATE_LIMITED";

  /** 超限审计码（用于可观测记录）。 */
  public static final String AUDIT_RATE_LIMITED = "RATE_LIMITED";

  /** 放行（静态工厂命名为 pass，避免与 record 自动生成的 {@code allowed()} 访问器冲突）。 */
  public static RateLimitResult pass() {
    return new RateLimitResult(true, REASON_OK, null, null);
  }

  /**
   * 超限（含保护性拒绝）。
   *
   * @param retryAfterSeconds 可选粗粒度重试秒数；仅供审计 / 日志，响应不暴露精确窗口。
   * @return 不放行的限流结果，reason / auditCode 均为 RATE_LIMITED。
   */
  public static RateLimitResult limited(final Integer retryAfterSeconds) {
    return new RateLimitResult(false, REASON_RATE_LIMITED, retryAfterSeconds, AUDIT_RATE_LIMITED);
  }
}

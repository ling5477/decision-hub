package com.guidinglight.decisionhub.security.nq;

/**
 * NQ feedback 来源认证结果。
 *
 * <p>status 用于 controller 映射 HTTP 状态；reason 只包含错误类型，不包含签名或密钥明文。
 */
public record NqFeedbackAuthResult(boolean allowed, int status, String reason) {

  /** 认证通过。 */
  public static NqFeedbackAuthResult success() {
    return new NqFeedbackAuthResult(true, 202, "OK");
  }

  /** 认证失败。 */
  public static NqFeedbackAuthResult rejected(final int status, final String reason) {
    return new NqFeedbackAuthResult(false, status, reason);
  }
}

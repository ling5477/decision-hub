package com.guidinglight.decisionhub.domain.qdr.feedback;

/**
 * 结构化反馈允许使用的隔离环境。
 *
 * <p>Stage-QDR-8 仅允许开发与测试环境，其他环境必须在枚举解析或构造阶段 fail-closed。
 */
public enum FeedbackEnvironment {
  /** 开发环境。 */
  DEV,
  /** 测试环境。 */
  TEST;

  /**
   * Parses the signed wire value without aliases, whitespace trimming, or environment defaults.
   *
   * @param wireValue exact environment value supplied by the caller
   * @return the canonical environment
   * @throws FeedbackExecutionScopeException when the value is absent or outside the DEV/TEST allowlist
   */
  public static FeedbackEnvironment fromWire(final String wireValue) {
    if (wireValue == null || wireValue.isBlank()) {
      throw new FeedbackExecutionScopeException("feedback environment is required");
    }
    try {
      return FeedbackEnvironment.valueOf(wireValue);
    } catch (final IllegalArgumentException error) {
      throw new FeedbackExecutionScopeException("feedback environment is not supported", error);
    }
  }
}

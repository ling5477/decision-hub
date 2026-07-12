package com.guidinglight.decisionhub.usecase.qdr.guard;

/** Persistent rate-limit admission状态；store错误不得伪装成quota拒绝。 */
public enum RateLimitAdmissionStatus {
  /** 当前请求原子占用一个quota。 */
  ACCEPTED,
  /** 当前bucket已经达到quota。 */
  RATE_LIMITED,
  /** PostgreSQL不可用或SQL失败。 */
  STORE_UNAVAILABLE,
  /** 事务提交结果无法确定。 */
  COMMIT_UNKNOWN,
  /** stored bucket与当前配置矛盾。 */
  CONFIGURATION_INVALID
}

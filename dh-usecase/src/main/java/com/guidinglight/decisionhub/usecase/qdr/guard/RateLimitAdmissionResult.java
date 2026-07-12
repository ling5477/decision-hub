package com.guidinglight.decisionhub.usecase.qdr.guard;

import java.time.Instant;

/**
 * Persistent rate admission安全结果。
 *
 * @param status admission分类。
 * @param windowStart DB UTC窗口起点；store错误时为空。
 * @param windowEnd DB UTC窗口终点；store错误时为空。
 * @param databaseNow 当前事务的PostgreSQL时间，用于计算非持久化retry提示。
 * @param acceptedCount 已accepted计数；store错误时为空。
 * @param limitValue bucket quota；store错误时为空。
 */
public record RateLimitAdmissionResult(
    RateLimitAdmissionStatus status,
    Instant windowStart,
    Instant windowEnd,
    Instant databaseNow,
    Long acceptedCount,
    Integer limitValue) {

  /** 返回store不可用的fail-closed结果。 */
  public static RateLimitAdmissionResult storeUnavailable() {
    return new RateLimitAdmissionResult(
        RateLimitAdmissionStatus.STORE_UNAVAILABLE, null, null, null, null, null);
  }
}

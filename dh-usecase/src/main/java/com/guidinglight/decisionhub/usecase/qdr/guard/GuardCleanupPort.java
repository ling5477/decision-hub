package com.guidinglight.decisionhub.usecase.qdr.guard;

/** Bounded、indexed、short-transaction cleanup能力；不暴露任意delete。 */
public interface GuardCleanupPort {

  /** 清理已过安全cutoff的旧rate buckets，返回实际删除数。 */
  int cleanupExpiredRateBuckets(GuardCleanupCommand command);

  /** 清理达到retention的terminal idempotency tombstones；active lease永不删除。 */
  int cleanupRetainedIdempotency(GuardCleanupCommand command);
}

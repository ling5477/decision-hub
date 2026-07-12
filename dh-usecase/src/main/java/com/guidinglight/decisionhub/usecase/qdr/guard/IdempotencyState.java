package com.guidinglight.decisionhub.usecase.qdr.guard;

/** Stage-QDR-7 persistent idempotency状态。 */
public enum IdempotencyState {
  /** 已登记，尚未取得执行lease。 */
  RECEIVED,
  /** 内部worker持有有效lease。 */
  IN_PROGRESS,
  /** 已冻结safe result reference。 */
  COMPLETED,
  /** 已冻结stable failure，不自动重试。 */
  FAILED,
  /** tombstone；requestId不得复用。 */
  EXPIRED
}

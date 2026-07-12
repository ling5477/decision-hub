package com.guidinglight.decisionhub.usecase.qdr.guard;

/** Exact idempotency admission分类。 */
public enum IdempotencyAdmissionStatus {
  /** 首次原子insert成功。 */
  ADMITTED,
  /** same hash处于RECEIVED或IN_PROGRESS。 */
  IN_PROGRESS,
  /** same hash已有immutable safe result。 */
  COMPLETED,
  /** same hash已有冻结失败。 */
  FAILED,
  /** same hash已过期，必须使用新requestId。 */
  EXPIRED,
  /** same identity使用不同hash。 */
  CONFLICT,
  /** store不可用。 */
  STORE_UNAVAILABLE,
  /** commit结果未知。 */
  COMMIT_UNKNOWN,
  /** unknown/corrupt state。 */
  STATE_INVALID,
  /** completed result缺失或checksum不符。 */
  RESULT_UNAVAILABLE
}

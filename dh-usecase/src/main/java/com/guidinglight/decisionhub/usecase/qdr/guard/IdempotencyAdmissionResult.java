package com.guidinglight.decisionhub.usecase.qdr.guard;

/**
 * Idempotency admission结果。
 *
 * @param status 稳定分类。
 * @param record exact persisted view；store错误时为空。
 */
public record IdempotencyAdmissionResult(
    IdempotencyAdmissionStatus status, IdempotencyRecordView record) {

  /** Store不可用的fail-closed结果。 */
  public static IdempotencyAdmissionResult storeUnavailable() {
    return new IdempotencyAdmissionResult(IdempotencyAdmissionStatus.STORE_UNAVAILABLE, null);
  }
}

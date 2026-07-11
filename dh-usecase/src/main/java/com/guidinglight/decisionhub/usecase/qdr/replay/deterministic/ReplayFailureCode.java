package com.guidinglight.decisionhub.usecase.qdr.replay.deterministic;

/** Deterministic replay 的冻结 fail-closed taxonomy 与唯一状态映射。 */
public enum ReplayFailureCode {
  /** Tenant-bound exact snapshot 不存在。 */
  SNAPSHOT_NOT_FOUND(ReplayReproducibilityStatus.INCOMPLETE),
  /** Tenant 或完整 physical/business identity 不一致。 */
  TENANT_IDENTITY_MISMATCH(ReplayReproducibilityStatus.INVALID_INPUT),
  /** Trace/request/decision correlation 不一致。 */
  CORRELATION_MISMATCH(ReplayReproducibilityStatus.INVALID_INPUT),
  /** Persisted、command 或重新计算的 canonical input hash 不一致。 */
  CANONICAL_HASH_MISMATCH(ReplayReproducibilityStatus.INVALID_INPUT),
  /** Version vector required field 缺失。 */
  VERSION_VECTOR_INCOMPLETE(ReplayReproducibilityStatus.INCOMPLETE),
  /** Snapshot schema 或 canonicalization version 不受支持。 */
  CANONICALIZATION_VERSION_UNSUPPORTED(ReplayReproducibilityStatus.UNSUPPORTED_VERSION),
  /** Requested 或 persisted executor version 不受支持。 */
  EXECUTOR_VERSION_UNSUPPORTED(ReplayReproducibilityStatus.UNSUPPORTED_VERSION),
  /** Legacy row 不能形成完整 immutable snapshot。 */
  LEGACY_NOT_REPLAYABLE(ReplayReproducibilityStatus.INVALID_INPUT),
  /** Snapshot 或 result 违反 raw/trading/safety guard。 */
  UNSAFE_INPUT(ReplayReproducibilityStatus.INVALID_INPUT),
  /** Structured baseline 不完整。 */
  BASELINE_INCOMPLETE(ReplayReproducibilityStatus.INCOMPLETE),
  /** Tenant-bound persistence read 失败。 */
  PERSISTENCE_READ_FAILED(ReplayReproducibilityStatus.EXECUTION_FAILED),
  /** QDR6-MOCK-REPLAY-1 本地转换失败。 */
  MOCK_TRANSFORMATION_FAILED(ReplayReproducibilityStatus.EXECUTION_FAILED),
  /** Replay output canonicalization 或 SHA-256 失败。 */
  OUTPUT_CANONICALIZATION_FAILED(ReplayReproducibilityStatus.EXECUTION_FAILED);

  private final ReplayReproducibilityStatus status;

  ReplayFailureCode(final ReplayReproducibilityStatus status) {
    this.status = status;
  }

  /** 返回该 failure code 唯一允许的 reproducibility status。 */
  public ReplayReproducibilityStatus status() {
    return status;
  }
}

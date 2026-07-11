package com.guidinglight.decisionhub.usecase.qdr.replay.deterministic;

/** Deterministic replay 的冻结、无模糊成功语义状态。 */
public enum ReplayReproducibilityStatus {
  /** 输入、版本、baseline 与 replay output 全部一致。 */
  REPRODUCIBLE,
  /** 至少一个冻结结构化字段存在差异。 */
  DIFFERENT,
  /** Snapshot 或 baseline required input 缺失。 */
  INCOMPLETE,
  /** Schema、canonicalization 或 executor version 不受支持。 */
  UNSUPPORTED_VERSION,
  /** Tenant、identity、hash 或安全合同无效。 */
  INVALID_INPUT,
  /** Persistence read、local transformation 或 canonicalization 执行失败。 */
  EXECUTION_FAILED
}

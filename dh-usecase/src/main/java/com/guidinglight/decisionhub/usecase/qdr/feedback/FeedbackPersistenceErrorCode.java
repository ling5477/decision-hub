package com.guidinglight.decisionhub.usecase.qdr.feedback;

/** Stage-QDR-9 feedback persistence 的稳定 fail-closed 错误分类。 */
public enum FeedbackPersistenceErrorCode {
  /** 同一 tenant/environment/idempotency key 对应不同 canonical hash。 */
  IDEMPOTENCY_CONFLICT,
  /** persistence aggregate 的 tenant 关联不一致。 */
  TENANT_SCOPE_MISMATCH,
  /** persistence aggregate 的 environment 关联不一致。 */
  ENVIRONMENT_SCOPE_MISMATCH,
  /** audit/replay/evaluation/evidence 引用不存在或不匹配。 */
  REFERENCE_INVALID,
  /** PostgreSQL 写入、读取或事务前置阶段失败。 */
  PERSISTENCE_FAILURE,
  /** 底层唯一约束冲突；只允许在 scoped reconciliation 前内部使用。 */
  DUPLICATE_KEY,
  /** commit 结果无法证明，禁止自动重试或声称成功。 */
  COMMIT_OUTCOME_UNKNOWN,
  /** 已有 persistence aggregate 缺少必须的 observation、attribution 或 children。 */
  AGGREGATE_INCOMPLETE,
  /** 内部历史证据查询参数无效。 */
  QUERY_VALIDATION_FAILED,
  /** 内部历史证据查询失败。 */
  QUERY_FAILURE,
  /** active reference 或完整性问题阻止 retention。 */
  RETENTION_BLOCKED,
  /** retention command 明确保持关闭，未执行数据库写入。 */
  RETENTION_DISABLED,
  /** retention command 缺少单一 tenant/environment scope。 */
  INVALID_RETENTION_SCOPE,
  /** retention age 不为正或超过安全上限。 */
  INVALID_RETENTION_AGE,
  /** retention batch 不在固定安全上限内。 */
  INVALID_BATCH_SIZE,
  /** AUDIT、REPLAY 或 EVALUATION reference 仍为 active。 */
  REFERENCE_ACTIVE,
  /** reference 缺失、格式错误、状态未知或无法证明 scope/liveness。 */
  REFERENCE_STATUS_UNKNOWN,
  /** retention transaction 或 PostgreSQL statement 超过固定 deadline。 */
  RETENTION_TIMEOUT,
  /** concurrent cleanup 无法证明安全结果。 */
  RETENTION_CONCURRENT_CONFLICT,
  /** bounded retention 执行失败。 */
  RETENTION_FAILURE
}

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
  /** commit 结果无法证明，禁止自动重试或声称成功。 */
  COMMIT_OUTCOME_UNKNOWN,
  /** 内部历史证据查询参数无效。 */
  QUERY_VALIDATION_FAILED,
  /** 内部历史证据查询失败。 */
  QUERY_FAILURE,
  /** active reference 或完整性问题阻止 retention。 */
  RETENTION_BLOCKED,
  /** bounded retention 执行失败。 */
  RETENTION_FAILURE
}

package com.guidinglight.decisionhub.usecase.decision.dryrun;

/**
 * Integration-1 limited dry-run endpoint 的 canonical error taxonomy。
 *
 * <p>该枚举只服务 DH inbound dry-run endpoint 的 fail-closed envelope，不写入 formal contracts，也不授权
 * NQ runtime client、real provider、Agent/LangGraph runtime 或 LIVE。
 */
public enum DecisionDryRunErrorCode {
  /** HMAC 缺失或无效。 */
  SIGNATURE_INVALID,

  /** timestamp 格式不是 RFC3339 UTC Z。 */
  TIMESTAMP_INVALID,

  /** timestamp 超出 ±300s 或配置窗口。 */
  TIMESTAMP_OUT_OF_WINDOW,

  /** nonce / requestId 在 replay window 内重复。 */
  NONCE_REPLAY,

  /** body/header tenant 与认证 tenant 不一致。 */
  TENANT_MISMATCH,

  /** source 或 tenant/source pair 不在允许列表。 */
  SOURCE_DENIED,

  /** raw body 或签名 payload 超过大小上限。 */
  PAYLOAD_TOO_LARGE,

  /** 入站限流拒绝。 */
  RATE_LIMITED,

  /** Persistent rate-limit store不可用。 */
  RATE_LIMIT_STORE_UNAVAILABLE,

  /** Persistent rate-limit transaction提交结果未知。 */
  RATE_LIMIT_COMMIT_UNKNOWN,

  /** Same requestId/hash仍在处理。 */
  IDEMPOTENCY_IN_PROGRESS,

  /** Same requestId使用不同业务hash。 */
  IDEMPOTENCY_CONFLICT,

  /** Idempotency tombstone已过期，必须使用新requestId。 */
  IDEMPOTENCY_EXPIRED,

  /** Persistent idempotency store不可用。 */
  IDEMPOTENCY_STORE_UNAVAILABLE,

  /** Persistent idempotency transaction提交结果未知。 */
  IDEMPOTENCY_COMMIT_UNKNOWN,

  /** Idempotency状态损坏或CAS非法。 */
  IDEMPOTENCY_STATE_INVALID,

  /** COMPLETED safe result缺失或checksum不符。 */
  IDEMPOTENCY_RESULT_UNAVAILABLE,

  /** Persistent guard配置非法；正常应在启动时阻断。 */
  GUARD_CONFIGURATION_INVALID,

  /** dry-run 上下文超过内存上限。 */
  MEMORY_LIMIT_EXCEEDED,

  /** feature flag、dryRun、forbidden field 或只读 policy 拒绝。 */
  POLICY_DENIED,

  /** provider gate 显式关闭。 */
  PROVIDER_DISABLED,

  /** provider gate 超时。 */
  PROVIDER_TIMEOUT,

  /** provider budget 已耗尽。 */
  BUDGET_EXCEEDED,

  /** 未分类异常或 audit 写入失败。 */
  UNKNOWN_ERROR
}

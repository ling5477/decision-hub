package com.guidinglight.decisionhub.usecase.decision.dryrun;

/**
 * Limited dry-run runtime 的稳定低基数失败分类。
 *
 * <p>分类只描述内部运行边界，不扩展 HTTP/API schema，也不包含配置值、异常消息或原始 payload。
 */
public enum RuntimeFailureClassification {
  /** Runtime feature flag 未开启。 */
  RUNTIME_DISABLED,

  /** 当前 profile 不属于唯一 dev/test 环境。 */
  ENVIRONMENT_DENIED,

  /** Kill switch 非明确允许状态。 */
  KILL_SWITCH_DENIED,

  /** 总 deadline 已耗尽。 */
  DEADLINE_EXCEEDED,

  /** 并发槽、队列或队列等待上限拒绝请求。 */
  CAPACITY_REJECTED,

  /** Runtime provider 合同不是 deterministic mock-only。 */
  MOCK_PROVIDER_REQUIRED,

  /** 既有安全 guard 拒绝请求。 */
  SECURITY_GUARD_REJECTED,

  /** 成功结果违反只读、无副作用 action 合同。 */
  NO_SIDE_EFFECT_VIOLATION,

  /** Deadline、并发或队列配置非法。 */
  RUNTIME_CONFIGURATION_INVALID,

  /** 未知执行异常；必须 fail-closed。 */
  INTERNAL_RUNTIME_FAILURE
}

package com.guidinglight.decisionhub.domain.decision;

/**
 * K5 provider guard 失败分类。
 *
 * <p>失败分类用于审计、replay 与 provider call summary；它不是异常类型，也不得携带 raw provider response、
 * credential、token 或任何外部敏感上下文。
 */
public enum DecisionProviderFailureClass {
    /**
     * 没有 guard 失败。
     */
    NONE,

    /**
     * provider 调用或本地 latency policy 超时。
     */
    TIMEOUT,

    /**
     * provider 本地 mock 调用失败或返回不可用状态。
     */
    FAILED,

    /**
     * provider 信号不可信，或出现真实 provider 成功态伪装。
     */
    UNTRUSTED,

    /**
     * provider 被配置或本地 gate 禁用。
     */
    DISABLED,

    /**
     * 本地预算估算超过允许阈值。
     */
    BUDGET_EXCEEDED,

    /**
     * provider signal 与 K1/K2/K5 只读合同不一致。
     */
    INVALID_SIGNAL,

    /**
     * 失败原因无法安全分类。
     */
    UNKNOWN
}

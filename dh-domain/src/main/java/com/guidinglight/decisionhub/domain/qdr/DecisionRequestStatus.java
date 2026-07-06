package com.guidinglight.decisionhub.domain.qdr;

/**
 * Quant Decision Review 请求状态。
 *
 * <p>状态仅表示 DH 内部只读审查请求生命周期，不表示交易授权或 NQ runtime 已连接。
 */
public enum DecisionRequestStatus {
    /**
     * 请求已通过 dry-run 前置安全门并进入 Decision Core 主线。
     */
    ACCEPTED,

    /**
     * 请求被安全门或策略拒绝；当前 stage-qdr-1 仅预留，不落审批闭环。
     */
    REJECTED
}

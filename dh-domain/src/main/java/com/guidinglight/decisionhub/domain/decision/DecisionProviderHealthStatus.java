package com.guidinglight.decisionhub.domain.decision;

/**
 * K5 provider health 观测状态。
 *
 * <p>这些状态只描述 mock provider 在单次只读 decision 中的可用性观测，不表示真实 provider runtime、外部模型、
 * NQ runtime 或交易通道状态。
 */
public enum DecisionProviderHealthStatus {
    /**
     * provider 本地 mock 信号可用，允许继续只读编排。
     */
    HEALTHY,

    /**
     * provider 有轻微异常但仍可审计；K5 首版不会把 degraded 当作真实 provider 成功。
     */
    DEGRADED,

    /**
     * provider 不可用或返回失败信号，调用方必须 fail-closed。
     */
    UNHEALTHY,

    /**
     * provider 被本地 gate 禁用，调用方必须 fail-closed。
     */
    DISABLED,

    /**
     * provider 状态无法确认，调用方必须 fail-closed，避免把未知当作可用。
     */
    UNKNOWN
}

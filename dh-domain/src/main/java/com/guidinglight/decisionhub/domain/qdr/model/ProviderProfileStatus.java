package com.guidinglight.decisionhub.domain.qdr.model;

/**
 * Provider profile 状态。
 */
public enum ProviderProfileStatus {
    /**
     * mock profile 可用于 B1/B2 本地 deterministic tests。
     */
    ENABLED,
    /**
     * profile 已禁用，调用链必须拒绝。
     */
    DISABLED,
    /**
     * 仅规划态，不允许 runtime 调用。
     */
    PLANNED
}

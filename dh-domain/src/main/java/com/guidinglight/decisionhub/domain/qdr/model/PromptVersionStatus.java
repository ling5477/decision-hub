package com.guidinglight.decisionhub.domain.qdr.model;

/**
 * Prompt version 生命周期状态。
 */
public enum PromptVersionStatus {
    /**
     * 草稿版本，只能用于 mock/local validation。
     */
    DRAFT,
    /**
     * 当前允许使用的 active 版本。
     */
    ACTIVE,
    /**
     * 已废弃但仍可用于审计回放引用的版本。
     */
    DEPRECATED,
    /**
     * 禁用版本，registry 与 render policy 必须拒绝。
     */
    DISABLED
}

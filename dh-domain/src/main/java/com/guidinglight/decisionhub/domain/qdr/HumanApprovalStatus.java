package com.guidinglight.decisionhub.domain.qdr;

/**
 * 人工审批状态预留枚举。
 *
 * <p>stage-qdr-1 不实现 human_approval_packet 或审批 API，仅在 `quant_decision` 中保留状态字段供
 * stage-qdr-2 使用。
 */
public enum HumanApprovalStatus {
    /**
     * 当前只读 dry-run 不需要人工审批包。
     */
    NOT_REQUIRED,

    /**
     * 后续阶段判定需要人工审批。
     */
    REQUIRED,

    /**
     * 审批包已创建但尚未完成。
     */
    PENDING,

    /**
     * 人工审批通过；stage-qdr-1 不产生该状态。
     */
    APPROVED,

    /**
     * 人工审批拒绝或 fail-closed。
     */
    REJECTED
}

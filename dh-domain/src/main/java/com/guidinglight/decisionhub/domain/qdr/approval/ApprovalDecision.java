package com.guidinglight.decisionhub.domain.qdr.approval;

/**
 * 人工审批决策值。
 *
 * <p>决策值仅能驱动审批状态机，不得被映射为 BUY / SELL / 下单 / 撤单。
 */
public enum ApprovalDecision {
    /**
     * 通过人工审查；不是 BUY。
     */
    APPROVED(ApprovalStatus.APPROVED),

    /**
     * 拒绝人工审查；不是 SELL。
     */
    REJECTED(ApprovalStatus.REJECTED),

    /**
     * 要求继续补充审查。
     */
    NEEDS_REVIEW(ApprovalStatus.NEEDS_REVIEW);

    private final ApprovalStatus targetStatus;

    ApprovalDecision(final ApprovalStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    /**
     * 返回该决策对应的审批状态。
     *
     * @return 审批状态机目标状态。
     */
    public ApprovalStatus targetStatus() {
        return targetStatus;
    }
}

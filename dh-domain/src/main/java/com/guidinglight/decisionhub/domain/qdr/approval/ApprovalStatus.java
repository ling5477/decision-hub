package com.guidinglight.decisionhub.domain.qdr.approval;

/**
 * Human Approval Packet 状态。
 *
 * <p>`APPROVED` 只是人工审查通过，不是 `BUY`；`REJECTED` 只是人工审查拒绝，不是 `SELL`。
 * 终态不得重新打开或转换为交易动作。
 */
public enum ApprovalStatus {
    /**
     * 已创建，等待人工审查。
     */
    PENDING(false),

    /**
     * 人工审查通过；不是交易授权。
     */
    APPROVED(true),

    /**
     * 人工审查拒绝；不是 SELL。
     */
    REJECTED(true),

    /**
     * 需要补充人工审查。
     */
    NEEDS_REVIEW(false),

    /**
     * 审批包过期；终态。
     */
    EXPIRED(true);

    private final boolean terminal;

    ApprovalStatus(final boolean terminal) {
        this.terminal = terminal;
    }

    /**
     * 判断状态是否终态。
     *
     * @return true 表示不可再转移。
     */
    public boolean terminal() {
        return terminal;
    }
}

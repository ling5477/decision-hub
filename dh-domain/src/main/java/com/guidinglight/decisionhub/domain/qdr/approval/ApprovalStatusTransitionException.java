package com.guidinglight.decisionhub.domain.qdr.approval;

/**
 * Human Approval Packet 状态机拒绝异常。
 *
 * <p>非法状态转移必须明确失败，不能 silently fallback 到 APPROVED 或其它终态。
 */
public final class ApprovalStatusTransitionException extends RuntimeException {

    /**
     * 创建状态机拒绝异常。
     *
     * @param message 脱敏错误摘要。
     */
    public ApprovalStatusTransitionException(final String message) {
        super(message);
    }
}

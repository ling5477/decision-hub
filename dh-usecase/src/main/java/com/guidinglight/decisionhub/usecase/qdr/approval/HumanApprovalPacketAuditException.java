package com.guidinglight.decisionhub.usecase.qdr.approval;

/**
 * Human Approval Packet 审计写入失败。
 *
 * <p>审计失败必须 fail-closed：调用方不得在该异常后返回 success，也不得把审批状态解释为交易信号。
 */
public final class HumanApprovalPacketAuditException extends RuntimeException {

    /**
     * 创建审计失败异常。
     *
     * @param message 脱敏错误摘要。
     * @param cause   原始异常；API 不得透出 cause message。
     */
    public HumanApprovalPacketAuditException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

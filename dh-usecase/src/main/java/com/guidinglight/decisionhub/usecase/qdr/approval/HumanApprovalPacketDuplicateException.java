package com.guidinglight.decisionhub.usecase.qdr.approval;

/**
 * Human Approval Packet 幂等 key 重复异常。
 *
 * <p>duplicate `tenant_id + approval_key` 必须 fail-closed，不允许静默复用或创建第二份审批包。
 */
public final class HumanApprovalPacketDuplicateException extends HumanApprovalPacketPersistenceException {

    /**
     * 创建重复 key 异常。
     *
     * @param message 脱敏错误摘要。
     * @param cause 原始异常。
     */
    public HumanApprovalPacketDuplicateException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

package com.guidinglight.decisionhub.usecase.qdr.approval;

/**
 * Human Approval Packet 持久化异常。
 *
 * <p>repository save、query、update 或 JSON 转换失败时必须抛出本异常或其子类；不得吞异常后返回成功。
 */
public class HumanApprovalPacketPersistenceException extends RuntimeException {

    /**
     * 创建持久化异常。
     *
     * @param message 脱敏错误摘要。
     * @param cause 原始异常。
     */
    public HumanApprovalPacketPersistenceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

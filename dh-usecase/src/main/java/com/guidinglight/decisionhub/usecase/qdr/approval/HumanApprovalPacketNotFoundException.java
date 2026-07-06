package com.guidinglight.decisionhub.usecase.qdr.approval;

/**
 * Human Approval Packet 或其绑定 decision_run 不存在。
 *
 * <p>调用方必须把该异常映射为 404 或 403/404 类响应，不得泄露跨 tenant 资源存在性。
 */
public final class HumanApprovalPacketNotFoundException extends RuntimeException {

    /**
     * 创建 not found 异常。
     *
     * @param message 脱敏错误摘要。
     */
    public HumanApprovalPacketNotFoundException(final String message) {
        super(message);
    }
}

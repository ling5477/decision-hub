package com.guidinglight.decisionhub.usecase.qdr.approval;

/**
 * Approval decision 输入非法时使用的固定安全异常。
 *
 * <p>异常 message 故意不包含原始输入、enum class 名或 Java `Enum.valueOf` cause，避免 API error
 * body、日志聚合或审计摘要把交易动作词误当成可执行指令传播。该异常只表示请求被 fail-closed 拒绝，
 * 不代表 approval 状态机已经执行。
 */
public final class InvalidApprovalDecisionException extends RuntimeException {

    /**
     * 创建固定安全异常；message 不包含 raw decision value。
     */
    public InvalidApprovalDecisionException() {
        super("Invalid approval decision.");
    }
}

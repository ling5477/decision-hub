package com.guidinglight.decisionhub.usecase.qdr.approval;

import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalDecision;

/**
 * Approval decision 安全解析器。
 *
 * <p>该解析器不用 `Enum.valueOf` 解析外部输入，避免 unknown enum 把 `No enum constant ...` 与原始请求值
 * 传入 API error response。它只接受人工审批状态机允许的三个值；任何空值、空白、大小写不匹配或交易动作词
 * 都固定 fail-closed。
 */
public final class ApprovalDecisionParser {

    private ApprovalDecisionParser() {
        // utility class
    }

    /**
     * 将 API 原始 decision 文本解析为内部 approval decision。
     *
     * <p>Why：approval decision 只能驱动 DH 内部审批状态机，不能被解释为交易授权。使用显式 switch 而不是
     * `ApprovalDecision.valueOf`，是为了让非法值统一进入固定安全错误，不泄露 raw enum cause、原始输入或
     * allowed enum list。
     *
     * @param rawValue API request 中的 decision 原始文本；允许首尾空白，但必须精确匹配受支持值。
     * @return 解析后的 approval decision。
     * @throws InvalidApprovalDecisionException 当输入为空、空白或不在安全白名单中时抛出。
     */
    public static ApprovalDecision parseSafe(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new InvalidApprovalDecisionException();
        }
        return switch (rawValue.trim()) {
            case "APPROVED" -> ApprovalDecision.APPROVED;
            case "REJECTED" -> ApprovalDecision.REJECTED;
            case "NEEDS_REVIEW" -> ApprovalDecision.NEEDS_REVIEW;
            default -> throw new InvalidApprovalDecisionException();
        };
    }
}

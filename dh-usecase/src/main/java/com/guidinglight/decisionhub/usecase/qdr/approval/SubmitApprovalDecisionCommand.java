package com.guidinglight.decisionhub.usecase.qdr.approval;

import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalDecision;

import java.util.Objects;

/**
 * B4 submit approval decision API 对应的 usecase command。
 *
 * <p>decision 只能是 APPROVED / REJECTED / NEEDS_REVIEW，且只驱动 DH 内部 approval 状态机；
 * 不得被解释为 BUY / SELL / replay / provider / NQ mutation。
 */
public record SubmitApprovalDecisionCommand(
        String tenantId,
        String approvalPacketId,
        ApprovalDecision decision,
        String reviewerId,
        String reviewerNote,
        String requesterId,
        String traceId) {

    /**
     * 校验提交命令，阻止匿名或不可审计的人工决策。
     */
    public SubmitApprovalDecisionCommand {
        tenantId = ApprovalCommandValidation.requireText(tenantId, "tenantId");
        approvalPacketId = ApprovalCommandValidation.requireUuidText(approvalPacketId, "approvalPacketId");
        decision = Objects.requireNonNull(decision, "decision");
        reviewerId = ApprovalCommandValidation.optionalSafeText(reviewerId, "reviewerId");
        reviewerNote = ApprovalCommandValidation.optionalSafeText(reviewerNote, "reviewerNote");
        requesterId = ApprovalCommandValidation.optionalSafeText(requesterId, "requesterId");
        traceId = ApprovalCommandValidation.optionalSafeText(traceId, "traceId");
    }
}

package com.guidinglight.decisionhub.usecase.qdr.approval;

import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalReviewer;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalStatus;
import com.guidinglight.decisionhub.domain.qdr.approval.HumanApprovalPacketId;

import java.time.Instant;
import java.util.Objects;

/**
 * Human Approval Packet 状态更新 command。
 *
 * <p>该 command 仅供 domain/usecase/repository 内部 contract 使用，本轮不暴露 API endpoint。
 */
public record ApprovalStatusUpdateCommand(
        String tenantId,
        HumanApprovalPacketId id,
        ApprovalStatus nextStatus,
        ApprovalReviewer reviewer,
        Instant decidedAt) {

    /**
     * 校验状态更新命令。
     */
    public ApprovalStatusUpdateCommand {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        id = Objects.requireNonNull(id, "id");
        nextStatus = Objects.requireNonNull(nextStatus, "nextStatus");
        if (nextStatus == ApprovalStatus.PENDING) {
            throw new IllegalArgumentException("nextStatus must not be PENDING");
        }
        reviewer = reviewer == null ? ApprovalReviewer.none() : reviewer;
    }
}

package com.guidinglight.decisionhub.usecase.qdr.approval;

import com.guidinglight.decisionhub.domain.qdr.approval.HumanApprovalPacket;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * B4 Human Approval Packet API-safe usecase view。
 *
 * <p>该 view 不暴露 tenantId，不包含 raw provider response、raw prompt、credential 或任何可执行交易字段；
 * `approvalStatus=APPROVED` 仍只表示人工审查状态。
 */
public record ApprovalPacketView(
        String approvalPacketId,
        String decisionRunId,
        String approvalKey,
        String approvalType,
        String approvalStatus,
        String riskLevel,
        String decisionAction,
        BigDecimal confidenceScore,
        String summary,
        Map<String, Object> checklistJson,
        Map<String, Object> evidenceRefsJson,
        String reviewerId,
        String reviewerNote,
        Instant decidedAt,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * 从 domain 聚合生成 API-safe view。
     *
     * @param packet DH 内部 approval packet 聚合。
     * @return 不含 tenantId 和执行字段的 view。
     */
    public static ApprovalPacketView from(final HumanApprovalPacket packet) {
        final HumanApprovalPacket checked = Objects.requireNonNull(packet, "packet");
        return new ApprovalPacketView(
                checked.id().value().toString(),
                checked.decisionRunId().toString(),
                checked.approvalKey().value(),
                checked.approvalType().name(),
                checked.approvalStatus().name(),
                checked.riskLevel().name(),
                checked.decisionAction().name(),
                checked.confidenceScore(),
                checked.summary(),
                checked.checklist().items(),
                checked.evidenceRefs() == null ? Map.of() : checked.evidenceRefs().refs(),
                checked.reviewer().reviewerId(),
                checked.reviewer().reviewerNote(),
                checked.decidedAt(),
                checked.createdAt(),
                checked.updatedAt());
    }
}

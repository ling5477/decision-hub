package com.guidinglight.decisionhub.domain.qdr.approval;

import com.guidinglight.decisionhub.domain.qdr.QuantDecisionAction;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Human Approval Packet 聚合。
 *
 * <p>该对象只表示 DH 内部人工审查证据。`approvalStatus=APPROVED` 不等于 `BUY`，
 * `approvalStatus=REJECTED` 不等于 `SELL`，`decisionAction` 只允许 QDR 只读 action 集合，
 * 不触发 NQ、订单、风控、账本、Paper 或 LIVE mutation。
 */
public record HumanApprovalPacket(
        HumanApprovalPacketId id,
        UUID decisionRunId,
        String tenantId,
        String traceId,
        String requestId,
        ApprovalKey approvalKey,
        ApprovalType approvalType,
        ApprovalStatus approvalStatus,
        RiskLevel riskLevel,
        QuantDecisionAction decisionAction,
        BigDecimal confidenceScore,
        String summary,
        ApprovalChecklist checklist,
        ApprovalEvidenceRefs evidenceRefs,
        ApprovalReviewer reviewer,
        Instant decidedAt,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * 校验审批包的 tenant、trace、action、risk、confidence 与脱敏内容。
     */
    public HumanApprovalPacket {
        id = Objects.requireNonNull(id, "id");
        decisionRunId = Objects.requireNonNull(decisionRunId, "decisionRunId");
        tenantId = ApprovalContentGuard.requiredText(tenantId, "tenantId");
        traceId = ApprovalContentGuard.requiredText(traceId, "traceId");
        requestId = ApprovalContentGuard.requiredText(requestId, "requestId");
        approvalKey = Objects.requireNonNull(approvalKey, "approvalKey");
        approvalType = Objects.requireNonNull(approvalType, "approvalType");
        approvalStatus = Objects.requireNonNull(approvalStatus, "approvalStatus");
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel");
        decisionAction = Objects.requireNonNull(decisionAction, "decisionAction");
        confidenceScore = normalizeConfidence(confidenceScore);
        ApprovalContentGuard.rejectSecretLike("summary", summary);
        checklist = Objects.requireNonNull(checklist, "checklist");
        reviewer = reviewer == null ? ApprovalReviewer.none() : reviewer;
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        if (approvalStatus.terminal() && decidedAt == null) {
            throw new IllegalArgumentException("decidedAt is required for terminal approval status");
        }
    }

    /**
     * 从数据库或外部 contract 字符串解析只读 decision action。
     *
     * @param value action 字符串。
     * @return QDR 只读 action。
     */
    public static QuantDecisionAction decisionActionFrom(final String value) {
        final String checked = ApprovalContentGuard.requiredText(value, "decisionAction");
        return switch (checked) {
            case "OBSERVE" -> QuantDecisionAction.OBSERVE;
            case "NO_TRADE" -> QuantDecisionAction.NO_TRADE;
            case "LONG_BIAS" -> QuantDecisionAction.LONG_BIAS;
            case "SHORT_BIAS" -> QuantDecisionAction.SHORT_BIAS;
            case "NEEDS_REVIEW" -> QuantDecisionAction.NEEDS_REVIEW;
            case "REJECTED" -> QuantDecisionAction.REJECTED;
            default -> throw new IllegalArgumentException("decisionAction is not allowed: " + checked);
        };
    }

    /**
     * 按状态机生成新状态副本；该方法只修改审批字段，不修改 decision action。
     */
    HumanApprovalPacket withStatus(
            final ApprovalStatus nextStatus,
            final ApprovalReviewer nextReviewer,
            final Instant nextDecidedAt,
            final Instant nextUpdatedAt) {
        return new HumanApprovalPacket(
                id,
                decisionRunId,
                tenantId,
                traceId,
                requestId,
                approvalKey,
                approvalType,
                nextStatus,
                riskLevel,
                decisionAction,
                confidenceScore,
                summary,
                checklist,
                evidenceRefs,
                nextReviewer,
                nextDecidedAt,
                createdAt,
                nextUpdatedAt);
    }

    private static BigDecimal normalizeConfidence(final BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("confidenceScore must be between 0 and 1");
        }
        return value;
    }
}

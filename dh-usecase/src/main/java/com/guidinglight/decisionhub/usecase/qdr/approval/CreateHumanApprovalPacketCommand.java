package com.guidinglight.decisionhub.usecase.qdr.approval;

import com.guidinglight.decisionhub.domain.qdr.QuantDecisionAction;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalChecklist;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalEvidenceRefs;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalKey;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalType;
import com.guidinglight.decisionhub.domain.qdr.approval.HumanApprovalPacketId;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * 创建 Human Approval Packet 的 usecase command。
 *
 * <p>创建命令永远只能创建 `PENDING` 审批包，防止默认 APPROVED 或自动审批。
 */
public record CreateHumanApprovalPacketCommand(
        HumanApprovalPacketId id,
        UUID decisionRunId,
        String tenantId,
        String traceId,
        String requestId,
        ApprovalKey approvalKey,
        ApprovalType approvalType,
        RiskLevel riskLevel,
        QuantDecisionAction decisionAction,
        BigDecimal confidenceScore,
        String summary,
        ApprovalChecklist checklist,
        ApprovalEvidenceRefs evidenceRefs) {

    /**
     * 校验创建命令关键字段。
     */
    public CreateHumanApprovalPacketCommand {
        id = Objects.requireNonNull(id, "id");
        decisionRunId = Objects.requireNonNull(decisionRunId, "decisionRunId");
        approvalKey = Objects.requireNonNull(approvalKey, "approvalKey");
        approvalType = Objects.requireNonNull(approvalType, "approvalType");
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel");
        decisionAction = Objects.requireNonNull(decisionAction, "decisionAction");
        checklist = Objects.requireNonNull(checklist, "checklist");
    }
}

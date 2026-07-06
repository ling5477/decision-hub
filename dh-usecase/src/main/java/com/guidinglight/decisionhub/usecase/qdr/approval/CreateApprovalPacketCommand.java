package com.guidinglight.decisionhub.usecase.qdr.approval;

import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalType;

import java.util.Map;
import java.util.Objects;

/**
 * B4 create approval packet API 对应的 usecase command。
 *
 * <p>该 command 只接受已认证 tenant / requester 上下文和脱敏人工审查材料；tenantId 不来自请求 body，
 * approvalStatus、decisionAction、traceId、final execution action 均不允许由客户端指定。
 */
public record CreateApprovalPacketCommand(
        String tenantId,
        String decisionRunId,
        ApprovalType approvalType,
        String summary,
        Map<String, Object> checklistJson,
        Map<String, Object> evidenceRefsJson,
        String requesterId,
        String traceId) {

    /**
     * 校验命令边界，确保创建请求 fail-closed。
     */
    public CreateApprovalPacketCommand {
        tenantId = ApprovalCommandValidation.requireText(tenantId, "tenantId");
        decisionRunId = ApprovalCommandValidation.requireUuidText(decisionRunId, "decisionRunId");
        approvalType = Objects.requireNonNull(approvalType, "approvalType");
        summary = ApprovalCommandValidation.optionalSafeText(summary, "summary");
        checklistJson = ApprovalCommandValidation.safeMap(checklistJson, "checklistJson");
        evidenceRefsJson = ApprovalCommandValidation.safeMap(evidenceRefsJson, "evidenceRefsJson");
        requesterId = ApprovalCommandValidation.optionalSafeText(requesterId, "requesterId");
        traceId = ApprovalCommandValidation.optionalSafeText(traceId, "traceId");
    }
}

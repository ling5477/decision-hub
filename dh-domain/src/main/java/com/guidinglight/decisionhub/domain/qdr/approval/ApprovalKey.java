package com.guidinglight.decisionhub.domain.qdr.approval;

/**
 * 租户内 Human Approval Packet 幂等 key。
 *
 * <p>该 key 必须由上层以可追踪方式生成，并与 `tenantId` 组成唯一约束；空 key 必须 fail-closed。
 */
public record ApprovalKey(String value) {

    /**
     * 校验 approval key 非空。
     */
    public ApprovalKey {
        value = ApprovalContentGuard.requiredText(value, "approvalKey");
    }
}

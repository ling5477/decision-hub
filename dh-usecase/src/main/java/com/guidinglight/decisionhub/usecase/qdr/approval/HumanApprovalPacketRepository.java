package com.guidinglight.decisionhub.usecase.qdr.approval;

import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalKey;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalReviewer;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalStatus;
import com.guidinglight.decisionhub.domain.qdr.approval.HumanApprovalPacket;
import com.guidinglight.decisionhub.domain.qdr.approval.HumanApprovalPacketId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Human Approval Packet repository port。
 *
 * <p>所有查询和更新必须 tenant-bound；禁止只用 UUID 查询或更新。实现不得触发外部 HTTP、provider、NQ、
 * 订单、风控、账本、Paper 或 LIVE 副作用。
 */
public interface HumanApprovalPacketRepository {

    /**
     * 保存审批包。
     *
     * @param packet 审批包。
     * @return 已保存审批包。
     */
    HumanApprovalPacket save(HumanApprovalPacket packet);

    /**
     * 按租户和 approval key 查询审批包。
     *
     * @param tenantId 租户 ID。
     * @param approvalKey approval key。
     * @return 命中的审批包。
     */
    Optional<HumanApprovalPacket> findByTenantAndApprovalKey(String tenantId, ApprovalKey approvalKey);

    /**
     * 按租户和审批包 ID 查询。
     *
     * @param tenantId 租户 ID。
     * @param id 审批包 ID。
     * @return 命中的审批包。
     */
    Optional<HumanApprovalPacket> findByTenantAndId(String tenantId, HumanApprovalPacketId id);

    /**
     * 按租户和 decision run ID 查询审批包。
     *
     * @param tenantId 租户 ID。
     * @param decisionRunId decision run ID。
     * @return 审批包列表。
     */
    List<HumanApprovalPacket> findByDecisionRunId(String tenantId, UUID decisionRunId);

    /**
     * 按租户、ID 与当前状态更新审批状态。
     *
     * @param tenantId 租户 ID。
     * @param id 审批包 ID。
     * @param previousStatus 当前状态，用于防止无条件覆盖。
     * @param nextStatus 目标状态。
     * @param reviewer 审查人信息。
     * @param decidedAt 决策时间。
     * @param updatedAt 更新时间。
     */
    void updateStatus(
            String tenantId,
            HumanApprovalPacketId id,
            ApprovalStatus previousStatus,
            ApprovalStatus nextStatus,
            ApprovalReviewer reviewer,
            Instant decidedAt,
            Instant updatedAt);
}

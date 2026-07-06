package com.guidinglight.decisionhub.domain.qdr.approval;

import java.util.Objects;
import java.util.UUID;

/**
 * Human Approval Packet 主键。
 *
 * <p>该 ID 不得单独用于 repository 查询或更新；所有持久化访问必须同时携带 tenantId。
 */
public record HumanApprovalPacketId(UUID value) {

    /**
     * 校验 UUID 非空。
     */
    public HumanApprovalPacketId {
        value = Objects.requireNonNull(value, "value");
    }
}

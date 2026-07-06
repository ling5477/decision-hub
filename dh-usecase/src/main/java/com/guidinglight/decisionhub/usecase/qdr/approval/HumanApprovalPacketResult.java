package com.guidinglight.decisionhub.usecase.qdr.approval;

import com.guidinglight.decisionhub.domain.qdr.approval.HumanApprovalPacket;

import java.util.Objects;

/**
 * Human Approval Packet usecase 返回值。
 *
 * <p>结果只包装 DH 内部审批证据，不表示交易授权、不触发外部副作用。
 */
public record HumanApprovalPacketResult(HumanApprovalPacket packet) {

    /**
     * 校验返回包非空。
     */
    public HumanApprovalPacketResult {
        packet = Objects.requireNonNull(packet, "packet");
    }
}

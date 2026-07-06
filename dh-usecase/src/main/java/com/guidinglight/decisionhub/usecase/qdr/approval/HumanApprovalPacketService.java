package com.guidinglight.decisionhub.usecase.qdr.approval;

import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalReviewer;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalStatus;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalStatusTransitionPolicy;
import com.guidinglight.decisionhub.domain.qdr.approval.HumanApprovalPacket;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Human Approval Packet 内部 usecase service。
 *
 * <p>本 service 只创建 DH 内部人工审查证据并按状态机更新审批状态；不暴露 API、不调用 HTTP、
 * 不连接 provider、不触发 NQ、不触发交易、风控、账本、Paper 或 LIVE mutation。
 */
public final class HumanApprovalPacketService {

    private final HumanApprovalPacketRepository repository;
    private final ApprovalStatusTransitionPolicy transitionPolicy;
    private final Clock clock;

    /**
     * 创建 service。
     *
     * @param repository approval repository port。
     * @param transitionPolicy 状态机策略。
     * @param clock 时钟。
     */
    public HumanApprovalPacketService(
            final HumanApprovalPacketRepository repository,
            final ApprovalStatusTransitionPolicy transitionPolicy,
            final Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.transitionPolicy = Objects.requireNonNull(transitionPolicy, "transitionPolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 创建 PENDING 审批包。
     *
     * @param command 创建命令。
     * @return 创建结果。
     */
    public HumanApprovalPacketResult create(final CreateHumanApprovalPacketCommand command) {
        final CreateHumanApprovalPacketCommand checked = Objects.requireNonNull(command, "command");
        final Instant now = Instant.now(clock);
        final HumanApprovalPacket packet =
                new HumanApprovalPacket(
                        checked.id(),
                        checked.decisionRunId(),
                        checked.tenantId(),
                        checked.traceId(),
                        checked.requestId(),
                        checked.approvalKey(),
                        checked.approvalType(),
                        ApprovalStatus.PENDING,
                        checked.riskLevel(),
                        checked.decisionAction(),
                        checked.confidenceScore(),
                        checked.summary(),
                        checked.checklist(),
                        checked.evidenceRefs(),
                        ApprovalReviewer.none(),
                        null,
                        now,
                        now);
        return new HumanApprovalPacketResult(repository.save(packet));
    }

    /**
     * 按状态机更新审批包状态。
     *
     * @param command 状态更新命令。
     * @return 更新后的审批包快照。
     */
    public HumanApprovalPacketResult updateStatus(final ApprovalStatusUpdateCommand command) {
        final ApprovalStatusUpdateCommand checked = Objects.requireNonNull(command, "command");
        final HumanApprovalPacket current =
                repository
                        .findByTenantAndId(checked.tenantId(), checked.id())
                        .orElseThrow(
                                () ->
                                        new HumanApprovalPacketPersistenceException(
                                                "human approval packet not found",
                                                new IllegalStateException("missing approval packet")));
        final Instant decidedAt = checked.decidedAt() == null ? Instant.now(clock) : checked.decidedAt();
        final HumanApprovalPacket transitioned =
                transitionPolicy.transition(
                        current,
                        checked.nextStatus(),
                        checked.reviewer(),
                        decidedAt,
                        Instant.now(clock));
        repository.updateStatus(
                checked.tenantId(),
                checked.id(),
                current.approvalStatus(),
                transitioned.approvalStatus(),
                transitioned.reviewer(),
                transitioned.decidedAt(),
                transitioned.updatedAt());
        return new HumanApprovalPacketResult(transitioned);
    }
}

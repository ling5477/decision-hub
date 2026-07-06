package com.guidinglight.decisionhub.domain.qdr.approval;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Human Approval Packet 状态机策略。
 *
 * <p>合法转移只允许从 PENDING 或 NEEDS_REVIEW 进入人工决策或过期状态；APPROVED / REJECTED /
 * EXPIRED 均为终态。策略不触发任何外部 HTTP、provider、NQ 或交易副作用。
 */
public final class ApprovalStatusTransitionPolicy {

    private static final Set<Transition> ALLOWED = Set.of(
            new Transition(ApprovalStatus.PENDING, ApprovalStatus.APPROVED),
            new Transition(ApprovalStatus.PENDING, ApprovalStatus.REJECTED),
            new Transition(ApprovalStatus.PENDING, ApprovalStatus.NEEDS_REVIEW),
            new Transition(ApprovalStatus.PENDING, ApprovalStatus.EXPIRED),
            new Transition(ApprovalStatus.NEEDS_REVIEW, ApprovalStatus.APPROVED),
            new Transition(ApprovalStatus.NEEDS_REVIEW, ApprovalStatus.REJECTED),
            new Transition(ApprovalStatus.NEEDS_REVIEW, ApprovalStatus.EXPIRED));

    /**
     * 校验状态机转移是否合法。
     *
     * @param from 当前状态。
     * @param to 目标状态。
     */
    public void validate(final ApprovalStatus from, final ApprovalStatus to) {
        final ApprovalStatus checkedFrom = Objects.requireNonNull(from, "from");
        final ApprovalStatus checkedTo = Objects.requireNonNull(to, "to");
        if (!ALLOWED.contains(new Transition(checkedFrom, checkedTo))) {
            throw new ApprovalStatusTransitionException(
                    "approval transition denied: " + checkedFrom + " -> " + checkedTo);
        }
    }

    /**
     * 按状态机规则生成转移后的审批包。
     *
     * @param packet 当前审批包。
     * @param to 目标状态。
     * @param reviewer 审查人；APPROVED / REJECTED / NEEDS_REVIEW 必须存在。
     * @param decidedAt 决策时间。
     * @param updatedAt 更新时间。
     * @return 转移后的审批包。
     */
    public HumanApprovalPacket transition(
            final HumanApprovalPacket packet,
            final ApprovalStatus to,
            final ApprovalReviewer reviewer,
            final Instant decidedAt,
            final Instant updatedAt) {
        final HumanApprovalPacket checkedPacket = Objects.requireNonNull(packet, "packet");
        final ApprovalStatus checkedTo = Objects.requireNonNull(to, "to");
        validate(checkedPacket.approvalStatus(), checkedTo);
        final ApprovalReviewer checkedReviewer =
                reviewer == null ? ApprovalReviewer.none() : reviewer;
        if (checkedTo != ApprovalStatus.EXPIRED && !checkedReviewer.present()) {
            throw new ApprovalStatusTransitionException("approval reviewer is required for " + checkedTo);
        }
        return checkedPacket.withStatus(checkedTo, checkedReviewer, decidedAt, updatedAt);
    }

    private record Transition(ApprovalStatus from, ApprovalStatus to) {
    }
}

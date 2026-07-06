package com.guidinglight.decisionhub.domain.qdr.approval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.domain.qdr.QuantDecisionAction;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Human Approval Packet 状态机回归。
 *
 * <p>确保非法转移 fail-closed，终态不可重开，且不存在 reviewer 缺失时自动通过。
 */
class ApprovalStatusTransitionPolicyTest {

    private static final Instant NOW = Instant.parse("2026-07-06T00:00:00Z");
    private final ApprovalStatusTransitionPolicy policy = new ApprovalStatusTransitionPolicy();

    @Test
    void pendingToApprovedPasses() {
        assertTransition(ApprovalStatus.PENDING, ApprovalStatus.APPROVED);
    }

    @Test
    void pendingToRejectedPasses() {
        assertTransition(ApprovalStatus.PENDING, ApprovalStatus.REJECTED);
    }

    @Test
    void pendingToNeedsReviewPasses() {
        assertTransition(ApprovalStatus.PENDING, ApprovalStatus.NEEDS_REVIEW);
    }

    @Test
    void needsReviewToApprovedPasses() {
        assertTransition(ApprovalStatus.NEEDS_REVIEW, ApprovalStatus.APPROVED);
    }

    @Test
    void needsReviewToRejectedPasses() {
        assertTransition(ApprovalStatus.NEEDS_REVIEW, ApprovalStatus.REJECTED);
    }

    @Test
    void approvedToRejectedFails() {
        assertDenied(ApprovalStatus.APPROVED, ApprovalStatus.REJECTED);
    }

    @Test
    void rejectedToApprovedFails() {
        assertDenied(ApprovalStatus.REJECTED, ApprovalStatus.APPROVED);
    }

    @Test
    void expiredToApprovedFails() {
        assertDenied(ApprovalStatus.EXPIRED, ApprovalStatus.APPROVED);
    }

    @Test
    void nullTransitionFails() {
        assertThrows(NullPointerException.class, () -> policy.validate(ApprovalStatus.PENDING, null));
    }

    @Test
    void reviewerMissingDoesNotAutoApprove() {
        assertThrows(
                ApprovalStatusTransitionException.class,
                () ->
                        policy.transition(
                                packet(ApprovalStatus.PENDING),
                                ApprovalStatus.APPROVED,
                                ApprovalReviewer.none(),
                                NOW,
                                NOW));
    }

    private void assertTransition(final ApprovalStatus from, final ApprovalStatus to) {
        final HumanApprovalPacket transitioned =
                policy.transition(
                        packet(from),
                        to,
                        new ApprovalReviewer("reviewer-a", "reviewed"),
                        NOW,
                        NOW);

        assertEquals(to, transitioned.approvalStatus());
    }

    private void assertDenied(final ApprovalStatus from, final ApprovalStatus to) {
        assertThrows(
                ApprovalStatusTransitionException.class,
                () ->
                        policy.transition(
                                packet(from),
                                to,
                                new ApprovalReviewer("reviewer-a", "reviewed"),
                                NOW,
                                NOW));
    }

    private static HumanApprovalPacket packet(final ApprovalStatus status) {
        return new HumanApprovalPacket(
                new HumanApprovalPacketId(UUID.randomUUID()),
                UUID.randomUUID(),
                "tenant-a",
                "trace-a",
                "request-a",
                new ApprovalKey("approval-key-a-" + status),
                ApprovalType.QUANT_DECISION_REVIEW,
                status,
                RiskLevel.LOW,
                QuantDecisionAction.OBSERVE,
                new BigDecimal("0.5000"),
                "readonly summary",
                new ApprovalChecklist(Map.of("riskReviewed", true)),
                new ApprovalEvidenceRefs(Map.of("trace", "trace://trace-a")),
                status == ApprovalStatus.PENDING || status == ApprovalStatus.NEEDS_REVIEW
                        ? ApprovalReviewer.none()
                        : new ApprovalReviewer("reviewer-a", "reviewed"),
                status.terminal() ? NOW : null,
                NOW,
                NOW);
    }
}

package com.guidinglight.decisionhub.domain.qdr.approval;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.domain.qdr.QuantDecisionAction;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Human Approval Packet domain 校验回归。
 *
 * <p>覆盖必填字段、敏感内容、confidence 边界与交易动作禁用，确保 approval 只是人工审查证据。
 */
class HumanApprovalPacketTest {

    private static final Instant NOW = Instant.parse("2026-07-06T00:00:00Z");

    @Test
    void validPacketPasses() {
        assertDoesNotThrow(HumanApprovalPacketTest::packet);
    }

    @Test
    void blankTenantFailsClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> packetBuilder(" ", new ApprovalKey("approval-key-a")));
    }

    @Test
    void blankApprovalKeyFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> new ApprovalKey(" "));
    }

    @Test
    void executableDecisionActionsFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> HumanApprovalPacket.decisionActionFrom("BUY"));
        assertThrows(IllegalArgumentException.class, () -> HumanApprovalPacket.decisionActionFrom("SELL"));
        assertThrows(
                IllegalArgumentException.class,
                () -> HumanApprovalPacket.decisionActionFrom("PLACE_ORDER"));
        assertThrows(
                IllegalArgumentException.class,
                () -> HumanApprovalPacket.decisionActionFrom("CANCEL_ORDER"));
    }

    @Test
    void confidenceOutsideZeroToOneFailsClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> packetWithConfidence(new BigDecimal("-0.0001")));
        assertThrows(
                IllegalArgumentException.class,
                () -> packetWithConfidence(new BigDecimal("1.0001")));
    }

    @Test
    void summarySecretLikeContentFailsClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new HumanApprovalPacket(
                                id(),
                                UUID.randomUUID(),
                                "tenant-a",
                                "trace-a",
                                "request-a",
                                new ApprovalKey("approval-key-a"),
                                ApprovalType.QUANT_DECISION_REVIEW,
                                ApprovalStatus.PENDING,
                                RiskLevel.LOW,
                                QuantDecisionAction.OBSERVE,
                                new BigDecimal("0.5000"),
                                "contains apiKey material",
                                checklist(),
                                evidenceRefs(),
                                ApprovalReviewer.none(),
                                null,
                                NOW,
                                NOW));
    }

    @Test
    void evidenceRefsSecretLikeContentFailsClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ApprovalEvidenceRefs(Map.of("credentialRef", "secret-ref")));
    }

    @Test
    void checklistSecretLikeContentFailsClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ApprovalChecklist(Map.of("cookie", "session-value")));
    }

    @Test
    void approvedAndRejectedAreNotBuyOrSell() {
        assertNotEquals("BUY", ApprovalDecision.APPROVED.name());
        assertNotEquals("SELL", ApprovalDecision.REJECTED.name());
        assertEquals(ApprovalStatus.APPROVED, ApprovalDecision.APPROVED.targetStatus());
        assertEquals(ApprovalStatus.REJECTED, ApprovalDecision.REJECTED.targetStatus());
    }

    private static HumanApprovalPacket packet() {
        return packetBuilder("tenant-a", new ApprovalKey("approval-key-a"));
    }

    private static HumanApprovalPacket packetBuilder(final String tenantId, final ApprovalKey approvalKey) {
        return new HumanApprovalPacket(
                id(),
                UUID.randomUUID(),
                tenantId,
                "trace-a",
                "request-a",
                approvalKey,
                ApprovalType.QUANT_DECISION_REVIEW,
                ApprovalStatus.PENDING,
                RiskLevel.LOW,
                QuantDecisionAction.OBSERVE,
                new BigDecimal("0.5000"),
                "readonly summary",
                checklist(),
                evidenceRefs(),
                ApprovalReviewer.none(),
                null,
                NOW,
                NOW);
    }

    private static HumanApprovalPacket packetWithConfidence(final BigDecimal confidence) {
        return new HumanApprovalPacket(
                id(),
                UUID.randomUUID(),
                "tenant-a",
                "trace-a",
                "request-a",
                new ApprovalKey("approval-key-a"),
                ApprovalType.QUANT_DECISION_REVIEW,
                ApprovalStatus.PENDING,
                RiskLevel.LOW,
                QuantDecisionAction.OBSERVE,
                confidence,
                "readonly summary",
                checklist(),
                evidenceRefs(),
                ApprovalReviewer.none(),
                null,
                NOW,
                NOW);
    }

    private static HumanApprovalPacketId id() {
        return new HumanApprovalPacketId(UUID.randomUUID());
    }

    private static ApprovalChecklist checklist() {
        return new ApprovalChecklist(Map.of("riskReviewed", true));
    }

    private static ApprovalEvidenceRefs evidenceRefs() {
        return new ApprovalEvidenceRefs(Map.of("trace", "trace://trace-a"));
    }
}

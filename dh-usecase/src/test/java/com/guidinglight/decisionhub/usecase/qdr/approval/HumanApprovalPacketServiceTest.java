package com.guidinglight.decisionhub.usecase.qdr.approval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.domain.qdr.QuantDecisionAction;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalChecklist;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalEvidenceRefs;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalKey;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalReviewer;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalStatus;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalStatusTransitionException;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalStatusTransitionPolicy;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalType;
import com.guidinglight.decisionhub.domain.qdr.approval.HumanApprovalPacket;
import com.guidinglight.decisionhub.domain.qdr.approval.HumanApprovalPacketId;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * HumanApprovalPacketService 回归测试。
 *
 * <p>覆盖创建 PENDING、tenant-bound 查询、状态机拒绝与 repository 更新条件，不暴露任何 API。
 */
class HumanApprovalPacketServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-06T00:00:00Z");
    private static final String TENANT_ID = "tenant-a";
    private static final HumanApprovalPacketId PACKET_ID =
            new HumanApprovalPacketId(UUID.fromString("00000000-0000-0000-0000-000000000101"));

    private RecordingRepository repository;
    private HumanApprovalPacketService service;

    @BeforeEach
    void setUp() {
        repository = new RecordingRepository();
        service =
                new HumanApprovalPacketService(
                        repository,
                        new ApprovalStatusTransitionPolicy(),
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createAlwaysCreatesPendingPacket() {
        final HumanApprovalPacketResult result = service.create(createCommand());

        assertEquals(ApprovalStatus.PENDING, result.packet().approvalStatus());
        assertEquals(ApprovalStatus.PENDING, repository.saved.approvalStatus());
    }

    @Test
    void updateStatusUsesTenantBoundRepositoryAndCurrentStatus() {
        repository.packet = packet(ApprovalStatus.PENDING);

        final HumanApprovalPacketResult result =
                service.updateStatus(
                        new ApprovalStatusUpdateCommand(
                                TENANT_ID,
                                PACKET_ID,
                                ApprovalStatus.APPROVED,
                                new ApprovalReviewer("reviewer-a", "reviewed"),
                                NOW));

        assertEquals(ApprovalStatus.APPROVED, result.packet().approvalStatus());
        assertEquals(TENANT_ID, repository.updatedTenantId);
        assertEquals(PACKET_ID, repository.updatedId);
        assertEquals(ApprovalStatus.PENDING, repository.previousStatus);
        assertEquals(ApprovalStatus.APPROVED, repository.nextStatus);
    }

    @Test
    void terminalInvalidTransitionFailsBeforeRepositoryUpdate() {
        repository.packet = packet(ApprovalStatus.APPROVED);

        assertThrows(
                ApprovalStatusTransitionException.class,
                () ->
                        service.updateStatus(
                                new ApprovalStatusUpdateCommand(
                                        TENANT_ID,
                                        PACKET_ID,
                                        ApprovalStatus.REJECTED,
                                        new ApprovalReviewer("reviewer-a", "reviewed"),
                                        NOW)));
        assertEquals(0, repository.updateCalls);
    }

    @Test
    void missingPacketFailsClosed() {
        assertThrows(
                HumanApprovalPacketPersistenceException.class,
                () ->
                        service.updateStatus(
                                new ApprovalStatusUpdateCommand(
                                        TENANT_ID,
                                        PACKET_ID,
                                        ApprovalStatus.APPROVED,
                                        new ApprovalReviewer("reviewer-a", "reviewed"),
                                        NOW)));
    }

    private static CreateHumanApprovalPacketCommand createCommand() {
        return new CreateHumanApprovalPacketCommand(
                PACKET_ID,
                UUID.fromString("00000000-0000-0000-0000-000000000102"),
                TENANT_ID,
                "trace-a",
                "request-a",
                new ApprovalKey("approval-key-a"),
                ApprovalType.QUANT_DECISION_REVIEW,
                RiskLevel.LOW,
                QuantDecisionAction.OBSERVE,
                new BigDecimal("0.5000"),
                "readonly summary",
                new ApprovalChecklist(Map.of("riskReviewed", true)),
                new ApprovalEvidenceRefs(Map.of("trace", "trace://trace-a")));
    }

    private static HumanApprovalPacket packet(final ApprovalStatus status) {
        return new HumanApprovalPacket(
                PACKET_ID,
                UUID.fromString("00000000-0000-0000-0000-000000000102"),
                TENANT_ID,
                "trace-a",
                "request-a",
                new ApprovalKey("approval-key-a"),
                ApprovalType.QUANT_DECISION_REVIEW,
                status,
                RiskLevel.LOW,
                QuantDecisionAction.OBSERVE,
                new BigDecimal("0.5000"),
                "readonly summary",
                new ApprovalChecklist(Map.of("riskReviewed", true)),
                new ApprovalEvidenceRefs(Map.of("trace", "trace://trace-a")),
                status.terminal()
                        ? new ApprovalReviewer("reviewer-a", "reviewed")
                        : ApprovalReviewer.none(),
                status.terminal() ? NOW : null,
                NOW,
                NOW);
    }

    private static final class RecordingRepository implements HumanApprovalPacketRepository {
        private final Map<HumanApprovalPacketId, HumanApprovalPacket> packets = new LinkedHashMap<>();
        private HumanApprovalPacket packet;
        private HumanApprovalPacket saved;
        private String updatedTenantId;
        private HumanApprovalPacketId updatedId;
        private ApprovalStatus previousStatus;
        private ApprovalStatus nextStatus;
        private int updateCalls;

        @Override
        public HumanApprovalPacket save(final HumanApprovalPacket packet) {
            saved = packet;
            packets.put(packet.id(), packet);
            return packet;
        }

        @Override
        public Optional<HumanApprovalPacket> findByTenantAndApprovalKey(
                final String tenantId, final ApprovalKey approvalKey) {
            return packets.values().stream()
                    .filter(
                            p ->
                                    p.tenantId().equals(tenantId)
                                            && p.approvalKey().equals(approvalKey))
                    .findFirst();
        }

        @Override
        public Optional<HumanApprovalPacket> findByTenantAndId(
                final String tenantId, final HumanApprovalPacketId id) {
            return Optional.ofNullable(packet).filter(p -> p.tenantId().equals(tenantId) && p.id().equals(id));
        }

        @Override
        public List<HumanApprovalPacket> findByDecisionRunId(
                final String tenantId, final UUID decisionRunId) {
            return packets.values().stream()
                    .filter(p -> p.tenantId().equals(tenantId) && p.decisionRunId().equals(decisionRunId))
                    .toList();
        }

        @Override
        public void updateStatus(
                final String tenantId,
                final HumanApprovalPacketId id,
                final ApprovalStatus previousStatus,
                final ApprovalStatus nextStatus,
                final ApprovalReviewer reviewer,
                final Instant decidedAt,
                final Instant updatedAt) {
            updateCalls++;
            updatedTenantId = tenantId;
            updatedId = id;
            this.previousStatus = previousStatus;
            this.nextStatus = nextStatus;
        }
    }
}

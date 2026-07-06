package com.guidinglight.decisionhub.usecase.qdr.approval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.domain.qdr.DecisionRunStatus;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalDecision;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalKey;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalReviewer;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalStatus;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalStatusTransitionException;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalStatusTransitionPolicy;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalType;
import com.guidinglight.decisionhub.domain.qdr.approval.HumanApprovalPacket;
import com.guidinglight.decisionhub.domain.qdr.approval.HumanApprovalPacketId;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventType;
import com.guidinglight.decisionhub.usecase.decision.DecisionPersistenceRecords;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionEvidenceReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionEvidenceView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelQueryPort;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelService;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunDetailView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionTraceReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionTraceTimelineView;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * B4 HumanApprovalPacketCommandService 回归测试。
 *
 * <p>覆盖 tenant-bound create / decision submit、状态机、audit fail-closed、repository fail-closed 与
 * no-live-order 语义；测试不接 API、JDBC、HTTP、provider、NQ、replay execution 或 LIVE。
 */
final class HumanApprovalPacketCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-06T00:00:00Z");
    private static final String TENANT_ID = "tenant-a";
    private static final String RUN_ID = "00000000-0000-0000-0000-000000000201";
    private static final UUID PACKET_UUID =
            UUID.fromString("00000000-0000-0000-0000-000000000202");

    private RecordingApprovalRepository approvalRepository;
    private RecordingReadModelPort readModelPort;
    private RecordingAuditRepository auditRepository;
    private HumanApprovalPacketCommandService commandService;

    @BeforeEach
    void setUp() {
        approvalRepository = new RecordingApprovalRepository();
        readModelPort = new RecordingReadModelPort();
        readModelPort.detail = Optional.of(validDetail());
        auditRepository = new RecordingAuditRepository();
        final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        final HumanApprovalPacketService packetService =
                new HumanApprovalPacketService(
                        approvalRepository, new ApprovalStatusTransitionPolicy(), clock);
        commandService =
                new HumanApprovalPacketCommandService(
                        approvalRepository,
                        packetService,
                        new DecisionReadModelService(readModelPort),
                        auditRepository,
                        clock,
                        new FixedUuidSupplier(PACKET_UUID));
    }

    @Test
    void createApprovalPacketSuccessWritesPendingPacketAndAudit() {
        final ApprovalPacketView view = commandService.create(createCommand());

        assertEquals(PACKET_UUID.toString(), view.approvalPacketId());
        assertEquals(ApprovalStatus.PENDING.name(), view.approvalStatus());
        assertEquals("OBSERVE", view.decisionAction());
        assertEquals("LOW", view.riskLevel());
        assertEquals(TENANT_ID, readModelPort.lastDetailQuery.tenantId());
        assertEquals(1, auditRepository.auditEvents.size());
        assertEquals(
                DecisionAuditEventType.HUMAN_APPROVAL_PACKET_CREATED,
                auditRepository.auditEvents.getFirst().eventType());
    }

    @Test
    void createApprovalPacketTenantBoundMissingRunFailsClosed() {
        readModelPort.detail = Optional.empty();

        assertThrows(HumanApprovalPacketNotFoundException.class, () -> commandService.create(createCommand()));
        assertEquals(0, approvalRepository.saveCalls);
        assertEquals(0, auditRepository.auditEvents.size());
    }

    @Test
    void createApprovalPacketDuplicateKeyFailsClosed() {
        commandService.create(createCommand());

        assertThrows(HumanApprovalPacketDuplicateException.class, () -> commandService.create(createCommand()));
        assertEquals(1, approvalRepository.saveCalls);
    }

    @Test
    void createApprovalPacketRepositoryFailureFailsClosed() {
        approvalRepository.saveFailure = new HumanApprovalPacketPersistenceException("repo down", null);

        assertThrows(HumanApprovalPacketPersistenceException.class, () -> commandService.create(createCommand()));
        assertEquals(0, auditRepository.auditEvents.size());
    }

    @Test
    void createApprovalPacketAuditFailureFailsClosed() {
        auditRepository.auditFailure = new RuntimeException("audit down");

        assertThrows(HumanApprovalPacketAuditException.class, () -> commandService.create(createCommand()));
        assertEquals(1, approvalRepository.saveCalls);
    }

    @Test
    void createApprovalPacketAuditFailureCanRollbackThroughWriteBoundary() {
        auditRepository.auditFailure = new RuntimeException("audit down");
        commandService = commandServiceWithRollbackBoundary();

        assertThrows(HumanApprovalPacketAuditException.class, () -> commandService.create(createCommand()));
        assertEquals(0, approvalRepository.packets.size());
    }

    @Test
    void createApprovalPacketDoesNotTriggerReplayOrExternalCall() {
        commandService.create(createCommand());

        assertEquals(1, readModelPort.detailInvocations);
        assertEquals(0, readModelPort.traceInvocations);
        assertEquals(0, readModelPort.evidenceInvocations);
    }

    @Test
    void submitApprovedSuccessDoesNotProduceBuy() {
        final ApprovalPacketView created = commandService.create(createCommand());

        final ApprovalPacketView result =
                commandService.submitDecision(submitCommand(created.approvalPacketId(), ApprovalDecision.APPROVED));

        assertEquals(ApprovalStatus.APPROVED.name(), result.approvalStatus());
        assertFalse(result.toString().contains("BUY"));
        assertEquals(
                DecisionAuditEventType.HUMAN_APPROVAL_DECISION_SUBMITTED,
                auditRepository.auditEvents.getLast().eventType());
    }

    @Test
    void submitRejectedSuccessDoesNotProduceSell() {
        final ApprovalPacketView created = commandService.create(createCommand());

        final ApprovalPacketView result =
                commandService.submitDecision(submitCommand(created.approvalPacketId(), ApprovalDecision.REJECTED));

        assertEquals(ApprovalStatus.REJECTED.name(), result.approvalStatus());
        assertFalse(result.toString().contains("SELL"));
        assertEquals(
                DecisionAuditEventType.HUMAN_APPROVAL_DECISION_REJECTED,
                auditRepository.auditEvents.getLast().eventType());
    }

    @Test
    void submitNeedsReviewSuccessKeepsPacketNonTerminal() {
        final ApprovalPacketView created = commandService.create(createCommand());

        final ApprovalPacketView result =
                commandService.submitDecision(
                        submitCommand(created.approvalPacketId(), ApprovalDecision.NEEDS_REVIEW));

        assertEquals(ApprovalStatus.NEEDS_REVIEW.name(), result.approvalStatus());
        assertEquals(
                DecisionAuditEventType.HUMAN_APPROVAL_DECISION_NEEDS_REVIEW,
                auditRepository.auditEvents.getLast().eventType());
    }

    @Test
    void invalidTransitionFailsClosedAndWritesDeniedAudit() {
        final ApprovalPacketView created = commandService.create(createCommand());
        commandService.submitDecision(submitCommand(created.approvalPacketId(), ApprovalDecision.APPROVED));

        assertThrows(
                ApprovalStatusTransitionException.class,
                () ->
                        commandService.submitDecision(
                                submitCommand(created.approvalPacketId(), ApprovalDecision.REJECTED)));
        assertEquals(
                DecisionAuditEventType.HUMAN_APPROVAL_TRANSITION_DENIED,
                auditRepository.auditEvents.getLast().eventType());
    }

    @Test
    void terminalStateSubmitFailsClosed() {
        final ApprovalPacketView created = commandService.create(createCommand());
        commandService.submitDecision(submitCommand(created.approvalPacketId(), ApprovalDecision.REJECTED));

        assertThrows(
                ApprovalStatusTransitionException.class,
                () ->
                        commandService.submitDecision(
                                submitCommand(created.approvalPacketId(), ApprovalDecision.APPROVED)));
    }

    @Test
    void submitTenantMismatchFailsWithoutUpdate() {
        final ApprovalPacketView created = commandService.create(createCommand());

        assertThrows(
                HumanApprovalPacketNotFoundException.class,
                () ->
                        commandService.submitDecision(
                                new SubmitApprovalDecisionCommand(
                                        "tenant-b",
                                        created.approvalPacketId(),
                                        ApprovalDecision.APPROVED,
                                        null,
                                        "reviewed",
                                        "reviewer-a",
                                        "trace-a")));
        assertEquals(0, approvalRepository.updateCalls);
    }

    @Test
    void submitAuditFailureFailsClosed() {
        final ApprovalPacketView created = commandService.create(createCommand());
        auditRepository.auditFailure = new RuntimeException("audit down");

        assertThrows(
                HumanApprovalPacketAuditException.class,
                () ->
                        commandService.submitDecision(
                                submitCommand(created.approvalPacketId(), ApprovalDecision.APPROVED)));
    }

    @Test
    void submitAuditFailureCanRollbackStatusThroughWriteBoundary() {
        final ApprovalPacketView created = commandService.create(createCommand());
        auditRepository.auditFailure = new RuntimeException("audit down");
        commandService = commandServiceWithRollbackBoundary();

        assertThrows(
                HumanApprovalPacketAuditException.class,
                () ->
                        commandService.submitDecision(
                                submitCommand(created.approvalPacketId(), ApprovalDecision.APPROVED)));
        final HumanApprovalPacketId id =
                new HumanApprovalPacketId(UUID.fromString(created.approvalPacketId()));
        assertEquals(
                ApprovalStatus.PENDING,
                approvalRepository.packets.get(id).approvalStatus());
    }

    private static CreateApprovalPacketCommand createCommand() {
        return new CreateApprovalPacketCommand(
                TENANT_ID,
                RUN_ID,
                ApprovalType.QUANT_DECISION_REVIEW,
                "readonly approval summary",
                Map.of("riskReviewed", true),
                Map.of("trace", "trace://trace-a"),
                "reviewer-a",
                "trace-a");
    }

    private static SubmitApprovalDecisionCommand submitCommand(
            final String approvalPacketId, final ApprovalDecision decision) {
        return new SubmitApprovalDecisionCommand(
                TENANT_ID, approvalPacketId, decision, null, "reviewed", "reviewer-a", "trace-a");
    }

    private HumanApprovalPacketCommandService commandServiceWithRollbackBoundary() {
        final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        final HumanApprovalPacketService packetService =
                new HumanApprovalPacketService(
                        approvalRepository, new ApprovalStatusTransitionPolicy(), clock);
        final Map<HumanApprovalPacketId, HumanApprovalPacket> before = new LinkedHashMap<>(approvalRepository.packets);
        return new HumanApprovalPacketCommandService(
                approvalRepository,
                packetService,
                new DecisionReadModelService(readModelPort),
                auditRepository,
                clock,
                new FixedUuidSupplier(PACKET_UUID),
                new ApprovalWriteBoundary() {
                    @Override
                    public <T> T execute(final ApprovalWriteBoundary.ApprovalWriteAction<T> action) {
                        before.clear();
                        before.putAll(approvalRepository.packets);
                        try {
                            return action.get();
                        } catch (final RuntimeException error) {
                            approvalRepository.packets.clear();
                            approvalRepository.packets.putAll(before);
                            throw error;
                        }
                    }
                });
    }

    private static DecisionRunDetailView validDetail() {
        return new DecisionRunDetailView(
                "00000000-0000-0000-0000-000000000200",
                RUN_ID,
                TENANT_ID,
                "trace-a",
                "request-a",
                "request-key-a",
                "QUANT_DECISION_REVIEW",
                "NQ_DRYRUN",
                "snapshot-a",
                1,
                DecisionRunStatus.SUCCEEDED,
                NOW,
                NOW.plusMillis(10),
                10L,
                null,
                null,
                "quant-signal://signal-a;signalType=UNKNOWN_REVIEW_INPUT",
                "quant-decision://decision-a;action=OBSERVE;risk=LOW;confidence=0.5000",
                "decision-output://request-a;action=OBSERVE",
                NOW);
    }

    private static final class FixedUuidSupplier implements java.util.function.Supplier<UUID> {
        private final UUID first;
        private int index;

        private FixedUuidSupplier(final UUID first) {
            this.first = first;
        }

        @Override
        public UUID get() {
            if (index++ == 0) {
                return first;
            }
            return UUID.nameUUIDFromBytes(("audit-" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static final class RecordingReadModelPort implements DecisionReadModelQueryPort {
        private Optional<DecisionRunDetailView> detail = Optional.empty();
        private DecisionRunReadQuery lastDetailQuery;
        private int detailInvocations;
        private int traceInvocations;
        private int evidenceInvocations;

        @Override
        public Optional<DecisionRunDetailView> findDecisionRunDetail(final DecisionRunReadQuery query) {
            detailInvocations++;
            lastDetailQuery = query;
            return detail;
        }

        @Override
        public DecisionTraceTimelineView getDecisionTrace(final DecisionTraceReadQuery query) {
            traceInvocations++;
            return null;
        }

        @Override
        public Optional<DecisionEvidenceView> findDecisionEvidence(final DecisionEvidenceReadQuery query) {
            evidenceInvocations++;
            return Optional.empty();
        }
    }

    private static final class RecordingApprovalRepository implements HumanApprovalPacketRepository {
        private final Map<HumanApprovalPacketId, HumanApprovalPacket> packets = new LinkedHashMap<>();
        private HumanApprovalPacketPersistenceException saveFailure;
        private int saveCalls;
        private int updateCalls;

        @Override
        public HumanApprovalPacket save(final HumanApprovalPacket packet) {
            saveCalls++;
            if (saveFailure != null) {
                throw saveFailure;
            }
            packets.put(packet.id(), packet);
            return packet;
        }

        @Override
        public Optional<HumanApprovalPacket> findByTenantAndApprovalKey(
                final String tenantId, final ApprovalKey approvalKey) {
            return packets.values().stream()
                    .filter(p -> p.tenantId().equals(tenantId))
                    .filter(p -> p.approvalKey().equals(approvalKey))
                    .findFirst();
        }

        @Override
        public Optional<HumanApprovalPacket> findByTenantAndId(
                final String tenantId, final HumanApprovalPacketId id) {
            return Optional.ofNullable(packets.get(id)).filter(p -> p.tenantId().equals(tenantId));
        }

        @Override
        public List<HumanApprovalPacket> findByDecisionRunId(
                final String tenantId, final UUID decisionRunId) {
            return packets.values().stream()
                    .filter(p -> p.tenantId().equals(tenantId))
                    .filter(p -> p.decisionRunId().equals(decisionRunId))
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
            final HumanApprovalPacket current = packets.get(id);
            if (current == null
                    || !current.tenantId().equals(tenantId)
                    || current.approvalStatus() != previousStatus) {
                throw new HumanApprovalPacketPersistenceException(
                        "update mismatch", new IllegalStateException("mismatch"));
            }
            packets.put(
                    id,
                    new HumanApprovalPacket(
                            current.id(),
                            current.decisionRunId(),
                            current.tenantId(),
                            current.traceId(),
                            current.requestId(),
                            current.approvalKey(),
                            current.approvalType(),
                            nextStatus,
                            current.riskLevel(),
                            current.decisionAction(),
                            current.confidenceScore(),
                            current.summary(),
                            current.checklist(),
                            current.evidenceRefs(),
                            reviewer,
                            decidedAt,
                            current.createdAt(),
                            updatedAt));
        }
    }

    private static final class RecordingAuditRepository implements DecisionAuditRepository {
        private final List<DecisionPersistenceRecords.AuditEventRecord> auditEvents = new ArrayList<>();
        private RuntimeException auditFailure;

        @Override
        public void saveRequest(final DecisionPersistenceRecords.RequestRecord record) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public void saveContextSnapshot(final DecisionPersistenceRecords.ContextSnapshotRecord record) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public void saveTraceStep(final DecisionPersistenceRecords.TraceStepRecord record) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public void saveProviderCall(final DecisionPersistenceRecords.ProviderCallRecord record) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public void saveOutput(final DecisionPersistenceRecords.OutputRecord record) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public void saveAuditEvent(final DecisionPersistenceRecords.AuditEventRecord record) {
            if (auditFailure != null) {
                throw auditFailure;
            }
            auditEvents.add(record);
        }
    }

}

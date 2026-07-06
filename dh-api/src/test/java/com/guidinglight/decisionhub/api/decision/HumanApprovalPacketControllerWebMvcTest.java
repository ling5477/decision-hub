package com.guidinglight.decisionhub.api.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guidinglight.decisionhub.api.GlobalExceptionHandler;
import com.guidinglight.decisionhub.api.security.AuthenticatedRequest;
import com.guidinglight.decisionhub.api.security.DhApiAuthenticationFilter;
import com.guidinglight.decisionhub.domain.qdr.DecisionRunStatus;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalKey;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalReviewer;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalStatus;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalStatusTransitionPolicy;
import com.guidinglight.decisionhub.domain.qdr.approval.HumanApprovalPacket;
import com.guidinglight.decisionhub.domain.qdr.approval.HumanApprovalPacketId;
import com.guidinglight.decisionhub.security.AuthContext;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionPersistenceRecords;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketCommandService;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketRepository;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketService;
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
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * stage-qdr-2 B4 Human Approval Packet API MockMvc 测试。
 *
 * <p>覆盖 authenticated / tenant-bound approval API、状态机冲突、audit/repository fail-closed、redaction 与
 * no executable instruction。测试使用内存 fake，不触发 JDBC、外部 HTTP、provider、replay execution、NQ 或 LIVE。
 */
final class HumanApprovalPacketControllerWebMvcTest {

    private static final String GOOD_TOKEN = "good-token";
    private static final String RUN_ID = "00000000-0000-0000-0000-000000000301";
    private static final String PACKET_ID = "00000000-0000-0000-0000-000000000302";
    private static final Instant NOW = Instant.parse("2026-07-06T00:00:00Z");

    private RecordingApprovalRepository approvalRepository;
    private RecordingReadModelPort readModelPort;
    private RecordingAuditRepository auditRepository;
    private MockMvc mockMvc;

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
        final HumanApprovalPacketCommandService commandService =
                new HumanApprovalPacketCommandService(
                        approvalRepository,
                        packetService,
                        new DecisionReadModelService(readModelPort),
                        auditRepository,
                        clock,
                        new FixedUuidSupplier(UUID.fromString(PACKET_ID)));

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        final MappingJackson2HttpMessageConverter jacksonConverter =
                new MappingJackson2HttpMessageConverter();
        jacksonConverter.setObjectMapper(objectMapper);

        mockMvc =
                MockMvcBuilders.standaloneSetup(new HumanApprovalPacketController(commandService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setMessageConverters(jacksonConverter)
                        .addFilters(
                                new DhApiAuthenticationFilter(
                                        token ->
                                                GOOD_TOKEN.equals(token)
                                                        ? new AuthContext(
                                                        "user-a", "tenant-a", Set.of("DH_API"))
                                                        : null))
                        .build();
    }

    @Test
    void createApprovalPacketSuccess() throws Exception {
        final MvcResult result =
                mockMvc.perform(authenticatedPost(
                                "/api/ai/decision-runs/{decisionRunId}/approval-packets", RUN_ID)
                                .content(createBody()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.approvalPacketId").value(PACKET_ID))
                        .andExpect(jsonPath("$.data.decisionRunId").value(RUN_ID))
                        .andExpect(jsonPath("$.data.approvalStatus").value("PENDING"))
                        .andExpect(jsonPath("$.data.decisionAction").value("OBSERVE"))
                        .andExpect(jsonPath("$.data.tenantId").doesNotExist())
                        .andReturn();

        assertNoForbiddenResponseTerms(result);
        assertEquals(1, auditRepository.auditEvents.size());
        assertEquals(0, readModelPort.traceInvocations);
        assertEquals(0, readModelPort.evidenceInvocations);
    }

    @Test
    void getApprovalPacketSuccess() throws Exception {
        createPacket();

        mockMvc.perform(authenticatedGet("/api/ai/approval-packets/{approvalPacketId}", PACKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalPacketId").value(PACKET_ID))
                .andExpect(jsonPath("$.data.tenantId").doesNotExist())
                .andExpect(jsonPath("$.data.approvalStatus").value("PENDING"));
    }

    @Test
    void submitApprovedDecisionSuccess() throws Exception {
        createPacket();

        final MvcResult result =
                mockMvc.perform(authenticatedPost(
                                "/api/ai/approval-packets/{approvalPacketId}/decision", PACKET_ID)
                                .content(decisionBody("APPROVED")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.approvalStatus").value("APPROVED"))
                        .andExpect(jsonPath("$.data.reviewerId").value("user-a"))
                        .andReturn();

        assertNoForbiddenResponseTerms(result);
    }

    @Test
    void submitRejectedDecisionSuccess() throws Exception {
        createPacket();

        final MvcResult result =
                mockMvc.perform(authenticatedPost(
                                "/api/ai/approval-packets/{approvalPacketId}/decision", PACKET_ID)
                                .content(decisionBody("REJECTED")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.approvalStatus").value("REJECTED"))
                        .andReturn();

        assertNoForbiddenResponseTerms(result);
    }

    @Test
    void submitNeedsReviewDecisionSuccess() throws Exception {
        createPacket();

        final MvcResult result =
                mockMvc.perform(authenticatedPost(
                                "/api/ai/approval-packets/{approvalPacketId}/decision", PACKET_ID)
                                .content(decisionBody("NEEDS_REVIEW")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.approvalStatus").value("NEEDS_REVIEW"))
                        .andReturn();

        assertNoForbiddenResponseTerms(result);
    }

    @Test
    void anonymousAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/ai/approval-packets/{approvalPacketId}", PACKET_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tenantMismatchReturns403() throws Exception {
        mockMvc.perform(authenticatedGet("/api/ai/approval-packets/{approvalPacketId}", PACKET_ID)
                        .header(AuthenticatedRequest.TENANT_HEADER, "tenant-b"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidDecisionReturnsFixedRedacted400WithoutSideEffects() throws Exception {
        createPacket();

        final int auditEventsBefore = auditRepository.auditEvents.size();
        final int updateCallsBefore = approvalRepository.updateCalls;
        final int detailCallsBefore = readModelPort.detailInvocations;

        for (String invalidDecision : List.of("BUY", "SELL", "PLACE_ORDER", "CANCEL_ORDER")) {
            final MvcResult result =
                    mockMvc.perform(authenticatedPost(
                                    "/api/ai/approval-packets/{approvalPacketId}/decision",
                                    PACKET_ID)
                                    .content(decisionBody(invalidDecision)))
                            .andExpect(status().isBadRequest())
                            .andExpect(jsonPath("$.success").value(false))
                            .andExpect(jsonPath("$.code").value("APPROVAL_DECISION_INVALID"))
                            .andExpect(jsonPath("$.message").value("Invalid approval decision."))
                            .andExpect(jsonPath("$.detail").doesNotExist())
                            .andReturn();

            assertInvalidDecisionErrorRedacted(result);
        }

        assertEquals(updateCallsBefore, approvalRepository.updateCalls);
        assertEquals(auditEventsBefore, auditRepository.auditEvents.size());
        assertEquals(detailCallsBefore, readModelPort.detailInvocations);
        assertEquals(0, readModelPort.traceInvocations);
        assertEquals(0, readModelPort.evidenceInvocations);
        assertEquals(0, auditRepository.providerCallWrites);
    }

    @Test
    void terminalTransitionDeniedReturns409() throws Exception {
        createPacket();
        mockMvc.perform(authenticatedPost(
                        "/api/ai/approval-packets/{approvalPacketId}/decision", PACKET_ID)
                        .content(decisionBody("APPROVED")))
                .andExpect(status().isOk());

        mockMvc.perform(authenticatedPost(
                        "/api/ai/approval-packets/{approvalPacketId}/decision", PACKET_ID)
                        .content(decisionBody("REJECTED")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail.errorCode").value("APPROVAL_TRANSITION_DENIED"));
    }

    @Test
    void notFoundReturns404() throws Exception {
        mockMvc.perform(authenticatedGet("/api/ai/approval-packets/{approvalPacketId}", PACKET_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void auditFailureReturns500() throws Exception {
        createPacket();
        auditRepository.auditFailure = new RuntimeException("audit down");

        mockMvc.perform(authenticatedPost(
                        "/api/ai/approval-packets/{approvalPacketId}/decision", PACKET_ID)
                        .content(decisionBody("APPROVED")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail.errorCode").value("QDR_APPROVAL_AUDIT_FAILED"));
    }

    @Test
    void repositoryFailureReturns500() throws Exception {
        approvalRepository.saveFailure = new HumanApprovalPacketPersistenceException("repo down", null);

        mockMvc.perform(authenticatedPost(
                        "/api/ai/decision-runs/{decisionRunId}/approval-packets", RUN_ID)
                        .header("Authorization", "Bearer " + GOOD_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail.errorCode").value("QDR_APPROVAL_WRITE_FAILED"));
    }

    @Test
    void approvalApiDoesNotTriggerReplayExternalHttpOrProvider() throws Exception {
        createPacket();

        assertEquals(1, readModelPort.detailInvocations);
        assertEquals(0, readModelPort.traceInvocations);
        assertEquals(0, readModelPort.evidenceInvocations);
        assertEquals(0, auditRepository.providerCallWrites);
    }

    private void createPacket() throws Exception {
        mockMvc.perform(authenticatedPost(
                        "/api/ai/decision-runs/{decisionRunId}/approval-packets", RUN_ID)
                        .content(createBody()))
                .andExpect(status().isOk());
    }

    private static MockHttpServletRequestBuilder authenticatedGet(
            final String path, final Object... uriVars) {
        return get(path, uriVars).header("Authorization", "Bearer " + GOOD_TOKEN);
    }

    private static MockHttpServletRequestBuilder authenticatedPost(
            final String path, final Object... uriVars) {
        return post(path, uriVars)
                .header("Authorization", "Bearer " + GOOD_TOKEN)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private static String createBody() {
        return """
                {
                  "approvalType": "QUANT_DECISION_REVIEW",
                  "summary": "readonly approval summary",
                  "checklistJson": {"riskReviewed": true},
                  "evidenceRefsJson": {"trace": "trace://trace-a"},
                  "reviewerNote": "created for review"
                }
                """;
    }

    private static String decisionBody(final String decision) {
        return """
                {
                  "decision": "%s",
                  "reviewerNote": "reviewed"
                }
                """
                .formatted(decision);
    }

    private static void assertNoForbiddenResponseTerms(final MvcResult result) throws Exception {
        final String response = result.getResponse().getContentAsString();
        for (String forbidden :
                List.of(
                        "BUY",
                        "SELL",
                        "PLACE_ORDER",
                        "CANCEL_ORDER",
                        "MARKET_ORDER",
                        "LIMIT_ORDER",
                        "No enum constant",
                        "ApprovalDecision",
                        "credential",
                        "token",
                        "apiKey",
                        "apiSecret",
                        "passphrase")) {
            assertFalse(response.contains(forbidden), "response contains forbidden term " + forbidden);
        }
    }

    private static void assertInvalidDecisionErrorRedacted(final MvcResult result)
            throws Exception {
        final String response = result.getResponse().getContentAsString();
        for (String forbidden :
                List.of(
                        "BUY",
                        "SELL",
                        "PLACE_ORDER",
                        "CANCEL_ORDER",
                        "MARKET_ORDER",
                        "LIMIT_ORDER",
                        "No enum constant",
                        "ApprovalDecision",
                        "com.guidinglight.decisionhub.domain.qdr.approval.ApprovalDecision")) {
            assertFalse(response.contains(forbidden), "response contains forbidden term " + forbidden);
        }
    }

    private static DecisionRunDetailView validDetail() {
        return new DecisionRunDetailView(
                "00000000-0000-0000-0000-000000000300",
                RUN_ID,
                "tenant-a",
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
        private int detailInvocations;
        private int traceInvocations;
        private int evidenceInvocations;

        @Override
        public Optional<DecisionRunDetailView> findDecisionRunDetail(final DecisionRunReadQuery query) {
            detailInvocations++;
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
        private int updateCalls;

        @Override
        public HumanApprovalPacket save(final HumanApprovalPacket packet) {
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
        private int providerCallWrites;

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
            providerCallWrites++;
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

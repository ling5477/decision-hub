package com.guidinglight.decisionhub.api.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.guidinglight.decisionhub.security.AuthContext;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionEvidenceReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionEvidenceView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelQueryPort;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelService;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelUnavailableException;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunDetailView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionTraceReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionTraceStepView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionTraceTimelineView;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * stage-qdr-2 B2 decision run read API MockMvc 测试。
 *
 * <p>测试只覆盖 authenticated / tenant-bound read-only GET，不触发 replay execution、approval write、
 * outbound HTTP、provider SDK、Agent runtime 或 LIVE。
 */
final class DecisionRunReadControllerWebMvcTest {

    private static final String GOOD_TOKEN = "good-token";
    private static final String RUN_ID = "00000000-0000-0000-0000-000000000002";
    private static final Instant NOW = Instant.parse("2026-07-06T00:00:00Z");

    private RecordingPort port;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        port = new RecordingPort();
        port.detail = Optional.of(validDetail());
        port.timeline = validTimeline();

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        final MappingJackson2HttpMessageConverter jacksonConverter =
                new MappingJackson2HttpMessageConverter();
        jacksonConverter.setObjectMapper(objectMapper);

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new DecisionRunReadController(new DecisionReadModelService(port)))
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
    void detailWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/api/ai/decision-runs/{decisionRunId}", RUN_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void detailWithTenantHeaderMismatchReturns403() throws Exception {
        mockMvc
                .perform(
                        authenticatedGet("/api/ai/decision-runs/{decisionRunId}", RUN_ID)
                                .header(AuthenticatedRequest.TENANT_HEADER, "tenant-b"))
                .andExpect(status().isForbidden());
    }

    @Test
    void detailWithValidAuthenticationReturnsApiResponseWithoutTenantId() throws Exception {
        final MvcResult result =
                mockMvc
                        .perform(authenticatedGet("/api/ai/decision-runs/{decisionRunId}", RUN_ID))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.decisionRunId").value(RUN_ID))
                        .andExpect(jsonPath("$.data.traceId").value("trace-a"))
                        .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                        .andExpect(jsonPath("$.data.tenantId").doesNotExist())
                        .andExpect(jsonPath("$.data.quantDecisionSummary").value("quant-decision://decision-a;action=OBSERVE"))
                        .andReturn();

        assertEquals("tenant-a", port.lastDetailQuery.tenantId());
        assertEquals("user-a", port.lastDetailQuery.requesterId());
        final String response = result.getResponse().getContentAsString();
        assertFalse(response.contains("BUY"));
        assertFalse(response.contains("SELL"));
        assertFalse(response.contains("PLACE_ORDER"));
        assertFalse(response.contains("CANCEL_ORDER"));
    }

    @Test
    void traceWithValidAuthenticationReturnsOrderedStepsWithoutTenantId() throws Exception {
        mockMvc
                .perform(authenticatedGet("/api/ai/decision-runs/{decisionRunId}/trace", RUN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decisionRunId").value(RUN_ID))
                .andExpect(jsonPath("$.data.tenantId").doesNotExist())
                .andExpect(jsonPath("$.data.steps.length()").value(1))
                .andExpect(jsonPath("$.data.steps[0].auditRef").value("decision-trace-step://step-a"));

        assertEquals(1, port.traceInvocations);
        assertEquals("tenant-a", port.lastTraceQuery.tenantId());
    }

    @Test
    void invalidDecisionRunIdReturns400AndDoesNotReachPort() throws Exception {
        mockMvc
                .perform(authenticatedGet("/api/ai/decision-runs/{decisionRunId}", "not-a-uuid"))
                .andExpect(status().isBadRequest());

        assertEquals(0, port.detailInvocations);
    }

    @Test
    void notFoundReturns404WithoutCrossTenantDetail() throws Exception {
        port.detail = Optional.empty();

        mockMvc
                .perform(authenticatedGet("/api/ai/decision-runs/{decisionRunId}", RUN_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DH-COMMON-404"));
    }

    @Test
    void repositoryUnavailableReturnsSafe500() throws Exception {
        port.failure = new DecisionReadModelUnavailableException("raw internal failure", null);

        mockMvc
                .perform(authenticatedGet("/api/ai/decision-runs/{decisionRunId}", RUN_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("decision read model unavailable"))
                .andExpect(jsonPath("$.detail.errorCode").value("QDR_READMODEL_UNAVAILABLE"));
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedGet(
            final String path, final Object... uriVars) {
        return get(path, uriVars).header("Authorization", "Bearer " + GOOD_TOKEN);
    }

    private static DecisionRunDetailView validDetail() {
        return new DecisionRunDetailView(
                "00000000-0000-0000-0000-000000000001",
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
                "quant-decision://decision-a;action=OBSERVE",
                "decision-output://request-a;action=OBSERVE",
                NOW);
    }

    private static DecisionTraceTimelineView validTimeline() {
        return new DecisionTraceTimelineView(
                RUN_ID,
                "tenant-a",
                "trace-a",
                "request-a",
                List.of(
                        new DecisionTraceStepView(
                                "step-a",
                                1,
                                "POLICY_CHECK",
                                "AUDIT_TRACE_STEP",
                                "COMPLETED",
                                NOW,
                                NOW.plusMillis(10),
                                10L,
                                null,
                                null,
                                null,
                                null,
                                null,
                                "decision-trace-step://step-a")));
    }

    private static final class RecordingPort implements DecisionReadModelQueryPort {
        private Optional<DecisionRunDetailView> detail = Optional.empty();
        private DecisionTraceTimelineView timeline;
        private DecisionReadModelUnavailableException failure;
        private DecisionRunReadQuery lastDetailQuery;
        private DecisionTraceReadQuery lastTraceQuery;
        private int detailInvocations;
        private int traceInvocations;

        @Override
        public Optional<DecisionRunDetailView> findDecisionRunDetail(final DecisionRunReadQuery query) {
            detailInvocations++;
            lastDetailQuery = query;
            if (failure != null) {
                throw failure;
            }
            return detail;
        }

        @Override
        public DecisionTraceTimelineView getDecisionTrace(final DecisionTraceReadQuery query) {
            traceInvocations++;
            lastTraceQuery = query;
            if (failure != null) {
                throw failure;
            }
            return timeline;
        }

        @Override
        public Optional<DecisionEvidenceView> findDecisionEvidence(final DecisionEvidenceReadQuery query) {
            if (failure != null) {
                throw failure;
            }
            return Optional.empty();
        }
    }
}

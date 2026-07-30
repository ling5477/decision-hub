package com.guidinglight.decisionhub.api.decision;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.api.GlobalExceptionHandler;
import com.guidinglight.decisionhub.api.security.DhApiAuthenticationFilter;
import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.security.AuthContext;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.security.nq.HmacNqDryRunAuthenticator;
import com.guidinglight.decisionhub.security.nq.InMemoryNonceReplayGuard;
import com.guidinglight.decisionhub.security.nq.InMemoryRateLimiter;
import com.guidinglight.decisionhub.security.nq.NqDryRunAuthRequest;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionOutputAssembler;
import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import com.guidinglight.decisionhub.usecase.decision.DecisionPersistenceRecords;
import com.guidinglight.decisionhub.usecase.decision.DecisionProviderGuard;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionContextBuilder;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionOrchestrator;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionPolicyChecker;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionRiskReviewer;
import com.guidinglight.decisionhub.usecase.decision.MockDecisionSignalProvider;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionProviderGuard;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionProviderLatencyRecorder;
import com.guidinglight.decisionhub.usecase.decision.InMemoryDecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionOrchestrator;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunRuntimeProperties;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DefaultDecisionDryRunService;
import com.guidinglight.decisionhub.usecase.qdr.InMemoryDecisionCoreRepository;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayFailureCode;
import com.guidinglight.decisionhub.usecase.qdr.gateway.QdrModelGatewayIntegrationCommand;
import com.guidinglight.decisionhub.usecase.qdr.gateway.QdrModelGatewayIntegrationException;
import com.guidinglight.decisionhub.usecase.qdr.gateway.QdrModelGatewayIntegrationPort;
import com.guidinglight.decisionhub.usecase.qdr.gateway.QdrModelGatewayIntegrationResult;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Integration-1 limited dry-run endpoint MockMvc 回归。
 *
 * <p>测试只验证 DH inbound endpoint、安全 gate 与 read-only response，不发起 outbound HTTP、不调用 NQ、不接
 * provider、不启用 Agent/LangGraph 或 LIVE。
 */
class DecisionDryRunControllerWebMvcTest {

    private static final String GOOD_TOKEN = "good-token";
    private static final String SECRET = "unit-test-dryrun-secret";
    private static final Instant NOW = Instant.parse("2026-07-04T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;
    private FeedbackEnvironment authenticatedEnvironment;

    @BeforeEach
    void setUp() {
        TimeProvider.useClock(CLOCK);
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        authenticatedEnvironment = FeedbackEnvironment.DEV;
        mockMvc = newMockMvc(true, new InMemoryDecisionAuditRepository(), 2048, 1000);
    }

    @AfterEach
    void tearDown() {
        TimeProvider.useClock(Clock.systemUTC());
    }

    @Test
    void postWithoutApiAuthenticationReturns401() throws Exception {
        final String body = objectMapper.writeValueAsString(legalEnvelope("req-no-auth"));

        mockMvc
                .perform(post("/api/ai/decision-dry-runs").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validSignedRequestReturnsReadonlyDryRunSnapshot() throws Exception {
        final Map<String, Object> envelope = legalEnvelope("req-ok");
        final String body = objectMapper.writeValueAsString(envelope);

        final MvcResult result =
                mockMvc
                        .perform(signedPost(envelope, body))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.decisionId").value("req-ok"))
                        .andExpect(jsonPath("$.dryRun").value(true))
                        .andExpect(jsonPath("$.auditRef").exists())
                        .andExpect(jsonPath("$.replayRef").exists())
                        .andExpect(jsonPath("$.traceSummary").isArray())
                        .andExpect(jsonPath("$.schemaVersion").value("1.0.0"))
                        .andExpect(header().exists("X-Trace-Id"))
                        .andReturn();

        final String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("MODEL_GATEWAY_MOCK_CALL"));
        assertTrue(response.contains("prompt-version-webmvc"));
        assertTrue(response.contains("model-version-webmvc"));
        assertTrue(response.contains("provider-profile-webmvc"));
        assertTrue(response.contains("model-call:webmvc"));
        assertFalse(response.contains("raw prompt"));
        assertFalse(response.contains("raw provider response"));
        assertFalse(response.contains("credential"));
        assertFalse(response.contains("BUY"));
        assertFalse(response.contains("SELL"));
        assertFalse(response.contains("PLACE_ORDER"));
        assertFalse(response.contains("CANCEL_ORDER"));
    }

    @Test
    void validSignedTestEnvironmentRequestIsAccepted() throws Exception {
        authenticatedEnvironment = FeedbackEnvironment.TEST;
        final Map<String, Object> envelope = legalEnvelope("req-test-environment");
        envelope.put("environment", "TEST");

        mockMvc
                .perform(signedPost(envelope, objectMapper.writeValueAsString(envelope)))
                .andExpect(status().isOk());
    }

    @Test
    void missingInvalidAndTamperedEnvironmentFailClosed() throws Exception {
        final Map<String, Object> missing = legalEnvelope("req-env-missing");
        missing.remove("environment");
        mockMvc
                .perform(signedPost(missing, objectMapper.writeValueAsString(missing)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ENVIRONMENT_REQUIRED"));

        final Map<String, Object> invalid = legalEnvelope("req-env-invalid");
        invalid.put("environment", "LOCAL");
        mockMvc
                .perform(signedPost(invalid, objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ENVIRONMENT_INVALID"));

        final Map<String, Object> signedDev = legalEnvelope("req-env-tamper");
        final String signedBody = objectMapper.writeValueAsString(signedDev);
        final MockHttpServletRequestBuilder signed = signedPost(signedDev, signedBody);
        signedDev.put("environment", "TEST");
        signed.content(objectMapper.writeValueAsString(signedDev));
        mockMvc
                .perform(signed)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SIGNATURE_INVALID"));
    }

    @Test
    void environmentMismatchRejectsBeforeOrchestratorAndBusinessAudit() throws Exception {
        final AtomicInteger orchestratorInvocations = new AtomicInteger();
        final InMemoryDecisionAuditRepository auditRepository = new InMemoryDecisionAuditRepository();
        mockMvc =
                newMockMvc(
                        true,
                        auditRepository,
                        2048,
                        1000,
                        32768,
                        request -> {
                            orchestratorInvocations.incrementAndGet();
                            throw new AssertionError("environment mismatch must not reach orchestrator");
                        });
        authenticatedEnvironment = FeedbackEnvironment.TEST;
        final Map<String, Object> envelope = legalEnvelope("req-env-mismatch");

        mockMvc
                .perform(signedPost(envelope, objectMapper.writeValueAsString(envelope)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("TENANT_ENVIRONMENT_MISMATCH"));

        assertEquals(0, orchestratorInvocations.get());
        assertEquals(0, auditRepository.auditEvents().size());
    }

    @Test
    void validSignedRequestPersistsDecisionCoreMainlineRecords() throws Exception {
        final InMemoryDecisionCoreRepository decisionCoreRepository =
                new InMemoryDecisionCoreRepository();
        final InMemoryDecisionAuditRepository auditRepository = new InMemoryDecisionAuditRepository();
        mockMvc =
                newMockMvc(
                        true,
                        auditRepository,
                        2048,
                        1000,
                        32768,
                        defaultOrchestrator(auditRepository),
                        decisionCoreRepository);
        final Map<String, Object> envelope = legalEnvelope("req-core");
        final String body = objectMapper.writeValueAsString(envelope);

        mockMvc.perform(signedPost(envelope, body)).andExpect(status().isOk());

        final var request =
                decisionCoreRepository
                        .findByTenantIdAndRequestKey("tenant-a", "req-core")
                        .orElseThrow();
        final var runs = decisionCoreRepository.findByDecisionRequestId(request.id());
        assertEquals(1, runs.size());
        assertEquals("trace-req-core", request.traceId());
        assertEquals(1, decisionCoreRepository.findByDecisionRunId(runs.getFirst().id()).size());
        assertTrue(
                decisionCoreRepository.findByDecisionRunId(runs.getFirst().id()).stream()
                        .noneMatch(
                                decision ->
                                        Set.of("BUY", "SELL", "PLACE_ORDER", "CANCEL_ORDER")
                                                .contains(decision.action().name())));
    }

    @Test
    void featureFlagDisabledFailsClosed() throws Exception {
        mockMvc = newMockMvc(false, new InMemoryDecisionAuditRepository(), 2048, 1000);
        final Map<String, Object> envelope = legalEnvelope("req-disabled");
        final String body = objectMapper.writeValueAsString(envelope);

        mockMvc
                .perform(signedPost(envelope, body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("POLICY_DENIED"));
    }

    @Test
    void missingAndInvalidSignatureReturnSignatureInvalid() throws Exception {
        final Map<String, Object> envelope = legalEnvelope("req-missing-sig");
        final String body = objectMapper.writeValueAsString(envelope);

        mockMvc
                .perform(
                        authenticatedPost(body)
                                .header("X-NQ-DH-Source", "NQ_DRYRUN")
                                .header("X-NQ-DH-Tenant-Id", "tenant-a")
                                .header("X-NQ-DH-Request-Id", value(envelope.get("requestId")))
                                .header("X-NQ-DH-Trace-Id", value(envelope.get("traceId")))
                                .header("X-NQ-DH-Timestamp", value(envelope.get("timestamp")))
                                .header("X-NQ-DH-Nonce", value(envelope.get("nonce"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SIGNATURE_INVALID"));

        mockMvc
                .perform(postWithSignature(envelope, body, "bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SIGNATURE_INVALID"));
    }

    @Test
    void bodyHeaderNonceMismatchReturnsSignatureInvalid() throws Exception {
        final Map<String, Object> envelope = legalEnvelope("req-nonce-mismatch");
        final String body = objectMapper.writeValueAsString(envelope);

        mockMvc
                .perform(signedPostWithNonceHeader(envelope, body, "different-nonce"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SIGNATURE_INVALID"));
    }

    @Test
    void invalidTimestampShapesReturnTimestampInvalid() throws Exception {
        for (String timestamp : List.of("1783123200", "1783123200000", "2026-07-04T08:00:00+08:00")) {
            final Map<String, Object> envelope = legalEnvelope("req-ts-" + timestamp.length());
            envelope.put("timestamp", timestamp);
            final String body = objectMapper.writeValueAsString(envelope);

            mockMvc
                    .perform(signedPost(envelope, body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("TIMESTAMP_INVALID"));
        }
    }

    @Test
    void timestampOutsideWindowReturnsOutOfWindow() throws Exception {
        final Map<String, Object> envelope = legalEnvelope("req-window");
        envelope.put("timestamp", NOW.minus(Duration.ofMinutes(6)).toString());
        final String body = objectMapper.writeValueAsString(envelope);

        mockMvc
                .perform(signedPost(envelope, body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("TIMESTAMP_OUT_OF_WINDOW"));
    }

    @Test
    void replayNonceReturnsNonceReplay() throws Exception {
        final Map<String, Object> envelope = legalEnvelope("req-replay");
        final String body = objectMapper.writeValueAsString(envelope);

        mockMvc.perform(signedPost(envelope, body)).andExpect(status().isOk());
        mockMvc
                .perform(signedPost(envelope, body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NONCE_REPLAY"));
    }

    @Test
    void sourceDeniedAndTenantMismatchFailClosed() throws Exception {
        final Map<String, Object> sourceDenied = legalEnvelope("req-source");
        sourceDenied.put("source", "unknown-source");
        mockMvc
                .perform(signedPost(sourceDenied, objectMapper.writeValueAsString(sourceDenied)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("SOURCE_DENIED"));

        final Map<String, Object> tenantMismatch = legalEnvelope("req-tenant");
        tenantMismatch.put("tenantId", "tenant-b");
        mockMvc
                .perform(signedPost(tenantMismatch, objectMapper.writeValueAsString(tenantMismatch)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("TENANT_MISMATCH"));
    }

    @Test
    void sourceWireValueMustBeExactInBodyAndHeader() throws Exception {
        final Map<String, Object> lowercase = legalEnvelope("req-source-lowercase");
        lowercase.put("source", "nq_dryrun");
        final String lowercaseBody = objectMapper.writeValueAsString(lowercase);
        mockMvc
                .perform(signedPost(lowercase, lowercaseBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("SOURCE_DENIED"));

        final Map<String, Object> whitespace = legalEnvelope("req-source-whitespace");
        whitespace.put("source", " NQ_DRYRUN ");
        final String whitespaceBody = objectMapper.writeValueAsString(whitespace);
        mockMvc
                .perform(signedPost(whitespace, whitespaceBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("SOURCE_DENIED"));

        final Map<String, Object> mismatch = legalEnvelope("req-source-mismatch");
        mismatch.put("source", "nq_dryrun");
        final String mismatchBody = objectMapper.writeValueAsString(mismatch);
        mockMvc
                .perform(signedPostWithSourceHeader(mismatch, mismatchBody, "NQ_DRYRUN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("SOURCE_DENIED"));
    }

    @Test
    void dryRunFalseAndForbiddenExecutionMaterialReturnPolicyDenied() throws Exception {
        final Map<String, Object> notDryRun = legalEnvelope("req-not-dryrun");
        notDryRun.put("dryRun", false);
        mockMvc
                .perform(signedPost(notDryRun, objectMapper.writeValueAsString(notDryRun)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("POLICY_DENIED"));

        final Map<String, Object> forbidden = legalEnvelope("req-forbidden");
        forbidden.put("executableOrder", Map.of("side", "BUY"));
        mockMvc
                .perform(signedPost(forbidden, objectMapper.writeValueAsString(forbidden)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("POLICY_DENIED"));
    }

    @Test
    void payloadTooLargeAndRateLimitedReturnCanonicalErrors() throws Exception {
        final Map<String, Object> large = legalEnvelope("req-large");
        large.put("padding", "x".repeat(512));
        mockMvc = newMockMvc(true, new InMemoryDecisionAuditRepository(), 128, 1000);
        mockMvc
                .perform(signedPost(large, objectMapper.writeValueAsString(large)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.errorCode").value("PAYLOAD_TOO_LARGE"));

        mockMvc = newMockMvc(true, new InMemoryDecisionAuditRepository(), 2048, 1);
        final Map<String, Object> first = legalEnvelope("req-rate-1");
        final Map<String, Object> second = legalEnvelope("req-rate-2");
        mockMvc.perform(signedPost(first, objectMapper.writeValueAsString(first))).andExpect(status().isOk());
        mockMvc
                .perform(signedPost(second, objectMapper.writeValueAsString(second)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"));
    }

    @Test
    void memoryCapExceededReturnsMemoryLimitExceeded() throws Exception {
        mockMvc = newMockMvc(true, new InMemoryDecisionAuditRepository(), 4096, 1000, 64);
        final Map<String, Object> envelope = legalEnvelope("req-memory-cap");
        final Map<String, Object> context = new LinkedHashMap<>(context(envelope));
        context.put("padding", "x".repeat(512));
        envelope.put("decisionContext", context);
        final String body = objectMapper.writeValueAsString(envelope);

        mockMvc
                .perform(signedPost(envelope, body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("MEMORY_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.auditRef").exists());
    }

    @Test
    void providerFailuresReturnCanonicalFailClosedErrors() throws Exception {
        assertProviderFailure(ProviderSignalStatus.DISABLED, 503, "PROVIDER_DISABLED", "req-provider-disabled");
        assertProviderFailure(ProviderSignalStatus.TIMEOUT, 504, "PROVIDER_TIMEOUT", "req-provider-timeout");
        assertProviderFailure(ProviderSignalStatus.BUDGET_EXCEEDED, 429, "BUDGET_EXCEEDED", "req-provider-budget");
    }

    @Test
    void auditWriteFailureFailsClosed() throws Exception {
        mockMvc = newMockMvc(true, new FailingAuditRepository(), 2048, 1000);
        final Map<String, Object> envelope = legalEnvelope("req-audit-fail");
        final String body = objectMapper.writeValueAsString(envelope);

        mockMvc
                .perform(signedPost(envelope, body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("UNKNOWN_ERROR"));
    }

    @Test
    void gatewayFailureReturnsSafeRedactedError() throws Exception {
        final InMemoryDecisionAuditRepository auditRepository = new InMemoryDecisionAuditRepository();
        mockMvc =
                newMockMvc(
                        true,
                        auditRepository,
                        2048,
                        1000,
                        new DecisionDryRunRuntimeProperties(
                                true,
                                false,
                                false,
                                true,
                                Set.of("NQ_DRYRUN"),
                                Set.of("tenant-a:NQ_DRYRUN"),
                                32768),
                        defaultOrchestrator(auditRepository),
                        new InMemoryDecisionCoreRepository(),
                        new RecordingGatewayIntegration(ModelGatewayFailureCode.POLICY_DENIED));
        final Map<String, Object> envelope = legalEnvelope("req-gateway-fail");
        final String body = objectMapper.writeValueAsString(envelope);

        final MvcResult result =
                mockMvc
                        .perform(signedPost(envelope, body))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.errorCode").value("POLICY_DENIED"))
                        .andReturn();

        final String response = result.getResponse().getContentAsString();
        assertFalse(response.contains("raw prompt"));
        assertFalse(response.contains("raw provider response"));
        assertFalse(response.contains("credential"));
        assertFalse(response.contains("PLACE_ORDER"));
        assertFalse(response.contains("CANCEL_ORDER"));
    }

    private MockMvc newMockMvc(
            final boolean enabled,
            final DecisionAuditRepository repository,
            final long maxPayloadBytes,
            final int maxRequests) {
        return newMockMvc(enabled, repository, maxPayloadBytes, maxRequests, 32768);
    }

    private MockMvc newMockMvc(
            final boolean enabled,
            final DecisionAuditRepository repository,
            final long maxPayloadBytes,
            final int maxRequests,
            final int memoryCapBytes) {
        final DecisionDryRunRuntimeProperties properties =
                new DecisionDryRunRuntimeProperties(
                        enabled,
                        false,
                        false,
                        true,
                        Set.of("NQ_DRYRUN"),
                        Set.of("tenant-a:NQ_DRYRUN"),
                        memoryCapBytes);
        return newMockMvc(enabled, repository, maxPayloadBytes, maxRequests, properties, defaultOrchestrator(repository));
    }

    private MockMvc newMockMvc(
            final boolean enabled,
            final DecisionAuditRepository repository,
            final long maxPayloadBytes,
            final int maxRequests,
            final int memoryCapBytes,
            final DecisionOrchestrator orchestrator) {
        return newMockMvc(
                enabled,
                repository,
                maxPayloadBytes,
                maxRequests,
                memoryCapBytes,
                orchestrator,
                new InMemoryDecisionCoreRepository());
    }

    private MockMvc newMockMvc(
            final boolean enabled,
            final DecisionAuditRepository repository,
            final long maxPayloadBytes,
            final int maxRequests,
            final int memoryCapBytes,
            final DecisionOrchestrator orchestrator,
            final InMemoryDecisionCoreRepository decisionCoreRepository) {
        final DecisionDryRunRuntimeProperties properties =
                new DecisionDryRunRuntimeProperties(
                        enabled,
                        false,
                        false,
                        true,
                        Set.of("NQ_DRYRUN"),
                        Set.of("tenant-a:NQ_DRYRUN"),
                        memoryCapBytes);
        return newMockMvc(
                enabled,
                repository,
                maxPayloadBytes,
                maxRequests,
                properties,
                orchestrator,
                decisionCoreRepository);
    }

    private MockMvc newMockMvc(
            final boolean enabled,
            final DecisionAuditRepository repository,
            final long maxPayloadBytes,
            final int maxRequests,
            final DecisionDryRunRuntimeProperties properties,
            final DecisionOrchestrator orchestrator) {
        return newMockMvc(
                enabled,
                repository,
                maxPayloadBytes,
                maxRequests,
                properties,
                orchestrator,
                new InMemoryDecisionCoreRepository());
    }

    private MockMvc newMockMvc(
            final boolean enabled,
            final DecisionAuditRepository repository,
            final long maxPayloadBytes,
            final int maxRequests,
            final DecisionDryRunRuntimeProperties properties,
            final DecisionOrchestrator orchestrator,
            final InMemoryDecisionCoreRepository decisionCoreRepository) {
        return newMockMvc(
                enabled,
                repository,
                maxPayloadBytes,
                maxRequests,
                properties,
                orchestrator,
                decisionCoreRepository,
                new RecordingGatewayIntegration());
    }

    private MockMvc newMockMvc(
            final boolean enabled,
            final DecisionAuditRepository repository,
            final long maxPayloadBytes,
            final int maxRequests,
            final DecisionDryRunRuntimeProperties properties,
            final DecisionOrchestrator orchestrator,
            final InMemoryDecisionCoreRepository decisionCoreRepository,
            final QdrModelGatewayIntegrationPort gatewayIntegration) {
        // enabled 参数保留在签名中，便于测试调用点直接表达 feature gate 场景；实际 gate 值已在 properties 中冻结。
        assert enabled == properties.enabled();
        final DecisionDryRunController controller =
                new DecisionDryRunController(
                        new DefaultDecisionDryRunService(
                                orchestrator,
                                repository,
                                gatewayIntegration,
                                decisionCoreRepository,
                                decisionCoreRepository,
                                decisionCoreRepository,
                                decisionCoreRepository,
                                properties,
                                CLOCK),
                        new HmacNqDryRunAuthenticator(
                                Set.of("NQ_DRYRUN"),
                                Set.of("tenant-a:NQ_DRYRUN"),
                                SECRET,
                                Duration.ofMinutes(5),
                                maxPayloadBytes,
                                new InMemoryNonceReplayGuard(1000, Duration.ofMinutes(10), CLOCK)),
                        new InMemoryRateLimiter(60, maxRequests, 1000, CLOCK),
                        objectMapper);
        final MappingJackson2HttpMessageConverter jacksonConverter =
                new MappingJackson2HttpMessageConverter();
        jacksonConverter.setObjectMapper(objectMapper);
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8), jacksonConverter)
                .addFilters(
                        new DhApiAuthenticationFilter(
                                token ->
                                        GOOD_TOKEN.equals(token)
                                                ? new AuthContext(
                                                        "user-a",
                                                        "tenant-a",
                                                        Set.of("DH_API"),
                                                        authenticatedEnvironment)
                                                : null))
                .build();
    }

    private static DecisionOrchestrator defaultOrchestrator(final DecisionAuditRepository repository) {
        final DecisionProviderGuard providerGuard = new DefaultDecisionProviderGuard();
        return new DefaultDecisionOrchestrator(
                new DefaultDecisionContextBuilder(),
                new DefaultDecisionPolicyChecker(),
                new MockDecisionSignalProvider(),
                new DefaultDecisionRiskReviewer(),
                new DecisionOutputAssembler(),
                repository,
                providerGuard,
                new DefaultDecisionProviderLatencyRecorder(),
                CLOCK);
    }

    private static Map<String, Object> legalEnvelope(final String requestId) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("requestId", requestId);
        m.put("traceId", "trace-" + requestId);
        m.put("tenantId", "tenant-a");
        m.put("source", "NQ_DRYRUN");
        m.put("environment", "DEV");
        m.put("timestamp", NOW.toString());
        m.put("nonce", "nonce-" + requestId);
        m.put("schemaVersion", "1.0.0");
        m.put("dryRun", true);
        m.put(
                "decisionContext",
                Map.of(
                        "subject",
                        Map.of(
                                "symbol", "BTC-USDT",
                                "market", "CRYPTO",
                                "timeframe", "1h",
                                "strategyRef", "strategy-readonly",
                                "researchRef", "research-readonly"),
                        "contextRef",
                        "context://readonly",
                        "contextSnapshot",
                        Map.of(
                                "snapshotId", "snapshot-" + requestId,
                                "capturedAt", NOW.toString(),
                                "evidenceRefs", List.of("evidence://" + requestId))));
        m.put(
                "forbiddenCapabilities",
                List.of("PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE", "READ_NQ_DB", "WRITE_NQ_DB"));
        return m;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> context(final Map<String, Object> envelope) {
        return (Map<String, Object>) envelope.get("decisionContext");
    }

    private void assertProviderFailure(
            final ProviderSignalStatus providerStatus,
            final int status,
            final String errorCode,
            final String requestId)
            throws Exception {
        mockMvc =
                newMockMvc(
                        true,
                        new InMemoryDecisionAuditRepository(),
                        2048,
                        1000,
                        32768,
                        providerFailureOrchestrator(providerStatus));
        final Map<String, Object> envelope = legalEnvelope(requestId);
        final String body = objectMapper.writeValueAsString(envelope);

        mockMvc
                .perform(signedPost(envelope, body))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.errorCode").value(errorCode))
                .andExpect(jsonPath("$.auditRef").exists());
    }

    private static DecisionOrchestrator providerFailureOrchestrator(final ProviderSignalStatus status) {
        return request ->
                DecisionOutput.abstainForProviderFailure(
                        request.getRequestId(), request.getTraceId(), request.getTenantId(), status, NOW);
    }

    private MockHttpServletRequestBuilder signedPost(
            final Map<String, Object> envelope, final String body) {
        return signedPostWithSourceHeaderAndNonce(
                envelope, body, value(envelope.get("source")), value(envelope.get("nonce")));
    }

    private MockHttpServletRequestBuilder signedPostWithNonceHeader(
            final Map<String, Object> envelope, final String body, final String headerNonce) {
        return signedPostWithSourceHeaderAndNonce(
                envelope, body, value(envelope.get("source")), headerNonce);
    }

    private MockHttpServletRequestBuilder signedPostWithSourceHeader(
            final Map<String, Object> envelope, final String body, final String sourceHeader) {
        return signedPostWithSourceHeaderAndNonce(
                envelope, body, sourceHeader, value(envelope.get("nonce")));
    }

    private MockHttpServletRequestBuilder signedPostWithSourceHeaderAndNonce(
            final Map<String, Object> envelope,
            final String body,
            final String sourceHeader,
            final String headerNonce) {
        final NqDryRunAuthRequest unsigned =
                new NqDryRunAuthRequest(
                        "POST",
                        "/api/ai/decision-dry-runs",
                        value(envelope.get("source")),
                        value(envelope.get("source")),
                        "tenant-a",
                        value(envelope.get("tenantId")),
                        authenticatedEnvironment,
                        value(envelope.get("environment")),
                        value(envelope.get("timestamp")),
                        value(envelope.get("nonce")),
                        "",
                        value(envelope.get("requestId")),
                        value(envelope.get("traceId")),
                        value(envelope.get("schemaVersion")),
                        body,
                        body.getBytes(StandardCharsets.UTF_8).length,
                        NOW);
        final String signature =
                HmacNqDryRunAuthenticator.hmacSha256Hex(
                        SECRET, HmacNqDryRunAuthenticator.signatureMaterial(unsigned));
        return authenticatedPost(body)
                .header("X-NQ-DH-Source", sourceHeader)
                .header("X-NQ-DH-Tenant-Id", value(envelope.get("tenantId")))
                .header("X-NQ-DH-Request-Id", value(envelope.get("requestId")))
                .header("X-NQ-DH-Trace-Id", value(envelope.get("traceId")))
                .header("X-NQ-DH-Timestamp", value(envelope.get("timestamp")))
                .header("X-NQ-DH-Nonce", headerNonce)
                .header("X-NQ-DH-Signature", signature);
    }

    private MockHttpServletRequestBuilder postWithSignature(
            final Map<String, Object> envelope, final String body, final String signature) {
        return authenticatedPost(body)
                .header("X-NQ-DH-Source", value(envelope.get("source")))
                .header("X-NQ-DH-Tenant-Id", value(envelope.get("tenantId")))
                .header("X-NQ-DH-Request-Id", value(envelope.get("requestId")))
                .header("X-NQ-DH-Trace-Id", value(envelope.get("traceId")))
                .header("X-NQ-DH-Timestamp", value(envelope.get("timestamp")))
                .header("X-NQ-DH-Nonce", value(envelope.get("nonce")))
                .header("X-NQ-DH-Signature", signature);
    }

    private static MockHttpServletRequestBuilder authenticatedPost(final String body) {
        return post("/api/ai/decision-dry-runs")
                .header("Authorization", "Bearer " + GOOD_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static String value(final Object value) {
        return value == null ? "" : value.toString();
    }

    private static final class FailingAuditRepository implements DecisionAuditRepository {
        @Override
        public void saveRequest(final DecisionPersistenceRecords.RequestRecord record) {
            throw failure();
        }

        @Override
        public void saveContextSnapshot(final DecisionPersistenceRecords.ContextSnapshotRecord record) {
            throw failure();
        }

        @Override
        public void saveTraceStep(final DecisionPersistenceRecords.TraceStepRecord record) {
            throw failure();
        }

        @Override
        public void saveProviderCall(final DecisionPersistenceRecords.ProviderCallRecord record) {
            throw failure();
        }

        @Override
        public void saveOutput(final DecisionPersistenceRecords.OutputRecord record) {
            throw failure();
        }

        @Override
        public void saveAuditEvent(final DecisionPersistenceRecords.AuditEventRecord record) {
            throw failure();
        }

        private static RuntimeException failure() {
            return new IllegalStateException("audit write failure");
        }
    }

    private static final class RecordingGatewayIntegration implements QdrModelGatewayIntegrationPort {

        private final ModelGatewayFailureCode failureCode;

        private RecordingGatewayIntegration() {
            this(null);
        }

        private RecordingGatewayIntegration(final ModelGatewayFailureCode failureCode) {
            this.failureCode = failureCode;
        }

        @Override
        public QdrModelGatewayIntegrationResult invoke(
                final QdrModelGatewayIntegrationCommand command) {
            if (failureCode != null) {
                throw new QdrModelGatewayIntegrationException(failureCode, "gateway failed closed");
            }
            return new QdrModelGatewayIntegrationResult(
                    "prompt-version-webmvc",
                    "model-version-webmvc",
                    "provider-profile-webmvc",
                    "model-call:webmvc",
                    "ALLOWED",
                    "PASSED",
                    "input=10,rendered=20,output=5,estimated=9,memory=1",
                    "mock-qdr-review:webmvc",
                    "trace:model-call:webmvc",
                    "audit:model-call:webmvc");
        }
    }
}

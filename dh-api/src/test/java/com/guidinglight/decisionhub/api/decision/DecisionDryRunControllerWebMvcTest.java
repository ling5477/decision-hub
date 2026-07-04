package com.guidinglight.decisionhub.api.decision;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.guidinglight.decisionhub.security.nq.HmacNqDryRunAuthenticator;
import com.guidinglight.decisionhub.security.nq.InMemoryNonceReplayGuard;
import com.guidinglight.decisionhub.security.nq.InMemoryRateLimiter;
import com.guidinglight.decisionhub.security.nq.NqDryRunAuthRequest;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionOutputAssembler;
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
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunRuntimeProperties;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DefaultDecisionDryRunService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  @BeforeEach
  void setUp() {
    TimeProvider.useClock(CLOCK);
    objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
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
            .andExpect(header().exists("X-Trace-Id"))
            .andReturn();

    final String response = result.getResponse().getContentAsString();
    assertFalse(response.contains("BUY"));
    assertFalse(response.contains("SELL"));
    assertFalse(response.contains("PLACE_ORDER"));
    assertFalse(response.contains("CANCEL_ORDER"));
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
  void auditWriteFailureFailsClosed() throws Exception {
    mockMvc = newMockMvc(true, new FailingAuditRepository(), 2048, 1000);
    final Map<String, Object> envelope = legalEnvelope("req-audit-fail");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(signedPost(envelope, body))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.errorCode").value("UNKNOWN_ERROR"));
  }

  private MockMvc newMockMvc(
      final boolean enabled,
      final DecisionAuditRepository repository,
      final long maxPayloadBytes,
      final int maxRequests) {
    final DecisionDryRunRuntimeProperties properties =
        new DecisionDryRunRuntimeProperties(
            enabled, false, false, true, Set.of("NQ_DRYRUN"), Set.of("tenant-a:NQ_DRYRUN"), 32768);
    final DecisionProviderGuard providerGuard = new DefaultDecisionProviderGuard();
    final DefaultDecisionOrchestrator orchestrator =
        new DefaultDecisionOrchestrator(
            new DefaultDecisionContextBuilder(),
            new DefaultDecisionPolicyChecker(),
            new MockDecisionSignalProvider(),
            new DefaultDecisionRiskReviewer(),
            new DecisionOutputAssembler(),
            repository,
            providerGuard,
            new DefaultDecisionProviderLatencyRecorder(),
            CLOCK);
    final DecisionDryRunController controller =
        new DecisionDryRunController(
            new DefaultDecisionDryRunService(orchestrator, repository, properties, CLOCK),
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
                        ? new AuthContext("user-a", "tenant-a", Set.of("DH_API"))
                        : null))
        .build();
  }

  private static Map<String, Object> legalEnvelope(final String requestId) {
    final Map<String, Object> m = new LinkedHashMap<>();
    m.put("requestId", requestId);
    m.put("traceId", "trace-" + requestId);
    m.put("tenantId", "tenant-a");
    m.put("source", "NQ_DRYRUN");
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

  private MockHttpServletRequestBuilder signedPost(
      final Map<String, Object> envelope, final String body) {
    final NqDryRunAuthRequest unsigned =
        new NqDryRunAuthRequest(
            "POST",
            "/api/ai/decision-dry-runs",
            value(envelope.get("source")),
            value(envelope.get("source")),
            "tenant-a",
            value(envelope.get("tenantId")),
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
        .header("X-NQ-DH-Source", value(envelope.get("source")))
        .header("X-NQ-DH-Tenant-Id", value(envelope.get("tenantId")))
        .header("X-NQ-DH-Request-Id", value(envelope.get("requestId")))
        .header("X-NQ-DH-Trace-Id", value(envelope.get("traceId")))
        .header("X-NQ-DH-Timestamp", value(envelope.get("timestamp")))
        .header("X-NQ-DH-Nonce", value(envelope.get("nonce")))
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
}

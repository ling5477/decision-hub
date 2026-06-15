package com.guidinglight.decisionhub.api.feedback;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guidinglight.decisionhub.api.GlobalExceptionHandler;
import com.guidinglight.decisionhub.api.security.DhApiAuthenticationFilter;
import com.guidinglight.decisionhub.security.AuthContext;
import com.guidinglight.decisionhub.security.nq.HmacNqFeedbackAuthenticator;
import com.guidinglight.decisionhub.security.nq.InMemoryNonceReplayGuard;
import com.guidinglight.decisionhub.security.nq.InMemoryRateLimiter;
import com.guidinglight.decisionhub.security.nq.NqFeedbackAuthRequest;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionCommand;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionErrorCode;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionResult;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionService;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Stage2-PoC-B2 / DH-AUDIT-FIX / DH-NQ-HEADER-ALIGNMENT Batch 2：NqFeedbackController 的 MockMvc 入口测试。
 *
 * <p>覆盖正常 envelope、service 结果映射，以及 P1 修复要求的 API 认证、HMAC 签名、timestamp、
 * nonce/requestId 防重放、source allowlist 和 payload 大小限制。
 *
 * <p>Batch 2：入站为 <b>canonical-only</b> {@code X-NQ-DH-*}；成功路径用 canonical header，仅 legacy
 * {@code X-DH-NQ-*} 与缺失任一必需 canonical header 一律 fail-closed 拒绝（保持现有 401/403 语义）。
 */
class NqFeedbackControllerWebMvcTest {

  private static final String GOOD_TOKEN = "good-token";
  private static final String NQ_SECRET = "unit-test-nq-secret";

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;
  private AtomicReference<IngestionResult> nextResult;
  private AtomicReference<IngestionCommand> lastCommand;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(
        com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    nextResult = new AtomicReference<>(IngestionResult.accepted("evt-default"));
    lastCommand = new AtomicReference<>();

    final NqFeedbackIngestionService stubService =
        cmd -> {
          lastCommand.set(cmd);
          return nextResult.get();
        };
    final HmacNqFeedbackAuthenticator authenticator =
        new HmacNqFeedbackAuthenticator(
            Set.of("nexus-quant"),
            NQ_SECRET,
            Duration.ofMinutes(5),
            2048,
            new InMemoryNonceReplayGuard());
    // 既有用例不验证限流：注入宽松 limiter（每窗口 1000 次），避免与 429 分支耦合；
    // 429 行为由独立的 NqFeedbackRateLimitWebMvcTest 用严格阈值覆盖。
    final NqFeedbackController controller =
        new NqFeedbackController(
            stubService,
            authenticator,
            new InMemoryRateLimiter(60, 1000, 1000, java.time.Clock.systemUTC()));

    final MappingJackson2HttpMessageConverter jacksonConverter =
        new MappingJackson2HttpMessageConverter();
    jacksonConverter.setObjectMapper(objectMapper);

    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(jacksonConverter)
            .addFilters(
                new DhApiAuthenticationFilter(
                    token ->
                        GOOD_TOKEN.equals(token)
                            ? new AuthContext("user-a", "tenant-a", Set.of("DH_API"))
                            : null))
            .build();
  }

  @Test
  void post_withoutApiAuthentication_returns401() throws Exception {
    final String body = objectMapper.writeValueAsString(legalEnvelope("evt-no-auth"));

    mockMvc
        .perform(post("/api/ai/feedback/nq").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void post_missingSignature_returns401() throws Exception {
    final Map<String, Object> envelope = legalEnvelope("evt-missing-sig");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(
            authenticatedPost(body)
                .header("X-NQ-DH-Source", "nexus-quant")
                .header("X-NQ-DH-Timestamp", Instant.now().toString())
                .header("X-NQ-DH-Nonce", "nonce-missing-sig"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void post_wrongSignature_returns401() throws Exception {
    final Map<String, Object> envelope = legalEnvelope("evt-bad-sig");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(
            authenticatedPost(body)
                .header("X-NQ-DH-Source", "nexus-quant")
                .header("X-NQ-DH-Timestamp", Instant.now().toString())
                .header("X-NQ-DH-Nonce", "nonce-bad-sig")
                .header("X-NQ-DH-Signature", "bad"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("BAD_SIGNATURE"));
  }

  @Test
  void post_expiredTimestamp_returns401() throws Exception {
    final Map<String, Object> envelope = legalEnvelope("evt-expired");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(signedPost(envelope, body, "nonce-expired", Instant.now().minus(Duration.ofHours(1))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("TIMESTAMP_EXPIRED"));
  }

  @Test
  void post_replayedNonceAndRequestId_returns409() throws Exception {
    final Map<String, Object> envelope = legalEnvelope("evt-replay");
    final String body = objectMapper.writeValueAsString(envelope);
    final String nonce = "nonce-replay";
    final Instant timestamp = Instant.now();

    mockMvc.perform(signedPost(envelope, body, nonce, timestamp)).andExpect(status().isAccepted());

    mockMvc
        .perform(signedPost(envelope, body, nonce, timestamp))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("REPLAY_DETECTED"));
  }

  @Test
  void post_sourceOutsideAllowlist_returns403() throws Exception {
    final Map<String, Object> envelope = legalEnvelope("evt-source");
    envelope.put("sourceSystem", "unknown-source");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(signedPost(envelope, body, "nonce-source", Instant.now(), "unknown-source"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("SOURCE_NOT_ALLOWED"));
  }

  @Test
  void post_payloadTooLarge_returns413() throws Exception {
    final Map<String, Object> envelope = legalEnvelope("evt-large");
    envelope.put("payloadJson", "{\"rawPayloadJson\":\"" + "x".repeat(3000) + "\"}");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(signedPost(envelope, body, "nonce-large", Instant.now()))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.errorCode").value("PAYLOAD_TOO_LARGE"));
  }

  @Test
  void post_legacyOnlyHeaders_areRejected_canonicalOnly() throws Exception {
    // canonical-only：仅发送 legacy X-DH-NQ-* 等同缺失 canonical -> fail-closed（canonical source 缺失 -> 403）。
    final Map<String, Object> envelope = legalEnvelope("evt-legacy-only");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(
            authenticatedPost(body)
                .header("X-DH-NQ-Source", "nexus-quant")
                .header("X-DH-NQ-Timestamp", Instant.now().toString())
                .header("X-DH-NQ-Nonce", "nonce-legacy-only")
                .header("X-DH-NQ-Signature", "irrelevant-legacy-sig"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("SOURCE_NOT_ALLOWED"));
  }

  @Test
  void post_missingCanonicalSource_returns403() throws Exception {
    // 缺 canonical Source：body.sourceSystem 与（空的）header source 不一致 -> 403 SOURCE_NOT_ALLOWED。
    final Map<String, Object> envelope = legalEnvelope("evt-miss-src");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(
            authenticatedPost(body)
                .header("X-NQ-DH-Timestamp", Instant.now().toString())
                .header("X-NQ-DH-Nonce", "nonce-miss-src")
                .header("X-NQ-DH-Signature", "irrelevant"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("SOURCE_NOT_ALLOWED"));
  }

  @Test
  void post_missingCanonicalTimestamp_returns401() throws Exception {
    // 缺 canonical Timestamp：source 校验通过后 timestamp 为 null -> 401 TIMESTAMP_EXPIRED。
    final Map<String, Object> envelope = legalEnvelope("evt-miss-ts");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(
            authenticatedPost(body)
                .header("X-NQ-DH-Source", "nexus-quant")
                .header("X-NQ-DH-Nonce", "nonce-miss-ts")
                .header("X-NQ-DH-Signature", "irrelevant"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("TIMESTAMP_EXPIRED"));
  }

  @Test
  void post_missingCanonicalNonce_returns401() throws Exception {
    // 缺 canonical Nonce：source/timestamp 通过后 nonce 为空 -> 401 REPLAY_KEY_MISSING。
    final Map<String, Object> envelope = legalEnvelope("evt-miss-nonce");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(
            authenticatedPost(body)
                .header("X-NQ-DH-Source", "nexus-quant")
                .header("X-NQ-DH-Timestamp", Instant.now().toString())
                .header("X-NQ-DH-Signature", "irrelevant"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("REPLAY_KEY_MISSING"));
  }

  @Test
  void post_canonicalSuccess_doesNotLeakSignatureRawMaterial() throws Exception {
    nextResult.set(IngestionResult.accepted("evt-nosig-leak"));
    final Map<String, Object> envelope = legalEnvelope("evt-nosig-leak");
    final String body = objectMapper.writeValueAsString(envelope);
    final String nonce = "nonce-nosig-leak";
    final Instant ts = Instant.now();
    final String signature =
        canonicalSignature(envelope, body, nonce, ts, envelope.get("sourceSystem").toString());

    final MvcResult result =
        mockMvc
            .perform(signedPost(envelope, body, nonce, ts))
            .andExpect(status().isAccepted())
            .andReturn();

    final String responseBody = result.getResponse().getContentAsString();
    org.junit.jupiter.api.Assertions.assertFalse(
        responseBody.contains(signature), "202 body must not echo raw signature");
    org.junit.jupiter.api.Assertions.assertFalse(
        responseBody.contains(NQ_SECRET), "202 body must not leak secret");
  }

  @Test
  void post_canonicalBindingAllConsistent_returns202() throws Exception {
    // Batch 3：canonical Tenant/Request/Trace 与权威来源全一致 -> 通过（header 不覆盖权威来源）。
    nextResult.set(IngestionResult.accepted("evt-bind-ok"));
    final Map<String, Object> envelope = legalEnvelope("evt-bind-ok");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(
            signedPost(envelope, body, "nonce-bind-ok", Instant.now())
                .header("X-NQ-DH-Tenant-Id", "tenant-a")
                .header("X-NQ-DH-Request-Id", envelope.get("requestId").toString())
                .header("X-NQ-DH-Trace-Id", envelope.get("traceId").toString()))
        .andExpect(status().isAccepted());
  }

  @Test
  void post_tenantHeaderMismatch_returns403BindingMismatch() throws Exception {
    // canonical Tenant-Id 与认证上下文 tenant（tenant-a）不一致 -> 403 HEADER_BINDING_MISMATCH。
    final Map<String, Object> envelope = legalEnvelope("evt-bind-tenant");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(
            signedPost(envelope, body, "nonce-bind-tenant", Instant.now())
                .header("X-NQ-DH-Tenant-Id", "tenant-OTHER"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("HEADER_BINDING_MISMATCH"));
  }

  @Test
  void post_requestIdHeaderMismatch_returns403BindingMismatch() throws Exception {
    // canonical Request-Id 与 body requestId 不一致 -> 403 HEADER_BINDING_MISMATCH。
    final Map<String, Object> envelope = legalEnvelope("evt-bind-req");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(
            signedPost(envelope, body, "nonce-bind-req", Instant.now())
                .header("X-NQ-DH-Request-Id", "req-WRONG"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("HEADER_BINDING_MISMATCH"));
  }

  @Test
  void post_traceIdHeaderMismatch_returns403BindingMismatch() throws Exception {
    // canonical Trace-Id 与 body traceId 不一致 -> 403 HEADER_BINDING_MISMATCH。
    final Map<String, Object> envelope = legalEnvelope("evt-bind-trace");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(
            signedPost(envelope, body, "nonce-bind-trace", Instant.now())
                .header("X-NQ-DH-Trace-Id", "trace-WRONG"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("HEADER_BINDING_MISMATCH"));
  }

  @Test
  void post_bindingMismatch_doesNotLeakSensitive() throws Exception {
    final Map<String, Object> envelope = legalEnvelope("evt-bind-leak");
    final String body = objectMapper.writeValueAsString(envelope);
    final String nonce = "nonce-bind-leak";
    final Instant ts = Instant.now();
    final String signature =
        canonicalSignature(envelope, body, nonce, ts, envelope.get("sourceSystem").toString());

    final MvcResult result =
        mockMvc
            .perform(
                signedPost(envelope, body, nonce, ts).header("X-NQ-DH-Tenant-Id", "tenant-OTHER"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("HEADER_BINDING_MISMATCH"))
            .andReturn();

    final String responseBody = result.getResponse().getContentAsString();
    org.junit.jupiter.api.Assertions.assertFalse(
        responseBody.contains(signature), "binding mismatch body must not echo raw signature");
    org.junit.jupiter.api.Assertions.assertFalse(
        responseBody.contains(NQ_SECRET), "binding mismatch body must not leak secret");
    org.junit.jupiter.api.Assertions.assertFalse(
        responseBody.contains(envelope.get("payloadJson").toString()),
        "binding mismatch body must not echo full payload");
  }

  @Test
  void post_validEnvelope_returns202AcceptedReceivedOutcome() throws Exception {
    nextResult.set(IngestionResult.accepted("evt-1"));
    final Map<String, Object> envelope = legalEnvelope("evt-1");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(signedPost(envelope, body, "nonce-1", Instant.now()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.eventId").value("evt-1"))
        .andExpect(jsonPath("$.status").value("RECEIVED"))
        .andExpect(jsonPath("$.outcome").value("ACCEPTED"))
        .andExpect(jsonPath("$.traceId").value("trace-1"))
        .andExpect(jsonPath("$.correlationId").value("corr-1"))
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void post_validEnvelope_passesAuthenticatedTenantToService() throws Exception {
    nextResult.set(IngestionResult.accepted("evt-tenant"));
    final Map<String, Object> envelope = legalEnvelope("evt-tenant");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc.perform(signedPost(envelope, body, "nonce-tenant", Instant.now())).andExpect(status().isAccepted());

    org.junit.jupiter.api.Assertions.assertEquals("tenant-a", lastCommand.get().getTenantId());
  }

  @Test
  void post_duplicateOutcome_returns202WithDuplicateMarker() throws Exception {
    nextResult.set(IngestionResult.duplicate("evt-2"));
    final Map<String, Object> envelope = legalEnvelope("evt-2");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(signedPost(envelope, body, "nonce-2", Instant.now()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("RECEIVED"))
        .andExpect(jsonPath("$.outcome").value("DUPLICATE"));
  }

  @Test
  void post_unknownEventType_returns400WithErrorCode() throws Exception {
    nextResult.set(
        IngestionResult.rejected("evt-3", IngestionErrorCode.UNKNOWN_EVENT_TYPE, "boom"));
    final Map<String, Object> envelope = legalEnvelope("evt-3");
    envelope.put("eventType", "WHO_KNOWS");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(signedPost(envelope, body, "nonce-3", Instant.now()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.errorCode").value("UNKNOWN_EVENT_TYPE"))
        .andExpect(jsonPath("$.eventId").value("evt-3"));
  }

  @Test
  void post_invalidSchema_returns400() throws Exception {
    nextResult.set(
        IngestionResult.rejected("evt-4", IngestionErrorCode.INVALID_SCHEMA, "bad payload"));
    final Map<String, Object> envelope = legalEnvelope("evt-4");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(signedPost(envelope, body, "nonce-4", Instant.now()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_SCHEMA"));
  }

  @Test
  void post_unknownTrace_returns400() throws Exception {
    nextResult.set(IngestionResult.rejected("evt-5", IngestionErrorCode.UNKNOWN_TRACE, "no run"));
    final Map<String, Object> envelope = legalEnvelope("evt-5");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(signedPost(envelope, body, "nonce-5", Instant.now()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("UNKNOWN_TRACE"));
  }

  @Test
  void post_beanValidationFailure_returns400FromGlobalHandler() throws Exception {
    final Map<String, Object> envelope = legalEnvelope("evt-6");
    envelope.remove("eventId");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc.perform(authenticatedPost(body)).andExpect(status().isBadRequest());
  }

  @Test
  void post_passesDistinctTraceRequestCorrelationSourceJob_toService() throws Exception {
    nextResult.set(IngestionResult.accepted("evt-7"));
    final Map<String, Object> envelope = legalEnvelope("evt-7");
    envelope.put("traceId", "trace-X");
    envelope.put("requestId", "req-X");
    envelope.put("correlationId", "corr-X");
    envelope.put("sourceJobId", "src-X");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc.perform(signedPost(envelope, body, "nonce-7", Instant.now())).andExpect(status().isAccepted());

    final IngestionCommand cmd = lastCommand.get();
    org.junit.jupiter.api.Assertions.assertEquals("trace-X", cmd.getTraceId());
    org.junit.jupiter.api.Assertions.assertEquals("req-X", cmd.getRequestId());
    org.junit.jupiter.api.Assertions.assertEquals("corr-X", cmd.getCorrelationId());
    org.junit.jupiter.api.Assertions.assertEquals("src-X", cmd.getSourceJobId());
  }

  /** 最小合法 envelope；用 LinkedHashMap 保留序列化顺序，便于排查。 */
  private static Map<String, Object> legalEnvelope(final String eventId) {
    final Map<String, Object> m = new LinkedHashMap<>();
    m.put("eventId", eventId);
    m.put("eventType", "PAPER_RUN_CREATED");
    m.put("occurredAt", "2026-05-25T08:00:00Z");
    m.put("sourceSystem", "nexus-quant");
    m.put("sourceJobId", "src-job-1");
    m.put("traceId", "trace-1");
    m.put("requestId", "req-" + eventId);
    m.put("correlationId", "corr-1");
    m.put("schemaVersion", "1.0.0");
    m.put(
        "payloadJson",
        "{\"paperRunId\":\"pr-1\",\"candidateId\":\"c-1\","
            + "\"strategyName\":\"S1\",\"requestedBy\":\"alice\","
            + "\"createdAt\":\"2026-05-25T08:00:00Z\",\"rawPayloadJson\":\"{}\"}");
    return m;
  }

  private MockHttpServletRequestBuilder signedPost(
      final Map<String, Object> envelope,
      final String body,
      final String nonce,
      final Instant timestamp) {
    return signedPost(envelope, body, nonce, timestamp, envelope.get("sourceSystem").toString());
  }

  private MockHttpServletRequestBuilder signedPost(
      final Map<String, Object> envelope,
      final String body,
      final String nonce,
      final Instant timestamp,
      final String sourceHeader) {
    final String signature = canonicalSignature(envelope, body, nonce, timestamp, sourceHeader);
    return authenticatedPost(body)
        .header("X-NQ-DH-Source", sourceHeader)
        .header("X-NQ-DH-Timestamp", timestamp.toString())
        .header("X-NQ-DH-Nonce", nonce)
        .header("X-NQ-DH-Signature", signature);
  }

  /** 计算 value-based 签名（与生产 authenticator 的 signatureMaterial 一致；不含 header name）。 */
  private static String canonicalSignature(
      final Map<String, Object> envelope,
      final String body,
      final String nonce,
      final Instant timestamp,
      final String sourceHeader) {
    return HmacNqFeedbackAuthenticator.hmacSha256Hex(
        NQ_SECRET,
        HmacNqFeedbackAuthenticator.signatureMaterial(
            new NqFeedbackAuthRequest(
                sourceHeader,
                envelope.get("sourceSystem").toString(),
                timestamp.toString(),
                nonce,
                "",
                value(envelope.get("eventId")),
                value(envelope.get("requestId")),
                value(envelope.get("traceId")),
                value(envelope.get("payloadJson")),
                body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                timestamp)));
  }

  private static MockHttpServletRequestBuilder authenticatedPost(final String body) {
    return post("/api/ai/feedback/nq")
        .header("Authorization", "Bearer " + GOOD_TOKEN)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
  }

  private static String value(final Object value) {
    return value == null ? "" : value.toString();
  }
}

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Stage2-PoC-B2 / DH-AUDIT-FIX：NqFeedbackController 的 MockMvc 入口测试。
 *
 * <p>覆盖正常 envelope、service 结果映射，以及 P1 修复要求的 API 认证、HMAC 签名、timestamp、
 * nonce/requestId 防重放、source allowlist 和 payload 大小限制。
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
    final NqFeedbackController controller = new NqFeedbackController(stubService, authenticator);

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
                .header("X-DH-NQ-Source", "nexus-quant")
                .header("X-DH-NQ-Timestamp", Instant.now().toString())
                .header("X-DH-NQ-Nonce", "nonce-missing-sig"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void post_wrongSignature_returns401() throws Exception {
    final Map<String, Object> envelope = legalEnvelope("evt-bad-sig");
    final String body = objectMapper.writeValueAsString(envelope);

    mockMvc
        .perform(
            authenticatedPost(body)
                .header("X-DH-NQ-Source", "nexus-quant")
                .header("X-DH-NQ-Timestamp", Instant.now().toString())
                .header("X-DH-NQ-Nonce", "nonce-bad-sig")
                .header("X-DH-NQ-Signature", "bad"))
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
    final String signature =
        HmacNqFeedbackAuthenticator.hmacSha256Hex(
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
    return authenticatedPost(body)
        .header("X-DH-NQ-Source", sourceHeader)
        .header("X-DH-NQ-Timestamp", timestamp.toString())
        .header("X-DH-NQ-Nonce", nonce)
        .header("X-DH-NQ-Signature", signature);
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

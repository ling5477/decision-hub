package com.guidinglight.decisionhub.api.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionResult;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-3：NqFeedbackController 入站限流（429 RATE_LIMITED）入口测试。
 *
 * <p>覆盖：超阈值返回 429 + RATE_LIMITED；429 响应不泄露阈值 / 窗口 / 计数 / 密钥；限流在 HMAC / ingestion 之前
 * 短路（被限流请求不触发任何下游处理，no_trading_side_effect 代理验证）。严格阈值 maxRequests=1。
 */
class NqFeedbackRateLimitWebMvcTest {

  private static final String GOOD_TOKEN = "good-token";
  private static final String NQ_SECRET = "unit-test-nq-secret";

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;
  private AtomicInteger ingestionCalls;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(
        com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    ingestionCalls = new AtomicInteger(0);
    final NqFeedbackIngestionService stubService =
        (final IngestionCommand cmd) -> {
          ingestionCalls.incrementAndGet();
          return IngestionResult.accepted(cmd.getEventId());
        };

    final HmacNqFeedbackAuthenticator authenticator =
        new HmacNqFeedbackAuthenticator(
            Set.of("nexus-quant"),
            NQ_SECRET,
            Duration.ofMinutes(5),
            2048,
            new InMemoryNonceReplayGuard());

    // 严格限流：每窗口仅 1 次，便于第二次请求触发 429；窗口足够长，避免测试期间自然过期。
    final NqFeedbackController controller =
        new NqFeedbackController(
            stubService, authenticator, new InMemoryRateLimiter(60, 1, 1000, Clock.systemUTC()));

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
  void rate_limit_returns_429_with_rate_limited() throws Exception {
    // 第一次（合法签名）放行进入认证链路 -> 202。
    final Map<String, Object> first = legalEnvelope("evt-rl-1");
    final String firstBody = objectMapper.writeValueAsString(first);
    mockMvc
        .perform(signedPost(first, firstBody, "nonce-rl-1", Instant.now()))
        .andExpect(status().isAccepted());

    // 第二次使用不同 nonce/requestId（绝非 replay），仅因 source+tenant+route 同桶超限 -> 429 RATE_LIMITED。
    final Map<String, Object> second = legalEnvelope("evt-rl-2");
    final String secondBody = objectMapper.writeValueAsString(second);
    mockMvc
        .perform(signedPost(second, secondBody, "nonce-rl-2", Instant.now()))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.error").value("RATE_LIMITED"))
        .andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"));
  }

  @Test
  void rate_limit_does_not_leak_internal_thresholds() throws Exception {
    final Map<String, Object> first = legalEnvelope("evt-leak-1");
    mockMvc
        .perform(signedPost(first, objectMapper.writeValueAsString(first), "nonce-leak-1", Instant.now()))
        .andExpect(status().isAccepted());

    final Map<String, Object> second = legalEnvelope("evt-leak-2");
    final MvcResult result =
        mockMvc
            .perform(
                signedPost(
                    second, objectMapper.writeValueAsString(second), "nonce-leak-2", Instant.now()))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"))
            .andReturn();

    final String body = result.getResponse().getContentAsString();
    // 不得泄露内部阈值 / 窗口 / 计数 / 密钥 / 签名材料。
    final String lower = body.toLowerCase(java.util.Locale.ROOT);
    assertFalse(lower.contains("window"), "429 body must not leak window: " + body);
    assertFalse(lower.contains("maxrequest"), "429 body must not leak maxRequests: " + body);
    assertFalse(lower.contains("max-request"), "429 body must not leak max-requests: " + body);
    assertFalse(lower.contains("maxkey"), "429 body must not leak maxKeys: " + body);
    assertFalse(lower.contains("count"), "429 body must not leak count: " + body);
    assertFalse(lower.contains("threshold"), "429 body must not leak threshold: " + body);
    assertFalse(lower.contains("retry-after"), "429 body must not leak retry window: " + body);
    assertFalse(body.contains(NQ_SECRET), "429 body must not leak secret");
  }

  @Test
  void rate_limited_request_does_not_invoke_downstream_ingestion() throws Exception {
    // no_trading_side_effect 代理：被限流的请求必须在进入认证 / ingestion 之前短路，零下游副作用。
    final Map<String, Object> first = legalEnvelope("evt-side-1");
    mockMvc
        .perform(signedPost(first, objectMapper.writeValueAsString(first), "nonce-side-1", Instant.now()))
        .andExpect(status().isAccepted());
    assertEquals(1, ingestionCalls.get());

    final Map<String, Object> second = legalEnvelope("evt-side-2");
    mockMvc
        .perform(signedPost(second, objectMapper.writeValueAsString(second), "nonce-side-2", Instant.now()))
        .andExpect(status().isTooManyRequests());
    // ingestion 计数保持 1：限流短路，未触发任何下游入库 / 处理。
    assertEquals(1, ingestionCalls.get());
  }

  /** 最小合法 envelope。 */
  private static Map<String, Object> legalEnvelope(final String eventId) {
    final Map<String, Object> m = new LinkedHashMap<>();
    m.put("eventId", eventId);
    m.put("eventType", "PAPER_RUN_CREATED");
    m.put("occurredAt", "2026-05-25T08:00:00Z");
    m.put("sourceSystem", "nexus-quant");
    m.put("sourceJobId", "src-job-1");
    m.put("traceId", "trace-rl");
    m.put("requestId", "req-" + eventId);
    m.put("correlationId", "corr-rl");
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
    final String sourceHeader = envelope.get("sourceSystem").toString();
    final String signature =
        HmacNqFeedbackAuthenticator.hmacSha256Hex(
            NQ_SECRET,
            HmacNqFeedbackAuthenticator.signatureMaterial(
                new NqFeedbackAuthRequest(
                    sourceHeader,
                    sourceHeader,
                    timestamp.toString(),
                    nonce,
                    "",
                    value(envelope.get("eventId")),
                    value(envelope.get("requestId")),
                    value(envelope.get("traceId")),
                    value(envelope.get("payloadJson")),
                    body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                    timestamp)));
    return post("/api/ai/feedback/nq")
        .header("Authorization", "Bearer " + GOOD_TOKEN)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .header("X-NQ-DH-Source", sourceHeader)
        .header("X-NQ-DH-Timestamp", timestamp.toString())
        .header("X-NQ-DH-Nonce", nonce)
        .header("X-NQ-DH-Signature", signature);
  }

  private static String value(final Object value) {
    return value == null ? "" : value.toString();
  }
}

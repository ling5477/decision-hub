package com.guidinglight.decisionhub.api.feedback;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guidinglight.decisionhub.api.GlobalExceptionHandler;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionCommand;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionErrorCode;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionResult;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Stage2-PoC-B2：NqFeedbackController 的 MockMvc 入口测试。
 *
 * <p>覆盖：
 *
 * <ol>
 *   <li>合法 envelope -> 202 + status=RECEIVED + outcome=ACCEPTED。
 *   <li>service 返回 DUPLICATE -> 202 + outcome=DUPLICATE。
 *   <li>service 返回 REJECTED UNKNOWN_EVENT_TYPE -> 400 + errorCode=UNKNOWN_EVENT_TYPE。
 *   <li>service 返回 REJECTED INVALID_SCHEMA -> 400 + errorCode=INVALID_SCHEMA。
 *   <li>缺失字段（bean validation）-> 400（GlobalExceptionHandler 包装为 ApiResponse 形态）。
 * </ol>
 */
class NqFeedbackControllerWebMvcTest {

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
    final NqFeedbackController controller = new NqFeedbackController(stubService);

    final MappingJackson2HttpMessageConverter jacksonConverter =
        new MappingJackson2HttpMessageConverter();
    jacksonConverter.setObjectMapper(objectMapper);

    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(jacksonConverter)
            .build();
  }

  @Test
  void post_validEnvelope_returns202AcceptedReceivedOutcome() throws Exception {
    nextResult.set(IngestionResult.accepted("evt-1"));
    final String body = objectMapper.writeValueAsString(legalEnvelope("evt-1"));
    mockMvc
        .perform(post("/api/ai/feedback/nq").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.eventId").value("evt-1"))
        .andExpect(jsonPath("$.status").value("RECEIVED"))
        .andExpect(jsonPath("$.outcome").value("ACCEPTED"))
        .andExpect(jsonPath("$.traceId").value("trace-1"))
        .andExpect(jsonPath("$.correlationId").value("corr-1"))
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void post_duplicateOutcome_returns202WithDuplicateMarker() throws Exception {
    nextResult.set(IngestionResult.duplicate("evt-2"));
    final String body = objectMapper.writeValueAsString(legalEnvelope("evt-2"));
    mockMvc
        .perform(post("/api/ai/feedback/nq").contentType(MediaType.APPLICATION_JSON).content(body))
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
        .perform(post("/api/ai/feedback/nq").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.errorCode").value("UNKNOWN_EVENT_TYPE"))
        .andExpect(jsonPath("$.eventId").value("evt-3"));
  }

  @Test
  void post_invalidSchema_returns400() throws Exception {
    nextResult.set(
        IngestionResult.rejected("evt-4", IngestionErrorCode.INVALID_SCHEMA, "bad payload"));
    final String body = objectMapper.writeValueAsString(legalEnvelope("evt-4"));
    mockMvc
        .perform(post("/api/ai/feedback/nq").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_SCHEMA"));
  }

  @Test
  void post_unknownTrace_returns400() throws Exception {
    nextResult.set(IngestionResult.rejected("evt-5", IngestionErrorCode.UNKNOWN_TRACE, "no run"));
    final String body = objectMapper.writeValueAsString(legalEnvelope("evt-5"));
    mockMvc
        .perform(post("/api/ai/feedback/nq").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("UNKNOWN_TRACE"));
  }

  @Test
  void post_beanValidationFailure_returns400FromGlobalHandler() throws Exception {
    final Map<String, Object> envelope = legalEnvelope("evt-6");
    envelope.remove("eventId"); // 触发 @NotBlank 失败
    final String body = objectMapper.writeValueAsString(envelope);
    mockMvc
        .perform(post("/api/ai/feedback/nq").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
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
    mockMvc
        .perform(post("/api/ai/feedback/nq").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isAccepted());

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
    m.put("requestId", "req-1");
    m.put("correlationId", "corr-1");
    m.put("schemaVersion", "1.0.0");
    m.put(
        "payloadJson",
        "{\"paperRunId\":\"pr-1\",\"candidateId\":\"c-1\","
            + "\"strategyName\":\"S1\",\"requestedBy\":\"alice\","
            + "\"createdAt\":\"2026-05-25T08:00:00Z\",\"rawPayloadJson\":\"{}\"}");
    return m;
  }
}

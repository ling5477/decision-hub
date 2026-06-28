package com.guidinglight.decisionhub.api.legacy.run;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guidinglight.decisionhub.api.GlobalExceptionHandler;
import com.guidinglight.decisionhub.api.security.DhApiAuthenticationFilter;
import com.guidinglight.decisionhub.domain.run.Run;
import com.guidinglight.decisionhub.domain.run.RunStatus;
import com.guidinglight.decisionhub.ledger.EventStore;
import com.guidinglight.decisionhub.ledger.LedgerEvent;
import com.guidinglight.decisionhub.security.AuthContext;
import com.guidinglight.decisionhub.usecase.run.RunRepository;
import com.guidinglight.decisionhub.usecase.run.RunService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** DH-CODE-REALITY-AUDIT-FIX-PACK：deprecated /legacy/runs 入口的认证与 tenant 隔离回归。 */
@SuppressWarnings("deprecation")
final class LegacyRunControllerSecurityWebMvcTest {

  private static final String GOOD_TOKEN = "good-token";

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;
  private RecordingRunService runService;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(
        com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    runService = new RecordingRunService();
    final RunController controller = new RunController(runService);
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
  void post_withoutAuthentication_returns401AndDoesNotCreateRun() throws Exception {
    mockMvc
        .perform(
            post("/legacy/runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"legacy?\"}"))
        .andExpect(status().isUnauthorized());

    org.junit.jupiter.api.Assertions.assertEquals(
        0, runService.createCalls, "anonymous legacy POST must not reach RunService");
  }

  @Test
  void get_withoutAuthentication_returns401AndDoesNotReadRun() throws Exception {
    mockMvc.perform(get("/legacy/runs/run-a")).andExpect(status().isUnauthorized());

    org.junit.jupiter.api.Assertions.assertEquals(
        0, runService.getCalls, "anonymous legacy GET must not reach RunService");
  }

  @Test
  void post_withAuthentication_usesAuthenticatedTenant() throws Exception {
    mockMvc
        .perform(
            post("/legacy/runs")
                .header("Authorization", "Bearer " + GOOD_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"legacy?\",\"configSnapshot\":{\"mode\":\"test\"}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.tenantId").value("tenant-a"));

    org.junit.jupiter.api.Assertions.assertEquals("tenant-a", runService.lastCreateTenant);
  }

  @Test
  void get_withAuthentication_rejectsCrossTenantRun() throws Exception {
    mockMvc
        .perform(get("/legacy/runs/run-b").header("Authorization", "Bearer " + GOOD_TOKEN))
        .andExpect(status().isForbidden());
  }

  @Test
  void get_withAuthentication_returnsSameTenantRun() throws Exception {
    mockMvc
        .perform(get("/legacy/runs/run-a").header("Authorization", "Bearer " + GOOD_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.runId").value("run-a"))
        .andExpect(jsonPath("$.data.tenantId").value("tenant-a"));
  }

  /**
   * 只记录 controller 调用，不调用 provider 或旧执行链路。
   *
   * <p>Why：本测试只验证 legacy HTTP 安全边界，不允许为了测试触发任何真实外联或模型 provider 行为。
   */
  private static final class RecordingRunService extends RunService {

    private final Map<String, Run> runs = new LinkedHashMap<>();
    private int createCalls;
    private int getCalls;
    private String lastCreateTenant;

    RecordingRunService() {
      super(new NoopRunRepository(), new NoopEventStore(), List.of());
      runs.put("run-a", fixedRun("run-a", "tenant-a"));
      runs.put("run-b", fixedRun("run-b", "tenant-b"));
    }

    @Override
    public Run create(
        final String tenantId, final String question, final Map<String, Object> configSnapshot) {
      createCalls++;
      lastCreateTenant = tenantId;
      final Run run =
          Run.rehydrate(
              "run-created",
              tenantId,
              RunStatus.DRAFT,
              question,
              configSnapshot,
              Instant.parse("2026-06-28T00:00:00Z"),
              Instant.parse("2026-06-28T00:00:00Z"));
      runs.put(run.getRunId(), run);
      return run;
    }

    @Override
    public Run get(final String runId) {
      getCalls++;
      return runs.get(runId);
    }

    private static Run fixedRun(final String runId, final String tenantId) {
      return Run.rehydrate(
          runId,
          tenantId,
          RunStatus.DRAFT,
          "legacy question",
          Map.of(),
          Instant.parse("2026-06-28T00:00:00Z"),
          Instant.parse("2026-06-28T00:00:00Z"));
    }
  }

  /** RunService 父类构造所需的空仓储；测试覆盖的是 override 后的 controller 行为。 */
  private static final class NoopRunRepository implements RunRepository {

    @Override
    public void save(final Run run) {}

    @Override
    public Optional<Run> findById(final String runId) {
      return Optional.empty();
    }
  }

  /** RunService 父类构造所需的空事件仓储；不会在本测试路径被调用。 */
  private static final class NoopEventStore implements EventStore {

    @Override
    public void append(final LedgerEvent event) {}

    @Override
    public List<LedgerEvent> listByRunId(final String runId) {
      return List.of();
    }
  }
}

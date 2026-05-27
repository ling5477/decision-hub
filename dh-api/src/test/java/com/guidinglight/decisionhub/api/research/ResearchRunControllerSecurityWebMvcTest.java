package com.guidinglight.decisionhub.api.research;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guidinglight.decisionhub.api.GlobalExceptionHandler;
import com.guidinglight.decisionhub.api.security.AuthenticatedRequest;
import com.guidinglight.decisionhub.api.security.DhApiAuthenticationFilter;
import com.guidinglight.decisionhub.domain.agent.AgentTask;
import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import com.guidinglight.decisionhub.domain.judge.JudgeDecision;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.security.AuthContext;
import com.guidinglight.decisionhub.security.TokenVerifier;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunCommandService;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunQueryService;
import java.time.Instant;
import java.util.ArrayList;
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

/** DH-AUDIT-FIX：ResearchRun API 认证与 tenant 隔离测试。 */
final class ResearchRunControllerSecurityWebMvcTest {

  private static final String GOOD_TOKEN = "good-token";

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(
        com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    final InMemoryResearchServices services = new InMemoryResearchServices();
    services.seed(ResearchRun.create("tenant-b", "other tenant", Map.of(), Instant.parse("2026-05-26T00:00:00Z")));

    final ResearchRunController controller = new ResearchRunController(services, services);
    final MappingJackson2HttpMessageConverter jacksonConverter =
        new MappingJackson2HttpMessageConverter();
    jacksonConverter.setObjectMapper(objectMapper);

    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(jacksonConverter)
            .addFilters(new DhApiAuthenticationFilter(tokenVerifier()))
            .build();
  }

  @Test
  void create_withoutAuthentication_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/research-runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"x\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void list_withoutAuthentication_returns401() throws Exception {
    mockMvc.perform(get("/api/ai/research-runs")).andExpect(status().isUnauthorized());
  }

  @Test
  void create_withValidAuthentication_usesAuthenticatedTenant() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/research-runs")
                .header("Authorization", "Bearer " + GOOD_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"x\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.tenantId").value("tenant-a"));
  }

  @Test
  void list_withValidAuthentication_isTenantScoped() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/research-runs")
                .header("Authorization", "Bearer " + GOOD_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"tenant-a topic\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/ai/research-runs").header("Authorization", "Bearer " + GOOD_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].tenantId").value("tenant-a"));
  }

  @Test
  void requestTenantHeaderMismatch_returns403() throws Exception {
    mockMvc
        .perform(
            get("/api/ai/research-runs")
                .header("Authorization", "Bearer " + GOOD_TOKEN)
                .header(AuthenticatedRequest.TENANT_HEADER, "tenant-b"))
        .andExpect(status().isForbidden());
  }

  private static TokenVerifier tokenVerifier() {
    return token ->
        GOOD_TOKEN.equals(token) ? new AuthContext("user-a", "tenant-a", Set.of("DH_API")) : null;
  }

  private static final class InMemoryResearchServices
      implements ResearchRunCommandService, ResearchRunQueryService {

    private final List<ResearchRun> runs = new ArrayList<>();

    void seed(final ResearchRun run) {
      runs.add(run);
    }

    @Override
    public ResearchRun create(
        final String tenantId, final String topic, final Map<String, Object> payloadJson) {
      final ResearchRun run = ResearchRun.create(tenantId, topic, payloadJson, Instant.parse("2026-05-26T00:00:00Z"));
      runs.add(run);
      return run;
    }

    @Override
    public ResearchRun start(final String runId) {
      return findRun(runId).orElseThrow();
    }

    @Override
    public List<ResearchRun> listRuns(final String tenantId) {
      return runs.stream().filter(r -> tenantId.equals(r.getTenantId())).toList();
    }

    @Override
    public Optional<ResearchRun> findRun(final String runId) {
      return runs.stream().filter(r -> runId.equals(r.getRunId())).findFirst();
    }

    @Override
    public Optional<AgentTask> findTask(final String runId) {
      return Optional.empty();
    }

    @Override
    public List<StrategyCandidate> listCandidates(final String runId) {
      return List.of();
    }

    @Override
    public Optional<JudgeDecision> findJudgeDecision(final String runId) {
      return Optional.empty();
    }
  }
}

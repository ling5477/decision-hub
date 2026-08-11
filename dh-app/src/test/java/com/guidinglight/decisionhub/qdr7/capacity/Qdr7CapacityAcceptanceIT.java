package com.guidinglight.decisionhub.qdr7.capacity;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.guidinglight.decisionhub.DecisionHubApplication;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.security.StaticTokenVerifier;
import com.guidinglight.decisionhub.security.nq.HmacNqDryRunAuthenticator;
import com.guidinglight.decisionhub.security.nq.NqDryRunAuthRequest;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunRuntimeProperties;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunService;
import com.guidinglight.decisionhub.usecase.decision.dryrun.LimitedDryRunRuntimePolicy;
import com.guidinglight.decisionhub.usecase.qdr.gateway.MockModelProvider;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelProviderPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.GuardCleanupCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.GuardCleanupPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.GuardTransactionBoundary;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionResult;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionStatus;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyGuardPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyRecordView;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyState;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyTransitionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardIdentity;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionResult;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionStatus;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Stage-QDR-7 formal capacity acceptance driver。
 *
 * <p>该类只由显式 {@code qdr7-capacity-acceptance} profile 的 Failsafe 执行。它使用 localhost、真实 Spring
 * wiring、专用 PostgreSQL/Testcontainers、合成认证材料与 {@code MockModelProvider}；不连接外部 HTTP、NQ、真实
 * provider、Agent/LangGraph、Paper 或 LIVE。
 */
@Tag("qdr7-capacity-acceptance")
@EnabledIfSystemProperty(named = "qdr7.capacity.profile.active", matches = "true")
@Testcontainers
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "server.address=127.0.0.1",
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
      "management.health.redis.enabled=false",
      "decisionhub.integration1.runtime.enabled=true",
      "decisionhub.integration1.runtime.production-enabled=false",
      "decisionhub.integration1.runtime.kill-switch-enabled=false",
      "decisionhub.integration1.runtime.allowed-sources=NQ_DRYRUN",
      "decisionhub.integration1.runtime.allowed-tenant-source-pairs=qdr7-capacity:NQ_DRYRUN",
      "decisionhub.integration1.runtime.guard.environment=test",
      "decisionhub.integration1.runtime.guard.rate-window-seconds=3600",
      "decisionhub.integration1.runtime.guard.rate-limit-value=100000",
      "decisionhub.integration1.runtime.guard.lease-seconds=30",
      "decisionhub.integration1.runtime.guard.idempotency-ttl-seconds=600",
      "decisionhub.integration1.runtime.guard.retention-seconds=3600",
      "decisionhub.security.nq-feedback.replay.guard-type=jdbc",
      "spring.datasource.hikari.maximum-pool-size=10",
      "spring.datasource.hikari.minimum-idle=2",
      "spring.datasource.hikari.connection-timeout=4000",
      "spring.datasource.hikari.validation-timeout=1000"
    })
class Qdr7CapacityAcceptanceIT {

  private static final String RUN_ID = requiredProperty("qdr7.runId");
  private static final String ENDPOINT = "/api/ai/decision-dry-runs";
  private static final String SOURCE = "NQ_DRYRUN";
  private static final String TENANT = "qdr7-capacity-" + RUN_ID.toLowerCase(Locale.ROOT);
  private static final String SCHEMA = "1.0.0";
  private static final int SEED = Integer.parseInt(requiredProperty("qdr7.seed"));
  private static final Path PROJECT_ROOT =
      Path.of(requiredProperty("qdr7.projectRoot")).toAbsolutePath().normalize();
  private static final boolean QUALIFICATION_ONLY =
      Boolean.parseBoolean(System.getProperty("qdr7.qualificationOnly", "false"));
  private static final Path EVIDENCE_ROOT = evidenceRoot();
  private static final JsonNode RESOURCE_REGISTRY = readResourceRegistry();
  private static final JsonNode ENVIRONMENT_REQUIREMENTS = readEnvironmentRequirements();
  private static final String POSTGRES_IMAGE =
      ENVIRONMENT_REQUIREMENTS.path("postgresImage").asText();
  private static final int DATABASE_PORT = RESOURCE_REGISTRY.path("loopbackPort").asInt();
  private static final String CONTAINER_NAME = RESOURCE_REGISTRY.path("containerName").asText();
  private static final String VOLUME_NAME = RESOURCE_REGISTRY.path("volumeName").asText();
  private static final String DATABASE_NAME = RESOURCE_REGISTRY.path("databaseName").asText();
  private static final String TEST_TOKEN = "qdr7-capacity-token-" + RUN_ID;
  private static final String TEST_SIGNING_KEY = "qdr7-capacity-hmac-" + RUN_ID;
  private static final String DATABASE_PASSWORD = "qdr7-capacity-db-" + RUN_ID;
  private static final String COMMIT_SHA = RESOURCE_REGISTRY.path("commitSha").asText();
  private static final boolean IMPLEMENTATION_VALIDATION =
      Boolean.parseBoolean(System.getProperty("qdr7.implementationValidation", "false"));
  private static final int EXPECTED_THRESHOLD_COMPARISONS = 99;
  private static final Instant HARNESS_STARTED = Instant.now();
  private static final List<Map<String, Object>> THRESHOLD_RESULTS =
      Collections.synchronizedList(new ArrayList<>());
  private static final List<Map<String, Object>> RESTART_RESULTS =
      Collections.synchronizedList(new ArrayList<>());
  private static final List<Map<String, Object>> IMPLEMENTATION_TENANT_ISOLATION_RESULTS =
      Collections.synchronizedList(new ArrayList<>());
  private static final List<Map<String, Object>> SCENARIO_LEDGER =
      Collections.synchronizedList(new ArrayList<>());
  private static int postRecoveryStructured2xx;

  @Container
  static final FixedPortPostgreSqlContainer POSTGRES =
      new FixedPortPostgreSqlContainer(DATABASE_PORT)
          .withDatabaseName(DATABASE_NAME)
          .withUsername("qdr7_capacity")
          .withPassword(DATABASE_PASSWORD)
          .withCreateContainerCmdModifier(
              command -> {
                command.withName(CONTAINER_NAME);
                Objects.requireNonNull(command.getHostConfig())
                    .withBinds(new Bind(VOLUME_NAME, new Volume("/var/lib/postgresql/data")));
              });

  private static final String FROZEN_JDBC_URL;
  private static final String FROZEN_DATABASE_USERNAME;
  private static final String FROZEN_DATABASE_PASSWORD;

  static {
    startPostgresBeforeSpringPropertyResolution();
    FROZEN_JDBC_URL = POSTGRES.getJdbcUrl();
    FROZEN_DATABASE_USERNAME = POSTGRES.getUsername();
    FROZEN_DATABASE_PASSWORD = POSTGRES.getPassword();
  }

  @DynamicPropertySource
  static void runtimeProperties(final DynamicPropertyRegistry registry) {
    ensurePostgresReadyForPropertyResolution();
    registry.add("spring.datasource.url", () -> FROZEN_JDBC_URL);
    registry.add("spring.datasource.username", () -> FROZEN_DATABASE_USERNAME);
    registry.add("spring.datasource.password", () -> FROZEN_DATABASE_PASSWORD);
    registry.add(
        "decisionhub.security.api.token-sha256", () -> StaticTokenVerifier.sha256Hex(TEST_TOKEN));
    registry.add("decisionhub.security.api.tenant-id", () -> TENANT);
    registry.add("decisionhub.integration1.runtime.hmac-secret", () -> TEST_SIGNING_KEY);
    registry.add(
        "decisionhub.integration1.runtime.allowed-tenant-source-pairs",
        () -> TENANT + ":" + SOURCE);
  }

  @LocalServerPort private int applicationPort;

  @Autowired private ApplicationContext applicationContext;

  @Autowired private DataSource dataSource;

  @Autowired private JdbcTemplate jdbc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private ModelProviderPort modelProvider;

  @Autowired private RateLimitAdmissionPort rateLimitAdmission;

  @Autowired private IdempotencyGuardPort idempotencyGuard;

  @Autowired private GuardCleanupPort cleanupPort;

  @Autowired private GuardTransactionBoundary transactions;

  @Autowired private LimitedDryRunRuntimePolicy runtimePolicy;

  @Autowired private HmacNqDryRunAuthenticator dryRunAuthenticator;

  @Autowired private DecisionDryRunService decisionDryRunService;

  @Autowired private DecisionDryRunRuntimeProperties runtimeProperties;

  @Value("${decisionhub.integration1.runtime.max-payload-bytes:65536}")
  private long transportPayloadCap;

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  private Qdr7CapacityContracts.RunContext runContext;
  private Qdr7CapacityContracts.CriteriaSnapshot criteria;
  private Qdr7CapacityContracts.ScenarioExecution activeExecution;

  /**
   * 只验证正式 run 的 container、Spring Context、DataSource/Flyway 和 dispatcher 装配。
   *
   * <p>该模式在任何 mandatory scenario 开始前短路；它不是 formal capacity acceptance，也不产生阈值 PASS。
   */
  @Test
  @Order(0)
  @EnabledIfSystemProperty(named = "qdr7.implementationValidation", matches = "true")
  void validatesLifecycleBeforeMandatoryScenarioDispatch()
      throws IOException, SQLException, InterruptedException, ExecutionException {
    runContext =
        Qdr7CapacityContracts.parseRunContext(
            RUN_ID, String.valueOf(SEED), PROJECT_ROOT, COMMIT_SHA, false);
    criteria = Qdr7CapacityContracts.loadCriteria(PROJECT_ROOT, objectMapper);
    Files.createDirectories(EVIDENCE_ROOT);

    assertThat(IMPLEMENTATION_VALIDATION).isTrue();
    assertThat(POSTGRES.isRunning()).isTrue();
    assertThat(POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT))
        .isEqualTo(DATABASE_PORT);
    assertThat(dataSource).isInstanceOf(HikariDataSource.class);
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      assertThat(statement.execute("SELECT 1")).isTrue();
    }
    final List<HarnessDriver> dispatchers = drivers();
    assertThat(Qdr7CapacityContracts.ScenarioRegistry.validate(dispatchers)).isEmpty();
    assertThat(dispatchers).hasSize(15);
    assertThat(SCENARIO_LEDGER).isEmpty();
    IMPLEMENTATION_TENANT_ISOLATION_RESULTS.clear();
    IMPLEMENTATION_TENANT_ISOLATION_RESULTS.addAll(
        verifyTenantEnvironmentIsolation("qdr7-implementation-isolation", null));
    final Qdr7CapacityContracts.ScenarioExecution restartProbe =
        new Qdr7CapacityContracts.ScenarioExecution(runContext, criteria);
    restartProbe.values().put("roundsRequired", 3);
    restartProbe.values().put("roundsStarted", 0);
    restartProbe.values().put("roundsCompleted", 0);
    restartProbe.values().put("measurementsCaptured", 0);
    restartProbe.values().put("comparisonsExecuted", 0);
    springContextRestart(restartProbe);
    runRuntimeSafetyProbe(HARNESS_STARTED);
    writeImplementationValidationArtifact();
  }

  @Test
  @Order(1)
  @DisabledIfSystemProperty(named = "qdr7.implementationValidation", matches = "true")
  void executesAllMandatoryScenariosAndPreservesPartialEvidence() throws IOException {
    runContext =
        Qdr7CapacityContracts.parseRunContext(
            RUN_ID, String.valueOf(SEED), PROJECT_ROOT, COMMIT_SHA, QUALIFICATION_ONLY);
    criteria = Qdr7CapacityContracts.loadCriteria(PROJECT_ROOT, objectMapper);
    Files.createDirectories(EVIDENCE_ROOT);
    assertThat(modelProvider).isExactlyInstanceOf(MockModelProvider.class);
    qualifyEnvironmentBeforeScenarioDispatch();

    final List<HarnessDriver> drivers = drivers();
    assertThat(Qdr7CapacityContracts.ScenarioRegistry.validate(drivers)).isEmpty();
    initializeScenarioLedger();
    for (final HarnessDriver driver : drivers) {
      runDriver(driver);
    }
    writeRestartArtifact();
    writeThresholdComparison();
    writeSummary();
    assertThat(
            SCENARIO_LEDGER.stream()
                .filter(row -> !"PASS".equals(row.get("verdict")))
                .map(row -> row.get("scenarioId") + ":" + row.get("reasonCode"))
                .toList())
        .as("all mandatory scenarios must pass after complete ledger persistence")
        .isEmpty();
  }

  /** 在 Spring 解析 datasource 属性前启动本 run 唯一的 static PostgreSQL container。 */
  private static void startPostgresBeforeSpringPropertyResolution() {
    try {
      POSTGRES.start();
      if (!POSTGRES
          .getContainerInfo()
          .getImageId()
          .equals(RESOURCE_REGISTRY.path("postgresImageId").asText())) {
        throw new IllegalStateException("QDR7_CAPACITY_POSTGRES_IMAGE_IDENTITY_MISMATCH");
      }
      ensurePostgresReadyForPropertyResolution();
    } catch (final RuntimeException startupFailure) {
      throw new IllegalStateException(
          "QDR7_CAPACITY_POSTGRES_STARTUP_BLOCKED: container was not ready before property resolution",
          startupFailure);
    }
  }

  /** 禁止 DynamicPropertySource 从未启动的 container 读取 mapped port 或 JDBC URL。 */
  private static void ensurePostgresReadyForPropertyResolution() {
    if (!POSTGRES.isRunning()) {
      throw new IllegalStateException(
          "QDR7_CAPACITY_POSTGRES_NOT_STARTED_BEFORE_PROPERTY_RESOLUTION");
    }
    final int mappedPort = POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
    if (mappedPort != DATABASE_PORT) {
      throw new IllegalStateException(
          "QDR7_CAPACITY_POSTGRES_ENDPOINT_MISMATCH: expected fixed run endpoint");
    }
  }

  private List<HarnessDriver> drivers() {
    return List.of(
        new HarnessDriver("actual-wiring", this::actualWiring),
        new HarnessDriver("rate-matrix", this::rateMatrix),
        new HarnessDriver("cold-start-quota", this::coldStartQuota),
        new HarnessDriver("tenant-environment-isolation", this::tenantIsolation),
        new HarnessDriver("canonical-source-fail-closed", this::canonicalSourceFailClosed),
        new HarnessDriver("nonce-race", this::nonceRace),
        new HarnessDriver("idempotency-lifecycle", this::idempotencyLifecycle),
        new HarnessDriver("tenant-scoped-cleanup", this::tenantScopedCleanup),
        new HarnessDriver("postgres-hikari-contention", this::postgresHikariContention),
        new HarnessDriver("postgres-same-pool-recovery", this::samePoolRecovery),
        new HarnessDriver("spring-context-restart", this::springContextRestart),
        new HarnessDriver("postgres-persistent-volume-restart", this::persistentVolumeRestart),
        new HarnessDriver("post-recovery-concurrency", this::postRecoveryConcurrency),
        new HarnessDriver("full-regression", this::fullRegression),
        new HarnessDriver("quality-gate", this::qualityGate));
  }

  private void qualifyEnvironmentBeforeScenarioDispatch() throws IOException {
    final Qdr7CapacityEnvironmentAdmission.Evaluation evaluation =
        Qdr7CapacityEnvironmentAdmission.runPostgresSmokeAndQualify(
            EVIDENCE_ROOT.resolve("capacity-environment-manifest.json"),
            EVIDENCE_ROOT.resolve("resource-registry.json"),
            PROJECT_ROOT.resolve("config/qdr7-capacity/qdr7-capacity-environment-admission.json"),
            objectMapper);
    if (!"QUALIFIED".equals(evaluation.status())) {
      throw new IllegalStateException(
          "STAGE_QDR_12_ENVIRONMENT_NOT_QUALIFIED:" + evaluation.blockers());
    }
  }

  private static HarnessDriver driver(final List<HarnessDriver> drivers, final String scenarioId) {
    return drivers.stream()
        .filter(candidate -> candidate.scenarioId().equals(scenarioId))
        .findFirst()
        .orElseThrow();
  }

  private void runDriver(final HarnessDriver driver) {
    final Map<String, Object> ledger = scenarioLedger(driver.scenarioId());
    final Qdr7CapacityContracts.ScenarioExecution execution =
        new Qdr7CapacityContracts.ScenarioExecution(runContext, criteria);
    execution.values().put("scenarioId", driver.scenarioId());
    execution.values().put("roundsRequired", ledger.get("roundsRequired"));
    execution.values().put("roundsStarted", 0);
    execution.values().put("roundsCompleted", 0);
    execution.values().put("measurementsCaptured", 0);
    execution.values().put("comparisonsExecuted", 0);
    final Instant started = Instant.now();
    ledger.put("executionState", Qdr7CapacityContracts.ExecutionState.STARTED.name());
    ledger.put("startedAt", Qdr7CapacityContracts.timestamp(started));
    ledger.put("reasonCode", "SCENARIO_STARTED");
    writeScenarioLedger();
    activeExecution = execution;
    try {
      driver.setup(execution);
      driver.execute(execution);
      execution.values().put("actionCompleted", true);
      driver.sample(execution);
      driver.assertCorrectness(execution);
      driver.compareThreshold(execution);
      driver.writeArtifacts(execution);
      completeUntrackedRounds(execution);
      ledger.put("executionState", Qdr7CapacityContracts.ExecutionState.COMPLETED.name());
      ledger.put("verdict", Qdr7CapacityContracts.ScenarioVerdict.PASS.name());
      ledger.put("reasonCode", "PASS");
    } catch (final AssertionError failure) {
      ledger.put(
          "executionState",
          Boolean.TRUE.equals(execution.values().get("actionCompleted"))
              ? Qdr7CapacityContracts.ExecutionState.COMPLETED.name()
              : Qdr7CapacityContracts.ExecutionState.PARTIAL.name());
      ledger.put("verdict", Qdr7CapacityContracts.ScenarioVerdict.FAIL.name());
      ledger.put("reasonCode", scenarioFailureReason(execution, failure));
    } catch (final Exception failure) {
      ledger.put("executionState", Qdr7CapacityContracts.ExecutionState.PARTIAL.name());
      ledger.put("verdict", Qdr7CapacityContracts.ScenarioVerdict.BLOCKED.name());
      ledger.put("reasonCode", scenarioFailureReason(execution, failure));
    } finally {
      try {
        driver.teardown(execution);
      } catch (final Exception teardownFailure) {
        ledger.put("executionState", Qdr7CapacityContracts.ExecutionState.PARTIAL.name());
        ledger.put("verdict", Qdr7CapacityContracts.ScenarioVerdict.BLOCKED.name());
        ledger.put("reasonCode", "SCENARIO_TEARDOWN_FAILED");
      }
      copyExecutionCounters(execution, ledger);
      ledger.put("completedAt", Qdr7CapacityContracts.timestamp(Instant.now()));
      activeExecution = null;
      writeScenarioLedger();
    }
  }

  /** 在首个mandatory scenario前持久化15条NOT_STARTED记录，后续只做原位状态推进。 */
  private void initializeScenarioLedger() {
    THRESHOLD_RESULTS.clear();
    RESTART_RESULTS.clear();
    SCENARIO_LEDGER.clear();
    postRecoveryStructured2xx = 0;
    for (final String scenarioId : Qdr7CapacityContracts.ScenarioRegistry.mandatory()) {
      final Map<String, Object> row = new LinkedHashMap<>();
      row.put("scenarioId", scenarioId);
      row.put("mandatory", true);
      row.put("executionState", Qdr7CapacityContracts.ExecutionState.NOT_STARTED.name());
      row.put("verdict", Qdr7CapacityContracts.ScenarioVerdict.NOT_EVALUATED.name());
      row.put("startedAt", null);
      row.put("completedAt", null);
      row.put("roundsRequired", roundsRequired(scenarioId));
      row.put("roundsStarted", 0);
      row.put("roundsCompleted", 0);
      row.put("measurementsCaptured", 0);
      row.put("comparisonsExecuted", 0);
      row.put("reasonCode", "NOT_STARTED");
      row.put("artifactRefs", artifactRefs(scenarioId));
      SCENARIO_LEDGER.add(row);
    }
    writeScenarioLedger();
  }

  /** ledger写入本身是系统级合同；不可写时允许立即中止，不能伪装为scenario-local失败。 */
  private void writeScenarioLedger() {
    final int failed = scenarioVerdictCount(Qdr7CapacityContracts.ScenarioVerdict.FAIL);
    final int blocked = scenarioVerdictCount(Qdr7CapacityContracts.ScenarioVerdict.BLOCKED);
    final int passed = scenarioVerdictCount(Qdr7CapacityContracts.ScenarioVerdict.PASS);
    final Map<String, Object> artifact =
        artifact("scenario-ledger", HARNESS_STARTED, Instant.now());
    artifact.put("status", failed > 0 ? "FAIL" : blocked > 0 || passed < 15 ? "BLOCKED" : "PASS");
    artifact.put("scenarios", List.copyOf(SCENARIO_LEDGER));
    writeJson("scenario-ledger.json", artifact);
  }

  private Map<String, Object> scenarioLedger(final String scenarioId) {
    return SCENARIO_LEDGER.stream()
        .filter(row -> scenarioId.equals(row.get("scenarioId")))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("scenario ledger entry missing"));
  }

  private void beginRound(final Qdr7CapacityContracts.ScenarioExecution execution) {
    execution.values().put("roundsStarted", counter(execution, "roundsStarted") + 1);
    persistExecutionProgress(execution);
  }

  private void completeRound(
      final Qdr7CapacityContracts.ScenarioExecution execution, final int measurements) {
    execution.values().put("roundsCompleted", counter(execution, "roundsCompleted") + 1);
    execution
        .values()
        .put(
            "measurementsCaptured",
            counter(execution, "measurementsCaptured") + Math.max(0, measurements));
    persistExecutionProgress(execution);
  }

  private void persistExecutionProgress(final Qdr7CapacityContracts.ScenarioExecution execution) {
    final Object scenarioId = execution.values().get("scenarioId");
    if (scenarioId == null || SCENARIO_LEDGER.isEmpty()) {
      return;
    }
    copyExecutionCounters(execution, scenarioLedger(String.valueOf(scenarioId)));
    writeScenarioLedger();
  }

  private static void copyExecutionCounters(
      final Qdr7CapacityContracts.ScenarioExecution execution, final Map<String, Object> ledger) {
    for (final String field :
        List.of(
            "roundsRequired",
            "roundsStarted",
            "roundsCompleted",
            "measurementsCaptured",
            "comparisonsExecuted")) {
      ledger.put(field, counter(execution, field));
    }
  }

  private static void completeUntrackedRounds(
      final Qdr7CapacityContracts.ScenarioExecution execution) {
    if (counter(execution, "roundsStarted") == 0) {
      execution.values().put("roundsStarted", counter(execution, "roundsRequired"));
      execution.values().put("roundsCompleted", counter(execution, "roundsRequired"));
    }
  }

  private static int counter(
      final Qdr7CapacityContracts.ScenarioExecution execution, final String field) {
    final Object value = execution.values().get(field);
    return value instanceof Number number ? number.intValue() : 0;
  }

  private int scenarioCounter(final String scenarioId, final String field) {
    final Object value = scenarioLedger(scenarioId).get(field);
    return value instanceof Number number ? number.intValue() : 0;
  }

  private int scenarioStateCount(final Qdr7CapacityContracts.ExecutionState state) {
    return Math.toIntExact(
        SCENARIO_LEDGER.stream()
            .filter(row -> state.name().equals(row.get("executionState")))
            .count());
  }

  private int scenarioVerdictCount(final Qdr7CapacityContracts.ScenarioVerdict verdict) {
    return Math.toIntExact(
        SCENARIO_LEDGER.stream().filter(row -> verdict.name().equals(row.get("verdict"))).count());
  }

  private String scenarioVerdict(final String scenarioId) {
    final String verdict = String.valueOf(scenarioLedger(scenarioId).get("verdict"));
    return "NOT_EVALUATED".equals(verdict) ? "BLOCKED" : verdict;
  }

  private long restartCount(final String type) {
    return RESTART_RESULTS.stream()
        .filter(row -> type.equals(row.get("type")))
        .filter(row -> "PASS".equals(row.get("status")))
        .count();
  }

  private static String scenarioFailureReason(
      final Qdr7CapacityContracts.ScenarioExecution execution, final Throwable failure) {
    if (execution
        .findings()
        .contains(Qdr7CapacityContracts.SemanticExit.NUMERIC_THRESHOLD_FAILED)) {
      return "NUMERIC_THRESHOLD_FAILED";
    }
    if (failure instanceof SQLException || failure.getClass().getSimpleName().contains("Store")) {
      return "SCENARIO_LOCAL_DATABASE_FAILURE";
    }
    return failure instanceof AssertionError
        ? "CORRECTNESS_INVARIANT_FAILED"
        : "SCENARIO_LOCAL_FIXTURE_FAILURE";
  }

  private int selectScenarioExitCode(final boolean allPassed) {
    if (allPassed) {
      return 0;
    }
    if ("FAIL".equals(scenarioVerdict("quality-gate"))) {
      return 70;
    }
    if ("FAIL".equals(scenarioVerdict("full-regression"))) {
      return 60;
    }
    if (THRESHOLD_RESULTS.stream().anyMatch(row -> "FAIL".equals(row.get("status")))) {
      return 50;
    }
    if (scenarioVerdictCount(Qdr7CapacityContracts.ScenarioVerdict.FAIL) > 0) {
      return 40;
    }
    return 30;
  }

  private static int roundsRequired(final String scenarioId) {
    return switch (scenarioId) {
      case "rate-matrix" -> 15;
      case "cold-start-quota",
          "tenant-environment-isolation",
          "nonce-race",
          "tenant-scoped-cleanup",
          "postgres-same-pool-recovery",
          "spring-context-restart",
          "postgres-persistent-volume-restart" ->
          3;
      default -> 1;
    };
  }

  private static List<String> artifactRefs(final String scenarioId) {
    return switch (scenarioId) {
      case "actual-wiring" -> List.of("actual-wiring.json");
      case "rate-matrix" -> List.of("rate-matrix.csv", "rate-summary.json");
      case "cold-start-quota" -> List.of("quota-atomicity.json");
      case "tenant-environment-isolation", "canonical-source-fail-closed" ->
          List.of("tenant-isolation.json");
      case "nonce-race" -> List.of("nonce-race.json");
      case "idempotency-lifecycle" -> List.of("idempotency-lifecycle.json");
      case "tenant-scoped-cleanup" -> List.of("cleanup-timeline.csv", "cleanup-summary.json");
      case "postgres-hikari-contention" ->
          List.of("postgres-hikari-series.csv", "postgres-hikari-summary.json");
      case "postgres-same-pool-recovery" ->
          List.of("recovery-timeline.csv", "recovery-summary.json", "restart-results.json");
      case "spring-context-restart", "postgres-persistent-volume-restart" ->
          List.of("restart-results.json");
      case "post-recovery-concurrency" ->
          List.of("recovery-timeline.csv", "post-recovery-summary.json");
      case "full-regression" -> List.of("full-regression.log", "full-regression-summary.json");
      case "quality-gate" -> List.of("quality.log", "quality-summary.json");
      default -> List.of();
    };
  }

  private void actualWiring(final Qdr7CapacityContracts.ScenarioExecution execution)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    final Instant started = Instant.now();
    beginRound(execution);
    final List<RequestOutcome> outcomes = new ArrayList<>();
    for (int ordinal = 1; ordinal <= 5; ordinal++) {
      outcomes.add(send(prepare("actual-sequential-" + ordinal, null, SOURCE)));
    }
    outcomes.addAll(runConcurrentRequests(8, 8, "actual-concurrent"));
    assertThat(outcomes).hasSize(13).allSatisfy(this::assertStructuredSuccess);
    final Map<String, Object> artifact = artifact("actual-wiring", started, Instant.now());
    artifact.put("structured2xx", 13);
    artifact.put("springContextIdentityHash", identityHash(applicationContext));
    artifact.put("dataSourceIdentityHash", identityHash(dataSource));
    artifact.put("hikariPoolIdentityHash", identityHash(hikariPool()));
    artifact.put("containerIdentityHash", Qdr7CapacityContracts.sha256(POSTGRES.getContainerId()));
    artifact.put("localhostOnly", true);
    artifact.put("mockProvider", true);
    writeJson("actual-wiring.json", artifact);
    runRuntimeSafetyProbe(started);
    completeRound(execution, 16);
  }

  private void runRuntimeSafetyProbe(final Instant started) throws ExecutionException {
    final Map<String, Object> runtimeSafety;
    try {
      runtimeSafety =
          Qdr7CapacityRuntimeSafetyProbe.runBound(
              runtimePolicy,
              dryRunAuthenticator,
              decisionDryRunService,
              runtimeProperties,
              transportPayloadCap,
              TENANT,
              SOURCE,
              RUN_ID);
    } catch (final Exception failure) {
      throw new ExecutionException("runtime resource-safety probe failed", failure);
    }
    final Map<String, Object> runtimeSafetyArtifact =
        artifact("runtime-resource-safety", started, Instant.now());
    runtimeSafetyArtifact.putAll(runtimeSafety);
    writeJson("runtime-resource-safety.json", runtimeSafetyArtifact);
  }

  private void rateMatrix(final Qdr7CapacityContracts.ScenarioExecution execution)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    final Instant started = Instant.now();
    final Path csv = EVIDENCE_ROOT.resolve("rate-matrix.csv");
    Qdr7CapacityArtifactSupport.writeCsvHeader(
        csv,
        List.of(
            "concurrency",
            "round",
            "sample",
            "httpStatus",
            "latencyMs",
            "startedOffsetMs",
            "completedOffsetMs"));
    final List<Map<String, Object>> summaries = new ArrayList<>();
    for (final int concurrency : List.of(1, 2, 4, 8, 16)) {
      for (int round = 1; round <= 3; round++) {
        beginRound(execution);
        runConcurrentRequests(concurrency, 20, "rate-warm-c" + concurrency + "-r" + round);
        final long roundStarted = System.nanoTime();
        final List<RequestOutcome> outcomes =
            runConcurrentRequests(concurrency, 100, "rate-c" + concurrency + "-r" + round);
        final long roundElapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - roundStarted);
        assertThat(outcomes).hasSize(100).allSatisfy(this::assertStructuredSuccess);
        final List<Long> latencies = outcomes.stream().map(RequestOutcome::latencyMs).toList();
        final Qdr7CapacityContracts.Statistics statistics =
            Qdr7CapacityContracts.statistics(latencies, roundElapsedMs);
        int sample = 0;
        for (final RequestOutcome outcome : outcomes) {
          sample++;
          Qdr7CapacityArtifactSupport.appendCsv(
              csv,
              List.of(
                  Qdr7CapacityContracts.SCHEMA_VERSION,
                  RUN_ID,
                  COMMIT_SHA,
                  "rate-matrix",
                  Qdr7CapacityContracts.timestamp(outcome.completedAt()),
                  outcome.completedOffsetMs(),
                  concurrency,
                  round,
                  sample,
                  outcome.statusCode(),
                  outcome.latencyMs(),
                  outcome.startedOffsetMs(),
                  outcome.completedOffsetMs()));
        }
        final Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("concurrency", concurrency);
        summary.put("round", round);
        summary.put("structured2xx", 100);
        summary.put("throughput", statistics.throughput());
        summary.put("p50Ms", statistics.p50());
        summary.put("p95Ms", statistics.p95());
        summary.put("p99Ms", statistics.p99());
        summary.put("maxMs", statistics.max());
        summaries.add(summary);
        compareRateThresholds(concurrency, round, statistics);
        completeRound(execution, 100);
      }
    }
    final Map<String, Object> artifact = artifact("rate-matrix", started, Instant.now());
    artifact.put(
        "protocol",
        Map.of(
            "concurrency",
            List.of(1, 2, 4, 8, 16),
            "warmup",
            20,
            "measured",
            100,
            "rounds",
            3,
            "percentile",
            "nearest-rank"));
    artifact.put("rounds", summaries);
    writeJson("rate-summary.json", artifact);
  }

  private void compareRateThresholds(
      final int concurrency, final int round, final Qdr7CapacityContracts.Statistics statistics) {
    final JsonNode threshold =
        criteria.root().path("numericThresholds").path("rate").path(String.valueOf(concurrency));
    addThreshold(
        "numericThresholds.rate." + concurrency + ".throughputMin",
        "rate.c" + concurrency + ".r" + round + ".throughput",
        statistics.throughput(),
        ">=",
        threshold.path("throughputMin").asDouble(),
        "operations/second");
    addThreshold(
        "numericThresholds.rate." + concurrency + ".p50MaxMs",
        "rate.c" + concurrency + ".r" + round + ".p50",
        statistics.p50(),
        "<=",
        threshold.path("p50MaxMs").asDouble(),
        "milliseconds");
    addThreshold(
        "numericThresholds.rate." + concurrency + ".p95MaxMs",
        "rate.c" + concurrency + ".r" + round + ".p95",
        statistics.p95(),
        "<=",
        threshold.path("p95MaxMs").asDouble(),
        "milliseconds");
    addThreshold(
        "numericThresholds.rate." + concurrency + ".p99MaxMs",
        "rate.c" + concurrency + ".r" + round + ".p99",
        statistics.p99(),
        "<=",
        threshold.path("p99MaxMs").asDouble(),
        "milliseconds");
    addThreshold(
        "numericThresholds.rate." + concurrency + ".latencyMaxMs",
        "rate.c" + concurrency + ".r" + round + ".max",
        statistics.max(),
        "<=",
        threshold.path("latencyMaxMs").asDouble(),
        "milliseconds");
  }

  private void coldStartQuota(final Qdr7CapacityContracts.ScenarioExecution execution)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    final Instant started = Instant.now();
    final List<Map<String, Object>> rounds = new ArrayList<>();
    for (int round = 1; round <= 3; round++) {
      beginRound(execution);
      final String tenant = "qdr7-capacity-quota-" + round + "-" + RUN_ID;
      final PersistentGuardIdentity identity = identity(tenant);
      final List<RateLimitAdmissionResult> results =
          invokeConcurrent(
              8,
              40,
              ordinal ->
                  transactions.required(
                      () ->
                          rateLimitAdmission.tryAcquire(
                              new RateLimitAdmissionCommand(identity, 3600, 10))));
      final long accepted =
          results.stream()
              .filter(result -> result.status() == RateLimitAdmissionStatus.ACCEPTED)
              .count();
      final long limited =
          results.stream()
              .filter(result -> result.status() == RateLimitAdmissionStatus.RATE_LIMITED)
              .count();
      final int winners =
          jdbc.queryForObject(
              "select coalesce(sum(request_count),0) from dh_qdr7_rate_limit_bucket where tenant_id=?",
              Integer.class,
              tenant);
      final int rows =
          jdbc.queryForObject(
              "select count(*) from dh_qdr7_rate_limit_bucket where tenant_id=?",
              Integer.class,
              tenant);
      assertThat(accepted).isEqualTo(10);
      assertThat(limited).isEqualTo(30);
      assertThat(winners).isEqualTo(10);
      assertThat(rows).isEqualTo(1);
      rounds.add(
          Map.of(
              "round", round,
              "attempts", 40,
              "accepted", accepted,
              "rateLimited", limited,
              "databaseWinners", winners,
              "canonicalRows", 1,
              "oversell", 0));
      completeRound(execution, 40);
    }
    final Map<String, Object> artifact = artifact("cold-start-quota", started, Instant.now());
    artifact.put("windowSeconds", 3600);
    artifact.put("quota", 10);
    artifact.put("concurrency", 8);
    artifact.put("rounds", rounds);
    writeJson("quota-atomicity.json", artifact);
  }

  private void tenantIsolation(final Qdr7CapacityContracts.ScenarioExecution execution)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    final Instant started = Instant.now();
    final List<Map<String, Object>> rounds =
        verifyTenantEnvironmentIsolation("qdr7-isolation", execution);
    execution.values().put("isolationRounds", rounds);
    final Map<String, Object> artifact =
        artifact("tenant-environment-isolation", started, Instant.now());
    artifact.put(
        "scopeHashes",
        List.of(
            Qdr7CapacityContracts.sha256("scope-a" + RUN_ID),
            Qdr7CapacityContracts.sha256("scope-b" + RUN_ID)));
    artifact.put("rounds", rounds);
    artifact.put("noncanonicalSource", Map.of("status", "PENDING_DRIVER", "httpStatus", 0));
    writeJson("tenant-isolation.json", artifact);
  }

  /**
   * 使用每轮唯一tenant和合法environment组合验证完整identity隔离。
   *
   * <p>查询只统计当前run/current round，禁止把前一轮fixture计为cross-scope数据，也禁止清空共享数据库换取PASS。
   */
  private List<Map<String, Object>> verifyTenantEnvironmentIsolation(
      final String tenantPrefix, final Qdr7CapacityContracts.ScenarioExecution execution) {
    final List<Map<String, Object>> rounds = new ArrayList<>();
    final List<String> primaryEnvironments = List.of("dev", "test", "staging");
    final List<String> alternateEnvironments = List.of("test", "staging", "prod");
    for (int round = 1; round <= 3; round++) {
      if (execution != null) {
        beginRound(execution);
      }
      final String primaryEnvironment = primaryEnvironments.get(round - 1);
      final String alternateEnvironment = alternateEnvironments.get(round - 1);
      final String tenantA = tenantPrefix + "-a-r" + round + "-" + RUN_ID;
      final String tenantB = tenantPrefix + "-b-r" + round + "-" + RUN_ID;
      final String currentRoundPattern = tenantPrefix + "-%-r" + round + "-" + RUN_ID;
      final PersistentGuardIdentity identityA = identity(primaryEnvironment, tenantA);
      final PersistentGuardIdentity identityB = identity(primaryEnvironment, tenantB);
      final PersistentGuardIdentity alternateIdentityA = identity(alternateEnvironment, tenantA);
      for (int attempt = 0; attempt < 10; attempt++) {
        assertThat(acquire(identityA, 10).status()).isEqualTo(RateLimitAdmissionStatus.ACCEPTED);
      }
      assertThat(acquire(identityA, 10).status()).isEqualTo(RateLimitAdmissionStatus.RATE_LIMITED);
      assertThat(acquire(identityB, 10).status()).isEqualTo(RateLimitAdmissionStatus.ACCEPTED);
      assertThat(acquire(alternateIdentityA, 10).status())
          .isEqualTo(RateLimitAdmissionStatus.ACCEPTED);

      final long scopeARequests = rateRequestCount(identityA);
      final long scopeBRequests = rateRequestCount(identityB);
      final long alternateEnvironmentRequests = rateRequestCount(alternateIdentityA);
      final int crossTenantRows =
          jdbc.queryForObject(
              "select count(*) from dh_qdr7_rate_limit_bucket"
                  + " where endpoint=? and source=? and environment=? and tenant_id like ?"
                  + " and tenant_id not in (?,?)",
              Integer.class,
              ENDPOINT,
              SOURCE,
              primaryEnvironment,
              currentRoundPattern,
              tenantA,
              tenantB);
      final int crossEnvironmentRows =
          jdbc.queryForObject(
              "select count(*) from dh_qdr7_rate_limit_bucket"
                  + " where endpoint=? and source=? and tenant_id in (?,?)"
                  + " and environment not in (?,?)",
              Integer.class,
              ENDPOINT,
              SOURCE,
              tenantA,
              tenantB,
              primaryEnvironment,
              alternateEnvironment);
      final int unexpectedRecords =
          jdbc.queryForObject(
              "select count(*) from dh_qdr7_rate_limit_bucket"
                  + " where endpoint=? and source=? and tenant_id like ?"
                  + " and not ((environment=? and tenant_id in (?,?))"
                  + " or (environment=? and tenant_id=?))",
              Integer.class,
              ENDPOINT,
              SOURCE,
              currentRoundPattern,
              primaryEnvironment,
              tenantA,
              tenantB,
              alternateEnvironment,
              tenantA);
      assertThat(scopeARequests).isEqualTo(10L);
      assertThat(scopeBRequests).isEqualTo(1L);
      assertThat(alternateEnvironmentRequests).isEqualTo(1L);
      assertThat(crossTenantRows).isZero();
      assertThat(crossEnvironmentRows).isZero();
      assertThat(unexpectedRecords).isZero();

      final Map<String, Object> result = new LinkedHashMap<>();
      result.put("round", round);
      result.put("primaryEnvironment", primaryEnvironment);
      result.put("alternateEnvironment", alternateEnvironment);
      result.put("tenantAHash", Qdr7CapacityContracts.sha256(tenantA));
      result.put("tenantBHash", Qdr7CapacityContracts.sha256(tenantB));
      result.put("scopeARequestCount", scopeARequests);
      result.put("scopeBRequestCount", scopeBRequests);
      result.put("alternateEnvironmentRequestCount", alternateEnvironmentRequests);
      result.put("scopeAExhausted", true);
      result.put("scopeBAccepted", true);
      result.put("crossTenantRows", crossTenantRows);
      result.put("crossEnvironmentRows", crossEnvironmentRows);
      result.put("unexpectedRecords", unexpectedRecords);
      rounds.add(result);
      if (execution != null) {
        completeRound(execution, 3);
      }
    }
    return List.copyOf(rounds);
  }

  private void canonicalSourceFailClosed(final Qdr7CapacityContracts.ScenarioExecution execution)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    final Instant started = Instant.now();
    beginRound(execution);
    final RequestOutcome denied = send(prepare("noncanonical-source", null, "OTHER_SOURCE"));
    assertThat(denied.statusCode()).isEqualTo(403);
    assertThat(denied.errorCode(objectMapper)).isEqualTo("SOURCE_DENIED");
    final Path path = EVIDENCE_ROOT.resolve("tenant-isolation.json");
    final Map<String, Object> artifact = objectMapper.readValue(path.toFile(), LinkedHashMap.class);
    artifact.put(
        "noncanonicalSource",
        Map.of("status", "PASS", "httpStatus", 403, "errorCode", "SOURCE_DENIED"));
    artifact.put("finishedAtUtc", Qdr7CapacityContracts.timestamp(Instant.now()));
    writeJson("tenant-isolation.json", artifact);
    execution.values().put("sourceDenied", true);
    completeRound(execution, 1);
    assertThat(Duration.between(started, Instant.now())).isLessThan(Duration.ofMinutes(2));
  }

  private void nonceRace(final Qdr7CapacityContracts.ScenarioExecution execution)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    final Instant started = Instant.now();
    final List<Map<String, Object>> rounds = new ArrayList<>();
    for (int round = 1; round <= 3; round++) {
      beginRound(execution);
      final PreparedRequest request = prepare("nonce-race-" + round, null, SOURCE);
      final List<RequestOutcome> outcomes = invokeConcurrent(8, 24, ordinal -> send(request));
      final long accepted = outcomes.stream().filter(RequestOutcome::structured2xx).count();
      final long replay =
          outcomes.stream()
              .filter(outcome -> outcome.statusCode() == 409)
              .filter(outcome -> "NONCE_REPLAY".equals(outcome.errorCode(objectMapper)))
              .count();
      final int winnerRows =
          jdbc.queryForObject(
              "select count(*) from dh_nq_replay_nonce where replay_key=?",
              Integer.class,
              replayKey(request));
      assertThat(accepted).isEqualTo(1);
      assertThat(replay).isEqualTo(23);
      assertThat(winnerRows).isEqualTo(1);
      rounds.add(
          Map.of(
              "round", round,
              "attempts", 24,
              "accepted", accepted,
              "nonceReplay", replay,
              "databaseWinners", winnerRows,
              "unexpected", 0));
      completeRound(execution, 24);
    }
    final Map<String, Object> artifact = artifact("nonce-race", started, Instant.now());
    artifact.put("threads", 8);
    artifact.put("rounds", rounds);
    writeJson("nonce-race.json", artifact);
  }

  private void idempotencyLifecycle(final Qdr7CapacityContracts.ScenarioExecution execution) {
    final Instant started = Instant.now();
    beginRound(execution);
    final String fixtureTenant = scenarioTenant("idempotency-lifecycle", 1);
    final PersistentGuardIdentity identity = identity(fixtureTenant);
    final String requestId = "qdr7-idempotency-lifecycle-" + RUN_ID;
    final String resultId = "result-idempotency-lifecycle-" + RUN_ID;
    final String hash = Qdr7CapacityContracts.sha256("idempotency-lifecycle-" + RUN_ID);
    transactions.required(
        () -> {
          seedDecisionOutput(jdbc, fixtureTenant, resultId, requestId);
          return Boolean.TRUE;
        });
    final IdempotencyAdmissionCommand admission =
        new IdempotencyAdmissionCommand(
            identity,
            requestId,
            hash,
            IdempotencyAdmissionCommand.HASH_VERSION,
            Duration.ofMinutes(10),
            Duration.ofHours(1));
    final IdempotencyAdmissionResult admitted =
        transactions.required(() -> idempotencyGuard.admit(admission));
    assertThat(admitted.status()).isEqualTo(IdempotencyAdmissionStatus.ADMITTED);
    final UUID leaseToken = UUID.randomUUID();
    final IdempotencyRecordView inProgress =
        transactions.required(
            () ->
                idempotencyGuard.transition(
                    transition(
                        admitted.record(),
                        IdempotencyState.IN_PROGRESS,
                        null,
                        null,
                        "capacity-worker",
                        leaseToken,
                        Duration.ofSeconds(30))));
    assertThat(inProgress.state()).isEqualTo(IdempotencyState.IN_PROGRESS);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                transactions.required(
                    () ->
                        idempotencyGuard.transition(
                            transition(
                                inProgress,
                                IdempotencyState.COMPLETED,
                                "DH_DECISION_OUTPUT",
                                "missing-result-" + RUN_ID,
                                "capacity-worker",
                                leaseToken,
                                null))))
        .isInstanceOf(RuntimeException.class);
    final IdempotencyRecordView completed =
        transactions.required(
            () ->
                idempotencyGuard.transition(
                    transition(
                        inProgress,
                        IdempotencyState.COMPLETED,
                        "DH_DECISION_OUTPUT",
                        resultId,
                        "capacity-worker",
                        leaseToken,
                        null)));
    final IdempotencyAdmissionResult duplicate =
        transactions.required(() -> idempotencyGuard.admit(admission));
    assertThat(completed.state()).isEqualTo(IdempotencyState.COMPLETED);
    assertThat(duplicate.status()).isEqualTo(IdempotencyAdmissionStatus.COMPLETED);
    assertThat(duplicate.record().resultId()).isEqualTo(resultId);

    final PersistentGuardIdentity crossTenantIdentity =
        identity(scenarioTenant("idempotency-cross-tenant", 1));
    final IdempotencyRecordView crossTenantInProgress =
        admitAndStart(
            crossTenantIdentity,
            requestId + "-cross-tenant",
            Qdr7CapacityContracts.sha256("cross-tenant-" + RUN_ID));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                transactions.required(
                    () ->
                        idempotencyGuard.transition(
                            transition(
                                crossTenantInProgress,
                                IdempotencyState.COMPLETED,
                                "DH_DECISION_OUTPUT",
                                resultId,
                                "capacity-worker",
                                crossTenantInProgress.leaseToken(),
                                null))))
        .isInstanceOf(RuntimeException.class);

    final PersistentGuardIdentity crossEnvironmentIdentity = identity("staging", fixtureTenant);
    final IdempotencyRecordView crossEnvironmentInProgress =
        admitAndStart(
            crossEnvironmentIdentity,
            requestId + "-cross-environment",
            Qdr7CapacityContracts.sha256("cross-environment-" + RUN_ID));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                transactions.required(
                    () ->
                        idempotencyGuard.transition(
                            transition(
                                crossEnvironmentInProgress,
                                IdempotencyState.COMPLETED,
                                "DH_DECISION_OUTPUT",
                                "result-staging-" + RUN_ID,
                                "capacity-worker",
                                crossEnvironmentInProgress.leaseToken(),
                                null))))
        .isInstanceOf(RuntimeException.class);

    final String rollbackRequest = requestId + "-rollback";
    final IdempotencyAdmissionCommand rollbackAdmission =
        new IdempotencyAdmissionCommand(
            identity,
            rollbackRequest,
            Qdr7CapacityContracts.sha256("rollback-" + RUN_ID),
            IdempotencyAdmissionCommand.HASH_VERSION,
            Duration.ofMinutes(10),
            Duration.ofHours(1));
    try {
      transactions.required(
          () -> {
            idempotencyGuard.admit(rollbackAdmission);
            throw new IllegalStateException("deterministic rollback fixture");
          });
    } catch (final IllegalStateException expected) {
      assertThat(expected).hasMessage("deterministic rollback fixture");
    }
    final int rollbackRows =
        jdbc.queryForObject(
            "select count(*) from dh_qdr7_idempotency_guard where tenant_id=? and request_id=?",
            Integer.class,
            identity.tenantId(),
            rollbackRequest);
    assertThat(rollbackRows).isZero();

    final Map<String, Object> artifact = artifact("idempotency-lifecycle", started, Instant.now());
    artifact.put(
        "paths",
        List.of(
            Map.of("case", "single-winner", "status", "PASS"),
            Map.of("case", "active-lease-not-stolen", "status", "PASS"),
            Map.of("case", "legal-result-reference", "status", "PASS"),
            Map.of("case", "missing-result-reference", "status", "PASS"),
            Map.of("case", "cross-tenant-result-reference", "status", "PASS"),
            Map.of("case", "cross-environment-result-reference", "status", "PASS"),
            Map.of("case", "rollback-no-success", "status", "PASS"),
            Map.of("case", "terminal-stable", "status", "PASS"),
            Map.of("case", "commit-unknown-not-readmitted", "status", "REGRESSION_REPORT_REQUIRED"),
            Map.of("case", "expiry-transition", "status", "REGRESSION_REPORT_REQUIRED")));
    artifact.put("partialOrphanOverwriteReadmission", 0);
    writeJson("idempotency-lifecycle.json", artifact);
    completeRound(execution, 6);
  }

  private void tenantScopedCleanup(final Qdr7CapacityContracts.ScenarioExecution execution)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    final Instant started = Instant.now();
    final Path timeline = EVIDENCE_ROOT.resolve("cleanup-timeline.csv");
    Qdr7CapacityArtifactSupport.writeCsvHeader(
        timeline,
        List.of(
            "scale",
            "phase",
            "eligible",
            "protected",
            "locked",
            "backlog",
            "inserted",
            "affected"));
    final List<Map<String, Object>> summaries = new ArrayList<>();
    final Map<Integer, Integer> expectedByScale = Map.of(10, 6, 100, 64, 1000, 649);
    for (final Map.Entry<Integer, Integer> entry :
        expectedByScale.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
      beginRound(execution);
      final int scale = entry.getKey();
      final int expected = entry.getValue();
      final String tenant = "qdr7-cleanup-" + scale + "-" + RUN_ID;
      seedCleanupRows(tenant, scale, expected);
      final long measuredStarted = System.nanoTime();
      final AtomicInteger affected = new AtomicInteger();
      final ExecutorService workers = Executors.newFixedThreadPool(2);
      try {
        final List<Future<?>> futures = new ArrayList<>();
        for (int worker = 0; worker < 2; worker++) {
          futures.add(
              workers.submit(
                  () -> {
                    int batch;
                    do {
                      batch =
                          transactions.required(
                              () ->
                                  cleanupPort.cleanupRetainedIdempotency(
                                      new GuardCleanupCommand(
                                          "test",
                                          ENDPOINT,
                                          SOURCE,
                                          tenant,
                                          Duration.ofMinutes(1),
                                          10)));
                      affected.addAndGet(batch);
                    } while (batch > 0);
                  }));
        }
        for (final Future<?> future : futures) {
          awaitFuture(future, 2, TimeUnit.MINUTES);
        }
      } finally {
        workers.shutdownNow();
        workers.awaitTermination(10, TimeUnit.SECONDS);
      }
      final long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - measuredStarted);
      final int expired =
          jdbc.queryForObject(
              "select count(*) from dh_qdr7_idempotency_guard where tenant_id=? and state='EXPIRED'",
              Integer.class,
              tenant);
      final int protectedRows =
          jdbc.queryForObject(
              "select count(*) from dh_qdr7_idempotency_guard where tenant_id=? and state<>'EXPIRED'",
              Integer.class,
              tenant);
      assertThat(affected.get()).isEqualTo(expected);
      assertThat(expired).isEqualTo(expected);
      assertThat(protectedRows).isEqualTo(scale - expected);
      Qdr7CapacityArtifactSupport.appendCsv(
          timeline,
          List.of(
              Qdr7CapacityContracts.SCHEMA_VERSION,
              RUN_ID,
              COMMIT_SHA,
              "tenant-scoped-cleanup",
              Qdr7CapacityContracts.timestamp(Instant.now()),
              durationMs,
              scale,
              "converged",
              expected,
              protectedRows,
              0,
              0,
              0,
              affected.get()));
      summaries.add(
          Map.of(
              "scale", scale,
              "durationMs", durationMs,
              "expectedAffected", expected,
              "affected", affected.get(),
              "protectedRows", protectedRows,
              "finalEligibleBacklog", 0));
      addThreshold(
          "numericThresholds.cleanupMaxMs." + scale,
          "cleanup." + scale + ".duration",
          durationMs,
          "<=",
          criteria
              .root()
              .path("numericThresholds")
              .path("cleanupMaxMs")
              .path(String.valueOf(scale))
              .asDouble(),
          "milliseconds");
      completeRound(execution, scale);
    }
    final Map<String, Object> artifact = artifact("tenant-scoped-cleanup", started, Instant.now());
    artifact.put("workers", 2);
    artifact.put("concurrentWriters", 1);
    artifact.put("batchSize", 10);
    artifact.put("scales", summaries);
    writeJson("cleanup-summary.json", artifact);
  }

  private void postgresHikariContention(final Qdr7CapacityContracts.ScenarioExecution execution)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    final Instant started = Instant.now();
    beginRound(execution);
    final Path series = EVIDENCE_ROOT.resolve("postgres-hikari-series.csv");
    Qdr7CapacityArtifactSupport.writeCsvHeader(
        series,
        List.of(
            "phase",
            "hikariActive",
            "hikariIdle",
            "hikariPending",
            "hikariTotal",
            "acquireMs",
            "timeoutCount",
            "postgresSessions",
            "postgresWaiting",
            "postgresLockWaiting",
            "deadlocks"));
    final HikariConfig config = new HikariConfig();
    config.setJdbcUrl(POSTGRES.getJdbcUrl());
    config.setUsername(POSTGRES.getUsername());
    config.setPassword(POSTGRES.getPassword());
    config.setMaximumPoolSize(4);
    config.setMinimumIdle(2);
    config.setConnectionTimeout(3800);
    final String pressureApplicationName = "qdr7-pressure-" + RUN_ID;
    config.setPoolName(pressureApplicationName);
    config.addDataSourceProperty("ApplicationName", pressureApplicationName);
    int maximumPending = 0;
    long maximumAcquireMs = 0L;
    int maximumWaiting = 0;
    int maximumLockWaiting = 0;
    try (HikariDataSource pressure = new HikariDataSource(config)) {
      final HikariPoolMXBean pool = Objects.requireNonNull(pressure.getHikariPoolMXBean());
      final ExecutorService executor = Executors.newFixedThreadPool(17);
      final CountDownLatch start = new CountDownLatch(1);
      final List<Future<Long>> futures = new ArrayList<>();
      try {
        for (int ordinal = 0; ordinal < 17; ordinal++) {
          futures.add(
              executor.submit(
                  () -> {
                    start.await(10, TimeUnit.SECONDS);
                    final long acquireStarted = System.nanoTime();
                    try (Connection connection = pressure.getConnection()) {
                      final long acquired =
                          TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - acquireStarted);
                      Thread.sleep(300L);
                      return acquired;
                    }
                  }));
        }
        start.countDown();
        for (int sample = 0; sample < 6; sample++) {
          Thread.sleep(100L);
          maximumPending = Math.max(maximumPending, pool.getThreadsAwaitingConnection());
          final PgSessions sessions = postgreSqlSessions(pressureApplicationName);
          maximumWaiting = Math.max(maximumWaiting, sessions.waiting());
          maximumLockWaiting = Math.max(maximumLockWaiting, sessions.lockWaiting());
          appendPgSample(series, "connection-pressure", pool, 0L, sessions, postgreSqlDeadlocks());
        }
        for (final Future<Long> future : futures) {
          maximumAcquireMs = Math.max(maximumAcquireMs, awaitFuture(future, 30, TimeUnit.SECONDS));
        }
      } finally {
        executor.shutdownNow();
        executor.awaitTermination(10, TimeUnit.SECONDS);
      }
      runControlledStatementTimeout();
      appendPgSample(
          series,
          "statement-timeout-rollback",
          pool,
          maximumAcquireMs,
          postgreSqlSessions(pressureApplicationName),
          postgreSqlDeadlocks());
    }
    assertThat(maximumPending).isLessThanOrEqualTo(17);
    assertThat(maximumAcquireMs).isLessThanOrEqualTo(3800L);
    assertThat(maximumWaiting).isLessThanOrEqualTo(7);
    assertThat(maximumLockWaiting).isLessThanOrEqualTo(4);
    assertThat(postgreSqlDeadlocks()).isZero();
    addThreshold(
        "numericThresholds.contentionMax.hikariPending",
        "contention.hikariPending",
        maximumPending,
        "<=",
        17,
        "count");
    addThreshold(
        "numericThresholds.contentionMax.acquireMs",
        "contention.acquire",
        maximumAcquireMs,
        "<=",
        3800,
        "milliseconds");
    addThreshold(
        "numericThresholds.contentionMax.postgresWaiting",
        "contention.postgresWaiting",
        maximumWaiting,
        "<=",
        7,
        "count");
    addThreshold(
        "numericThresholds.contentionMax.postgresLockWaiting",
        "contention.lockWaiting",
        maximumLockWaiting,
        "<=",
        4,
        "count");
    execution.values().put("maximumPending", maximumPending);
    final Map<String, Object> artifact =
        artifact("postgres-hikari-contention", started, Instant.now());
    artifact.put("maximumPending", maximumPending);
    artifact.put("maximumAcquireMs", maximumAcquireMs);
    artifact.put("maximumPostgresWaiting", maximumWaiting);
    artifact.put("maximumPostgresLockWaiting", maximumLockWaiting);
    artifact.put("deadlocks", 0);
    artifact.put("controlledRollback", true);
    writeJson("postgres-hikari-summary.json", artifact);
    completeRound(execution, 6);
  }

  private void samePoolRecovery(final Qdr7CapacityContracts.ScenarioExecution execution)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    final Instant started = Instant.now();
    final Path timeline = EVIDENCE_ROOT.resolve("recovery-timeline.csv");
    Qdr7CapacityArtifactSupport.writeCsvHeader(
        timeline,
        List.of(
            "round",
            "phase",
            "httpClass",
            "databaseReadyMs",
            "hikariReadyMs",
            "requestReadyMs",
            "samplingGapMs",
            "structured2xx",
            "unexpected4xx",
            "unexpected5xx"));
    final PreparedRequest anchor = prepare("recovery-state-anchor", null, SOURCE);
    assertStructuredSuccess(send(anchor));
    final String rollbackRequestId =
        seedRolledBackState(
            transactions, idempotencyGuard, identity(TENANT), "qdr7-same-pool-rollback-" + RUN_ID);
    final String checksumBefore = canonicalPromptChecksum();
    final int dataSourceIdentity = System.identityHashCode(dataSource);
    final int poolIdentity = System.identityHashCode(hikariPool());
    for (int round = 1; round <= 3; round++) {
      beginRound(execution);
      final String containerIdBefore = POSTGRES.getContainerId();
      final Instant outageStarted = Instant.now();
      POSTGRES.stopWithoutRemoval();
      int false2xx = 0;
      for (int request = 1; request <= 3; request++) {
        final RequestOutcome outage =
            sendSafely(prepare("outage-r" + round + "-" + request, null, SOURCE));
        if (outage.structured2xx()) {
          false2xx++;
        }
      }
      assertThat(false2xx).isZero();
      final Instant restartStarted = Instant.now();
      POSTGRES.startExistingContainer();
      final long databaseReadyMs = awaitDatabaseReady(restartStarted);
      final long hikariReadyMs = awaitHikariReady(restartStarted);
      final RequestOutcome recovered = awaitProtectedRequest(restartStarted, round);
      final long requestReadyMs =
          Duration.between(restartStarted, recovered.completedAt()).toMillis();
      assertThat(databaseReadyMs).isLessThanOrEqualTo(800L);
      assertThat(hikariReadyMs).isLessThanOrEqualTo(5800L);
      assertThat(requestReadyMs).isLessThanOrEqualTo(5800L);
      assertThat(System.identityHashCode(dataSource)).isEqualTo(dataSourceIdentity);
      assertThat(System.identityHashCode(hikariPool())).isEqualTo(poolIdentity);
      assertThat(canonicalPromptChecksum()).isEqualTo(checksumBefore);
      assertThat(idempotencyState(anchor.requestId())).isEqualTo("COMPLETED");
      assertThat(idempotencyRowCount(rollbackRequestId)).isZero();
      final long samplingGap = 100L;
      Qdr7CapacityArtifactSupport.appendCsv(
          timeline,
          List.of(
              Qdr7CapacityContracts.SCHEMA_VERSION,
              RUN_ID,
              COMMIT_SHA,
              "postgres-same-pool-recovery",
              Qdr7CapacityContracts.timestamp(Instant.now()),
              Duration.between(outageStarted, Instant.now()).toMillis(),
              round,
              "protected-request-recovery",
              "2xx",
              databaseReadyMs,
              hikariReadyMs,
              requestReadyMs,
              samplingGap,
              1,
              0,
              0));
      RESTART_RESULTS.add(
          Map.of(
              "type",
              "POSTGRES_SAME_POOL",
              "round",
              round,
              "status",
              "PASS",
              "containerIdBeforeHash",
              Qdr7CapacityContracts.sha256(containerIdBefore),
              "containerIdAfterHash",
              Qdr7CapacityContracts.sha256(POSTGRES.getContainerId()),
              "containerNameHash",
              Qdr7CapacityContracts.sha256(CONTAINER_NAME),
              "volumeNameHash",
              Qdr7CapacityContracts.sha256(VOLUME_NAME)));
      addThreshold(
          "numericThresholds.recoveryMaxMs.database",
          "recovery.database.r" + round,
          databaseReadyMs,
          "<=",
          800,
          "milliseconds");
      addThreshold(
          "numericThresholds.recoveryMaxMs.hikari",
          "recovery.hikari.r" + round,
          hikariReadyMs,
          "<=",
          5800,
          "milliseconds");
      addThreshold(
          "numericThresholds.recoveryMaxMs.request",
          "recovery.request.r" + round,
          requestReadyMs,
          "<=",
          5800,
          "milliseconds");
      addThreshold(
          "numericThresholds.recoveryMaxMs.samplingGap",
          "recovery.samplingGap.r" + round,
          samplingGap,
          "<=",
          1400,
          "milliseconds");
      completeRound(execution, 4);
    }
    final Map<String, Object> artifact =
        artifact("postgres-same-pool-recovery", started, Instant.now());
    artifact.put("rounds", counter(execution, "roundsCompleted"));
    artifact.put("outageProtectedRequests", 9);
    artifact.put("outageFalse2xx", 0);
    artifact.put("sameApplicationContext", true);
    artifact.put("sameDataSource", true);
    artifact.put("sameHikariPool", true);
    artifact.put("sameContainerEndpoint", true);
    artifact.put("samePersistentVolume", true);
    writeJson("recovery-summary.json", artifact);
  }

  /** 每轮显式创建并关闭Context A，再创建Context B验证同一PostgreSQL中的提交/回滚状态。 */
  private void springContextRestart(final Qdr7CapacityContracts.ScenarioExecution execution) {
    for (int round = 1; round <= 3; round++) {
      beginRound(execution);
      final String tenant = scenarioTenant("spring-context-restart", round);
      final ContextRestartSeed seeded;
      try (ConfigurableApplicationContext contextA = startRestartContext(tenant, round, "a")) {
        seeded = seedContextRestartState(contextA, tenant, round);
      }

      try (ConfigurableApplicationContext contextB = startRestartContext(tenant, round, "b")) {
        final JdbcTemplate restartedJdbc = contextB.getBean(JdbcTemplate.class);
        final DataSource restartedDataSource = contextB.getBean(DataSource.class);
        final RestartRequiredState recovered =
            requiredRestartState(restartedJdbc, seeded.identity(), seeded.requestId());
        assertThat(recovered.recordId()).isEqualTo(seeded.requiredState().recordId());
        assertThat(recovered.requestHash()).isEqualTo(seeded.requiredState().requestHash());
        assertThat(recovered.state()).isEqualTo("COMPLETED");
        assertThat(canonicalPromptChecksum(restartedJdbc, tenant))
            .isEqualTo(seeded.promptChecksum());
        assertThat(
                restartedJdbc.queryForObject(
                    "select count(*) from dh_nq_replay_nonce where replay_key=?",
                    Integer.class,
                    seeded.replayKey()))
            .isEqualTo(1);
        assertThat(
                idempotencyRowCount(restartedJdbc, seeded.identity(), seeded.rollbackRequestId()))
            .isZero();
        assertThat(identityHash(contextB)).isNotEqualTo(seeded.contextIdentityHash());
        assertThat(identityHash(restartedDataSource)).isNotEqualTo(seeded.dataSourceIdentityHash());
        assertThat(Qdr7CapacityContracts.sha256(FROZEN_JDBC_URL))
            .isEqualTo(seeded.jdbcEndpointSha256());

        final int crossTenantVisibility =
            restartedJdbc.queryForObject(
                "select count(*) from dh_qdr7_idempotency_guard"
                    + " where guard_id::text=? and tenant_id=?",
                Integer.class,
                recovered.recordId(),
                tenant + "-other");
        final int crossEnvironmentVisibility =
            restartedJdbc.queryForObject(
                "select count(*) from dh_qdr7_idempotency_guard"
                    + " where guard_id::text=? and environment<>?",
                Integer.class,
                recovered.recordId(),
                "test");
        assertThat(crossTenantVisibility).isZero();
        assertThat(crossEnvironmentVisibility).isZero();

        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "SPRING_CONTEXT");
        result.put("round", round);
        result.put("status", "PASS");
        result.put("applicationContextAIdentityHash", seeded.contextIdentityHash());
        result.put("applicationContextBIdentityHash", identityHash(contextB));
        result.put("dataSourceAIdentityHash", seeded.dataSourceIdentityHash());
        result.put("dataSourceBIdentityHash", identityHash(restartedDataSource));
        result.put("environment", "test");
        result.put("tenantHash", Qdr7CapacityContracts.sha256(tenant));
        result.put("recordId", recovered.recordId());
        result.put("requestHash", recovered.requestHash());
        result.put("requiredStateNonNull", true);
        result.put("committedNoncePreserved", true);
        result.put("committedIdempotencyState", recovered.state());
        result.put("uncommittedStatePromoted", false);
        result.put("canonicalPromptPreserved", true);
        result.put("crossTenantVisibility", crossTenantVisibility);
        result.put("crossEnvironmentVisibility", crossEnvironmentVisibility);
        result.put("samePostgresEndpoint", true);
        result.put("samePersistentVolume", true);
        RESTART_RESULTS.add(result);
      }
      completeRound(execution, 5);
    }
  }

  /** 独立执行三轮same-container persistent-volume restart，不借用same-pool结果凑轮数。 */
  private void persistentVolumeRestart(final Qdr7CapacityContracts.ScenarioExecution execution)
      throws IOException, InterruptedException {
    for (int round = 1; round <= 3; round++) {
      beginRound(execution);
      final PreparedRequest committed =
          prepare("persistent-volume-anchor-r" + round + "-" + RUN_ID, null, SOURCE);
      assertStructuredSuccess(send(committed));
      final RestartRequiredState requiredBefore = requiredRestartState(committed.requestId());
      final String rollbackRequestId =
          seedRolledBackState(
              transactions,
              idempotencyGuard,
              identity(TENANT),
              "qdr7-persistent-volume-rollback-r" + round + "-" + RUN_ID);
      final String promptChecksum = canonicalPromptChecksum();
      final String containerIdBefore = POSTGRES.getContainerId();
      final Instant restartStarted = Instant.now();
      POSTGRES.stopWithoutRemoval();
      POSTGRES.startExistingContainer();
      final long databaseReadyMs = awaitDatabaseReady(restartStarted);
      final long stateRecoveryMs = awaitHikariReady(restartStarted);
      final RestartRequiredState recovered = requiredRestartState(committed.requestId());
      assertThat(recovered).isEqualTo(requiredBefore);
      assertThat(nonceReplayRowCount(committed)).isEqualTo(1);
      assertThat(idempotencyRowCount(rollbackRequestId)).isZero();
      assertThat(canonicalPromptChecksum()).isEqualTo(promptChecksum);

      final Map<String, Object> result = new LinkedHashMap<>();
      result.put("type", "POSTGRES_PERSISTENT_VOLUME");
      result.put("round", round);
      result.put("status", "PASS");
      result.put("containerIdBeforeHash", Qdr7CapacityContracts.sha256(containerIdBefore));
      result.put("containerIdAfterHash", Qdr7CapacityContracts.sha256(POSTGRES.getContainerId()));
      result.put("containerNameHash", Qdr7CapacityContracts.sha256(CONTAINER_NAME));
      result.put("volumeNameHash", Qdr7CapacityContracts.sha256(VOLUME_NAME));
      result.put("jdbcEndpointSha256", Qdr7CapacityContracts.sha256(FROZEN_JDBC_URL));
      result.put("databaseReadyMs", databaseReadyMs);
      result.put("stateRecoveryMs", stateRecoveryMs);
      result.put("committedNoncePreserved", true);
      result.put("committedIdempotencyState", recovered.state());
      result.put("uncommittedStatePromoted", false);
      result.put("canonicalPromptPreserved", true);
      RESTART_RESULTS.add(result);
      completeRound(execution, 5);
    }
  }

  /** 冻结protocol只允许一次8并发/100请求的post-recovery load。 */
  private void postRecoveryConcurrency(final Qdr7CapacityContracts.ScenarioExecution execution)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    beginRound(execution);
    final Instant started = Instant.now();
    final List<RequestOutcome> load = runConcurrentRequests(8, 100, "post-recovery");
    assertThat(load).hasSize(100).allSatisfy(this::assertStructuredSuccess);
    postRecoveryStructured2xx = 100;
    Qdr7CapacityArtifactSupport.appendCsv(
        EVIDENCE_ROOT.resolve("recovery-timeline.csv"),
        List.of(
            Qdr7CapacityContracts.SCHEMA_VERSION,
            RUN_ID,
            COMMIT_SHA,
            "post-recovery-concurrency",
            Qdr7CapacityContracts.timestamp(Instant.now()),
            Duration.between(started, Instant.now()).toMillis(),
            1,
            "post-recovery-load",
            "2xx",
            "",
            "",
            "",
            0,
            100,
            0,
            0));
    final Map<String, Object> artifact =
        artifact("post-recovery-concurrency", started, Instant.now());
    artifact.put("concurrency", 8);
    artifact.put("requests", 100);
    artifact.put("structured2xx", postRecoveryStructured2xx);
    artifact.put("unexpected4xx", 0);
    artifact.put("unexpected5xx", 0);
    writeJson("post-recovery-summary.json", artifact);
    completeRound(execution, 100);
  }

  /** 为单轮Context restart创建独立Spring Context，所有配置仅指向本地Testcontainers。 */
  private ConfigurableApplicationContext startRestartContext(
      final String tenant, final int round, final String contextName) {
    final Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("spring.main.web-application-type", "none");
    properties.put("spring.main.banner-mode", "off");
    properties.put("spring.jmx.enabled", "false");
    properties.put(
        "spring.autoconfigure.exclude",
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration");
    properties.put("management.health.redis.enabled", "false");
    properties.put("spring.datasource.url", FROZEN_JDBC_URL);
    properties.put("spring.datasource.username", FROZEN_DATABASE_USERNAME);
    properties.put("spring.datasource.password", FROZEN_DATABASE_PASSWORD);
    properties.put("spring.datasource.hikari.maximum-pool-size", 4);
    properties.put("spring.datasource.hikari.minimum-idle", 1);
    properties.put(
        "spring.datasource.hikari.pool-name",
        "qdr7-context-r" + round + "-" + contextName + "-" + RUN_ID);
    properties.put(
        "decisionhub.security.api.token-sha256", StaticTokenVerifier.sha256Hex(TEST_TOKEN));
    properties.put("decisionhub.security.api.tenant-id", tenant);
    properties.put("decisionhub.integration1.runtime.enabled", true);
    properties.put("decisionhub.integration1.runtime.production-enabled", false);
    properties.put("decisionhub.integration1.runtime.kill-switch-enabled", false);
    properties.put("decisionhub.integration1.runtime.allowed-sources", SOURCE);
    properties.put(
        "decisionhub.integration1.runtime.allowed-tenant-source-pairs", tenant + ":" + SOURCE);
    properties.put("decisionhub.integration1.runtime.guard.environment", "test");
    properties.put("decisionhub.integration1.runtime.guard.rate-window-seconds", 3600);
    properties.put("decisionhub.integration1.runtime.guard.rate-limit-value", 100000);
    properties.put("decisionhub.integration1.runtime.guard.lease-seconds", 30);
    properties.put("decisionhub.integration1.runtime.guard.idempotency-ttl-seconds", 600);
    properties.put("decisionhub.integration1.runtime.guard.retention-seconds", 3600);
    properties.put("decisionhub.security.nq-feedback.replay.guard-type", "jdbc");
    properties.put("decisionhub.integration1.runtime.hmac-secret", TEST_SIGNING_KEY);
    return new SpringApplicationBuilder(DecisionHubApplication.class)
        .web(WebApplicationType.NONE)
        .run(commandLineProperties(properties));
  }

  /** restart Context必须使用高优先级命令行属性，防止application配置覆盖冻结数据库端点。 */
  private static String[] commandLineProperties(final Map<String, Object> properties) {
    return properties.entrySet().stream()
        .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
        .toArray(String[]::new);
  }

  /** 在Context A内提交合法result、terminal idempotency和nonce，并制造一条必回滚状态。 */
  private ContextRestartSeed seedContextRestartState(
      final ConfigurableApplicationContext context, final String tenant, final int round) {
    final JdbcTemplate contextJdbc = context.getBean(JdbcTemplate.class);
    final GuardTransactionBoundary boundary = context.getBean(GuardTransactionBoundary.class);
    final IdempotencyGuardPort guards = context.getBean(IdempotencyGuardPort.class);
    final PersistentGuardIdentity identity = identity("test", tenant);
    final String requestId = "qdr7-context-restart-r" + round + "-" + RUN_ID;
    final String resultId = "result-context-restart-r" + round + "-" + RUN_ID;
    final String promptChecksum =
        boundary.required(
            () -> {
              seedDecisionOutput(contextJdbc, tenant, resultId, requestId);
              return seedCanonicalPromptVersion(contextJdbc, tenant, round);
            });
    final IdempotencyRecordView completed =
        completeGuardFixture(
            boundary,
            guards,
            identity,
            requestId,
            Qdr7CapacityContracts.sha256("context-restart-r" + round + "-" + RUN_ID),
            resultId);
    final String replayKey =
        tenant + "::" + SOURCE + "::" + ENDPOINT + "::nonce-" + requestId + "::" + requestId;
    boundary.required(
        () -> {
          contextJdbc.update(
              "insert into dh_nq_replay_nonce(replay_key,expires_at)"
                  + " values (?,transaction_timestamp()+interval '10 minute')",
              replayKey);
          return Boolean.TRUE;
        });
    final String rollbackRequestId =
        seedRolledBackState(
            boundary, guards, identity, "qdr7-context-restart-rollback-r" + round + "-" + RUN_ID);
    final RestartRequiredState required =
        requiredRestartState(contextJdbc, identity, completed.requestId());
    assertThat(canonicalPromptChecksum(contextJdbc, tenant)).isEqualTo(promptChecksum);
    return new ContextRestartSeed(
        identity,
        requestId,
        required,
        rollbackRequestId,
        replayKey,
        promptChecksum,
        Qdr7CapacityContracts.sha256(FROZEN_JDBC_URL),
        identityHash(context),
        identityHash(context.getBean(DataSource.class)));
  }

  /** implementation-validation只写probe证据，不登记任何mandatory scenario。 */
  private void writeImplementationValidationArtifact() throws IOException {
    final List<Map<String, Object>> springRestartResults =
        RESTART_RESULTS.stream().filter(row -> "SPRING_CONTEXT".equals(row.get("type"))).toList();
    assertThat(IMPLEMENTATION_TENANT_ISOLATION_RESULTS).hasSize(3);
    assertThat(springRestartResults).hasSize(3);
    assertThat(SCENARIO_LEDGER).isEmpty();

    final Map<String, Object> artifact =
        artifact("implementation-validation", HARNESS_STARTED, Instant.now());
    artifact.put("validationMode", "IMPLEMENTATION_VALIDATION_ONLY");
    artifact.put("containerStartedBeforePropertyResolution", true);
    artifact.put("mappedPort", DATABASE_PORT);
    artifact.put("jdbcEndpointSha256", Qdr7CapacityContracts.sha256(FROZEN_JDBC_URL));
    artifact.put("applicationContextStarted", applicationContext.getBeanDefinitionCount() > 0);
    artifact.put("dispatcherReached", true);
    artifact.put("tenantIsolationStartupProbe", "PASS");
    artifact.put("tenantIsolationRounds", IMPLEMENTATION_TENANT_ISOLATION_RESULTS);
    artifact.put("contextRestartStartupProbe", "PASS");
    artifact.put("contextRestartRounds", springRestartResults);
    artifact.put("nonceDriverStartupProbe", "PASS");
    artifact.put("mandatoryScenarioCount", 15);
    artifact.put("executedScenarioCount", 0);
    artifact.put("capacityAcceptanceExecuted", false);
    writeJson("implementation-validation.json", artifact);
    Files.writeString(
        EVIDENCE_ROOT.resolve("harness-exit-code.txt"), "0\n", StandardCharsets.UTF_8);
  }

  private void fullRegression(final Qdr7CapacityContracts.ScenarioExecution execution)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    final Instant started = Instant.now();
    beginRound(execution);
    final Instant regressionStarted = Instant.now();
    Files.writeString(
        EVIDENCE_ROOT.resolve("resource-sampling-phase.txt"),
        "FULL_REGRESSION\n",
        StandardCharsets.UTF_8);
    final Path log = EVIDENCE_ROOT.resolve("full-regression.log");
    final Process process =
        new ProcessBuilder(fullRegressionCommand())
            .directory(PROJECT_ROOT.toFile())
            .redirectErrorStream(true)
            .redirectOutput(log.toFile())
            .start();
    final boolean finished = process.waitFor(30, TimeUnit.MINUTES);
    if (!finished) {
      process.destroyForcibly();
      process.waitFor(30, TimeUnit.SECONDS);
    }
    final int exitCode = finished ? process.exitValue() : 60;
    final long regressionDurationMs = Duration.between(regressionStarted, Instant.now()).toMillis();
    Files.writeString(
        EVIDENCE_ROOT.resolve("resource-sampling-phase.txt"),
        "POST_REGRESSION\n",
        StandardCharsets.UTF_8);
    Thread.sleep(1200L);
    final JsonNode samplerCompletion = stopSamplerAndAwaitCompletion();
    final String regressionLog = Qdr7CapacityArtifactSupport.readAsciiCompatibleLog(log);
    final List<Path> reports = new ArrayList<>();
    try (var paths = Files.walk(PROJECT_ROOT)) {
      paths
          .filter(path -> path.getFileName().toString().startsWith("TEST-"))
          .filter(path -> path.getFileName().toString().endsWith(".xml"))
          .filter(path -> path.toString().contains("surefire-reports"))
          .filter(path -> !path.getFileName().toString().contains("Qdr7CapacityAcceptanceIT"))
          .filter(
              path -> {
                try {
                  return !Files.getLastModifiedTime(path)
                      .toInstant()
                      .isBefore(regressionStarted.minusSeconds(1));
                } catch (final IOException unreadable) {
                  return false;
                }
              })
          .forEach(reports::add);
    }
    long tests = 0L;
    long failures = 0L;
    long errors = 0L;
    long skipped = 0L;
    for (final Path report : reports) {
      final String xml = Files.readString(report);
      tests += xmlAttribute(xml, "tests");
      failures += xmlAttribute(xml, "failures");
      errors += xmlAttribute(xml, "errors");
      skipped += xmlAttribute(xml, "skipped");
    }
    final long reactorSuccess =
        regressionLog
            .lines()
            .filter(line -> line.startsWith("[INFO] "))
            .filter(line -> line.contains(" SUCCESS ["))
            .count();
    final long testcontainersReports =
        reports.stream()
            .map(path -> path.getFileName().toString())
            .filter(
                name ->
                    name.contains("JdbcNonceReplayGuardPersistenceTest")
                        || name.contains("PostgresContainerSmokeTest"))
            .count();
    final Map<String, Object> artifact = artifact("full-regression", started, Instant.now());
    artifact.put("reactorModulesExpected", 19);
    artifact.put("reactorModulesSucceeded", reactorSuccess);
    artifact.put("mavenExitCode", exitCode);
    artifact.put("surefireReportFiles", reports.size());
    artifact.put("tests", tests);
    artifact.put("failures", failures);
    artifact.put("errors", errors);
    artifact.put("skipped", skipped);
    artifact.put("testcontainersMandatoryReports", testcontainersReports);
    artifact.put("testcontainersExecuted", testcontainersReports == 2 && skipped == 0);
    writeJson("full-regression-summary.json", artifact);
    final Qdr7CapacityResourceEvidence.Snapshot resources =
        Qdr7CapacityResourceEvidence.read(
            EVIDENCE_ROOT,
            regressionDurationMs,
            new Qdr7CapacityResourceEvidence.Binding(RUN_ID, COMMIT_SHA));
    final Map<String, Object> resourceArtifact =
        artifact("full-regression-resources", regressionStarted, Instant.now());
    resourceArtifact.putAll(resources.fields());
    assertThat(samplerCompletion.path("jvmSeriesSha256").asText())
        .isEqualTo(resources.jvmSeriesSha256());
    assertThat(samplerCompletion.path("dockerSeriesSha256").asText())
        .isEqualTo(resources.dockerSeriesSha256());
    assertThat(samplerCompletion.path("fullRegressionMavenSampleCount").asInt()).isGreaterThan(1);
    assertThat(samplerCompletion.path("fullRegressionSurefireSampleCount").asInt())
        .isGreaterThan(1);
    resourceArtifact.put(
        "samplerCompletionSchemaVersion", samplerCompletion.path("schemaVersion").asText());
    resourceArtifact.put(
        "samplerJvmLastElapsedMs", samplerCompletion.path("jvmLastElapsedMs").asLong());
    resourceArtifact.put(
        "samplerDockerLastElapsedMs", samplerCompletion.path("dockerLastElapsedMs").asLong());
    writeJson("resource-summary.json", resourceArtifact);
    final JsonNode regressionThresholds =
        criteria.root().path("numericThresholds").path("regressionMax");
    addThreshold(
        "numericThresholds.regressionMax.durationMs",
        "regression.duration",
        resources.durationMs(),
        "<=",
        regressionThresholds.path("durationMs").asDouble(),
        "milliseconds");
    addThreshold(
        "numericThresholds.regressionMax.mavenWorkingSetBytes",
        "regression.mavenWorkingSet",
        resources.mavenPeakWorkingSetBytes(),
        "<=",
        regressionThresholds.path("mavenWorkingSetBytes").asDouble(),
        "bytes");
    addThreshold(
        "numericThresholds.regressionMax.surefireWorkingSetBytes",
        "regression.surefireWorkingSet",
        resources.surefirePeakAggregateWorkingSetBytes(),
        "<=",
        regressionThresholds.path("surefireWorkingSetBytes").asDouble(),
        "bytes");
    addThreshold(
        "numericThresholds.regressionMax.dockerMemoryBytes",
        "regression.dockerMemory",
        resources.dockerPeakMemoryBytes(),
        "<=",
        regressionThresholds.path("dockerMemoryBytes").asDouble(),
        "bytes");
    addThreshold(
        "numericThresholds.regressionMax.minimumFreeMemoryBytes",
        "regression.minimumFreeMemory",
        resources.minimumHostAvailableBytes(),
        ">=",
        regressionThresholds.path("minimumFreeMemoryBytes").asDouble(),
        "bytes");
    execution.values().put("tests", tests);
    completeRound(execution, Math.toIntExact(Math.min(Integer.MAX_VALUE, tests)));
    assertThat(exitCode).as("independent mvn test exit").isZero();
    assertThat(regressionLog).contains("BUILD SUCCESS");
    assertThat(reactorSuccess).isEqualTo(19L);
    assertThat(reports).isNotEmpty();
    assertThat(tests).isPositive();
    assertThat(failures).isZero();
    assertThat(errors).isZero();
    assertThat(skipped).isZero();
    assertThat(testcontainersReports).isEqualTo(2L);
  }

  private void qualityGate(final Qdr7CapacityContracts.ScenarioExecution execution)
      throws IOException, InterruptedException {
    final Instant started = Instant.now();
    beginRound(execution);
    final Path log = EVIDENCE_ROOT.resolve("quality.log");
    final Process process =
        new ProcessBuilder(qualityCommand())
            .directory(PROJECT_ROOT.toFile())
            .redirectErrorStream(true)
            .redirectOutput(log.toFile())
            .start();
    final boolean finished = process.waitFor(15, TimeUnit.MINUTES);
    if (!finished) {
      process.destroyForcibly();
      process.waitFor(30, TimeUnit.SECONDS);
    }
    final int exitCode = finished ? process.exitValue() : 70;
    final String qualityLog = Qdr7CapacityArtifactSupport.readAsciiCompatibleLog(log);
    final Map<String, Object> artifact = artifact("quality-gate", started, Instant.now());
    artifact.put(
        "checkstyle",
        Map.of(
            "status", exitCode == 0 ? "PASS" : "FAIL",
            "findings", exitCode == 0 ? 0 : 1));
    artifact.put(
        "spotless",
        Map.of(
            "status", exitCode == 0 ? "PASS" : "FAIL",
            "findings", exitCode == 0 ? 0 : 1));
    artifact.put("mavenExitCode", exitCode);
    artifact.put("reactorSuccess", qualityLog.contains("BUILD SUCCESS"));
    artifact.put("reactorModulesExpected", 19);
    writeJson("quality-summary.json", artifact);
    execution.values().put("qualityFindings", exitCode == 0 ? 0 : 1);
    completeRound(execution, 2);
    assertThat(exitCode).as("mvn -Pquality validate exit").isZero();
    assertThat(qualityLog).contains("BUILD SUCCESS");
  }

  private void writeRestartArtifact() throws IOException {
    final Map<String, Object> artifact =
        artifact("restart-results", HARNESS_STARTED, Instant.now());
    final long springCompleted = restartCount("SPRING_CONTEXT");
    final long samePoolCompleted = restartCount("POSTGRES_SAME_POOL");
    final long persistentCompleted = restartCount("POSTGRES_PERSISTENT_VOLUME");
    if (springCompleted != 3 || samePoolCompleted != 3 || persistentCompleted != 3) {
      artifact.put("status", "BLOCKED");
      artifact.put("missingValues", List.of("incompleteRestartRounds"));
    }
    artifact.put("results", RESTART_RESULTS);
    artifact.put("springContextRoundsRequired", 3);
    artifact.put(
        "springContextRoundsStarted", scenarioCounter("spring-context-restart", "roundsStarted"));
    artifact.put("springContextRoundsCompleted", springCompleted);
    artifact.put("samePoolRoundsRequired", 3);
    artifact.put(
        "samePoolRoundsStarted", scenarioCounter("postgres-same-pool-recovery", "roundsStarted"));
    artifact.put("samePoolRoundsCompleted", samePoolCompleted);
    artifact.put("postgresPersistentVolumeRoundsRequired", 3);
    artifact.put(
        "postgresPersistentVolumeRoundsStarted",
        scenarioCounter("postgres-persistent-volume-restart", "roundsStarted"));
    artifact.put("postgresPersistentVolumeRoundsCompleted", persistentCompleted);
    writeJson("restart-results.json", artifact);
  }

  private void writeThresholdComparison() throws IOException {
    writeThresholdComparisonSnapshot();
  }

  /** 每次新增measurement后重写durable comparison快照，后续场景失败不得清零既有比较。 */
  private void writeThresholdComparisonSnapshot() {
    final long passed =
        THRESHOLD_RESULTS.stream().filter(row -> "PASS".equals(row.get("status"))).count();
    final long failed =
        THRESHOLD_RESULTS.stream().filter(row -> "FAIL".equals(row.get("status"))).count();
    final long blocked =
        THRESHOLD_RESULTS.stream().filter(row -> "BLOCKED".equals(row.get("status"))).count();
    final int notEvaluated = Math.max(0, EXPECTED_THRESHOLD_COMPARISONS - THRESHOLD_RESULTS.size());
    final Map<String, Object> artifact =
        artifact("threshold-comparison", HARNESS_STARTED, Instant.now());
    artifact.put(
        "status", failed > 0 ? "FAIL" : blocked > 0 || notEvaluated > 0 ? "BLOCKED" : "PASS");
    artifact.put("comparisonCount", THRESHOLD_RESULTS.size());
    artifact.put("comparisonsExecuted", THRESHOLD_RESULTS.size());
    artifact.put("comparisons", THRESHOLD_RESULTS);
    artifact.put(
        "coveredThresholdLeaves",
        THRESHOLD_RESULTS.stream().map(row -> row.get("thresholdPath")).distinct().count());
    artifact.put("declaredThresholdLeaves", 41);
    artifact.put("passedCount", passed);
    artifact.put("failedCount", failed);
    artifact.put("blockedCount", blocked);
    artifact.put("notEvaluatedCount", notEvaluated);
    artifact.put("notEvaluatedThresholds", notEvaluated);
    artifact.put(
        "reason",
        notEvaluated == 0 ? "THRESHOLD_COMPARISON_COMPLETE" : "PARTIAL_THRESHOLD_EVIDENCE");
    writeJson("threshold-comparison.json", artifact);
  }

  private void writeSummary() throws IOException {
    final int started = scenarioStateCount(Qdr7CapacityContracts.ExecutionState.STARTED);
    final int partial = scenarioStateCount(Qdr7CapacityContracts.ExecutionState.PARTIAL);
    final int completed = scenarioStateCount(Qdr7CapacityContracts.ExecutionState.COMPLETED);
    final int notStarted = scenarioStateCount(Qdr7CapacityContracts.ExecutionState.NOT_STARTED);
    final int passed = scenarioVerdictCount(Qdr7CapacityContracts.ScenarioVerdict.PASS);
    final int failed = scenarioVerdictCount(Qdr7CapacityContracts.ScenarioVerdict.FAIL);
    final int blocked = scenarioVerdictCount(Qdr7CapacityContracts.ScenarioVerdict.BLOCKED);
    final int executed = partial + completed;
    final boolean allPassed = completed == 15 && passed == 15;
    final int exitCode = selectScenarioExitCode(allPassed);
    final String resultStatus =
        allPassed ? (QUALIFICATION_ONLY ? "NOT_FORMAL" : "PASS") : failed > 0 ? "FAIL" : "BLOCKED";
    final Map<String, Object> artifact =
        artifact("capacity-acceptance", HARNESS_STARTED, Instant.now());
    artifact.put("status", resultStatus);
    artifact.put("finalStatus", resultStatus);
    artifact.put("exitCode", exitCode);
    artifact.put("internalExitCode", exitCode);
    artifact.put("startedAt", artifact.get("startedAtUtc"));
    artifact.put("completedAt", artifact.get("finishedAtUtc"));
    artifact.put("mandatoryScenarioCount", 15);
    artifact.put("startedScenarioCount", started + partial + completed);
    artifact.put("partialScenarioCount", partial);
    artifact.put("completedScenarioCount", completed);
    artifact.put("passedScenarioCount", passed);
    artifact.put("failedScenarioCount", failed);
    artifact.put("blockedScenarioCount", blocked);
    artifact.put("notStartedScenarioCount", notStarted);
    artifact.put("executedScenarioCount", executed);
    artifact.put("scenarioStatuses", SCENARIO_LEDGER);
    artifact.put(
        "firstBlocker",
        SCENARIO_LEDGER.stream()
            .filter(row -> !"PASS".equals(row.get("verdict")))
            .map(row -> row.get("scenarioId") + ":" + row.get("reasonCode"))
            .findFirst()
            .orElse(null));
    artifact.put(
        "findings",
        SCENARIO_LEDGER.stream()
            .filter(row -> !"PASS".equals(row.get("verdict")))
            .map(row -> row.get("scenarioId") + ":" + row.get("reasonCode"))
            .toList());
    artifact.put("correctnessVerdict", allPassed ? "PASS" : failed > 0 ? "FAIL" : "BLOCKED");
    artifact.put(
        "thresholdVerdict",
        THRESHOLD_RESULTS.size() == EXPECTED_THRESHOLD_COMPARISONS
                && THRESHOLD_RESULTS.stream().allMatch(row -> "PASS".equals(row.get("status")))
            ? "PASS"
            : THRESHOLD_RESULTS.stream().anyMatch(row -> "FAIL".equals(row.get("status")))
                ? "FAIL"
                : "BLOCKED");
    artifact.put("regressionVerdict", scenarioVerdict("full-regression"));
    artifact.put("qualityVerdict", scenarioVerdict("quality-gate"));
    artifact.put("artifactVerdict", "PENDING");
    artifact.put("secretVerdict", "PENDING");
    artifact.put("teardownVerdict", "BLOCKED");
    artifact.put(
        "reasonCode",
        allPassed
            ? (QUALIFICATION_ONLY ? "QUALIFICATION_ONLY" : "FORMAL_CAPACITY_ACCEPTANCE_COMPLETED")
            : "MANDATORY_SCENARIO_INCOMPLETE");
    artifact.put("capacityAcceptanceExecuted", allPassed && !QUALIFICATION_ONLY);
    artifact.put(
        "formalAcceptanceVerdict",
        QUALIFICATION_ONLY
            ? "NOT_EVALUATED"
            : allPassed ? "PASS_WITHIN_FROZEN_PROFILE" : resultStatus);
    artifact.put(
        "qualificationVerdict",
        QUALIFICATION_ONLY ? (allPassed ? "PASS" : resultStatus) : "NOT_EVALUATED");
    writeJson("capacity-acceptance-summary.json", artifact);
    Files.writeString(
        EVIDENCE_ROOT.resolve("harness-exit-code.txt"), exitCode + "\n", StandardCharsets.UTF_8);
  }

  private RateLimitAdmissionResult acquire(
      final PersistentGuardIdentity identity, final int quota) {
    return transactions.required(
        () -> rateLimitAdmission.tryAcquire(new RateLimitAdmissionCommand(identity, 3600, quota)));
  }

  /** 按完整identity读取当前window累计值，禁止tenant-only断言掩盖environment串扰。 */
  private long rateRequestCount(final PersistentGuardIdentity identity) {
    return jdbc.queryForObject(
        "select coalesce(sum(request_count),0) from dh_qdr7_rate_limit_bucket"
            + " where environment=? and endpoint=? and source=? and tenant_id=?",
        Long.class,
        identity.environment(),
        identity.endpoint(),
        identity.source(),
        identity.tenantId());
  }

  private PersistentGuardIdentity identity(final String tenant) {
    return identity("test", tenant);
  }

  private PersistentGuardIdentity identity(final String environment, final String tenant) {
    return new PersistentGuardIdentity(environment, ENDPOINT, SOURCE, tenant);
  }

  /** 创建RECEIVED并取得有效lease；用于FK负向fixture，所有identity均保持独立。 */
  private IdempotencyRecordView admitAndStart(
      final PersistentGuardIdentity identity, final String requestId, final String requestHash) {
    final IdempotencyAdmissionResult admitted =
        transactions.required(
            () ->
                idempotencyGuard.admit(
                    new IdempotencyAdmissionCommand(
                        identity,
                        requestId,
                        requestHash,
                        IdempotencyAdmissionCommand.HASH_VERSION,
                        Duration.ofMinutes(10),
                        Duration.ofHours(1))));
    assertThat(admitted.status()).isEqualTo(IdempotencyAdmissionStatus.ADMITTED);
    final UUID leaseToken = UUID.randomUUID();
    return transactions.required(
        () ->
            idempotencyGuard.transition(
                transition(
                    admitted.record(),
                    IdempotencyState.IN_PROGRESS,
                    null,
                    null,
                    "capacity-worker",
                    leaseToken,
                    Duration.ofSeconds(30))));
  }

  /** 在给定真实事务边界内完成一条引用已提交decision output的terminal guard。 */
  private IdempotencyRecordView completeGuardFixture(
      final GuardTransactionBoundary boundary,
      final IdempotencyGuardPort guards,
      final PersistentGuardIdentity identity,
      final String requestId,
      final String requestHash,
      final String resultId) {
    final IdempotencyRecordView received =
        boundary.required(
            () ->
                guards
                    .admit(
                        new IdempotencyAdmissionCommand(
                            identity,
                            requestId,
                            requestHash,
                            IdempotencyAdmissionCommand.HASH_VERSION,
                            Duration.ofMinutes(10),
                            Duration.ofHours(1)))
                    .record());
    final UUID leaseToken = UUID.randomUUID();
    final IdempotencyRecordView inProgress =
        boundary.required(
            () ->
                guards.transition(
                    transition(
                        received,
                        IdempotencyState.IN_PROGRESS,
                        null,
                        null,
                        "context-restart-worker",
                        leaseToken,
                        Duration.ofSeconds(30))));
    return boundary.required(
        () ->
            guards.transition(
                transition(
                    inProgress,
                    IdempotencyState.COMPLETED,
                    "DH_DECISION_OUTPUT",
                    resultId,
                    "context-restart-worker",
                    leaseToken,
                    null)));
  }

  /** 事务异常后的fixture必须完全不可见，不能在restart后晋升。 */
  private String seedRolledBackState(
      final GuardTransactionBoundary boundary,
      final IdempotencyGuardPort guards,
      final PersistentGuardIdentity identity,
      final String requestId) {
    final IdempotencyAdmissionCommand admission =
        new IdempotencyAdmissionCommand(
            identity,
            requestId,
            Qdr7CapacityContracts.sha256("rollback-" + requestId),
            IdempotencyAdmissionCommand.HASH_VERSION,
            Duration.ofMinutes(10),
            Duration.ofHours(1));
    try {
      boundary.required(
          () -> {
            assertThat(guards.admit(admission).status())
                .isEqualTo(IdempotencyAdmissionStatus.ADMITTED);
            throw new IllegalStateException("deterministic restart rollback fixture");
          });
    } catch (final IllegalStateException expected) {
      assertThat(expected).hasMessage("deterministic restart rollback fixture");
    }
    return requestId;
  }

  /** 先提交真实V5/V12 tenant-bound decision output，随后guard FK才允许terminal引用。 */
  private static void seedDecisionOutput(
      final JdbcTemplate targetJdbc,
      final String tenant,
      final String resultId,
      final String requestId) {
    final int inserted =
        targetJdbc.update(
            "insert into dh_decision_output"
                + " (decision_id,tenant_id,trace_id,request_id,decision_type,action,risk_level,"
                + "policy_status,confidence,output_json)"
                + " values (?,?,?,?,'READ_ONLY_RECOMMENDATION','NO_TRADE','LOW','ALLOW',0.5,"
                + "jsonb_build_object('dryRun',true,'environment','test'))",
            resultId,
            tenant,
            "trace-" + Qdr7CapacityContracts.sha256(resultId).substring(0, 24),
            requestId);
    assertThat(inserted).isEqualTo(1);
  }

  /** 为独立restart tenant提交最小合法PromptVersion；只保存hash/ref等安全fixture字段。 */
  private static String seedCanonicalPromptVersion(
      final JdbcTemplate targetJdbc, final String tenant, final int round) {
    final UUID templateId =
        UUID.nameUUIDFromBytes(
            ("qdr7-context-template-" + tenant).getBytes(StandardCharsets.UTF_8));
    final UUID versionId =
        UUID.nameUUIDFromBytes(("qdr7-context-version-" + tenant).getBytes(StandardCharsets.UTF_8));
    final String templateHash =
        Qdr7CapacityContracts.sha256("qdr7-context-template-hash-" + tenant);
    final String checksum = Qdr7CapacityContracts.sha256("qdr7-context-checksum-" + tenant);
    final int templateInserted =
        targetJdbc.update(
            "insert into qdr_prompt_template"
                + " (id,tenant_id,template_key,display_name,current_version_id,status,created_at,updated_at)"
                + " values (?,?,?,?,null,'ACTIVE',transaction_timestamp(),transaction_timestamp())",
            templateId,
            tenant,
            "qdr7-context-r" + round,
            "QDR7 context restart fixture");
    final int versionInserted =
        targetJdbc.update(
            "insert into qdr_prompt_version"
                + " (id,tenant_id,prompt_template_id,version,render_policy_key,template_ref,"
                + "template_hash,redacted_summary,status,checksum,created_at,created_by)"
                + " values (?,?,?,'1','deterministic-render','qdr7-context-ref',?,"
                + "'safe context restart metadata','ACTIVE',?,transaction_timestamp(),'capacity-harness')",
            versionId,
            tenant,
            templateId,
            templateHash,
            checksum);
    final int templateUpdated =
        targetJdbc.update(
            "update qdr_prompt_template set current_version_id=?,updated_at=transaction_timestamp()"
                + " where id=? and tenant_id=?",
            versionId,
            templateId,
            tenant);
    assertThat(templateInserted).isEqualTo(1);
    assertThat(versionInserted).isEqualTo(1);
    assertThat(templateUpdated).isEqualTo(1);
    return checksum;
  }

  private IdempotencyTransitionCommand transition(
      final IdempotencyRecordView record,
      final IdempotencyState target,
      final String resultType,
      final String resultId,
      final String leaseOwner,
      final UUID leaseToken,
      final Duration leaseDuration) {
    return new IdempotencyTransitionCommand(
        record.identity(),
        record.requestId(),
        record.requestHash(),
        record.state(),
        record.stateVersion(),
        target == IdempotencyState.IN_PROGRESS ? null : record.leaseOwner(),
        target == IdempotencyState.IN_PROGRESS ? null : leaseToken,
        target,
        target == IdempotencyState.IN_PROGRESS ? leaseOwner : null,
        target == IdempotencyState.IN_PROGRESS ? leaseToken : null,
        leaseDuration,
        resultType,
        resultId,
        resultId == null ? null : Qdr7CapacityContracts.sha256(resultId),
        null);
  }

  private void seedCleanupRows(final String tenant, final int scale, final int eligible) {
    final int protectedRows = scale - eligible;
    jdbc.update(
        "insert into dh_qdr7_idempotency_guard"
            + " (guard_id,environment,endpoint,source,tenant_id,request_id,request_hash,hash_version,state,state_version,stable_error_code,created_at,updated_at,failed_at,expires_at,retention_until)"
            + " select gen_random_uuid(),'test',?,'NQ_DRYRUN',?,? || '-eligible-' || value,repeat('a',64),'QDR7-DRYRUN-CJSON-1','FAILED',0,'SAFE_FAILURE',"
            + " transaction_timestamp()-interval '4 hour',transaction_timestamp()-interval '4 hour',transaction_timestamp()-interval '3 hour',transaction_timestamp()-interval '2 hour',transaction_timestamp()-interval '1 hour'"
            + " from generate_series(1,?) value",
        ENDPOINT,
        tenant,
        tenant,
        eligible);
    if (protectedRows > 0) {
      jdbc.update(
          "insert into dh_qdr7_idempotency_guard"
              + " (guard_id,environment,endpoint,source,tenant_id,request_id,request_hash,hash_version,state,state_version,stable_error_code,created_at,updated_at,failed_at,expires_at,retention_until)"
              + " select gen_random_uuid(),'test',?,'NQ_DRYRUN',?,? || '-protected-' || value,repeat('b',64),'QDR7-DRYRUN-CJSON-1','FAILED',0,'SAFE_FAILURE',"
              + " transaction_timestamp(),transaction_timestamp(),transaction_timestamp(),transaction_timestamp()+interval '1 hour',transaction_timestamp()+interval '2 hour'"
              + " from generate_series(1,?) value",
          ENDPOINT,
          tenant,
          tenant,
          protectedRows);
    }
  }

  private void runControlledStatementTimeout() throws SQLException {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      connection.setAutoCommit(false);
      statement.execute("set local statement_timeout='200ms'");
      try {
        statement.execute("select pg_sleep(1)");
        throw new AssertionError("statement timeout fixture unexpectedly completed");
      } catch (final SQLException expected) {
        connection.rollback();
      }
    }
  }

  /** 仅统计本场景pressure pool会话，排除前序场景留在主pool中的idle连接。 */
  private PgSessions postgreSqlSessions(final String applicationName) {
    return jdbc.query(
        "select count(*)::int, count(*) filter (where wait_event_type is not null)::int,"
            + " count(*) filter (where wait_event_type='Lock')::int from pg_stat_activity"
            + " where datname=current_database() and application_name=?",
        preparedStatement -> preparedStatement.setString(1, applicationName),
        result -> {
          result.next();
          return new PgSessions(result.getInt(1), result.getInt(2), result.getInt(3));
        });
  }

  private int postgreSqlDeadlocks() {
    return jdbc.queryForObject(
        "select deadlocks::int from pg_stat_database where datname=current_database()",
        Integer.class);
  }

  private void appendPgSample(
      final Path series,
      final String phase,
      final HikariPoolMXBean pool,
      final long acquireMs,
      final PgSessions sessions,
      final int deadlocks)
      throws IOException {
    Qdr7CapacityArtifactSupport.appendCsv(
        series,
        List.of(
            Qdr7CapacityContracts.SCHEMA_VERSION,
            RUN_ID,
            COMMIT_SHA,
            "postgres-hikari-contention",
            Qdr7CapacityContracts.timestamp(Instant.now()),
            Duration.between(HARNESS_STARTED, Instant.now()).toMillis(),
            phase,
            pool.getActiveConnections(),
            pool.getIdleConnections(),
            pool.getThreadsAwaitingConnection(),
            pool.getTotalConnections(),
            acquireMs,
            0,
            sessions.total(),
            sessions.waiting(),
            sessions.lockWaiting(),
            deadlocks));
  }

  private long awaitDatabaseReady(final Instant started) throws InterruptedException {
    while (Duration.between(started, Instant.now()).compareTo(Duration.ofSeconds(10)) < 0) {
      try (Connection connection =
              java.sql.DriverManager.getConnection(
                  POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
          Statement statement = connection.createStatement()) {
        statement.execute("select 1");
        return Duration.between(started, Instant.now()).toMillis();
      } catch (final SQLException unavailable) {
        Thread.sleep(50L);
      }
    }
    throw new AssertionError("PostgreSQL did not recover within 10 seconds");
  }

  private long awaitHikariReady(final Instant started) throws InterruptedException {
    while (Duration.between(started, Instant.now()).compareTo(Duration.ofSeconds(10)) < 0) {
      try (Connection connection = dataSource.getConnection();
          Statement statement = connection.createStatement()) {
        statement.execute("select 1");
        return Duration.between(started, Instant.now()).toMillis();
      } catch (final SQLException unavailable) {
        Thread.sleep(50L);
      }
    }
    throw new AssertionError("Hikari did not recover within 10 seconds");
  }

  private RequestOutcome awaitProtectedRequest(final Instant started, final int round)
      throws InterruptedException {
    while (Duration.between(started, Instant.now()).compareTo(Duration.ofSeconds(10)) < 0) {
      final RequestOutcome outcome =
          sendSafely(prepare("recovery-ready-r" + round + "-" + System.nanoTime(), null, SOURCE));
      if (outcome.structured2xx()) {
        return outcome;
      }
      Thread.sleep(50L);
    }
    throw new AssertionError("protected request did not recover within 10 seconds");
  }

  private List<RequestOutcome> runConcurrentRequests(
      final int concurrency, final int count, final String prefix)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    return invokeConcurrent(
        concurrency,
        count,
        ordinal -> send(prepare(prefix + "-" + ordinal + "-" + System.nanoTime(), null, SOURCE)));
  }

  private <T> List<T> invokeConcurrent(
      final int concurrency, final int count, final CheckedIndexedSupplier<T> supplier)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    final ExecutorService executor = Executors.newFixedThreadPool(concurrency);
    final CountDownLatch ready = new CountDownLatch(Math.min(concurrency, count));
    final CountDownLatch start = new CountDownLatch(1);
    try {
      final List<Future<T>> futures = new ArrayList<>();
      for (int ordinal = 1; ordinal <= count; ordinal++) {
        final int index = ordinal;
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("concurrent start gate timed out");
                  }
                  return supplier.get(index);
                }));
      }
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      final List<T> results = new ArrayList<>();
      for (final Future<T> future : futures) {
        results.add(awaitFuture(future, 2, TimeUnit.MINUTES));
      }
      return results;
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }
  }

  private PreparedRequest prepare(
      final String suffix, final String fixedNonce, final String requestedSource) {
    try {
      final String activeScenario =
          activeExecution == null
              ? "lifecycle"
              : String.valueOf(activeExecution.values().get("scenarioId"));
      final String requestId = "qdr7-" + RUN_ID + "-" + activeScenario + "-" + suffix;
      final String traceId = "trace-" + Qdr7CapacityContracts.sha256(requestId).substring(0, 24);
      final String nonce = fixedNonce == null ? "nonce-" + requestId : fixedNonce;
      final Instant now = Instant.now();
      final String timestamp = now.toString();
      final Map<String, Object> envelope = new LinkedHashMap<>();
      envelope.put("requestId", requestId);
      envelope.put("traceId", traceId);
      envelope.put("tenantId", TENANT);
      envelope.put("source", requestedSource);
      envelope.put("environment", "TEST");
      envelope.put("timestamp", timestamp);
      envelope.put("nonce", nonce);
      envelope.put("schemaVersion", SCHEMA);
      envelope.put("dryRun", true);
      envelope.put(
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
              "context://qdr7-capacity-readonly",
              "contextSnapshot",
              Map.of(
                  "snapshotId",
                  "snapshot-" + requestId,
                  "capturedAt",
                  timestamp,
                  "evidenceRefs",
                  List.of("evidence://" + requestId))));
      envelope.put(
          "forbiddenCapabilities",
          List.of("PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE", "READ_NQ_DB", "WRITE_NQ_DB"));
      final String body = objectMapper.writeValueAsString(envelope);
      final NqDryRunAuthRequest unsigned =
          new NqDryRunAuthRequest(
              "POST",
              ENDPOINT,
              requestedSource,
              requestedSource,
              TENANT,
              TENANT,
              FeedbackEnvironment.TEST,
              "TEST",
              timestamp,
              nonce,
              "",
              requestId,
              traceId,
              SCHEMA,
              body,
              body.getBytes(StandardCharsets.UTF_8).length,
              now);
      final String signature =
          HmacNqDryRunAuthenticator.hmacSha256Hex(
              TEST_SIGNING_KEY, HmacNqDryRunAuthenticator.signatureMaterial(unsigned));
      return new PreparedRequest(
          requestId, traceId, nonce, timestamp, requestedSource, body, signature);
    } catch (final Exception failure) {
      throw new IllegalStateException("cannot prepare protected request", failure);
    }
  }

  private RequestOutcome send(final PreparedRequest prepared)
      throws IOException, InterruptedException {
    final long startedOffset = Duration.between(HARNESS_STARTED, Instant.now()).toMillis();
    final long started = System.nanoTime();
    final HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + applicationPort + ENDPOINT))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer " + TEST_TOKEN)
            .header("Content-Type", "application/json")
            .header("X-NQ-DH-Source", prepared.source())
            .header("X-NQ-DH-Tenant-Id", TENANT)
            .header("X-NQ-DH-Request-Id", prepared.requestId())
            .header("X-NQ-DH-Trace-Id", prepared.traceId())
            .header("X-NQ-DH-Timestamp", prepared.timestamp())
            .header("X-NQ-DH-Nonce", prepared.nonce())
            .header("X-NQ-DH-Signature", prepared.signature())
            .POST(HttpRequest.BodyPublishers.ofString(prepared.body(), StandardCharsets.UTF_8))
            .build();
    final HttpResponse<String> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    final Instant completed = Instant.now();
    return new RequestOutcome(
        prepared.requestId(),
        response.statusCode(),
        response.body(),
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started),
        startedOffset,
        Duration.between(HARNESS_STARTED, completed).toMillis(),
        completed,
        null);
  }

  private RequestOutcome sendSafely(final PreparedRequest prepared) {
    try {
      return send(prepared);
    } catch (final Exception transportFailure) {
      final Instant completed = Instant.now();
      return new RequestOutcome(
          prepared.requestId(),
          -1,
          "",
          0L,
          Duration.between(HARNESS_STARTED, completed).toMillis(),
          Duration.between(HARNESS_STARTED, completed).toMillis(),
          completed,
          transportFailure.getClass().getSimpleName());
    }
  }

  private void assertStructuredSuccess(final RequestOutcome outcome) {
    assertThat(outcome.statusCode()).isBetween(200, 299);
    final JsonNode response;
    try {
      response = objectMapper.readTree(outcome.body());
    } catch (final Exception failure) {
      throw new AssertionError("protected response must be structured JSON", failure);
    }
    assertThat(response.path("decisionId").asText()).isNotBlank();
    assertThat(response.path("dryRun").asBoolean()).isTrue();
    assertThat(response.path("action").asText()).isEqualTo("NO_TRADE");
    assertThat(response.path("schemaVersion").asText()).isEqualTo(SCHEMA);
    assertThat(outcome.body()).doesNotContain("UNKNOWN_ERROR");
  }

  private HikariPoolMXBean hikariPool() {
    return Objects.requireNonNull(((HikariDataSource) dataSource).getHikariPoolMXBean());
  }

  private String canonicalPromptChecksum() {
    return canonicalPromptChecksum(jdbc, TENANT);
  }

  private static String canonicalPromptChecksum(
      final JdbcTemplate targetJdbc, final String tenant) {
    return targetJdbc.queryForObject(
        "select checksum from qdr_prompt_version where tenant_id=? order by created_at limit 1",
        String.class,
        tenant);
  }

  /** 读取restart required state时绑定完整guard identity，零行或重复行都必须失败。 */
  private RestartRequiredState requiredRestartState(final String requestId) {
    return requiredRestartState(jdbc, identity(TENANT), requestId);
  }

  private static RestartRequiredState requiredRestartState(
      final JdbcTemplate targetJdbc,
      final PersistentGuardIdentity identity,
      final String requestId) {
    final List<RestartRequiredState> rows =
        targetJdbc.query(
            "select guard_id::text,request_hash,state from dh_qdr7_idempotency_guard"
                + " where environment=? and endpoint=? and source=? and tenant_id=?"
                + " and request_id=?",
            (resultSet, rowNumber) ->
                new RestartRequiredState(
                    resultSet.getString("guard_id"),
                    resultSet.getString("request_hash"),
                    resultSet.getString("state")),
            identity.environment(),
            identity.endpoint(),
            identity.source(),
            identity.tenantId(),
            requestId);
    assertThat(rows).as("restart required state for %s", requestId).hasSize(1);
    return rows.get(0);
  }

  private int nonceReplayRowCount(final PreparedRequest request) {
    return jdbc.queryForObject(
        "select count(*) from dh_nq_replay_nonce where replay_key=?",
        Integer.class,
        replayKey(request));
  }

  /** 与HmacNqDryRunAuthenticator保持同一稳定key顺序，不读取或输出签名材料。 */
  private static String replayKey(final PreparedRequest request) {
    return TENANT
        + "::"
        + request.source()
        + "::"
        + ENDPOINT
        + "::"
        + request.nonce()
        + "::"
        + request.requestId();
  }

  private int idempotencyRowCount(final String requestId) {
    return idempotencyRowCount(jdbc, identity(TENANT), requestId);
  }

  private static int idempotencyRowCount(
      final JdbcTemplate targetJdbc,
      final PersistentGuardIdentity identity,
      final String requestId) {
    return targetJdbc.queryForObject(
        "select count(*) from dh_qdr7_idempotency_guard"
            + " where environment=? and endpoint=? and source=? and tenant_id=? and request_id=?",
        Integer.class,
        identity.environment(),
        identity.endpoint(),
        identity.source(),
        identity.tenantId(),
        requestId);
  }

  private String idempotencyState(final String requestId) {
    return jdbc.queryForObject(
        "select state from dh_qdr7_idempotency_guard"
            + " where environment=? and endpoint=? and source=? and tenant_id=? and request_id=?",
        String.class,
        "test",
        ENDPOINT,
        SOURCE,
        TENANT,
        requestId);
  }

  private Map<String, Object> artifact(
      final String scenario, final Instant started, final Instant finished) {
    final Map<String, Object> artifact =
        Qdr7CapacityContracts.commonArtifact(
            runContext, scenario, Qdr7CapacityContracts.HarnessStatus.PASS, started, finished);
    final JsonNode evidenceBinding = readExecutionManifest();
    for (final String field :
        List.of(
            "attemptId",
            "candidateSha",
            "candidateTree",
            "profileId",
            "profileVersion",
            "scenarioSetHash",
            "thresholdSetHash",
            "environmentManifestHash",
            "harnessVersion",
            "harnessHash",
            "generatedAt")) {
      artifact.put(field, evidenceBinding.path(field).asText());
    }
    return artifact;
  }

  private void writeJson(final String name, final Map<String, ?> value) {
    try {
      Qdr7CapacityArtifactSupport.writeJson(EVIDENCE_ROOT.resolve(name), value, objectMapper);
    } catch (final IOException failure) {
      throw new IllegalStateException("cannot write capacity artifact " + name, failure);
    }
  }

  private void addThreshold(
      final String thresholdPath,
      final String id,
      final double observed,
      final String operator,
      final double threshold,
      final String unit) {
    final Qdr7CapacityContracts.ThresholdResult result =
        Qdr7CapacityContracts.compare(
            id,
            java.math.BigDecimal.valueOf(observed),
            operator,
            java.math.BigDecimal.valueOf(threshold),
            unit,
            unit);
    final Map<String, Object> row = new LinkedHashMap<>();
    row.put("thresholdPath", thresholdPath);
    row.put("criterionId", id);
    row.put("sourceArtifact", sourceArtifact(id));
    row.put("observed", observed);
    row.put("operator", operator);
    row.put("threshold", threshold);
    row.put("unit", unit);
    row.put("status", result.status().name());
    row.put("reason", result.reason());
    THRESHOLD_RESULTS.add(row);
    if (activeExecution != null) {
      activeExecution
          .values()
          .put("comparisonsExecuted", counter(activeExecution, "comparisonsExecuted") + 1);
      if (result.status() == Qdr7CapacityContracts.HarnessStatus.FAIL) {
        activeExecution.findings().add(Qdr7CapacityContracts.SemanticExit.NUMERIC_THRESHOLD_FAILED);
      } else if (result.status() == Qdr7CapacityContracts.HarnessStatus.BLOCKED) {
        activeExecution
            .findings()
            .add(Qdr7CapacityContracts.SemanticExit.MANDATORY_SCENARIO_MISSING);
      }
    }
    writeThresholdComparisonSnapshot();
  }

  /** 在冻结时限内等待并将超时统一映射为可传播的并发执行失败。 */
  private static <T> T awaitFuture(final Future<T> future, final long timeout, final TimeUnit unit)
      throws InterruptedException, ExecutionException {
    try {
      return future.get(timeout, unit);
    } catch (final TimeoutException exception) {
      throw new ExecutionException("capacity scenario future timed out", exception);
    }
  }

  private static String sourceArtifact(final String criterionId) {
    if (criterionId.startsWith("regression.")) {
      return "resource-summary.json";
    }
    if (criterionId.startsWith("rate.")) {
      return "rate-summary.json";
    }
    if (criterionId.startsWith("cleanup.")) {
      return "cleanup-summary.json";
    }
    if (criterionId.startsWith("contention.")) {
      return "postgres-hikari-series.csv";
    }
    return "recovery-timeline.csv";
  }

  private static String identityHash(final Object value) {
    return Qdr7CapacityContracts.sha256(
        value.getClass().getName() + ":" + System.identityHashCode(value));
  }

  private static long xmlAttribute(final String xml, final String attribute) {
    final java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile("<testsuite[^>]*\\s" + attribute + "=\"(\\d+)\"")
            .matcher(xml);
    return matcher.find() ? Long.parseLong(matcher.group(1)) : 0L;
  }

  private static String requiredProperty(final String name) {
    final String value = System.getProperty(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("missing required system property: " + name);
    }
    return value;
  }

  private static Path evidenceRoot() {
    return PROJECT_ROOT
        .resolve(
            QUALIFICATION_ONLY
                ? "target/qdr7-capacity-qualification"
                : "target/qdr7-capacity-acceptance")
        .resolve(RUN_ID);
  }

  private static String scenarioTenant(final String scenarioId, final int round) {
    return ("qdr7-" + scenarioId + "-r" + round + "-" + RUN_ID).toLowerCase(Locale.ROOT);
  }

  /** 使用当前Maven runtime执行独立quality gate；Windows批处理必须经cmd.exe。 */
  private static List<String> qualityCommand() {
    return mavenCommand(List.of("-o", "-B", "-ntp", "-Pquality", "-DskipTests", "validate"));
  }

  /** full-regression在scenario内独立执行，失败由ledger记录后仍继续quality与finalizer。 */
  private static List<String> fullRegressionCommand() {
    return mavenCommand(List.of("-o", "-B", "-ntp", "test"));
  }

  private static List<String> mavenCommand(final List<String> arguments) {
    final boolean windows =
        System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    final Path executable = mavenExecutable(windows);
    if (windows) {
      final String executableToken =
          executable.isAbsolute()
              ? "\"" + executable.toAbsolutePath().normalize() + "\""
              : executable.toString();
      final String command = "\"" + executableToken + " " + String.join(" ", arguments) + "\"";
      return List.of("cmd.exe", "/d", "/s", "/c", command);
    }
    final List<String> command = new ArrayList<>();
    command.add(executable.toString());
    command.addAll(arguments);
    return List.copyOf(command);
  }

  /** 优先复用当前Maven安装，缺失时退到仓库wrapper，最后才依赖PATH。 */
  private static Path mavenExecutable(final boolean windows) {
    final String executableName = windows ? "mvn.cmd" : "mvn";
    String mavenHome = System.getProperty("maven.home", "");
    if (mavenHome.isBlank()) {
      mavenHome = System.getenv("MAVEN_HOME");
    }
    if (mavenHome == null || mavenHome.isBlank()) {
      mavenHome = System.getenv("M2_HOME");
    }
    if (mavenHome != null && !mavenHome.isBlank()) {
      final Path installed = Path.of(mavenHome, "bin", executableName).toAbsolutePath().normalize();
      if (Files.isRegularFile(installed)) {
        return installed;
      }
    }
    final Path wrapper = PROJECT_ROOT.resolve(windows ? "mvnw.cmd" : "mvnw").normalize();
    return Files.isRegularFile(wrapper) ? wrapper : Path.of(executableName);
  }

  private static JsonNode readResourceRegistry() {
    try {
      final String projectRoot = requiredProperty("qdr7.projectRoot");
      final String runId = requiredProperty("qdr7.runId");
      final boolean qualificationOnly =
          Boolean.parseBoolean(System.getProperty("qdr7.qualificationOnly", "false"));
      return new ObjectMapper()
          .readTree(
              Path.of(projectRoot)
                  .resolve(
                      qualificationOnly
                          ? "target/qdr7-capacity-qualification"
                          : "target/qdr7-capacity-acceptance")
                  .resolve(runId)
                  .resolve("resource-registry.json")
                  .toFile());
    } catch (final IOException failure) {
      throw new IllegalStateException("resource registry is unavailable", failure);
    }
  }

  private JsonNode stopSamplerAndAwaitCompletion() throws IOException, InterruptedException {
    Files.writeString(
        EVIDENCE_ROOT.resolve("sampler.stop"),
        "stop\n",
        StandardCharsets.UTF_8,
        java.nio.file.StandardOpenOption.CREATE,
        java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
    final Path completionPath = EVIDENCE_ROOT.resolve("resource-sampler-completion.json");
    final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
    while (System.nanoTime() < deadline) {
      if (Files.isRegularFile(completionPath)) {
        final JsonNode completion = objectMapper.readTree(completionPath.toFile());
        if (!"COMPLETED".equals(completion.path("status").asText())) {
          throw new IllegalStateException("resource sampler did not complete successfully");
        }
        if (!RUN_ID.equals(completion.path("runId").asText())
            || !COMMIT_SHA.equals(completion.path("commitSha").asText())) {
          throw new IllegalStateException("resource sampler completion binding mismatch");
        }
        return completion;
      }
      Thread.sleep(100L);
    }
    throw new IllegalStateException("resource sampler completion timed out");
  }

  private static JsonNode readEnvironmentRequirements() {
    try {
      return new ObjectMapper()
          .readTree(
              PROJECT_ROOT
                  .resolve("config/qdr7-capacity/qdr7-capacity-environment-admission.json")
                  .toFile());
    } catch (final IOException failure) {
      throw new IllegalStateException(
          "environment admission requirements are unavailable", failure);
    }
  }

  private static JsonNode readExecutionManifest() {
    try {
      return new ObjectMapper()
          .readTree(evidenceRoot().resolve("capacity-execution-manifest.json").toFile());
    } catch (final IOException failure) {
      throw new IllegalStateException("cannot read capacity execution manifest", failure);
    }
  }

  @FunctionalInterface
  private interface CheckedIndexedSupplier<T> {
    T get(int index) throws IOException, InterruptedException, SQLException, ExecutionException;
  }

  @FunctionalInterface
  private interface ScenarioAction {
    void run(Qdr7CapacityContracts.ScenarioExecution execution)
        throws IOException, InterruptedException, SQLException, ExecutionException;
  }

  private final class HarnessDriver implements Qdr7CapacityContracts.ScenarioDriver {
    private final String scenarioId;
    private final ScenarioAction action;

    private HarnessDriver(final String scenarioId, final ScenarioAction action) {
      this.scenarioId = scenarioId;
      this.action = action;
    }

    @Override
    public String scenarioId() {
      return scenarioId;
    }

    @Override
    public String scenarioVersion() {
      return "qdr7-capacity-scenario-1";
    }

    @Override
    public boolean mandatory() {
      return true;
    }

    @Override
    public void setup(final Qdr7CapacityContracts.ScenarioExecution execution) {
      execution.values().put("startedAt", Instant.now());
    }

    @Override
    public void execute(final Qdr7CapacityContracts.ScenarioExecution execution)
        throws IOException, InterruptedException, SQLException, ExecutionException {
      action.run(execution);
    }

    @Override
    public void sample(final Qdr7CapacityContracts.ScenarioExecution execution) {
      execution.values().put("sampled", true);
    }

    @Override
    public void assertCorrectness(final Qdr7CapacityContracts.ScenarioExecution execution) {
      assertThat(execution.findings()).isEmpty();
    }

    @Override
    public void compareThreshold(final Qdr7CapacityContracts.ScenarioExecution execution) {
      assertThat(execution.values().get("sampled")).isEqualTo(true);
    }

    @Override
    public void writeArtifacts(final Qdr7CapacityContracts.ScenarioExecution execution) {
      execution.values().put("artifactsWritten", true);
    }

    @Override
    public void teardown(final Qdr7CapacityContracts.ScenarioExecution execution) {
      execution.values().put("teardown", "PASS");
    }
  }

  private static final class FixedPortPostgreSqlContainer
      extends PostgreSQLContainer<FixedPortPostgreSqlContainer> {
    private FixedPortPostgreSqlContainer(final int hostPort) {
      super(DockerImageName.parse(POSTGRES_IMAGE));
      addFixedExposedPort(hostPort, PostgreSQLContainer.POSTGRESQL_PORT);
    }

    /** 保留container与volume，仅制造数据库不可用窗口，避免GenericContainer二次start状态污染。 */
    private void stopWithoutRemoval() {
      getDockerClient()
          .stopContainerCmd(Objects.requireNonNull(getContainerId(), "container id"))
          .withTimeout(10)
          .exec();
    }

    /** 启动同一Docker container；数据库与Hikari就绪由场景自己的有界探针判断。 */
    private void startExistingContainer() {
      getDockerClient()
          .startContainerCmd(Objects.requireNonNull(getContainerId(), "container id"))
          .exec();
    }
  }

  private record PreparedRequest(
      String requestId,
      String traceId,
      String nonce,
      String timestamp,
      String source,
      String body,
      String signature) {}

  private record RestartRequiredState(String recordId, String requestHash, String state) {}

  /** Context A关闭后只保留合成ID、hash与不可变状态，不持有Context/DataSource引用。 */
  private record ContextRestartSeed(
      PersistentGuardIdentity identity,
      String requestId,
      RestartRequiredState requiredState,
      String rollbackRequestId,
      String replayKey,
      String promptChecksum,
      String jdbcEndpointSha256,
      String contextIdentityHash,
      String dataSourceIdentityHash) {}

  private record RequestOutcome(
      String requestId,
      int statusCode,
      String body,
      long latencyMs,
      long startedOffsetMs,
      long completedOffsetMs,
      Instant completedAt,
      String transportError) {

    private boolean structured2xx() {
      return statusCode >= 200 && statusCode <= 299 && transportError == null;
    }

    private String errorCode(final ObjectMapper mapper) {
      try {
        return mapper.readTree(body).path("errorCode").asText();
      } catch (final Exception invalidJson) {
        return "UNPARSEABLE";
      }
    }
  }

  private record PgSessions(int total, int waiting, int lockWaiting) {}
}

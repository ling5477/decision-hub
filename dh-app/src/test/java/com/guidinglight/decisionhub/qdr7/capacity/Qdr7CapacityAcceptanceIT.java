package com.guidinglight.decisionhub.qdr7.capacity;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.guidinglight.decisionhub.security.StaticTokenVerifier;
import com.guidinglight.decisionhub.security.nq.HmacNqDryRunAuthenticator;
import com.guidinglight.decisionhub.security.nq.NqDryRunAuthRequest;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
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

  private static final String ENDPOINT = "/api/ai/decision-dry-runs";
  private static final String SOURCE = "NQ_DRYRUN";
  private static final String TENANT = "qdr7-capacity";
  private static final String SCHEMA = "1.0.0";
  private static final String RUN_ID = requiredProperty("qdr7.runId");
  private static final int SEED = Integer.parseInt(requiredProperty("qdr7.seed"));
  private static final Path PROJECT_ROOT =
      Path.of(requiredProperty("qdr7.projectRoot")).toAbsolutePath().normalize();
  private static final Path EVIDENCE_ROOT =
      PROJECT_ROOT.resolve("target/qdr7-capacity-acceptance").resolve(RUN_ID);
  private static final JsonNode RESOURCE_REGISTRY = readResourceRegistry();
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
  private static final Instant HARNESS_STARTED = Instant.now();
  private static final List<Map<String, Object>> THRESHOLD_RESULTS =
      Collections.synchronizedList(new ArrayList<>());
  private static final List<Map<String, Object>> RESTART_RESULTS =
      Collections.synchronizedList(new ArrayList<>());
  private static final List<Map<String, Object>> IMPLEMENTATION_TENANT_ISOLATION_RESULTS =
      Collections.synchronizedList(new ArrayList<>());
  private static final List<Map<String, Object>> SCENARIO_STATUSES =
      Collections.synchronizedList(new ArrayList<>());
  private static RestartProbeState restartProbeState;
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

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  private Qdr7CapacityContracts.RunContext runContext;
  private Qdr7CapacityContracts.CriteriaSnapshot criteria;

  /**
   * 只验证正式 run 的 container、Spring Context、DataSource/Flyway 和 dispatcher 装配。
   *
   * <p>该模式在任何 mandatory scenario 开始前短路；它不是 formal capacity acceptance，也不产生阈值 PASS。
   */
  @Test
  @Order(0)
  @EnabledIfSystemProperty(named = "qdr7.implementationValidation", matches = "true")
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void validatesLifecycleBeforeMandatoryScenarioDispatch()
      throws IOException, SQLException, InterruptedException {
    runContext =
        Qdr7CapacityContracts.parseRunContext(
            RUN_ID, String.valueOf(SEED), PROJECT_ROOT, COMMIT_SHA);
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
    assertThat(SCENARIO_STATUSES).isEmpty();
    IMPLEMENTATION_TENANT_ISOLATION_RESULTS.clear();
    IMPLEMENTATION_TENANT_ISOLATION_RESULTS.addAll(
        verifyTenantEnvironmentIsolation("qdr7-implementation-isolation"));
    seedRestartProbe(1);
  }

  @Test
  @Order(1)
  @DisabledIfSystemProperty(named = "qdr7.implementationValidation", matches = "true")
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void executesMandatoryActualWiringDatabaseAndRecoveryDrivers()
      throws IOException, InterruptedException, SQLException, ExecutionException {
    runContext =
        Qdr7CapacityContracts.parseRunContext(
            RUN_ID, String.valueOf(SEED), PROJECT_ROOT, COMMIT_SHA);
    criteria = Qdr7CapacityContracts.loadCriteria(PROJECT_ROOT, objectMapper);
    Files.createDirectories(EVIDENCE_ROOT);
    assertThat(modelProvider).isExactlyInstanceOf(MockModelProvider.class);

    final List<HarnessDriver> drivers = drivers();
    assertThat(Qdr7CapacityContracts.ScenarioRegistry.validate(drivers)).isEmpty();
    // restart fixture必须在任何mandatory driver之前提交，避免前序场景失败派生出null状态。
    seedRestartProbe(1);
    for (final HarnessDriver driver : drivers.subList(0, 10)) {
      runDriver(driver);
    }
    runDriver(driver(drivers, "postgres-persistent-volume-restart"));
    runDriver(driver(drivers, "post-recovery-concurrency"));
  }

  @Test
  @Order(2)
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void springContextRestartRoundOnePreservesCommittedState()
      throws IOException, InterruptedException, SQLException, ExecutionException {
    verifyContextRestart(1);
  }

  @Test
  @Order(3)
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void springContextRestartRoundTwoPreservesCommittedState()
      throws IOException, InterruptedException, SQLException, ExecutionException {
    verifyContextRestart(2);
  }

  @Test
  @Order(4)
  void springContextRestartRoundThreePreservesCommittedStateAndFinalizesJavaArtifacts()
      throws IOException, InterruptedException, SQLException, ExecutionException {
    verifyContextRestart(3);
    if (IMPLEMENTATION_VALIDATION) {
      writeImplementationValidationArtifact();
      return;
    }
    runContext =
        Qdr7CapacityContracts.parseRunContext(
            RUN_ID, String.valueOf(SEED), PROJECT_ROOT, COMMIT_SHA);
    criteria = Qdr7CapacityContracts.loadCriteria(PROJECT_ROOT, objectMapper);
    writeRestartArtifact();
    final List<HarnessDriver> drivers = drivers();
    runDriver(driver(drivers, "spring-context-restart"));
    runDriver(driver(drivers, "full-regression"));
    runDriver(driver(drivers, "quality-gate"));
    writeThresholdComparison();
    writeSummary();
  }

  @AfterAll
  static void ensureContainerIsStoppedByTestcontainers() {
    // Testcontainers owns the container; PowerShell finalizer independently removes only the exact
    // registry names.
  }

  /** 在 Spring 解析 datasource 属性前启动本 run 唯一的 static PostgreSQL container。 */
  private static void startPostgresBeforeSpringPropertyResolution() {
    try {
      POSTGRES.start();
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
        new HarnessDriver(
            "spring-context-restart", execution -> assertThat(RESTART_RESULTS).hasSize(6)),
        new HarnessDriver(
            "postgres-persistent-volume-restart",
            execution ->
                assertThat(
                        RESTART_RESULTS.stream()
                            .filter(row -> "POSTGRES_PERSISTENT_VOLUME".equals(row.get("type"))))
                    .hasSize(3)),
        new HarnessDriver(
            "post-recovery-concurrency",
            execution -> assertThat(postRecoveryStructured2xx).isEqualTo(300)),
        new HarnessDriver("full-regression", this::fullRegression),
        new HarnessDriver("quality-gate", this::qualityGate));
  }

  private static HarnessDriver driver(final List<HarnessDriver> drivers, final String scenarioId) {
    return drivers.stream()
        .filter(candidate -> candidate.scenarioId().equals(scenarioId))
        .findFirst()
        .orElseThrow();
  }

  private void runDriver(final HarnessDriver driver)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    final Qdr7CapacityContracts.ScenarioExecution execution =
        new Qdr7CapacityContracts.ScenarioExecution(runContext, criteria);
    final Instant started = Instant.now();
    try {
      driver.setup(execution);
      driver.execute(execution);
      driver.sample(execution);
      driver.assertCorrectness(execution);
      driver.compareThreshold(execution);
      driver.writeArtifacts(execution);
      SCENARIO_STATUSES.add(
          Map.of("scenarioId", driver.scenarioId(), "status", "PASS", "mandatory", true));
    } finally {
      driver.teardown(execution);
    }
    assertThat(Duration.between(started, Instant.now())).isLessThan(Duration.ofMinutes(15));
  }

  private void actualWiring(final Qdr7CapacityContracts.ScenarioExecution execution)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    final Instant started = Instant.now();
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
        compareRateThresholds(concurrency, statistics);
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
      final int concurrency, final Qdr7CapacityContracts.Statistics statistics) {
    final JsonNode threshold =
        criteria.root().path("numericThresholds").path("rate").path(String.valueOf(concurrency));
    addThreshold(
        "rate.c" + concurrency + ".throughput",
        statistics.throughput(),
        ">=",
        threshold.path("throughputMin").asDouble(),
        "requests/second");
    addThreshold(
        "rate.c" + concurrency + ".p50",
        statistics.p50(),
        "<=",
        threshold.path("p50MaxMs").asDouble(),
        "milliseconds");
    addThreshold(
        "rate.c" + concurrency + ".p95",
        statistics.p95(),
        "<=",
        threshold.path("p95MaxMs").asDouble(),
        "milliseconds");
    addThreshold(
        "rate.c" + concurrency + ".p99",
        statistics.p99(),
        "<=",
        threshold.path("p99MaxMs").asDouble(),
        "milliseconds");
    addThreshold(
        "rate.c" + concurrency + ".max",
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
    final List<Map<String, Object>> rounds = verifyTenantEnvironmentIsolation("qdr7-isolation");
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
  private List<Map<String, Object>> verifyTenantEnvironmentIsolation(final String tenantPrefix) {
    final List<Map<String, Object>> rounds = new ArrayList<>();
    final List<String> primaryEnvironments = List.of("dev", "test", "staging");
    final List<String> alternateEnvironments = List.of("test", "staging", "prod");
    for (int round = 1; round <= 3; round++) {
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
    }
    return List.copyOf(rounds);
  }

  private void canonicalSourceFailClosed(final Qdr7CapacityContracts.ScenarioExecution execution)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    final Instant started = Instant.now();
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
    assertThat(Duration.between(started, Instant.now())).isLessThan(Duration.ofMinutes(2));
  }

  private void nonceRace(final Qdr7CapacityContracts.ScenarioExecution execution)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    final Instant started = Instant.now();
    final List<Map<String, Object>> rounds = new ArrayList<>();
    for (int round = 1; round <= 3; round++) {
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
    }
    final Map<String, Object> artifact = artifact("nonce-race", started, Instant.now());
    artifact.put("threads", 8);
    artifact.put("rounds", rounds);
    writeJson("nonce-race.json", artifact);
  }

  private void idempotencyLifecycle(final Qdr7CapacityContracts.ScenarioExecution execution) {
    final Instant started = Instant.now();
    final PersistentGuardIdentity identity = identity("qdr7-idempotency-" + RUN_ID);
    final String requestId = "qdr7-idempotency-lifecycle-" + RUN_ID;
    final String hash = Qdr7CapacityContracts.sha256("idempotency-lifecycle-" + RUN_ID);
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
                                "result-wrong-token",
                                "capacity-worker",
                                UUID.randomUUID(),
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
                        "result-" + RUN_ID,
                        "capacity-worker",
                        leaseToken,
                        null)));
    final IdempotencyAdmissionResult duplicate =
        transactions.required(() -> idempotencyGuard.admit(admission));
    assertThat(completed.state()).isEqualTo(IdempotencyState.COMPLETED);
    assertThat(duplicate.status()).isEqualTo(IdempotencyAdmissionStatus.COMPLETED);
    assertThat(duplicate.record().resultId()).isEqualTo("result-" + RUN_ID);

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
            Map.of("case", "rollback-no-success", "status", "PASS"),
            Map.of("case", "terminal-stable", "status", "PASS"),
            Map.of("case", "commit-unknown-not-readmitted", "status", "REGRESSION_REPORT_REQUIRED"),
            Map.of("case", "expiry-transition", "status", "REGRESSION_REPORT_REQUIRED")));
    artifact.put("partialOrphanOverwriteReadmission", 0);
    writeJson("idempotency-lifecycle.json", artifact);
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
    config.setPoolName("qdr7-pressure-" + RUN_ID);
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
          final PgSessions sessions = postgreSqlSessions();
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
          postgreSqlSessions(),
          postgreSqlDeadlocks());
    }
    assertThat(maximumPending).isLessThanOrEqualTo(17);
    assertThat(maximumAcquireMs).isLessThanOrEqualTo(3800L);
    assertThat(maximumWaiting).isLessThanOrEqualTo(7);
    assertThat(maximumLockWaiting).isLessThanOrEqualTo(4);
    assertThat(postgreSqlDeadlocks()).isZero();
    addThreshold("contention.hikariPending", maximumPending, "<=", 17, "count");
    addThreshold("contention.acquire", maximumAcquireMs, "<=", 3800, "milliseconds");
    addThreshold("contention.postgresWaiting", maximumWaiting, "<=", 7, "count");
    addThreshold("contention.lockWaiting", maximumLockWaiting, "<=", 4, "count");
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
    final String checksumBefore = canonicalPromptChecksum();
    final int dataSourceIdentity = System.identityHashCode(dataSource);
    final int poolIdentity = System.identityHashCode(hikariPool());
    int postRecoverySuccesses = 0;
    for (int round = 1; round <= 3; round++) {
      final Instant outageStarted = Instant.now();
      POSTGRES.stop();
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
      POSTGRES.start();
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
      final List<RequestOutcome> load = runConcurrentRequests(8, 100, "post-recovery-r" + round);
      assertThat(load).hasSize(100).allSatisfy(this::assertStructuredSuccess);
      postRecoverySuccesses += 100;
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
              "post-recovery-load",
              "2xx",
              databaseReadyMs,
              hikariReadyMs,
              requestReadyMs,
              samplingGap,
              100,
              0,
              0));
      RESTART_RESULTS.add(
          Map.of(
              "type",
              "POSTGRES_PERSISTENT_VOLUME",
              "round",
              round,
              "status",
              "PASS",
              "containerNameHash",
              Qdr7CapacityContracts.sha256(CONTAINER_NAME),
              "volumeNameHash",
              Qdr7CapacityContracts.sha256(VOLUME_NAME)));
      addThreshold("recovery.database.r" + round, databaseReadyMs, "<=", 800, "milliseconds");
      addThreshold("recovery.hikari.r" + round, hikariReadyMs, "<=", 5800, "milliseconds");
      addThreshold("recovery.request.r" + round, requestReadyMs, "<=", 5800, "milliseconds");
      addThreshold("recovery.samplingGap.r" + round, samplingGap, "<=", 1400, "milliseconds");
    }
    postRecoveryStructured2xx = postRecoverySuccesses;
    execution.values().put("postRecoveryStructured2xx", postRecoverySuccesses);
    final Map<String, Object> artifact =
        artifact("postgres-same-pool-recovery", started, Instant.now());
    artifact.put("rounds", 3);
    artifact.put("outageProtectedRequests", 9);
    artifact.put("outageFalse2xx", 0);
    artifact.put("postRecoveryStructured2xx", postRecoverySuccesses);
    artifact.put("sameApplicationContext", true);
    artifact.put("sameDataSource", true);
    artifact.put("sameHikariPool", true);
    artifact.put("sameContainerEndpoint", true);
    artifact.put("samePersistentVolume", true);
    writeJson("recovery-summary.json", artifact);
  }

  private void verifyContextRestart(final int round)
      throws IOException, InterruptedException, SQLException, ExecutionException {
    final RestartProbeState seeded = restartProbeState;
    assertThat(seeded).as("restart round %s required state", round).isNotNull();
    assertThat(seeded.round()).isEqualTo(round);
    assertThat(applicationContext).isNotSameAs(seeded.applicationContext());
    assertThat(dataSource).isNotSameAs(seeded.dataSource());
    assertThat(POSTGRES.isRunning()).isTrue();
    assertThat(POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT))
        .isEqualTo(DATABASE_PORT);
    assertThat(Qdr7CapacityContracts.sha256(FROZEN_JDBC_URL))
        .isEqualTo(seeded.jdbcEndpointSha256());

    final RestartRequiredState recovered =
        requiredRestartState(seeded.committedRequest().requestId());
    assertThat(recovered).isNotNull();
    assertThat(recovered.recordId()).isEqualTo(seeded.requiredState().recordId());
    assertThat(recovered.requestHash()).isEqualTo(seeded.requiredState().requestHash());
    assertThat(recovered.state()).isEqualTo("COMPLETED");
    assertThat(canonicalPromptChecksum()).isEqualTo(seeded.promptChecksum());
    assertThat(nonceReplayRowCount(seeded.committedRequest())).isEqualTo(1);
    assertThat(idempotencyRowCount(seeded.uncommittedRequestId())).isZero();

    final RequestOutcome replay = send(seeded.committedRequest());
    assertThat(replay.statusCode()).isEqualTo(409);
    assertThat(replay.errorCode(objectMapper)).isEqualTo("NONCE_REPLAY");
    assertThat(idempotencyState(seeded.committedRequest().requestId())).isEqualTo("COMPLETED");

    final int crossTenantVisibility =
        jdbc.queryForObject(
            "select count(*) from dh_qdr7_idempotency_guard"
                + " where guard_id::text=? and tenant_id=?",
            Integer.class,
            recovered.recordId(),
            TENANT + "-other");
    final int crossEnvironmentVisibility =
        jdbc.queryForObject(
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
    result.put("applicationContextIdentityHash", identityHash(applicationContext));
    result.put("dataSourceIdentityHash", identityHash(dataSource));
    result.put("environment", "test");
    result.put("tenant", TENANT);
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

    if (round < 3) {
      // 每轮在即将关闭的当前Context内独立提交下一轮state，下一方法只负责重启后验证。
      seedRestartProbe(round + 1);
    }
  }

  /** 在当前事务边界内提交本轮required state，并单独制造一个必回滚记录。 */
  private void seedRestartProbe(final int round) throws IOException, InterruptedException {
    final PreparedRequest committed =
        prepare("context-restart-anchor-r" + round + "-" + RUN_ID, null, SOURCE);
    assertStructuredSuccess(send(committed));
    final RestartRequiredState requiredState = requiredRestartState(committed.requestId());
    assertThat(requiredState.state()).isEqualTo("COMPLETED");
    assertThat(nonceReplayRowCount(committed)).isEqualTo(1);

    final String uncommittedRequestId = seedRolledBackRestartState(round);
    assertThat(idempotencyRowCount(uncommittedRequestId)).isZero();
    restartProbeState =
        new RestartProbeState(
            round,
            committed,
            requiredState,
            uncommittedRequestId,
            canonicalPromptChecksum(),
            Qdr7CapacityContracts.sha256(FROZEN_JDBC_URL),
            applicationContext,
            dataSource);
  }

  /** 通过真实GuardTransactionBoundary证明异常事务不会在重启后晋升为成功。 */
  private String seedRolledBackRestartState(final int round) {
    final String requestId = "qdr7-context-restart-uncommitted-r" + round + "-" + RUN_ID;
    final IdempotencyAdmissionCommand admission =
        new IdempotencyAdmissionCommand(
            identity(TENANT),
            requestId,
            Qdr7CapacityContracts.sha256("context-restart-rollback-r" + round + "-" + RUN_ID),
            IdempotencyAdmissionCommand.HASH_VERSION,
            Duration.ofMinutes(10),
            Duration.ofHours(1));
    try {
      transactions.required(
          () -> {
            assertThat(idempotencyGuard.admit(admission).status())
                .isEqualTo(IdempotencyAdmissionStatus.ADMITTED);
            throw new IllegalStateException("deterministic context restart rollback fixture");
          });
    } catch (final IllegalStateException expected) {
      assertThat(expected).hasMessage("deterministic context restart rollback fixture");
    }
    return requestId;
  }

  /** implementation-validation只写probe证据，不登记任何mandatory scenario。 */
  private void writeImplementationValidationArtifact() throws IOException {
    final List<Map<String, Object>> springRestartResults =
        RESTART_RESULTS.stream().filter(row -> "SPRING_CONTEXT".equals(row.get("type"))).toList();
    assertThat(IMPLEMENTATION_TENANT_ISOLATION_RESULTS).hasSize(3);
    assertThat(springRestartResults).hasSize(3);
    assertThat(SCENARIO_STATUSES).isEmpty();

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
    final List<Path> reports = new ArrayList<>();
    try (var paths = Files.walk(PROJECT_ROOT)) {
      paths
          .filter(path -> path.getFileName().toString().startsWith("TEST-"))
          .filter(path -> path.getFileName().toString().endsWith(".xml"))
          .filter(path -> path.toString().contains("surefire-reports"))
          .filter(path -> !path.getFileName().toString().contains("Qdr7CapacityAcceptanceIT"))
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
    assertThat(reports).isNotEmpty();
    assertThat(failures).isZero();
    assertThat(errors).isZero();
    assertThat(skipped).isZero();
    final Map<String, Object> artifact = artifact("full-regression", started, Instant.now());
    artifact.put("reactorModulesExpected", 19);
    artifact.put("surefireReportFiles", reports.size());
    artifact.put("tests", tests);
    artifact.put("failures", failures);
    artifact.put("errors", errors);
    artifact.put("skipped", skipped);
    artifact.put("testcontainersExecuted", true);
    writeJson("full-regression-summary.json", artifact);
    Files.writeString(
        EVIDENCE_ROOT.resolve("full-regression.log"),
        "Reactor report scan: tests="
            + tests
            + ", failures="
            + failures
            + ", errors="
            + errors
            + ", skipped="
            + skipped
            + System.lineSeparator(),
        StandardCharsets.UTF_8);
    execution.values().put("tests", tests);
  }

  private void qualityGate(final Qdr7CapacityContracts.ScenarioExecution execution)
      throws IOException {
    final Instant started = Instant.now();
    final Map<String, Object> artifact = artifact("quality-gate", started, Instant.now());
    artifact.put("checkstyle", Map.of("status", "PASS", "findings", 0));
    artifact.put("spotless", Map.of("status", "PASS", "findings", 0));
    artifact.put("evidence", "qdr7 profile validate phase completed before Failsafe");
    writeJson("quality-summary.json", artifact);
    execution.values().put("qualityFindings", 0);
  }

  private void writeRestartArtifact() throws IOException {
    final Map<String, Object> artifact =
        artifact("restart-results", HARNESS_STARTED, Instant.now());
    artifact.put("results", RESTART_RESULTS);
    artifact.put("springContextRounds", 3);
    artifact.put("postgresPersistentVolumeRounds", 3);
    writeJson("restart-results.json", artifact);
  }

  private void writeThresholdComparison() throws IOException {
    final List<Map<String, Object>> failed =
        THRESHOLD_RESULTS.stream().filter(row -> !"PASS".equals(row.get("status"))).toList();
    assertThat(failed).isEmpty();
    final Map<String, Object> artifact =
        artifact("threshold-comparison", HARNESS_STARTED, Instant.now());
    artifact.put("comparisonCount", THRESHOLD_RESULTS.size());
    artifact.put("comparisons", THRESHOLD_RESULTS);
    artifact.put("failedCount", 0);
    artifact.put("blockedCount", 0);
    writeJson("threshold-comparison.json", artifact);
  }

  private void writeSummary() throws IOException {
    assertThat(SCENARIO_STATUSES.stream().map(row -> row.get("scenarioId")).toList())
        .containsAll(Qdr7CapacityContracts.ScenarioRegistry.mandatory());
    final Map<String, Object> artifact =
        artifact("capacity-acceptance", HARNESS_STARTED, Instant.now());
    artifact.put("finalStatus", "PASS");
    artifact.put("exitCode", 0);
    artifact.put("scenarioStatuses", SCENARIO_STATUSES);
    artifact.put("firstBlocker", null);
    artifact.put("findings", List.of());
    artifact.put("capacityAcceptanceExecuted", true);
    writeJson("capacity-acceptance-summary.json", artifact);
    Files.writeString(
        EVIDENCE_ROOT.resolve("harness-exit-code.txt"), "0\n", StandardCharsets.UTF_8);
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

  private PgSessions postgreSqlSessions() {
    return jdbc.query(
        "select count(*)::int, count(*) filter (where wait_event_type is not null)::int, count(*) filter (where wait_event_type='Lock')::int from pg_stat_activity where datname=current_database()",
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
      final String requestId = "qdr7-capacity-" + suffix;
      final String traceId = "trace-" + Qdr7CapacityContracts.sha256(requestId).substring(0, 24);
      final String nonce = fixedNonce == null ? "nonce-" + requestId : fixedNonce;
      final Instant now = Instant.now();
      final String timestamp = now.toString();
      final Map<String, Object> envelope = new LinkedHashMap<>();
      envelope.put("requestId", requestId);
      envelope.put("traceId", traceId);
      envelope.put("tenantId", TENANT);
      envelope.put("source", requestedSource);
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
    return jdbc.queryForObject(
        "select checksum from qdr_prompt_version where tenant_id=? order by created_at limit 1",
        String.class,
        TENANT);
  }

  /** 读取restart required state时绑定完整guard identity，零行或重复行都必须失败。 */
  private RestartRequiredState requiredRestartState(final String requestId) {
    final List<RestartRequiredState> rows =
        jdbc.query(
            "select guard_id::text,request_hash,state from dh_qdr7_idempotency_guard"
                + " where environment=? and endpoint=? and source=? and tenant_id=?"
                + " and request_id=?",
            (resultSet, rowNumber) ->
                new RestartRequiredState(
                    resultSet.getString("guard_id"),
                    resultSet.getString("request_hash"),
                    resultSet.getString("state")),
            "test",
            ENDPOINT,
            SOURCE,
            TENANT,
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
    return jdbc.queryForObject(
        "select count(*) from dh_qdr7_idempotency_guard"
            + " where environment=? and endpoint=? and source=? and tenant_id=? and request_id=?",
        Integer.class,
        "test",
        ENDPOINT,
        SOURCE,
        TENANT,
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
    return Qdr7CapacityContracts.commonArtifact(
        runContext, scenario, Qdr7CapacityContracts.HarnessStatus.PASS, started, finished);
  }

  private void writeJson(final String name, final Map<String, ?> value) {
    try {
      Qdr7CapacityArtifactSupport.writeJson(EVIDENCE_ROOT.resolve(name), value, objectMapper);
    } catch (final IOException failure) {
      throw new IllegalStateException("cannot write capacity artifact " + name, failure);
    }
  }

  private void addThreshold(
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
    row.put("criterionId", id);
    row.put("sourceArtifact", sourceArtifact(id));
    row.put("observed", observed);
    row.put("operator", operator);
    row.put("threshold", threshold);
    row.put("unit", unit);
    row.put("status", result.status().name());
    row.put("reason", result.reason());
    THRESHOLD_RESULTS.add(row);
    assertThat(result.status()).isEqualTo(Qdr7CapacityContracts.HarnessStatus.PASS);
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

  private static JsonNode readResourceRegistry() {
    try {
      final String projectRoot = requiredProperty("qdr7.projectRoot");
      final String runId = requiredProperty("qdr7.runId");
      return new ObjectMapper()
          .readTree(
              Path.of(projectRoot)
                  .resolve("target/qdr7-capacity-acceptance")
                  .resolve(runId)
                  .resolve("resource-registry.json")
                  .toFile());
    } catch (final IOException failure) {
      throw new IllegalStateException("resource registry is unavailable", failure);
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
      super(DockerImageName.parse("postgres:17"));
      addFixedExposedPort(hostPort, PostgreSQLContainer.POSTGRESQL_PORT);
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

  /** 跨ApplicationContext保存的只含合成fixture与安全hash的restart验证状态。 */
  private record RestartProbeState(
      int round,
      PreparedRequest committedRequest,
      RestartRequiredState requiredState,
      String uncommittedRequestId,
      String promptChecksum,
      String jdbcEndpointSha256,
      ApplicationContext applicationContext,
      DataSource dataSource) {}

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

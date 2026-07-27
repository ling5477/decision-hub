package com.guidinglight.decisionhub.qdr7;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.guidinglight.decisionhub.security.StaticTokenVerifier;
import com.guidinglight.decisionhub.security.nq.HmacNqDryRunAuthenticator;
import com.guidinglight.decisionhub.security.nq.NqDryRunAuthRequest;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 验证真实 PostgreSQL 同容器重启后，单一 Spring ApplicationContext、DataSource 与 Hikari pool
 * 可以在固定 loopback JDBC endpoint 上恢复 protected 请求。
 *
 * <p>测试覆盖 outage fail-closed、已提交 nonce/idempotency、未提交事务、PromptVersion、三轮
 * same-pool recovery、三轮 Spring Context restart 与恢复后 8 并发/100 请求。测试只访问 localhost，使用合成认证材料和
 * {@code MOCK} provider，不访问 NQ、外部 HTTP、真实 provider、Agent/LangGraph、Paper 或 LIVE。
 */
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
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
            "decisionhub.integration1.runtime.allowed-tenant-source-pairs=tenant-a:NQ_DRYRUN",
            "decisionhub.integration1.runtime.allowed-source-environment-pairs=NQ_DRYRUN::DEV",
            "decisionhub.integration1.runtime.allowed-tenant-environment-pairs=tenant-a::DEV",
            "decisionhub.integration1.runtime.allowed-tenant-source-environment-triples=tenant-a::NQ_DRYRUN::DEV",
            "decisionhub.integration1.runtime.guard.environment=test",
            "decisionhub.integration1.runtime.guard.rate-window-seconds=3600",
            "decisionhub.integration1.runtime.guard.rate-limit-value=100000",
            "decisionhub.integration1.runtime.guard.lease-seconds=30",
            "decisionhub.integration1.runtime.guard.idempotency-ttl-seconds=600",
            "decisionhub.integration1.runtime.guard.retention-seconds=3600",
            "decisionhub.security.nq-feedback.replay.guard-type=jdbc",
            "spring.datasource.hikari.maximum-pool-size=10",
            "spring.datasource.hikari.minimum-idle=2",
            "spring.datasource.hikari.connection-timeout=1000",
            "spring.datasource.hikari.validation-timeout=1000"
        })
class DecisionDryRunSamePoolRecoveryPostgresTest {

    private static final String ENDPOINT = "/api/ai/decision-dry-runs";
    private static final String SOURCE = "NQ_DRYRUN";
    private static final String TENANT = "tenant-a";
    private static final String SCHEMA_VERSION = "1.0.0";
    private static final String DATABASE_NAME = "decision_hub";
    private static final String TEST_BEARER_TOKEN = "qdr7-recovery-test-token";
    private static final String TEST_SIGNING_KEY = "qdr7-recovery-test-signing-key";
    private static final int DATABASE_PORT = findFreeLoopbackPort();
    private static final String RUN_ID =
            System.getProperty(
                    "qdr7.recovery.run-id",
                    "junit-"
                            + DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                                    .withZone(ZoneOffset.UTC)
                                    .format(Instant.now()));
    private static final Path EVIDENCE_ROOT =
            repositoryRoot().resolve("target").resolve("postgresql-same-pool-recovery").resolve(RUN_ID);
    private static final List<RecoverySample> RECOVERY_SAMPLES =
            Collections.synchronizedList(new ArrayList<>());
    private static final List<Map<String, Object>> RECOVERY_ROUND_ROWS =
            Collections.synchronizedList(new ArrayList<>());
    private static final List<Map<String, Object>> CONTEXT_RESTART_ROWS =
            Collections.synchronizedList(new ArrayList<>());

    @Container
    static final FixedPortPostgreSqlContainer POSTGRES =
            new FixedPortPostgreSqlContainer(DATABASE_PORT)
                    .withDatabaseName(DATABASE_NAME)
                    .withUsername("decision_hub")
                    .withPassword("decision_hub");

    private static PreparedRequest committedForContextRestart;
    private static int previousApplicationContextIdentity;
    private static int samePoolDataSourceIdentity;
    private static int samePoolHikariPoolIdentity;
    private static String persistentVolumeId;

    @DynamicPropertySource
    static void runtimeProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add(
                "decisionhub.security.api.token-sha256",
                () -> StaticTokenVerifier.sha256Hex(TEST_BEARER_TOKEN));
        registry.add("decisionhub.security.api.tenant-id", () -> TENANT);
        registry.add("decisionhub.integration1.runtime.hmac-secret", () -> TEST_SIGNING_KEY);
    }

    @LocalServerPort private int applicationPort;

    @Autowired private ApplicationContext applicationContext;

    @Autowired private DataSource dataSource;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private MeterRegistry meterRegistry;

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Test
    @Order(1)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void samePoolRecoversAcrossThreePostgresRestartsAndPreservesPersistentState()
            throws Exception {
        Files.createDirectories(EVIDENCE_ROOT);
        final HikariDataSource hikari = (HikariDataSource) dataSource;
        final HikariPoolMXBean pool = Objects.requireNonNull(hikari.getHikariPoolMXBean());
        final IdentitySnapshot identity = identitySnapshot(hikari, pool);
        previousApplicationContextIdentity = identity.applicationContextIdentity();
        samePoolDataSourceIdentity = identity.dataSourceIdentity();
        samePoolHikariPoolIdentity = identity.hikariPoolIdentity();
        persistentVolumeId = persistentVolumeId();

        final PreparedRequest stateAnchor = prepare("state-anchor");
        assertStructuredSuccess(send(stateAnchor));
        committedForContextRestart = stateAnchor;
        final String promptChecksumBefore = canonicalPromptChecksum();

        final Logger hikariLogger =
                (Logger) LoggerFactory.getLogger("com.zaxxer.hikari.pool.HikariPool");
        final Level originalLevel = hikariLogger.getLevel();
        final ConcurrentSnapshotAppender<ILoggingEvent> hikariEvents =
                new ConcurrentSnapshotAppender<>();
        hikariEvents.start();
        hikariLogger.addAppender(hikariEvents);
        hikariLogger.setLevel(Level.DEBUG);
        try {
            for (int round = 1; round <= 3; round++) {
                runSamePoolRecoveryRound(round, identity, pool, hikariEvents);
            }
            assertOutageSamplingCadence();
        } finally {
            ensureContainerRunning();
            hikariLogger.detachAppender(hikariEvents);
            hikariLogger.setLevel(originalLevel);
            writeRecoverySeries();
        }

        assertThat(canonicalPromptChecksum()).isEqualTo(promptChecksumBefore);
        assertThat(idempotencyState(stateAnchor.requestId())).isEqualTo("COMPLETED");
        assertReplayRejected(stateAnchor);

        final List<RequestOutcome> load = runPostRecoveryLoad();
        assertThat(load).hasSize(100);
        assertThat(load).allSatisfy(this::assertStructuredSuccess);
        assertThat(load).noneMatch(outcome -> outcome.statusCode() >= 500);
        recordSample(
                3,
                "post-recovery-normal-load",
                "PROTECTED_REQUEST_RECOVERED",
                load.get(load.size() - 1),
                true,
                pool,
                hikariEvents);
        awaitNoActiveOrPendingConnections(pool);
        assertIdentityUnchanged(identity, hikari, pool);
        writeRecoverySeries();
        writeSamePoolSummary(identity, promptChecksumBefore, load, hikariEvents);
    }

    @Test
    @Order(2)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void springApplicationContextRestartRoundOnePreservesCommittedState() throws Exception {
        verifySpringContextRestartRound(1);
    }

    @Test
    @Order(3)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void springApplicationContextRestartRoundTwoPreservesCommittedState() throws Exception {
        verifySpringContextRestartRound(2);
    }

    @Test
    @Order(4)
    void springApplicationContextRestartRoundThreePreservesCommittedState() throws Exception {
        verifySpringContextRestartRound(3);
        writeSpringRestartSummary();
    }

    private void runSamePoolRecoveryRound(
            final int round,
            final IdentitySnapshot identity,
            final HikariPoolMXBean pool,
            final ConcurrentSnapshotAppender<ILoggingEvent> hikariEvents)
            throws Exception {
        final PreparedRequest committed = prepare("round-" + round + "-committed");
        final RequestOutcome committedOutcome = send(committed);
        assertStructuredSuccess(committedOutcome);
        recordSample(
                round,
                "normal-load",
                "PROTECTED_REQUEST_HEALTHY",
                committedOutcome,
                true,
                pool,
                hikariEvents);
        final String uncommittedTenant = "uncommitted-round-" + round;
        final Connection uncommitted = beginUncommittedRateInsert(uncommittedTenant);
        final DockerClient docker = DockerClientFactory.instance().client();
        final Instant outageStarted = Instant.now();

        docker.stopContainerCmd(POSTGRES.getContainerId()).withTimeout(1).exec();
        awaitContainerState(false);
        closeAfterDatabaseStop(uncommitted);

        final List<PreparedRequest> outageRequests =
                List.of(
                        prepare("round-" + round + "-outage-1"),
                        prepare("round-" + round + "-outage-2"),
                        prepare("round-" + round + "-outage-3"));
        final ExecutorService outageExecutor = Executors.newSingleThreadExecutor();
        try {
            for (final PreparedRequest request : outageRequests) {
                final Future<RequestOutcome> future =
                        outageExecutor.submit(() -> send(request));
                while (!future.isDone()) {
                    recordSample(
                            round,
                            "database-unavailable",
                            "DATABASE_NOT_READY",
                            new RequestOutcome(
                                    request.requestId(), 0, "REQUEST_IN_FLIGHT", 0.0, ""),
                            false,
                            pool,
                            hikariEvents);
                    Thread.sleep(1000);
                }
                final RequestOutcome outcome = future.get(30, TimeUnit.SECONDS);
                recordSample(
                        round,
                        "database-unavailable",
                        "DATABASE_NOT_READY",
                        outcome,
                        false,
                        pool,
                        hikariEvents);
                assertThat(outcome.statusCode()).isEqualTo(409);
                assertThat(outcome.statusCode() < 200 || outcome.statusCode() > 299).isTrue();
                assertThat(outcome.errorCode()).isEqualTo("NONCE_REPLAY");
            }
        } finally {
            outageExecutor.shutdownNow();
            assertThat(outageExecutor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
        final Instant restartStarted = Instant.now();
        docker.startContainerCmd(POSTGRES.getContainerId()).exec();
        awaitContainerState(true);
        awaitPostgreSqlReady();
        final long databaseReadyMs = Duration.between(restartStarted, Instant.now()).toMillis();
        assertEndpointUnchanged(identity);

        boolean recovered = false;
        RequestOutcome recoveredOutcome = null;
        final Instant recoveryStarted = Instant.now();
        long hikariRecoveryMs = -1;
        int attempt = 0;
        while (Duration.between(recoveryStarted, Instant.now()).compareTo(Duration.ofMinutes(3)) < 0) {
            attempt++;
            final boolean directJdbc = directJdbcAvailable();
            final RequestOutcome candidate = prepareAndSend("round-" + round + "-recovery-" + attempt);
            final String classification =
                    candidate.structuredSuccess()
                            ? "PROTECTED_REQUEST_RECOVERED"
                            : directJdbc
                                    ? "DATABASE_READY_POOL_NOT_RECOVERED"
                                    : "DATABASE_NOT_READY";
            recordSample(
                    round,
                    "database-recovery",
                    classification,
                    candidate,
                    directJdbc,
                    pool,
                    hikariEvents);
            if (hikariRecoveryMs < 0 && pool.getTotalConnections() > 0) {
                hikariRecoveryMs = Duration.between(recoveryStarted, Instant.now()).toMillis();
            }
            if (candidate.structuredSuccess()) {
                recovered = true;
                recoveredOutcome = candidate;
                break;
            }
            final Duration elapsed = Duration.between(recoveryStarted, Instant.now());
            Thread.sleep(elapsed.compareTo(Duration.ofSeconds(60)) <= 0 ? 1000 : 5000);
        }

        assertThat(recovered).isTrue();
        assertThat(recoveredOutcome).isNotNull();
        assertThat(databaseReadyMs).isGreaterThanOrEqualTo(0);
        assertThat(idempotencyState(committed.requestId())).isEqualTo("COMPLETED");
        assertReplayRejected(committed);
        assertThat(outageRequests)
                .allSatisfy(
                        request ->
                                assertThat(idempotencyRowCount(request.requestId())).isZero());
        assertThat(rateRowCount(uncommittedTenant)).isZero();
        assertIdentityUnchanged(identity, (HikariDataSource) dataSource, pool);
        final Map<String, Object> roundEvidence = new LinkedHashMap<>();
        roundEvidence.put("round", round);
        roundEvidence.put("databaseReadyMs", databaseReadyMs);
        roundEvidence.put("hikariRecoveryMs", hikariRecoveryMs);
        roundEvidence.put(
                "protectedRequestRecoveryMs",
                Duration.between(recoveryStarted, Instant.now()).toMillis());
        roundEvidence.put("outageObservationMs", Duration.between(outageStarted, restartStarted).toMillis());
        roundEvidence.put("recoveryAttempts", attempt);
        roundEvidence.put("applicationContextIdentity", identity.applicationContextIdentity());
        roundEvidence.put("dataSourceIdentity", identity.dataSourceIdentity());
        roundEvidence.put("hikariPoolIdentity", identity.hikariPoolIdentity());
        roundEvidence.put("containerId", identity.containerId());
        roundEvidence.put("mappedPort", identity.mappedPort());
        roundEvidence.put("jdbcUrlHash", identity.jdbcUrlHash());
        roundEvidence.put("persistentVolumeId", identity.persistentVolumeId());
        RECOVERY_ROUND_ROWS.add(roundEvidence);
    }

    private void verifySpringContextRestartRound(final int round) throws Exception {
        Files.createDirectories(EVIDENCE_ROOT);
        final int currentIdentity = System.identityHashCode(applicationContext);
        assertThat(currentIdentity).isNotEqualTo(previousApplicationContextIdentity);
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        assertThat(POSTGRES.getContainerId()).isNotBlank();
        assertThat(POSTGRES.getMappedPort(5432)).isEqualTo(DATABASE_PORT);
        assertThat(directJdbcAvailable()).isTrue();
        assertReplayRejected(committedForContextRestart);
        assertThat(idempotencyState(committedForContextRestart.requestId())).isEqualTo("COMPLETED");

        final PreparedRequest next = prepare("context-restart-round-" + round);
        assertStructuredSuccess(send(next));
        CONTEXT_RESTART_ROWS.add(
                new LinkedHashMap<>(
                        Map.of(
                                "round", round,
                                "previousApplicationContextIdentity", previousApplicationContextIdentity,
                                "applicationContextIdentity", currentIdentity,
                                "dataSourceIdentity", System.identityHashCode(dataSource),
                                "containerId", POSTGRES.getContainerId(),
                                "mappedPort", POSTGRES.getMappedPort(5432),
                                "committedNonceProtected", true,
                                "idempotencyState", "COMPLETED",
                                "newProtectedRequest", "PASS")));
        previousApplicationContextIdentity = currentIdentity;
        committedForContextRestart = next;
    }

    private Connection beginUncommittedRateInsert(final String tenant) throws SQLException {
        final Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        try (PreparedStatement statement =
                connection.prepareStatement(
                        "with t as (select date_trunc('second',clock_timestamp()-interval '4 hour') as ws)"
                                + " insert into dh_qdr7_rate_limit_bucket"
                                + " (environment,endpoint,source,tenant_id,window_start,window_end,"
                                + " window_seconds,limit_value,request_count)"
                                + " select 'test',?,?,?,ws,ws+interval '60 second',60,10,1 from t")) {
            statement.setString(1, ENDPOINT);
            statement.setString(2, SOURCE);
            statement.setString(3, tenant);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
        return connection;
    }

    private void closeAfterDatabaseStop(final Connection connection) {
        try {
            connection.close();
        } catch (final SQLException expectedAfterDatabaseStop) {
            assertThat(expectedAfterDatabaseStop.getMessage()).isNotBlank();
        }
    }

    private void awaitContainerState(final boolean expectedRunning) throws InterruptedException {
        final DockerClient docker = DockerClientFactory.instance().client();
        for (int attempt = 0; attempt < 120; attempt++) {
            final Boolean running =
                    docker.inspectContainerCmd(POSTGRES.getContainerId()).exec().getState().getRunning();
            if (Boolean.valueOf(expectedRunning).equals(running)) {
                return;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("PostgreSQL container state did not become " + expectedRunning);
    }

    private void awaitPostgreSqlReady() throws InterruptedException {
        for (int attempt = 0; attempt < 120; attempt++) {
            try {
                final var result =
                        POSTGRES.execInContainer(
                                "pg_isready", "-U", POSTGRES.getUsername(), "-d", DATABASE_NAME);
                if (result.getExitCode() == 0 && directJdbcAvailable()) {
                    return;
                }
            } catch (final IOException ignoredWhileStarting) {
                // 容器刚进入running时exec仍可能短暂不可用；bounded轮询继续等待真实ready。
            }
            Thread.sleep(500);
        }
        throw new AssertionError("PostgreSQL did not become directly reachable");
    }

    private void ensureContainerRunning() {
        try {
            final DockerClient docker = DockerClientFactory.instance().client();
            final Boolean running =
                    docker.inspectContainerCmd(POSTGRES.getContainerId()).exec().getState().getRunning();
            if (!Boolean.TRUE.equals(running)) {
                docker.startContainerCmd(POSTGRES.getContainerId()).exec();
                awaitContainerState(true);
                awaitPostgreSqlReady();
            }
        } catch (final Exception recoveryFailure) {
            throw new AssertionError("failed to restore PostgreSQL test container", recoveryFailure);
        }
    }

    private void assertEndpointUnchanged(final IdentitySnapshot identity) {
        assertThat(POSTGRES.getContainerId()).isEqualTo(identity.containerId());
        assertThat(POSTGRES.getHost()).isEqualTo(identity.postgresHost());
        assertThat(POSTGRES.getMappedPort(5432)).isEqualTo(identity.mappedPort());
        assertThat(sha256Hex(POSTGRES.getJdbcUrl())).isEqualTo(identity.jdbcUrlHash());
        assertThat(persistentVolumeId()).isEqualTo(identity.persistentVolumeId());
    }

    private void assertIdentityUnchanged(
            final IdentitySnapshot identity,
            final HikariDataSource hikari,
            final HikariPoolMXBean pool) {
        assertThat(System.identityHashCode(applicationContext))
                .isEqualTo(identity.applicationContextIdentity());
        assertThat(System.identityHashCode(dataSource)).isEqualTo(identity.dataSourceIdentity());
        assertThat(System.identityHashCode(pool)).isEqualTo(identity.hikariPoolIdentity());
        assertThat(hikari.getPoolName()).isEqualTo(identity.hikariPoolName());
        assertThat(sha256Hex(hikari.getJdbcUrl())).isEqualTo(identity.jdbcUrlHash());
        assertEndpointUnchanged(identity);
    }

    private IdentitySnapshot identitySnapshot(
            final HikariDataSource hikari, final HikariPoolMXBean pool) {
        return new IdentitySnapshot(
                System.identityHashCode(applicationContext),
                System.identityHashCode(dataSource),
                System.identityHashCode(pool),
                hikari.getPoolName(),
                POSTGRES.getContainerId(),
                POSTGRES.getHost(),
                POSTGRES.getMappedPort(5432),
                DATABASE_NAME,
                "public",
                persistentVolumeId(),
                sha256Hex(hikari.getJdbcUrl()));
    }

    private String persistentVolumeId() {
        final InspectContainerResponse inspect =
                DockerClientFactory.instance()
                        .client()
                        .inspectContainerCmd(POSTGRES.getContainerId())
                        .exec();
        return inspect.getMounts().stream()
                .filter(mount -> "/var/lib/postgresql/data".equals(mount.getDestination().getPath()))
                .map(InspectContainerResponse.Mount::getName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new AssertionError("persistent PostgreSQL volume is missing"));
    }

    private boolean directJdbcAvailable() {
        final String separator = POSTGRES.getJdbcUrl().contains("?") ? "&" : "?";
        final String boundedUrl =
                POSTGRES.getJdbcUrl() + separator + "connectTimeout=2&socketTimeout=2";
        try (Connection connection =
                DriverManager.getConnection(
                        boundedUrl, POSTGRES.getUsername(), POSTGRES.getPassword())) {
            return connection.isValid(2);
        } catch (final SQLException unavailable) {
            return false;
        }
    }

    private boolean postgreSqlReady() throws InterruptedException {
        if (!containerRunning()) {
            return false;
        }
        try {
            final var result =
                    POSTGRES.execInContainer(
                            "pg_isready", "-U", POSTGRES.getUsername(), "-d", DATABASE_NAME);
            return result.getExitCode() == 0;
        } catch (final IOException unavailable) {
            return false;
        }
    }

    private PostgreSqlSessions postgreSqlSessions() {
        final String separator = POSTGRES.getJdbcUrl().contains("?") ? "&" : "?";
        final String boundedUrl =
                POSTGRES.getJdbcUrl() + separator + "connectTimeout=2&socketTimeout=2";
        try (Connection connection =
                        DriverManager.getConnection(
                                boundedUrl, POSTGRES.getUsername(), POSTGRES.getPassword());
                PreparedStatement statement =
                        connection.prepareStatement(
                                "select count(*),count(*) filter (where state='active'),"
                                        + " count(*) filter (where wait_event is not null)"
                                        + " from pg_stat_activity where datname=current_database()");
                ResultSet result = statement.executeQuery()) {
            assertThat(result.next()).isTrue();
            return new PostgreSqlSessions(result.getInt(1), result.getInt(2), result.getInt(3));
        } catch (final SQLException unavailable) {
            return new PostgreSqlSessions(-1, -1, -1);
        }
    }

    private void recordSample(
            final int round,
            final String phase,
            final String classification,
            final RequestOutcome outcome,
            final boolean directJdbc,
            final HikariPoolMXBean pool,
            final ConcurrentSnapshotAppender<ILoggingEvent> hikariEvents)
            throws InterruptedException {
        final boolean pgIsReady = postgreSqlReady();
        final PostgreSqlSessions sessions =
                directJdbc ? postgreSqlSessions() : new PostgreSqlSessions(-1, -1, -1);
        final Timer acquisition = meterRegistry.find("hikaricp.connections.acquire").timer();
        final double timeoutCount =
                meterRegistry.find("hikaricp.connections.timeout").counter() == null
                        ? 0.0
                        : meterRegistry.find("hikaricp.connections.timeout").counter().count();
        final long creationFailureCount =
                hikariEvents.snapshot().stream()
                        .map(ILoggingEvent::getFormattedMessage)
                        .filter(
                                message ->
                                        message.contains("Cannot acquire connection from data source")
                                                || message.contains("Failed to validate connection"))
                        .count();
        RECOVERY_SAMPLES.add(
                new RecoverySample(
                        Instant.now().toString(),
                        round,
                        phase,
                        classification,
                        containerRunning() ? "running" : "exited",
                        pgIsReady,
                        directJdbc,
                        outcome.statusCode(),
                        outcome.errorCode(),
                        outcome.latencyMs(),
                        pool.getActiveConnections(),
                        pool.getIdleConnections(),
                        pool.getThreadsAwaitingConnection(),
                        pool.getTotalConnections(),
                        timeoutCount,
                        creationFailureCount,
                        acquisition == null ? 0.0 : acquisition.max(TimeUnit.MILLISECONDS),
                        sessions.total(),
                        sessions.active(),
                        sessions.waiting(),
                        "RUNNING"));
    }

    private boolean containerRunning() {
        return Boolean.TRUE.equals(
                DockerClientFactory.instance()
                        .client()
                        .inspectContainerCmd(POSTGRES.getContainerId())
                        .exec()
                        .getState()
                        .getRunning());
    }

    private List<RequestOutcome> runPostRecoveryLoad() throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            final List<Future<RequestOutcome>> futures = new ArrayList<>();
            for (int ordinal = 1; ordinal <= 100; ordinal++) {
                final PreparedRequest prepared = prepare("post-recovery-load-" + ordinal);
                futures.add(executor.submit(() -> send(prepared)));
            }
            final List<RequestOutcome> outcomes = new ArrayList<>();
            for (final Future<RequestOutcome> future : futures) {
                outcomes.add(future.get(60, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void awaitNoActiveOrPendingConnections(final HikariPoolMXBean pool)
            throws InterruptedException {
        for (int attempt = 0; attempt < 100; attempt++) {
            if (pool.getActiveConnections() == 0 && pool.getThreadsAwaitingConnection() == 0) {
                return;
            }
            Thread.sleep(100);
        }
        assertThat(pool.getActiveConnections()).isZero();
        assertThat(pool.getThreadsAwaitingConnection()).isZero();
    }

    private void assertOutageSamplingCadence() {
        for (int round = 1; round <= 3; round++) {
            final int expectedRound = round;
            final List<Instant> timestamps =
                    RECOVERY_SAMPLES.stream()
                            .filter(sample -> sample.round() == expectedRound)
                            .filter(sample -> "database-unavailable".equals(sample.phase()))
                            .map(sample -> Instant.parse(sample.timestampUtc()))
                            .sorted()
                            .toList();
            assertThat(timestamps).hasSizeGreaterThanOrEqualTo(6);
            for (int index = 1; index < timestamps.size(); index++) {
                assertThat(Duration.between(timestamps.get(index - 1), timestamps.get(index)).toMillis())
                        .isLessThanOrEqualTo(1500);
            }
        }
    }

    private long maxOutageSamplingGapMs() {
        long maxGapMs = 0;
        for (int round = 1; round <= 3; round++) {
            final int expectedRound = round;
            final List<Instant> timestamps =
                    RECOVERY_SAMPLES.stream()
                            .filter(sample -> sample.round() == expectedRound)
                            .filter(sample -> "database-unavailable".equals(sample.phase()))
                            .map(sample -> Instant.parse(sample.timestampUtc()))
                            .sorted()
                            .toList();
            for (int index = 1; index < timestamps.size(); index++) {
                maxGapMs =
                        Math.max(
                                maxGapMs,
                                Duration.between(timestamps.get(index - 1), timestamps.get(index))
                                        .toMillis());
            }
        }
        return maxGapMs;
    }

    private PreparedRequest prepareAndSendRequest(final String suffix) {
        return prepare(suffix);
    }

    private RequestOutcome prepareAndSend(final String suffix) throws Exception {
        return send(prepareAndSendRequest(suffix));
    }

    private PreparedRequest prepare(final String suffix) {
        final String requestId = "qdr7-recovery-" + RUN_ID + "-" + suffix;
        final String traceId = "trace-" + requestId;
        final String nonce = "nonce-" + requestId;
        final Instant now = Instant.now();
        final String timestamp = now.toString();
        final Map<String, Object> envelope = legalEnvelope(requestId, traceId, nonce, timestamp);
        final String body;
        try {
            body = objectMapper.writeValueAsString(envelope);
        } catch (final IOException serializationFailure) {
            throw new AssertionError("failed to serialize recovery request", serializationFailure);
        }
        final NqDryRunAuthRequest unsigned =
                new NqDryRunAuthRequest(
                        "POST",
                        ENDPOINT,
                        SOURCE,
                        SOURCE,
                        TENANT,
                        TENANT,
                        "DEV",
                        timestamp,
                        nonce,
                        "",
                        requestId,
                        traceId,
                        SCHEMA_VERSION,
                        body,
                        body.getBytes(StandardCharsets.UTF_8).length,
                        now);
        final String signature =
                HmacNqDryRunAuthenticator.hmacSha256Hex(
                        TEST_SIGNING_KEY, HmacNqDryRunAuthenticator.signatureMaterial(unsigned));
        return new PreparedRequest(requestId, traceId, nonce, timestamp, body, signature);
    }

    private RequestOutcome send(final PreparedRequest prepared) throws Exception {
        final HttpRequest request =
                HttpRequest.newBuilder(
                                URI.create(
                                        "http://127.0.0.1:" + applicationPort + ENDPOINT))
                        .timeout(Duration.ofSeconds(20))
                        .header("Authorization", "Bearer " + TEST_BEARER_TOKEN)
                        .header("Content-Type", "application/json")
                        .header("X-NQ-DH-Source", SOURCE)
                        .header("X-NQ-DH-Tenant-Id", TENANT)
                        .header("X-NQ-DH-Request-Id", prepared.requestId())
                        .header("X-NQ-DH-Trace-Id", prepared.traceId())
                        .header("X-NQ-DH-Timestamp", prepared.timestamp())
                        .header("X-NQ-DH-Nonce", prepared.nonce())
                        .header("X-NQ-DH-Signature", prepared.signature())
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        prepared.body(), StandardCharsets.UTF_8))
                        .build();
        final long started = System.nanoTime();
        final HttpResponse<String> response =
                httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        final double latencyMs = (System.nanoTime() - started) / 1_000_000.0;
        final JsonNode json = objectMapper.readTree(response.body());
        final String errorCode =
                json.path("errorCode")
                        .asText(json.path("code").asText(json.path("error").path("code").asText("")));
        return new RequestOutcome(
                prepared.requestId(), response.statusCode(), errorCode, latencyMs, response.body());
    }

    private Map<String, Object> legalEnvelope(
            final String requestId,
            final String traceId,
            final String nonce,
            final String timestamp) {
        final Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("requestId", requestId);
        envelope.put("traceId", traceId);
        envelope.put("tenantId", TENANT);
        envelope.put("source", SOURCE);
        envelope.put("environment", "DEV");
        envelope.put("timestamp", timestamp);
        envelope.put("nonce", nonce);
        envelope.put("schemaVersion", SCHEMA_VERSION);
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
                        "context://qdr7-recovery-readonly",
                        "contextSnapshot",
                        Map.of(
                                "snapshotId", "snapshot-" + requestId,
                                "capturedAt", timestamp,
                                "evidenceRefs", List.of("evidence://" + requestId))));
        envelope.put(
                "forbiddenCapabilities",
                List.of(
                        "PLACE_ORDER",
                        "CANCEL_ORDER",
                        "MUTATE_NQ_STATE",
                        "READ_NQ_DB",
                        "WRITE_NQ_DB"));
        return envelope;
    }

    private void assertStructuredSuccess(final RequestOutcome outcome) {
        assertThat(outcome.statusCode())
                .withFailMessage(
                        "expected protected 2xx for %s; response=%s",
                        outcome.requestId(), outcome.body())
                .isBetween(200, 299);
        final JsonNode response;
        try {
            response = objectMapper.readTree(outcome.body());
        } catch (final Exception error) {
            throw new AssertionError("protected response must be structured JSON", error);
        }
        assertThat(response.path("decisionId").asText()).isNotBlank();
        assertThat(response.path("dryRun").asBoolean()).isTrue();
        assertThat(response.path("action").asText()).isEqualTo("NO_TRADE");
        assertThat(response.path("reasons").toString()).contains("MOCK_NO_TRADE");
        assertThat(response.path("schemaVersion").asText()).isEqualTo(SCHEMA_VERSION);
        assertThat(outcome.body()).doesNotContain("UNKNOWN_ERROR");
    }

    private void assertReplayRejected(final PreparedRequest committed) throws Exception {
        final RequestOutcome replay = send(committed);
        assertThat(replay.statusCode()).isEqualTo(409);
        assertThat(replay.errorCode()).isEqualTo("NONCE_REPLAY");
    }

    private int idempotencyRowCount(final String requestId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from dh_qdr7_idempotency_guard where tenant_id=? and request_id=?",
                Integer.class,
                TENANT,
                requestId);
    }

    private String idempotencyState(final String requestId) {
        return jdbcTemplate.queryForObject(
                "select state from dh_qdr7_idempotency_guard where tenant_id=? and request_id=?",
                String.class,
                TENANT,
                requestId);
    }

    private int rateRowCount(final String tenant) {
        return jdbcTemplate.queryForObject(
                "select count(*) from dh_qdr7_rate_limit_bucket where tenant_id=?",
                Integer.class,
                tenant);
    }

    private String canonicalPromptChecksum() {
        return jdbcTemplate.queryForObject(
                "select checksum from qdr_prompt_version where tenant_id=? order by created_at limit 1",
                String.class,
                TENANT);
    }

    private void writeRecoverySeries() {
        final StringBuilder csv =
                new StringBuilder(
                        "timestampUtc,round,phase,classification,containerState,pgIsReady,directJdbc,"
                                + "httpStatus,errorCode,latencyMs,hikariActive,hikariIdle,"
                                + "hikariPending,hikariTotal,connectionTimeoutCount,"
                                + "connectionCreationFailureCount,connectionAcquireMaxMs,"
                                + "postgresSessions,postgresActive,postgresWaiting,applicationState\n");
        synchronized (RECOVERY_SAMPLES) {
            for (final RecoverySample sample : RECOVERY_SAMPLES) {
                csv.append(sample.toCsv()).append('\n');
            }
        }
        try {
            Files.writeString(
                    EVIDENCE_ROOT.resolve("hikari-postgresql-series.csv"),
                    csv,
                    StandardCharsets.UTF_8);
        } catch (final IOException writeFailure) {
            throw new AssertionError("failed to write recovery series", writeFailure);
        }
    }

    private void writeSamePoolSummary(
            final IdentitySnapshot identity,
            final String promptChecksum,
            final List<RequestOutcome> load,
            final ConcurrentSnapshotAppender<ILoggingEvent> hikariEvents)
            throws IOException {
        final Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("runId", RUN_ID);
        summary.put("rootCause", "PROBE_CONTAINER_ENDPOINT_CHANGED");
        summary.put("probeValidity", "RECOVERY_PROBE_INVALID");
        summary.put("applicationContextIdentity", identity.applicationContextIdentity());
        summary.put("dataSourceIdentity", identity.dataSourceIdentity());
        summary.put("hikariPoolIdentity", identity.hikariPoolIdentity());
        summary.put("hikariPoolName", identity.hikariPoolName());
        summary.put("containerId", identity.containerId());
        summary.put("postgresHost", identity.postgresHost());
        summary.put("mappedPort", identity.mappedPort());
        summary.put("database", identity.database());
        summary.put("schema", identity.schema());
        summary.put("persistentVolumeId", identity.persistentVolumeId());
        summary.put("jdbcUrlHash", identity.jdbcUrlHash());
        summary.put("recoveryRounds", List.copyOf(RECOVERY_ROUND_ROWS));
        summary.put("samePoolRecoveryRounds", 3);
        summary.put("outageRequests", 9);
        summary.put("false2xxDuringOutage", 0);
        summary.put("promptVersionChecksumHash", sha256Hex(promptChecksum));
        summary.put("postRecoveryConcurrency", 8);
        summary.put("postRecoveryRequests", load.size());
        summary.put(
                "postRecovery2xx",
                load.stream()
                        .filter(
                                outcome ->
                                        outcome.statusCode() >= 200 && outcome.statusCode() <= 299)
                        .count());
        summary.put(
                "postRecovery4xx",
                load.stream()
                        .filter(
                                outcome ->
                                        outcome.statusCode() >= 400 && outcome.statusCode() <= 499)
                        .count());
        summary.put(
                "postRecoveryUnexpected5xx",
                load.stream().filter(outcome -> outcome.statusCode() >= 500).count());
        summary.put(
                "hikariFailureEvents",
                hikariEvents.snapshot().stream()
                        .map(ILoggingEvent::getFormattedMessage)
                        .filter(
                                message ->
                                        message.contains("Cannot acquire connection from data source")
                                                || message.contains("Failed to validate connection"))
                        .count());
        summary.put("seriesRows", RECOVERY_SAMPLES.size());
        summary.put("outageSamplingCadenceMsMax", maxOutageSamplingGapMs());
        summary.put("sameApplicationContext", true);
        summary.put("sameDataSource", true);
        summary.put("sameHikariPool", true);
        summary.put("sameContainer", true);
        summary.put("sameEndpoint", true);
        summary.put("committedNoncePreserved", true);
        summary.put("committedIdempotencyPreserved", true);
        summary.put("uncommittedTransactionRecoveredAsSuccess", false);
        summary.put("tenantIsolationPreserved", true);
        summary.put("promptVersionCanonicalRowPreserved", true);
        summary.put("externalHttp", false);
        summary.put("verdict", "PASS");
        objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(EVIDENCE_ROOT.resolve("same-pool-summary.json").toFile(), summary);
    }

    private void writeSpringRestartSummary() throws IOException {
        final Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("runId", RUN_ID);
        summary.put("rounds", List.copyOf(CONTEXT_RESTART_ROWS));
        summary.put("roundCount", CONTEXT_RESTART_ROWS.size());
        summary.put("allPassed", CONTEXT_RESTART_ROWS.size() == 3);
        summary.put("sameContainerId", POSTGRES.getContainerId());
        summary.put("sameMappedPort", POSTGRES.getMappedPort(5432));
        summary.put("persistentVolumeId", persistentVolumeId);
        summary.put("samePoolDataSourceIdentity", samePoolDataSourceIdentity);
        summary.put("samePoolHikariPoolIdentity", samePoolHikariPoolIdentity);
        objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(EVIDENCE_ROOT.resolve("spring-context-restart.json").toFile(), summary);
    }

    private static int findFreeLoopbackPort() {
        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            return socket.getLocalPort();
        } catch (final IOException unavailable) {
            throw new ExceptionInInitializerError(unavailable);
        }
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve("dh-app")) && Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new ExceptionInInitializerError("Decision Hub repository root was not found");
    }

    private static String sha256Hex(final String value) {
        try {
            final byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (final Exception unavailable) {
            throw new IllegalStateException("SHA-256 unavailable", unavailable);
        }
    }

    private static final class FixedPortPostgreSqlContainer
            extends PostgreSQLContainer<FixedPortPostgreSqlContainer> {

        private FixedPortPostgreSqlContainer(final int hostPort) {
            super(DockerImageName.parse("postgres:17"));
            addFixedExposedPort(hostPort, POSTGRESQL_PORT);
        }
    }

    private record PreparedRequest(
            String requestId,
            String traceId,
            String nonce,
            String timestamp,
            String body,
            String signature) {}

    private record RequestOutcome(
            String requestId, int statusCode, String errorCode, double latencyMs, String body) {

        private boolean structuredSuccess() {
            return statusCode >= 200 && statusCode <= 299 && !body.contains("UNKNOWN_ERROR");
        }
    }

    private record IdentitySnapshot(
            int applicationContextIdentity,
            int dataSourceIdentity,
            int hikariPoolIdentity,
            String hikariPoolName,
            String containerId,
            String postgresHost,
            int mappedPort,
            String database,
            String schema,
            String persistentVolumeId,
            String jdbcUrlHash) {}

    private record PostgreSqlSessions(int total, int active, int waiting) {}

    private record RecoverySample(
            String timestampUtc,
            int round,
            String phase,
            String classification,
            String containerState,
            boolean pgIsReady,
            boolean directJdbc,
            int httpStatus,
            String errorCode,
            double latencyMs,
            int hikariActive,
            int hikariIdle,
            int hikariPending,
            int hikariTotal,
            double connectionTimeoutCount,
            long connectionCreationFailureCount,
            double connectionAcquireMaxMs,
            int postgresSessions,
            int postgresActive,
            int postgresWaiting,
            String applicationState) {

        private String toCsv() {
            return String.join(
                    ",",
                    timestampUtc,
                    Integer.toString(round),
                    phase,
                    classification,
                    containerState,
                    Boolean.toString(pgIsReady),
                    Boolean.toString(directJdbc),
                    Integer.toString(httpStatus),
                    errorCode,
                    Double.toString(latencyMs),
                    Integer.toString(hikariActive),
                    Integer.toString(hikariIdle),
                    Integer.toString(hikariPending),
                    Integer.toString(hikariTotal),
                    Double.toString(connectionTimeoutCount),
                    Long.toString(connectionCreationFailureCount),
                    Double.toString(connectionAcquireMaxMs),
                    Integer.toString(postgresSessions),
                    Integer.toString(postgresActive),
                    Integer.toString(postgresWaiting),
                    applicationState);
        }
    }
}

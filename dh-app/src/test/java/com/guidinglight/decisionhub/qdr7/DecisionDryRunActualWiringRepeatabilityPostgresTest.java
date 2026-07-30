package com.guidinglight.decisionhub.qdr7;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.security.StaticTokenVerifier;
import com.guidinglight.decisionhub.security.nq.HmacNqDryRunAuthenticator;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.security.nq.NqDryRunAuthRequest;
import com.guidinglight.decisionhub.usecase.qdr.gateway.MockModelProvider;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelProviderPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 同一实际 Spring Boot wiring 中 protected dry-run 路径的顺序与并发可重复性回归。
 *
 * <p>测试只向随机 loopback 端口发送请求，使用一次性 PostgreSQL Testcontainer、合成测试认证材料和项目内
 * {@link MockModelProvider}；不发外部 HTTP，不调用真实 provider、NQ、Agent/LangGraph、Paper 或 LIVE。
 */
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
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
            "decisionhub.integration1.runtime.guard.environment=test",
            "decisionhub.integration1.runtime.guard.rate-window-seconds=60",
            "decisionhub.integration1.runtime.guard.rate-limit-value=100000",
            "decisionhub.integration1.runtime.guard.lease-seconds=30",
            "decisionhub.integration1.runtime.guard.idempotency-ttl-seconds=600",
            "decisionhub.integration1.runtime.guard.retention-seconds=3600",
            "decisionhub.security.nq-feedback.replay.guard-type=jdbc",
            "spring.datasource.hikari.maximum-pool-size=10",
            "spring.datasource.hikari.minimum-idle=2",
            "spring.datasource.hikari.connection-timeout=3000"
        })
class DecisionDryRunActualWiringRepeatabilityPostgresTest {

    private static final String ENDPOINT = "/api/ai/decision-dry-runs";
    private static final String SOURCE = "NQ_DRYRUN";
    private static final String TENANT = "tenant-a";
    private static final String SCHEMA_VERSION = "1.0.0";
    private static final String TEST_BEARER_TOKEN = "qdr7-repeatability-test-token";
    private static final String TEST_SIGNING_KEY = "qdr7-repeatability-test-signing-key";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("decision_hub")
                    .withUsername("decision_hub")
                    .withPassword("decision_hub");

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

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ModelProviderPort modelProvider;

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Test
    void fiveSequentialAndEightConcurrentProtectedRequestsRemainStructured2xx() throws Exception {
        assertThat(modelProvider).isExactlyInstanceOf(MockModelProvider.class);

        final String crossEnvironmentSuffix = "cross-environment";
        final String crossEnvironmentRequestId =
                "qdr7-repeatability-" + crossEnvironmentSuffix;
        seedFailedDevIdentity(crossEnvironmentRequestId);
        final RequestOutcome crossEnvironment =
                sendProtectedRequest(crossEnvironmentSuffix);
        assertStructuredSuccess(crossEnvironment);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select state from dh_qdr7_idempotency_guard"
                                        + " where environment='dev' and tenant_id=? and request_id=?",
                                String.class,
                                TENANT,
                                crossEnvironmentRequestId))
                .isEqualTo("FAILED");
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select state from dh_qdr7_idempotency_guard"
                                        + " where environment='test' and tenant_id=? and request_id=?",
                                String.class,
                                TENANT,
                                crossEnvironmentRequestId))
                .isEqualTo("COMPLETED");

        final List<RequestOutcome> sequential = new ArrayList<>();
        for (int ordinal = 1; ordinal <= 5; ordinal++) {
            sequential.add(sendProtectedRequest("sequential-" + ordinal));
        }
        sequential.forEach(this::assertStructuredSuccess);

        final ExecutorService executor = Executors.newFixedThreadPool(8);
        final CountDownLatch ready = new CountDownLatch(8);
        final CountDownLatch start = new CountDownLatch(1);
        final List<RequestOutcome> concurrent = new ArrayList<>();
        try {
            final List<Future<RequestOutcome>> futures = new ArrayList<>();
            for (int ordinal = 1; ordinal <= 8; ordinal++) {
                final int requestOrdinal = ordinal;
                futures.add(
                        executor.submit(
                                () -> {
                                    ready.countDown();
                                    if (!start.await(10, TimeUnit.SECONDS)) {
                                        throw new IllegalStateException("concurrent request start gate timed out");
                                    }
                                    return sendProtectedRequest("concurrent-" + requestOrdinal);
                                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (final Future<RequestOutcome> future : futures) {
                concurrent.add(future.get(30, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
        concurrent.forEach(this::assertStructuredSuccess);

        assertThat(sequential).hasSize(5);
        assertThat(concurrent).hasSize(8);
        assertThat(jdbcTemplate.queryForObject("select count(*) from dh_nq_replay_nonce", Integer.class))
                .isEqualTo(14);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from dh_qdr7_idempotency_guard"
                                        + " where tenant_id=? and state='COMPLETED'",
                                Integer.class,
                                TENANT))
                .isEqualTo(14);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select sum(request_count) from dh_qdr7_rate_limit_bucket where tenant_id=?",
                                Integer.class,
                                TENANT))
                .isEqualTo(14);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from qdr_model_gateway_call"
                                        + " where tenant_id=? and request_id like 'qdr7-repeatability-%'"
                                        + " and status='SUCCEEDED' and provider_kind='MOCK'",
                                Integer.class,
                                TENANT))
                .isEqualTo(14);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from qdr_model_gateway_call"
                                        + " where tenant_id=? and request_id like 'qdr7-repeatability-%'"
                                        + " and status='FAILED'",
                                Integer.class,
                                TENANT))
                .isZero();
    }

    private void seedFailedDevIdentity(final String requestId) {
        jdbcTemplate.update(
                "insert into dh_qdr7_idempotency_guard"
                        + " (guard_id,environment,endpoint,source,tenant_id,request_id,request_hash,"
                        + " hash_version,state,state_version,stable_error_code,created_at,updated_at,"
                        + " failed_at,expires_at,retention_until)"
                        + " values (?,'dev',?,'NQ_DRYRUN',?,?,'"
                        + "a".repeat(64)
                        + "','QDR7-DRYRUN-CJSON-1','FAILED',0,'DEV_ONLY_FAILURE',"
                        + " transaction_timestamp(),transaction_timestamp(),transaction_timestamp(),"
                        + " transaction_timestamp()+interval '10 minute',"
                        + " transaction_timestamp()+interval '1 hour')",
                UUID.randomUUID(),
                ENDPOINT,
                TENANT,
                requestId);
    }

    private RequestOutcome sendProtectedRequest(final String suffix) throws Exception {
        final String requestId = "qdr7-repeatability-" + suffix;
        final String traceId = "trace-" + requestId;
        final String nonce = "nonce-" + requestId;
        final Instant now = Instant.now();
        final String timestamp = now.toString();
        final Map<String, Object> envelope = legalEnvelope(requestId, traceId, nonce, timestamp);
        final String body = objectMapper.writeValueAsString(envelope);
        final NqDryRunAuthRequest unsigned =
                new NqDryRunAuthRequest(
                        "POST",
                        ENDPOINT,
                        SOURCE,
                        SOURCE,
                        TENANT,
                        TENANT,
                        FeedbackEnvironment.TEST,
                        "TEST",
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
        final HttpRequest request =
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + ENDPOINT))
                        .timeout(Duration.ofSeconds(20))
                        .header("Authorization", "Bearer " + TEST_BEARER_TOKEN)
                        .header("Content-Type", "application/json")
                        .header("X-NQ-DH-Source", SOURCE)
                        .header("X-NQ-DH-Tenant-Id", TENANT)
                        .header("X-NQ-DH-Request-Id", requestId)
                        .header("X-NQ-DH-Trace-Id", traceId)
                        .header("X-NQ-DH-Timestamp", timestamp)
                        .header("X-NQ-DH-Nonce", nonce)
                        .header("X-NQ-DH-Signature", signature)
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();
        final HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new RequestOutcome(requestId, response.statusCode(), response.body());
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
        envelope.put("environment", "TEST");
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
                        "context://qdr7-repeatability-readonly",
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
                .withFailMessage("expected protected 2xx for %s; response=%s", outcome.requestId(), outcome.body())
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
        assertThat(response.path("auditRef").asText()).isNotBlank();
        assertThat(response.path("replayRef").asText()).isNotBlank();
        assertThat(response.path("traceSummary").isArray()).isTrue();
        assertThat(outcome.body()).doesNotContain("UNKNOWN_ERROR", "provider profile bootstrap mismatch");
    }

    private record RequestOutcome(String requestId, int statusCode, String body) {
    }
}

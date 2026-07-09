package com.guidinglight.decisionhub.usecase.qdr.gateway;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Stage-QDR-5 B1 observability / provider readiness contract tests。
 *
 * <p>B1 只冻结 domain/usecase contract 与 fail-closed validation，不接真实 provider、HTTP、SDK、Agent、
 * LangGraph、LIVE、API、repository 或 migration。
 */
final class ModelGatewayObservabilityContractServiceTest {

    private static final String TENANT = "tenant-a";
    private static final String HASH =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final Instant NOW = Instant.parse("2026-07-09T00:00:00Z");
    private static final ModelGatewayObservabilityContractService SERVICE =
            new ModelGatewayObservabilityContractService();
    private static final List<String> B1_MAIN_FILES =
            List.of(
                    "ProviderFailureClassification.java",
                    "ProviderLatencyBudgetSummary.java",
                    "ProviderTrustDecisionSummary.java",
                    "ProviderReadinessStatus.java",
                    "ProviderReadinessSeverity.java",
                    "ProviderReadinessFinding.java",
                    "ProviderReadinessSignal.java",
                    "ProviderHealthSummary.java",
                    "ModelGatewayObservabilitySummary.java",
                    "ModelGatewayObservabilityContractException.java",
                    "ModelGatewayObservabilityContractService.java");
    private static final List<Pattern> FORBIDDEN_RUNTIME_PATTERNS =
            List.of(
                    Pattern.compile("import\\s+java\\.net\\.http\\."),
                    Pattern.compile("import\\s+org\\.springframework\\.web\\.client\\."),
                    Pattern.compile(
                            "import\\s+org\\.springframework\\.web\\.reactive\\.function\\.client\\.WebClient"),
                    Pattern.compile("\\bnew\\s+RestTemplate\\b"),
                    Pattern.compile("\\bnew\\s+WebClient\\b"),
                    Pattern.compile("\\bOpenAI(Client|Sdk|SDK)?\\b"),
                    Pattern.compile("\\bAnthropic(Client|Sdk|SDK)?\\b"),
                    Pattern.compile("\\bGemini(Client|Sdk|SDK)?\\b"),
                    Pattern.compile("\\bOllama(Client|Sdk|SDK)?\\b"),
                    Pattern.compile("\\bProvider(Sdk|SDK)\\b"),
                    Pattern.compile("\\bclass\\s+\\w*LangGraph\\w*\\b"),
                    Pattern.compile("\\bclass\\s+\\w*AutoGen\\w*\\b"),
                    Pattern.compile("\\bclass\\s+\\w*CrewAI\\w*\\b"),
                    Pattern.compile("\\bclass\\s+\\w*AgentRuntime\\w*\\b"));

    @Test
    void validObservabilitySummaryCanBeCreated() {
        final ModelGatewayObservabilitySummary summary = observabilitySummary();

        assertSame(summary, SERVICE.validate(summary));
    }

    @Test
    void validProviderHealthSummaryCanBeCreated() {
        final ProviderHealthSummary summary = providerHealthSummary();

        assertSame(summary, SERVICE.validate(summary));
    }

    @Test
    void missingTenantIdFailsClosed() {
        final ModelGatewayObservabilitySummary summary =
                new ModelGatewayObservabilitySummary(
                        null,
                        "trace-a",
                        "request-a",
                        "gateway-v1",
                        "mock-provider",
                        HASH,
                        latencyBudgetSummary(),
                        ProviderFailureClassification.NONE,
                        ProviderTrustDecisionSummary.allowed("trust-decision-ref-a"),
                        ProviderReadinessSignal.ready("readiness-ref-a"),
                        NOW);

        assertFailsClosed(() -> SERVICE.validate(summary), "OBSERVABILITY_CONTRACT_REJECTED");
    }

    @Test
    void missingProviderRefFailsClosed() {
        final ProviderHealthSummary summary =
                new ProviderHealthSummary(
                        TENANT,
                        null,
                        "gateway-v1",
                        ProviderReadinessStatus.NOT_READY,
                        ProviderFailureClassification.SOURCE_DENIED,
                        latencyBudgetSummary(),
                        NOW);

        assertFailsClosed(() -> SERVICE.validate(summary), "PROVIDER_HEALTH_CONTRACT_REJECTED");
    }

    @Test
    void missingModelGatewayVersionRefFailsClosed() {
        final ProviderHealthSummary summary =
                new ProviderHealthSummary(
                        TENANT,
                        "mock-provider",
                        null,
                        ProviderReadinessStatus.NOT_READY,
                        ProviderFailureClassification.SOURCE_DENIED,
                        latencyBudgetSummary(),
                        NOW);

        assertFailsClosed(() -> SERVICE.validate(summary), "PROVIDER_HEALTH_CONTRACT_REJECTED");
    }

    @Test
    void failureClassificationSupportsGatewayFailures() {
        assertAll(
                () ->
                        assertEquals(
                                ProviderFailureClassification.TIMEOUT,
                                ProviderFailureClassification.fromGatewayFailureCode(
                                        ModelGatewayFailureCode.PROVIDER_TIMEOUT)),
                () ->
                        assertEquals(
                                ProviderFailureClassification.BUDGET_EXCEEDED,
                                ProviderFailureClassification.fromGatewayFailureCode(
                                        ModelGatewayFailureCode.BUDGET_EXCEEDED)),
                () ->
                        assertEquals(
                                ProviderFailureClassification.POLICY_DENIED,
                                ProviderFailureClassification.fromGatewayFailureCode(
                                        ModelGatewayFailureCode.POLICY_DENIED)),
                () ->
                        assertEquals(
                                ProviderFailureClassification.SOURCE_DENIED,
                                ProviderFailureClassification.fromGatewayFailureCode(
                                        ModelGatewayFailureCode.REAL_PROVIDER_FORBIDDEN)),
                () ->
                        assertEquals(
                                ProviderFailureClassification.UNKNOWN,
                                ProviderFailureClassification.fromGatewayFailureCode(
                                        ModelGatewayFailureCode.UNKNOWN_ERROR)));
    }

    @Test
    void latencyBudgetSummaryRecordsSafePercentileFields() {
        final ProviderLatencyBudgetSummary summary = latencyBudgetSummary();

        assertAll(
                () -> assertEquals(20L, summary.p50Ms()),
                () -> assertEquals(60L, summary.p95Ms()),
                () -> assertEquals(90L, summary.p99Ms()),
                () -> assertSame(summary, SERVICE.validate(summary)));
    }

    @Test
    void latencyBudgetSummaryRejectsInvalidValues() {
        assertAll(
                () ->
                        assertFailsClosed(
                                () -> SERVICE.validate(
                                        new ProviderLatencyBudgetSummary(
                                                -1L, 60L, 90L, 200L, 0.4d, 10L)),
                                "LATENCY_BUDGET_CONTRACT_REJECTED"),
                () ->
                        assertFailsClosed(
                                () -> SERVICE.validate(
                                        new ProviderLatencyBudgetSummary(
                                                20L, 60L, 90L, 200L, 1.5d, 10L)),
                                "LATENCY_BUDGET_CONTRACT_REJECTED"),
                () ->
                        assertFailsClosed(
                                () -> SERVICE.validate(
                                        new ProviderLatencyBudgetSummary(
                                                20L, 60L, 90L, 200L, 0.4d, -1L)),
                                "LATENCY_BUDGET_CONTRACT_REJECTED"));
    }

    @Test
    void trustDecisionSummaryRecordsAllSafeStates() {
        assertAll(
                () ->
                        assertEquals(
                                ProviderTrustDecisionSummary.Decision.ALLOWED,
                                SERVICE.validate(ProviderTrustDecisionSummary.allowed("allowed-ref"))
                                        .decision()),
                () ->
                        assertEquals(
                                ProviderTrustDecisionSummary.Decision.DENIED,
                                SERVICE.validate(ProviderTrustDecisionSummary.denied("denied-ref"))
                                        .decision()),
                () ->
                        assertEquals(
                                ProviderTrustDecisionSummary.Decision.DEGRADED,
                                SERVICE.validate(ProviderTrustDecisionSummary.degraded("degraded-ref"))
                                        .decision()),
                () ->
                        assertEquals(
                                ProviderTrustDecisionSummary.Decision.SKIPPED,
                                SERVICE.validate(ProviderTrustDecisionSummary.skipped("skipped-ref"))
                                        .decision()));
    }

    @Test
    void readinessSignalCannotEnableRealProvider() {
        assertFailsClosed(
                () -> SERVICE.validate(ProviderReadinessSignal.ready("enable real provider")),
                "READINESS_SIGNAL_CONTRACT_REJECTED");
    }

    @Test
    void readinessSignalCannotEnableRealHttp() {
        assertFailsClosed(
                () -> SERVICE.validate(ProviderReadinessSignal.ready("enable real HTTP")),
                "READINESS_SIGNAL_CONTRACT_REJECTED");
    }

    @Test
    void readinessSignalCannotEnableLive() {
        assertFailsClosed(
                () -> SERVICE.validate(ProviderReadinessSignal.ready("enable LIVE")),
                "READINESS_SIGNAL_CONTRACT_REJECTED");
    }

    @Test
    void readinessSignalCannotGenerateTradingSignal() {
        final ProviderReadinessSignal signal =
                ProviderReadinessSignal.notReady(
                        "readiness-ref-a",
                        List.of(new ProviderReadinessFinding(
                                ProviderReadinessSeverity.BLOCKING,
                                "policy-denied",
                                "trading signal candidate")));

        assertFailsClosed(() -> SERVICE.validate(signal), "READINESS_SIGNAL_CONTRACT_REJECTED");
    }

    @Test
    void rawProviderResponseIsRejected() {
        final ProviderReadinessSignal signal =
                ProviderReadinessSignal.notReady(
                        "readiness-ref-a",
                        List.of(new ProviderReadinessFinding(
                                ProviderReadinessSeverity.ERROR,
                                "provider-output-invalid",
                                "raw provider response body")));

        assertFailsClosed(() -> SERVICE.validate(signal), "READINESS_SIGNAL_CONTRACT_REJECTED");
    }

    @Test
    void rawPromptIsRejected() {
        final ProviderReadinessSignal signal =
                ProviderReadinessSignal.notReady(
                        "raw prompt text",
                        List.of());

        assertFailsClosed(() -> SERVICE.validate(signal), "READINESS_SIGNAL_CONTRACT_REJECTED");
    }

    @Test
    void credentialLikeKeyIsRejected() {
        final ProviderHealthSummary summary =
                new ProviderHealthSummary(
                        TENANT,
                        "apiKey=ABC123",
                        "gateway-v1",
                        ProviderReadinessStatus.NOT_READY,
                        ProviderFailureClassification.SOURCE_DENIED,
                        latencyBudgetSummary(),
                        NOW);

        final ModelGatewayObservabilityContractException error =
                assertFailsClosed(
                        () -> SERVICE.validate(summary), "PROVIDER_HEALTH_CONTRACT_REJECTED");
        assertFalse(error.getMessage().contains("ABC123"));
    }

    @Test
    void safeRefsRejectInvalidProviderSummaryHashAndGatewayVersionRef() {
        assertAll(
                () ->
                        assertFailsClosed(
                                () ->
                                        SERVICE.validate(
                                                new ModelGatewayObservabilitySummary(
                                                        TENANT,
                                                        "trace-a",
                                                        "request-a",
                                                        "gateway-v1",
                                                        "mock-provider",
                                                        "not-a-hash",
                                                        latencyBudgetSummary(),
                                                        ProviderFailureClassification.NONE,
                                                        ProviderTrustDecisionSummary.allowed(
                                                                "trust-decision-ref-a"),
                                                        ProviderReadinessSignal.ready(
                                                                "readiness-ref-a"),
                                                        NOW)),
                                "OBSERVABILITY_CONTRACT_REJECTED"),
                () ->
                        assertFailsClosed(
                                () ->
                                        SERVICE.validate(
                                                new ProviderHealthSummary(
                                                        TENANT,
                                                        "mock-provider",
                                                        "raw provider response",
                                                        ProviderReadinessStatus.NOT_READY,
                                                        ProviderFailureClassification.SOURCE_DENIED,
                                                        latencyBudgetSummary(),
                                                        NOW)),
                                "PROVIDER_HEALTH_CONTRACT_REJECTED"));
    }

    @Test
    void contractRecordFieldsDoNotExposeRawOrCredentialMaterial() {
        assertAll(
                () -> assertSafeRecordComponents(ModelGatewayObservabilitySummary.class),
                () -> assertSafeRecordComponents(ProviderHealthSummary.class),
                () -> assertSafeRecordComponents(ProviderLatencyBudgetSummary.class),
                () -> assertSafeRecordComponents(ProviderTrustDecisionSummary.class),
                () -> assertSafeRecordComponents(ProviderReadinessSignal.class),
                () -> assertSafeRecordComponents(ProviderReadinessFinding.class));
    }

    @Test
    void b1FilesDoNotIntroduceProviderSdkHttpAgentOrLangGraphRuntime() throws Exception {
        final Path gatewayPackage =
                moduleRoot()
                        .resolve("src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway");

        for (final String fileName : B1_MAIN_FILES) {
            final String source = Files.readString(gatewayPackage.resolve(fileName));
            for (final Pattern pattern : FORBIDDEN_RUNTIME_PATTERNS) {
                assertFalse(
                        pattern.matcher(source).find(),
                        fileName + " must not introduce runtime dependency " + pattern);
            }
        }
    }

    private static ModelGatewayObservabilitySummary observabilitySummary() {
        return new ModelGatewayObservabilitySummary(
                TENANT,
                "trace-a",
                "request-a",
                "gateway-v1",
                "mock-provider",
                HASH,
                latencyBudgetSummary(),
                ProviderFailureClassification.NONE,
                ProviderTrustDecisionSummary.allowed("trust-decision-ref-a"),
                ProviderReadinessSignal.ready("readiness-ref-a"),
                NOW);
    }

    private static ProviderHealthSummary providerHealthSummary() {
        return new ProviderHealthSummary(
                TENANT,
                "mock-provider",
                "gateway-v1",
                ProviderReadinessStatus.READY,
                ProviderFailureClassification.NONE,
                latencyBudgetSummary(),
                NOW);
    }

    private static ProviderLatencyBudgetSummary latencyBudgetSummary() {
        return new ProviderLatencyBudgetSummary(20L, 60L, 90L, 200L, 0.45d, 15L);
    }

    private static ModelGatewayObservabilityContractException assertFailsClosed(
            final ThrowingRunnable action, final String expectedCode) {
        final ModelGatewayObservabilityContractException error =
                assertThrows(ModelGatewayObservabilityContractException.class, action::run);

        assertEquals(expectedCode, error.code());
        assertTrue(error.failClosed());
        return error;
    }

    private static void assertSafeRecordComponents(final Class<?> recordType) {
        for (final RecordComponent component : recordType.getRecordComponents()) {
            final String name = component.getName().toLowerCase(Locale.ROOT);
            assertFalse(name.contains("raw"));
            assertFalse(name.contains("credential"));
            assertFalse(name.contains("apikey"));
            assertFalse(name.contains("apisecret"));
            assertFalse(name.contains("passphrase"));
        }
    }

    private static Path moduleRoot() {
        final Path cwd = Path.of("").toAbsolutePath();
        if (Files.isDirectory(cwd.resolve("src/main/java"))) {
            return cwd;
        }
        return cwd.resolve("dh-usecase");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {

        void run();
    }
}

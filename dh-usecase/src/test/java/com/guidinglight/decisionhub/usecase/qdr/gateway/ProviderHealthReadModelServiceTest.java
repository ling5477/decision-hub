package com.guidinglight.decisionhub.usecase.qdr.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.qdr.model.ProviderKind;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Stage-QDR-5 B2 provider health / gateway call read model 回归测试。
 *
 * <p>测试只使用 B1 observability summary 和既有 gateway call record 的内存 evidence；不新增 API、
 * migration、repository/JDBC、provider、HTTP、Agent、LangGraph、NQ 或 LIVE 能力。
 */
final class ProviderHealthReadModelServiceTest {

    private static final String TENANT_A = "tenant-a";
    private static final String TENANT_B = "tenant-b";
    private static final String PROVIDER_REF = "mock-provider-a";
    private static final String GATEWAY_VERSION_REF = "gateway-v1";
    private static final String TRACE_ID = "trace-a";
    private static final String SOURCE_REQUEST_ID = "source-request-a";
    private static final String HASH_A =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String HASH_B =
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final Instant NOW = Instant.parse("2026-07-09T00:00:00Z");
    private static final List<String> B2_MAIN_FILES = List.of(
            "ProviderHealthReadModelQuery.java",
            "ProviderHealthReadModelView.java",
            "ModelGatewayCallObservabilityView.java",
            "ProviderFailureClassificationView.java",
            "ProviderLatencyBudgetView.java",
            "ProviderTrustDecisionView.java",
            "ProviderReadinessSignalView.java",
            "ProviderHealthReadModelService.java");
    private static final List<Pattern> FORBIDDEN_RUNTIME_PATTERNS = List.of(
            Pattern.compile("import\\s+java\\.net\\.http\\."),
            Pattern.compile("import\\s+org\\.springframework\\.web\\.client\\."),
            Pattern.compile("import\\s+org\\.springframework\\.web\\.reactive\\.function\\.client\\.WebClient"),
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
    void providerHealthQueryByTenantIdAndProviderRefSucceeds() {
        final ProviderHealthReadModelService service = service(summary("call-a", PROVIDER_REF));

        final List<ProviderHealthReadModelView> views =
                service.findProviderHealth(queryByProvider(TENANT_A, PROVIDER_REF));

        assertEquals(1, views.size());
        assertEquals(PROVIDER_REF, views.getFirst().providerRef());
        assertEquals(ProviderReadinessStatus.READY, views.getFirst().readinessSignal().status());
    }

    @Test
    void gatewayCallObservabilityQueryByTenantAndModelGatewayVersionRefSucceeds() {
        final ProviderHealthReadModelService service = service(summary("call-a", PROVIDER_REF));

        final List<ProviderHealthReadModelView> views =
                service.findProviderHealth(queryByGatewayVersion(TENANT_A, GATEWAY_VERSION_REF));

        assertEquals(1, views.size());
        final ModelGatewayCallObservabilityView callView = views.getFirst().gatewayCallObservability();
        assertEquals(GATEWAY_VERSION_REF, callView.modelGatewayVersionRef());
        assertEquals(ProviderTrustDecisionSummary.Decision.ALLOWED, callView.trustDecision().decision());
    }

    @Test
    void traceLookupByTenantIdAndTraceIdSucceeds() {
        final ProviderHealthReadModelService service = service(summary("call-a", PROVIDER_REF));

        final List<ProviderHealthReadModelView> views =
                service.findProviderHealth(queryByTrace(TENANT_A, TRACE_ID));

        assertEquals(1, views.size());
        assertEquals(TRACE_ID, views.getFirst().traceId());
    }

    @Test
    void requestLookupByTenantIdAndSourceRequestIdSucceeds() {
        final ProviderHealthReadModelService service = service(summary("call-a", PROVIDER_REF));

        final List<ProviderHealthReadModelView> views =
                service.findProviderHealth(queryBySourceRequest(TENANT_A, SOURCE_REQUEST_ID));

        assertEquals(1, views.size());
        assertEquals(SOURCE_REQUEST_ID, views.getFirst().sourceRequestId());
    }

    @Test
    void tenantlessQueryFailsClosed() {
        assertThrows(NullPointerException.class, () -> queryByProvider(null, PROVIDER_REF));
    }

    @Test
    void uuidOnlyQueryIsAbsentFromReadModelContract() {
        for (final RecordComponent component :
                ProviderHealthReadModelQuery.class.getRecordComponents()) {
            assertFalse(UUID.class.equals(component.getType()));
            assertFalse(component.getName().toLowerCase().contains("uuid"));
            assertFalse(component.getName().toLowerCase().contains("gatewaycallid"));
        }
    }

    @Test
    void crossTenantReadReturnsEmpty() {
        final ProviderHealthReadModelService service = service(summary("call-a", PROVIDER_REF));

        final List<ProviderHealthReadModelView> views =
                service.findProviderHealth(queryByProvider(TENANT_B, PROVIDER_REF));

        assertTrue(views.isEmpty());
    }

    @Test
    void listQueryIsPaginated() {
        final ProviderHealthReadModelService service = service(
                summary("call-a", "mock-provider-a"),
                summary("call-b", "mock-provider-b"),
                summary("call-c", "mock-provider-c"));

        final List<ProviderHealthReadModelView> views =
                service.findProviderHealth(new ProviderHealthReadModelQuery(
                        TENANT_A,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        ProviderReadinessStatus.READY,
                        null,
                        null,
                        null,
                        null,
                        2,
                        1));

        assertEquals(2, views.size());
        assertEquals("mock-provider-b", views.getFirst().providerRef());
        assertEquals("mock-provider-c", views.get(1).providerRef());
    }

    @Test
    void pageSizeOverOneHundredIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ProviderHealthReadModelQuery(
                TENANT_A,
                PROVIDER_REF,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                101,
                0));
    }

    @Test
    void viewDoesNotExposeRawPromptRawProviderResponseOrCredentialLikeFields() {
        final ProviderHealthReadModelView view =
                service(summary("call-a", PROVIDER_REF))
                        .findProviderHealth(queryByProvider(TENANT_A, PROVIDER_REF))
                        .getFirst();
        final String rendered = view.toString();

        assertFalse(rendered.contains("rawPrompt"));
        assertFalse(rendered.contains("raw_prompt"));
        assertFalse(rendered.contains("promptText"));
        assertFalse(rendered.contains("rawProviderResponse"));
        assertFalse(rendered.contains("raw_provider_response"));
        assertFalse(rendered.contains("providerRaw"));
        assertFalse(rendered.contains("credential"));
        assertFalse(rendered.contains("apiKey"));
        assertFalse(rendered.contains("apiSecret"));
        assertFalse(rendered.contains("passphrase"));
        assertFalse(rendered.contains("token"));
        assertFalse(rendered.contains("cookie"));
        assertFalse(rendered.contains("secret"));
    }

    @Test
    void failureClassificationAppearsAsSafeEnumOnly() {
        final ProviderHealthReadModelService service = service(failedSummary());

        final ProviderHealthReadModelView view = service.findProviderHealth(
                        new ProviderHealthReadModelQuery(
                                TENANT_A,
                                null,
                                null,
                                null,
                                null,
                                ProviderFailureClassification.TIMEOUT,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                50,
                                0))
                .getFirst();

        assertEquals(
                ProviderFailureClassification.TIMEOUT,
                view.failureClassification().classification());
        assertFalse(view.failureClassification().summaryRef().contains("Exception"));
    }

    @Test
    void trustDecisionDoesNotImplyProviderAuthorization() {
        final ProviderHealthReadModelView view =
                service(summary("call-a", PROVIDER_REF))
                        .findProviderHealth(queryByProvider(TENANT_A, PROVIDER_REF))
                        .getFirst();
        final String rendered = view.trustDecision().toString().toLowerCase();

        assertEquals(ProviderTrustDecisionSummary.Decision.ALLOWED, view.trustDecision().decision());
        assertFalse(rendered.contains("authorization"));
        assertFalse(rendered.contains("permission"));
    }

    @Test
    void readinessSignalDoesNotImplyRealHttpProviderOrLive() {
        final ProviderHealthReadModelView view =
                service(summary("call-a", PROVIDER_REF))
                        .findProviderHealth(queryByProvider(TENANT_A, PROVIDER_REF))
                        .getFirst();
        final String rendered = view.readinessSignal().toString();

        assertEquals(ProviderReadinessStatus.READY, view.readinessSignal().status());
        assertFalse(rendered.contains("real HTTP"));
        assertFalse(rendered.contains("real provider"));
        assertFalse(rendered.contains("LIVE"));
    }

    @Test
    void providerHealthIsNotExposedAsTradingSignal() {
        final ProviderHealthReadModelView view =
                service(summary("call-a", PROVIDER_REF))
                        .findProviderHealth(queryByProvider(TENANT_A, PROVIDER_REF))
                        .getFirst();
        final List<String> executableLabels =
                List.of("BUY", "SELL", "MARKET_ORDER", "PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE");

        assertFalse(executableLabels.stream().anyMatch(label -> view.toString().contains(label)));
    }

    @Test
    void projectionFromExistingGatewayCallRecordSucceedsWithoutRepositoryExpansion() {
        final ProviderHealthReadModelService service =
                ProviderHealthReadModelService.fromGatewayCalls(List.of(gatewayCallRecord()));

        final ProviderHealthReadModelView view = service.findProviderHealth(
                        new ProviderHealthReadModelQuery(
                                TENANT_A,
                                "mock-provider-record",
                                "model-version:" + uuid("model-version"),
                                "trace-record",
                                "request-record",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                50,
                                0))
                .getFirst();

        assertEquals("mock-provider-record", view.providerRef());
        assertEquals(HASH_B, view.providerSummaryHash());
        assertEquals(ProviderFailureClassification.NONE, view.failureClassification().classification());
    }

    @Test
    void providerHttpProviderSdkAgentAndLangGraphClassesAreNotIntroduced() throws IOException {
        final Path gatewayPackage =
                moduleRoot().resolve("src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway");

        for (final String fileName : B2_MAIN_FILES) {
            final String source = Files.readString(gatewayPackage.resolve(fileName));
            for (final Pattern pattern : FORBIDDEN_RUNTIME_PATTERNS) {
                assertFalse(pattern.matcher(source).find(), fileName + " must not introduce " + pattern);
            }
        }
    }

    @Test
    void readFailureFailsClosed() {
        final ProviderHealthReadModelService service = service(new ModelGatewayObservabilitySummary(
                TENANT_A,
                TRACE_ID,
                SOURCE_REQUEST_ID,
                GATEWAY_VERSION_REF,
                "apiKey=ABC123",
                HASH_A,
                latencyBudgetSummary(),
                ProviderFailureClassification.NONE,
                ProviderTrustDecisionSummary.allowed("trust-ref-a"),
                ProviderReadinessSignal.ready("readiness-ref-a"),
                NOW));

        final ModelGatewayObservabilityContractException error =
                assertThrows(
                        ModelGatewayObservabilityContractException.class,
                        () -> service.findProviderHealth(queryByGatewayVersion(TENANT_A, GATEWAY_VERSION_REF)));

        assertEquals("PROVIDER_HEALTH_READ_MODEL_REJECTED", error.code());
        assertTrue(error.failClosed());
        assertFalse(error.getMessage().contains("ABC123"));
    }

    private static ProviderHealthReadModelService service(
            final ModelGatewayObservabilitySummary... summaries) {
        return new ProviderHealthReadModelService(List.of(summaries));
    }

    private static ProviderHealthReadModelQuery queryByProvider(
            final String tenantId, final String providerRef) {
        return new ProviderHealthReadModelQuery(
                tenantId,
                providerRef,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                50,
                0);
    }

    private static ProviderHealthReadModelQuery queryByGatewayVersion(
            final String tenantId, final String modelGatewayVersionRef) {
        return new ProviderHealthReadModelQuery(
                tenantId,
                null,
                modelGatewayVersionRef,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                50,
                0);
    }

    private static ProviderHealthReadModelQuery queryByTrace(
            final String tenantId, final String traceId) {
        return new ProviderHealthReadModelQuery(
                tenantId,
                null,
                null,
                traceId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                50,
                0);
    }

    private static ProviderHealthReadModelQuery queryBySourceRequest(
            final String tenantId, final String sourceRequestId) {
        return new ProviderHealthReadModelQuery(
                tenantId,
                null,
                null,
                null,
                sourceRequestId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                50,
                0);
    }

    private static ModelGatewayObservabilitySummary summary(
            final String callRef, final String providerRef) {
        return new ModelGatewayObservabilitySummary(
                TENANT_A,
                traceFor(callRef),
                sourceRequestFor(callRef),
                GATEWAY_VERSION_REF,
                providerRef,
                HASH_A,
                latencyBudgetSummary(),
                ProviderFailureClassification.NONE,
                ProviderTrustDecisionSummary.allowed("trust-ref-" + callRef),
                ProviderReadinessSignal.ready("readiness-ref-" + callRef),
                NOW.plusSeconds(callRef.charAt(callRef.length() - 1)));
    }

    private static ModelGatewayObservabilitySummary failedSummary() {
        return new ModelGatewayObservabilitySummary(
                TENANT_A,
                "trace-failed",
                "source-request-failed",
                GATEWAY_VERSION_REF,
                PROVIDER_REF,
                HASH_A,
                latencyBudgetSummary(),
                ProviderFailureClassification.TIMEOUT,
                ProviderTrustDecisionSummary.denied("trust-ref-failed"),
                ProviderReadinessSignal.notReady(
                        "readiness-ref-failed",
                        List.of(new ProviderReadinessFinding(
                                ProviderReadinessSeverity.BLOCKING,
                                "timeout",
                                "gateway-call:failed"))),
                NOW.plusSeconds(10));
    }

    private static ProviderLatencyBudgetSummary latencyBudgetSummary() {
        return new ProviderLatencyBudgetSummary(20L, 60L, 90L, 200L, 0.45d, 15L);
    }

    private static ModelGatewayCallRecord gatewayCallRecord() {
        return new ModelGatewayCallRecord(
                uuid("gateway-call"),
                TENANT_A,
                "trace-record",
                "request-record",
                uuid("decision-run"),
                uuid("prompt-version"),
                uuid("model-version"),
                uuid("provider-profile"),
                ProviderKind.MOCK,
                "mock-provider-record",
                ModelGatewayCallStatus.SUCCEEDED,
                null,
                ModelGatewayCallTrustDecision.ALLOWED,
                "trust-record",
                "model-call-record",
                "budget:local",
                10,
                20,
                30,
                15,
                1,
                "input-summary",
                "output-summary",
                HASH_A,
                HASH_B,
                "audit-ref",
                "trace-ref",
                NOW);
    }

    private static String traceFor(final String callRef) {
        if ("call-a".equals(callRef)) {
            return TRACE_ID;
        }
        return "trace-" + callRef;
    }

    private static String sourceRequestFor(final String callRef) {
        if ("call-a".equals(callRef)) {
            return SOURCE_REQUEST_ID;
        }
        return "source-request-" + callRef;
    }

    private static UUID uuid(final String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private static Path moduleRoot() {
        final Path cwd = Path.of("").toAbsolutePath();
        if (Files.isDirectory(cwd.resolve("src/main/java"))) {
            return cwd;
        }
        return cwd.resolve("dh-usecase");
    }
}

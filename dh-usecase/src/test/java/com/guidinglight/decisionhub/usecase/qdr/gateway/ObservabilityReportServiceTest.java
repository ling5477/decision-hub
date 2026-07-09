package com.guidinglight.decisionhub.usecase.qdr.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Stage-QDR-5 B4 Observability Report / Acceptance Support 回归测试。
 *
 * <p>测试只组合 B1 safe summary、B2 provider health read model view 与 B3 readiness evaluation result；
 * 不新增 API、migration、repository/JDBC、真实 provider、HTTP、Provider SDK、Agent、LangGraph、NQ 或 LIVE 能力。
 */
final class ObservabilityReportServiceTest {

    private static final String TENANT_ID = "tenant-a";
    private static final String SOURCE_REF = "source:mock-safe";
    private static final String PROVIDER_REF = "mock-provider-a";
    private static final String GATEWAY_VERSION_REF = "gateway-v1";
    private static final String HASH =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String POLICY_VERSION = "provider-readiness-policy-v1";
    private static final String TRACE_ID = "trace-a";
    private static final String SOURCE_REQUEST_ID = "source-request-a";
    private static final Instant NOW = Instant.parse("2026-07-09T00:00:00Z");
    private static final List<String> B4_MAIN_FILES = List.of(
            "ObservabilityReportSafety.java",
            "ProviderReadinessAcceptanceStatus.java",
            "ObservabilityReportCommand.java",
            "ProviderFailureClassificationSummary.java",
            "ProviderLatencyBudgetReport.java",
            "ProviderTrustDecisionReport.java",
            "ProviderReadinessEvidenceView.java",
            "ProviderHealthReportSection.java",
            "ProviderReadinessReportSection.java",
            "ProviderReadinessAcceptanceSummary.java",
            "StageQdr5AcceptanceEvidence.java",
            "ModelGatewayObservabilityReport.java",
            "ObservabilityReportService.java");
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
    void validObservabilityReportCanBeGeneratedFromSafeInputs() {
        final ModelGatewayObservabilityReport report =
                new ObservabilityReportService().generate(validCommand());

        assertEquals(ProviderReadinessAcceptanceStatus.PASS, report.acceptanceStatus());
        assertEquals(TENANT_ID, report.tenantId());
        assertEquals(PROVIDER_REF, report.providerRef());
        assertEquals(SOURCE_REF, report.sourceRef());
        assertEquals(ProviderReadinessDecision.READY, report.providerReadiness().readinessDecision());
        assertEquals(ProviderReadinessStatus.READY, report.providerHealth().readinessStatus());
    }

    @Test
    void missingTenantIdFailsClosed() {
        final ModelGatewayObservabilityReport report =
                new ObservabilityReportService().generate(commandWithSummary(summaryWithoutTenant()));

        assertEquals(ProviderReadinessAcceptanceStatus.FAIL, report.acceptanceStatus());
        assertEquals(ProviderReadinessDecision.NOT_READY, report.providerReadiness().readinessDecision());
    }

    @Test
    void missingProviderRefFailsClosed() {
        final ModelGatewayObservabilityReport report =
                new ObservabilityReportService().generate(commandWithSummary(summaryWithoutProvider()));

        assertEquals(ProviderReadinessAcceptanceStatus.FAIL, report.acceptanceStatus());
        assertEquals(ProviderReadinessDecision.NOT_READY, report.providerReadiness().readinessDecision());
    }

    @Test
    void missingReadinessDecisionReturnsSkipped() {
        final ObservabilityReportCommand base = validCommand();
        final ModelGatewayObservabilityReport report = new ObservabilityReportService().generate(
                new ObservabilityReportCommand(
                        base.tenantId(),
                        base.sourceRef(),
                        base.observabilitySummary(),
                        base.providerHealthView(),
                        null,
                        base.createdAt(),
                        base.observedAt()));

        assertEquals(ProviderReadinessAcceptanceStatus.SKIPPED, report.acceptanceStatus());
        assertEquals(ProviderReadinessDecision.SKIPPED, report.providerReadiness().readinessDecision());
        assertEquals("missing-readiness-decision", report.providerReadiness().readinessFinding());
    }

    @Test
    void reportIncludesFailureClassificationSummary() {
        final ModelGatewayObservabilityReport report =
                new ObservabilityReportService().generate(validCommand());

        assertEquals(
                ProviderFailureClassification.NONE,
                report.failureClassificationSummary().classification());
        assertEquals(
                "failure:none",
                report.failureClassificationSummary().summaryRef());
    }

    @Test
    void reportIncludesLatencyBudgetSummary() {
        final ModelGatewayObservabilityReport report =
                new ObservabilityReportService().generate(validCommand());

        assertEquals(20L, report.latencyBudgetReport().p50Ms());
        assertEquals(60L, report.latencyBudgetReport().p95Ms());
        assertEquals(90L, report.latencyBudgetReport().p99Ms());
        assertEquals(0.45d, report.latencyBudgetReport().budgetUsedRatio());
    }

    @Test
    void reportIncludesTrustDecisionSummary() {
        final ModelGatewayObservabilityReport report =
                new ObservabilityReportService().generate(validCommand());

        assertEquals(
                ProviderTrustDecisionSummary.Decision.ALLOWED,
                report.trustDecisionReport().decision());
        assertEquals("trust-ref-a", report.trustDecisionReport().decisionRef());
    }

    @Test
    void reportIncludesReadinessFindingSummary() {
        final ModelGatewayObservabilityReport report =
                new ObservabilityReportService().generate(commandWithReadinessResult(
                        ProviderReadinessEvaluationResult.failClosed(
                                ProviderReadinessDecision.DEGRADED,
                                ProviderReadinessDecisionReason.TIMEOUT,
                                POLICY_VERSION,
                                TRACE_ID,
                                SOURCE_REQUEST_ID,
                                PROVIDER_REF)));

        assertEquals(ProviderReadinessAcceptanceStatus.WARN, report.acceptanceStatus());
        assertEquals("timeout", report.providerReadiness().readinessFinding());
        assertEquals(1, report.providerReadiness().findings().size());
    }

    @Test
    void reportIncludesAcceptanceStatus() {
        final ModelGatewayObservabilityReport report =
                new ObservabilityReportService().generate(validCommand());

        assertEquals(ProviderReadinessAcceptanceStatus.PASS, report.acceptanceStatus());
        assertEquals(
                ProviderReadinessAcceptanceStatus.PASS,
                report.acceptanceEvidence().acceptanceStatus());
    }

    @Test
    void reportDoesNotExposeRawPrompt() {
        final ModelGatewayObservabilityReport report =
                new ObservabilityReportService().generate(commandWithSourceRef("rawPrompt:value"));

        assertEquals(ProviderReadinessAcceptanceStatus.FAIL, report.acceptanceStatus());
        assertFalse(report.toString().contains("rawPrompt"));
        assertFalse(report.toString().contains("value"));
    }

    @Test
    void reportDoesNotExposeRawProviderResponse() {
        final ModelGatewayObservabilityReport report =
                new ObservabilityReportService().generate(commandWithSourceRef("rawProviderResponse:value"));

        assertEquals(ProviderReadinessAcceptanceStatus.FAIL, report.acceptanceStatus());
        assertFalse(report.toString().contains("rawProviderResponse"));
        assertFalse(report.toString().contains("value"));
    }

    @Test
    void reportDoesNotExposeCredentialLikeFields() {
        final ModelGatewayObservabilityReport report =
                new ObservabilityReportService().generate(commandWithSourceRef("apiKey=ABC123"));
        final String rendered = report.toString();

        assertEquals(ProviderReadinessAcceptanceStatus.FAIL, report.acceptanceStatus());
        assertFalse(rendered.contains("ABC123"));
        assertFalse(rendered.contains("credential"));
        assertFalse(rendered.contains("apiKey"));
        assertFalse(rendered.contains("apiSecret"));
        assertFalse(rendered.contains("passphrase"));
        assertFalse(rendered.contains("token"));
        assertFalse(rendered.contains("cookie"));
        assertFalse(rendered.contains("secret"));
    }

    @Test
    void passDoesNotImplyProviderAuthorization() {
        final ModelGatewayObservabilityReport report =
                new ObservabilityReportService().generate(validCommand());

        assertEquals(ProviderReadinessAcceptanceStatus.PASS, report.acceptanceStatus());
        assertFalse(report.acceptanceStatus().authorizesProvider());
        assertFalse(report.toString().contains("providerAuthorization"));
    }

    @Test
    void passDoesNotImplyRealHttpRealProviderOrLive() {
        final ProviderReadinessAcceptanceStatus status =
                new ObservabilityReportService().generate(validCommand()).acceptanceStatus();

        assertEquals(ProviderReadinessAcceptanceStatus.PASS, status);
        assertFalse(status.enablesRealHttp());
        assertFalse(status.enablesRealProvider());
        assertFalse(status.enablesLive());
    }

    @Test
    void reportDoesNotImplyTradingPermission() {
        final ModelGatewayObservabilityReport report =
                new ObservabilityReportService().generate(validCommand());

        assertFalse(report.acceptanceStatus().allowsTrading());
        assertFalse(report.acceptanceStatus().allowsNqExecution());
        assertFalse(report.toString().contains("tradingPermission"));
        assertFalse(report.toString().contains("tradingSignal"));
    }

    @Test
    void buySellMarketOrderInputFailsClosed() {
        final ModelGatewayObservabilityReport buyReport =
                new ObservabilityReportService().generate(commandWithSourceRef("BUY"));
        final ModelGatewayObservabilityReport sellReport =
                new ObservabilityReportService().generate(commandWithSourceRef("SELL"));
        final ModelGatewayObservabilityReport marketOrderReport =
                new ObservabilityReportService().generate(commandWithSourceRef("MARKET_ORDER"));

        assertEquals(ProviderReadinessAcceptanceStatus.FAIL, buyReport.acceptanceStatus());
        assertEquals(ProviderReadinessAcceptanceStatus.FAIL, sellReport.acceptanceStatus());
        assertEquals(ProviderReadinessAcceptanceStatus.FAIL, marketOrderReport.acceptanceStatus());
    }

    @Test
    void placeCancelMutateNqStateInputFailsClosed() {
        final ModelGatewayObservabilityReport placeReport =
                new ObservabilityReportService().generate(commandWithSourceRef("PLACE_ORDER"));
        final ModelGatewayObservabilityReport cancelReport =
                new ObservabilityReportService().generate(commandWithSourceRef("CANCEL_ORDER"));
        final ModelGatewayObservabilityReport mutateReport =
                new ObservabilityReportService().generate(commandWithSourceRef("MUTATE_NQ_STATE"));

        assertEquals(ProviderReadinessAcceptanceStatus.FAIL, placeReport.acceptanceStatus());
        assertEquals(ProviderReadinessAcceptanceStatus.FAIL, cancelReport.acceptanceStatus());
        assertEquals(ProviderReadinessAcceptanceStatus.FAIL, mutateReport.acceptanceStatus());
    }

    @Test
    void providerSdkHttpAgentAndLangGraphClassesAreNotIntroduced() throws IOException {
        final Path gatewayPackage =
                moduleRoot().resolve("src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway");

        for (final String fileName : B4_MAIN_FILES) {
            final String source = Files.readString(gatewayPackage.resolve(fileName));
            for (final Pattern pattern : FORBIDDEN_RUNTIME_PATTERNS) {
                assertFalse(pattern.matcher(source).find(), fileName + " must not introduce " + pattern);
            }
        }
    }

    @Test
    void reportGenerationFailureFailsClosed() {
        final ModelGatewayObservabilityReport report =
                new ObservabilityReportService().generate(commandWithSummary(summaryWithLatency(
                        new ProviderLatencyBudgetSummary(20L, 60L, 90L, 200L, 2.0d, 15L))));

        assertEquals(ProviderReadinessAcceptanceStatus.FAIL, report.acceptanceStatus());
        assertEquals(ProviderReadinessDecision.NOT_READY, report.providerReadiness().readinessDecision());
        assertTrue(report.redactedSummary().contains("fail-closed"));
    }

    private static ObservabilityReportCommand validCommand() {
        return commandWithSummary(validSummary());
    }

    private static ObservabilityReportCommand commandWithSummary(
            final ModelGatewayObservabilitySummary summary) {
        final ProviderHealthReadModelView healthView = validHealthView();
        return new ObservabilityReportCommand(
                summary.tenantId(),
                SOURCE_REF,
                summary,
                healthView,
                validReadinessResult(healthView),
                NOW,
                NOW);
    }

    private static ObservabilityReportCommand commandWithSourceRef(final String sourceRef) {
        final ProviderHealthReadModelView healthView = validHealthView();
        return new ObservabilityReportCommand(
                TENANT_ID,
                sourceRef,
                validSummary(),
                healthView,
                validReadinessResult(healthView),
                NOW,
                NOW);
    }

    private static ObservabilityReportCommand commandWithReadinessResult(
            final ProviderReadinessEvaluationResult readinessResult) {
        final ProviderHealthReadModelView healthView = validHealthView();
        return new ObservabilityReportCommand(
                TENANT_ID,
                SOURCE_REF,
                validSummary(),
                healthView,
                readinessResult,
                NOW,
                NOW);
    }

    private static ProviderReadinessEvaluationResult validReadinessResult(
            final ProviderHealthReadModelView healthView) {
        return ProviderReadinessEvaluationResult.ready(new ProviderReadinessEvaluationCommand(
                TENANT_ID,
                SOURCE_REF,
                PROVIDER_REF,
                GATEWAY_VERSION_REF,
                HASH,
                ProviderFailureClassification.NONE,
                latencyBudgetSummary(),
                ProviderTrustDecisionSummary.allowed("trust-ref-a"),
                ProviderReadinessSignal.ready("readiness-ref-a"),
                healthView,
                POLICY_VERSION,
                TRACE_ID,
                SOURCE_REQUEST_ID,
                NOW,
                NOW));
    }

    private static ProviderHealthReadModelView validHealthView() {
        return new ProviderHealthReadModelService(List.of(validSummary()))
                .findProviderHealth(new ProviderHealthReadModelQuery(
                        TENANT_ID,
                        PROVIDER_REF,
                        GATEWAY_VERSION_REF,
                        TRACE_ID,
                        SOURCE_REQUEST_ID,
                        null,
                        null,
                        ProviderReadinessStatus.READY,
                        null,
                        null,
                        null,
                        null,
                        50,
                        0))
                .getFirst();
    }

    private static ModelGatewayObservabilitySummary validSummary() {
        return new ModelGatewayObservabilitySummary(
                TENANT_ID,
                TRACE_ID,
                SOURCE_REQUEST_ID,
                GATEWAY_VERSION_REF,
                PROVIDER_REF,
                HASH,
                latencyBudgetSummary(),
                ProviderFailureClassification.NONE,
                ProviderTrustDecisionSummary.allowed("trust-ref-a"),
                ProviderReadinessSignal.ready("readiness-ref-a"),
                NOW);
    }

    private static ModelGatewayObservabilitySummary summaryWithoutTenant() {
        return new ModelGatewayObservabilitySummary(
                null,
                TRACE_ID,
                SOURCE_REQUEST_ID,
                GATEWAY_VERSION_REF,
                PROVIDER_REF,
                HASH,
                latencyBudgetSummary(),
                ProviderFailureClassification.NONE,
                ProviderTrustDecisionSummary.allowed("trust-ref-a"),
                ProviderReadinessSignal.ready("readiness-ref-a"),
                NOW);
    }

    private static ModelGatewayObservabilitySummary summaryWithoutProvider() {
        return new ModelGatewayObservabilitySummary(
                TENANT_ID,
                TRACE_ID,
                SOURCE_REQUEST_ID,
                GATEWAY_VERSION_REF,
                null,
                HASH,
                latencyBudgetSummary(),
                ProviderFailureClassification.NONE,
                ProviderTrustDecisionSummary.allowed("trust-ref-a"),
                ProviderReadinessSignal.ready("readiness-ref-a"),
                NOW);
    }

    private static ModelGatewayObservabilitySummary summaryWithLatency(
            final ProviderLatencyBudgetSummary latencyBudgetSummary) {
        return new ModelGatewayObservabilitySummary(
                TENANT_ID,
                TRACE_ID,
                SOURCE_REQUEST_ID,
                GATEWAY_VERSION_REF,
                PROVIDER_REF,
                HASH,
                latencyBudgetSummary,
                ProviderFailureClassification.NONE,
                ProviderTrustDecisionSummary.allowed("trust-ref-a"),
                ProviderReadinessSignal.ready("readiness-ref-a"),
                NOW);
    }

    private static ProviderLatencyBudgetSummary latencyBudgetSummary() {
        return new ProviderLatencyBudgetSummary(20L, 60L, 90L, 200L, 0.45d, 15L);
    }

    private static Path moduleRoot() {
        final Path cwd = Path.of("").toAbsolutePath();
        if (Files.isDirectory(cwd.resolve("src/main/java"))) {
            return cwd;
        }
        return cwd.resolve("dh-usecase");
    }
}

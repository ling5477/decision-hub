package com.guidinglight.decisionhub.usecase.qdr.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Stage-QDR-5 B3 Provider Readiness Guard / Policy Evaluation 回归测试。
 *
 * <p>测试只使用 B1/B2 safe contracts 与内存 evidence；不新增 API、migration、repository/JDBC、
 * provider、HTTP、Provider SDK、Agent、LangGraph、NQ 或 LIVE 能力。
 */
final class ProviderReadinessGuardServiceTest {

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
    private static final List<String> B3_MAIN_FILES = List.of(
            "ProviderReadinessPolicy.java",
            "ProviderReadinessGuard.java",
            "ProviderReadinessEvaluationCommand.java",
            "ProviderReadinessEvaluationResult.java",
            "ProviderReadinessDecision.java",
            "ProviderReadinessDecisionReason.java",
            "ProviderReadinessGuardService.java");
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
    void validReadinessPolicyEvaluatesReadyInMockSafeContext() {
        final ProviderReadinessEvaluationResult result =
                new ProviderReadinessGuardService().evaluate(validCommand());

        assertEquals(ProviderReadinessDecision.READY, result.decision());
        assertEquals(ProviderReadinessDecisionReason.READY_SAFE_CONTEXT, result.reason());
        assertEquals(ProviderReadinessStatus.READY, result.readinessSignal().status());
    }

    @Test
    void missingTenantIdReturnsNotReadyFailClosed() {
        final ProviderReadinessEvaluationResult result =
                new ProviderReadinessGuardService().evaluate(commandWithTenant(null));

        assertEquals(ProviderReadinessDecision.NOT_READY, result.decision());
        assertEquals(ProviderReadinessDecisionReason.MISSING_TENANT, result.reason());
    }

    @Test
    void missingProviderRefReturnsNotReadyFailClosed() {
        final ProviderReadinessEvaluationResult result =
                new ProviderReadinessGuardService().evaluate(commandWithProvider(null));

        assertEquals(ProviderReadinessDecision.NOT_READY, result.decision());
        assertEquals(ProviderReadinessDecisionReason.MISSING_PROVIDER_REF, result.reason());
    }

    @Test
    void missingPolicyVersionReturnsSkippedFailClosed() {
        final ProviderReadinessEvaluationResult result =
                new ProviderReadinessGuardService().evaluate(commandWithPolicyVersion(null));

        assertEquals(ProviderReadinessDecision.SKIPPED, result.decision());
        assertEquals(ProviderReadinessDecisionReason.MISSING_POLICY_VERSION, result.reason());
    }

    @Test
    void sourceDeniedReturnsNotReady() {
        final ProviderReadinessEvaluationResult result =
                new ProviderReadinessGuardService().evaluate(commandWithSourceRef("source-denied"));

        assertEquals(ProviderReadinessDecision.NOT_READY, result.decision());
        assertEquals(ProviderReadinessDecisionReason.SOURCE_DENIED, result.reason());
    }

    @Test
    void policyDeniedReturnsNotReady() {
        final ProviderReadinessEvaluationResult result =
                new ProviderReadinessGuardService().evaluate(commandWithTrustDecision(
                        ProviderTrustDecisionSummary.denied("trust-denied")));

        assertEquals(ProviderReadinessDecision.NOT_READY, result.decision());
        assertEquals(ProviderReadinessDecisionReason.POLICY_DENIED, result.reason());
    }

    @Test
    void timeoutClassificationReturnsDegraded() {
        final ProviderReadinessEvaluationResult result =
                new ProviderReadinessGuardService().evaluate(commandWithClassification(
                        ProviderFailureClassification.TIMEOUT));

        assertEquals(ProviderReadinessDecision.DEGRADED, result.decision());
        assertEquals(ProviderReadinessDecisionReason.TIMEOUT, result.reason());
    }

    @Test
    void budgetExceededReturnsDegraded() {
        final ProviderReadinessEvaluationResult result =
                new ProviderReadinessGuardService().evaluate(commandWithClassification(
                        ProviderFailureClassification.BUDGET_EXCEEDED));

        assertEquals(ProviderReadinessDecision.DEGRADED, result.decision());
        assertEquals(ProviderReadinessDecisionReason.BUDGET_EXCEEDED, result.reason());
    }

    @Test
    void unknownClassificationReturnsNotReady() {
        final ProviderReadinessEvaluationResult result =
                new ProviderReadinessGuardService().evaluate(commandWithClassification(
                        ProviderFailureClassification.UNKNOWN));

        assertEquals(ProviderReadinessDecision.NOT_READY, result.decision());
        assertEquals(ProviderReadinessDecisionReason.UNKNOWN_CLASSIFICATION, result.reason());
    }

    @Test
    void credentialLikeInputFailsClosedWithoutLeakingValue() {
        final ProviderReadinessEvaluationResult result =
                new ProviderReadinessGuardService().evaluate(commandWithSourceRef("apiKey=ABC123"));

        assertEquals(ProviderReadinessDecision.NOT_READY, result.decision());
        assertEquals(ProviderReadinessDecisionReason.SENSITIVE_INPUT_REJECTED, result.reason());
        assertFalse(result.toString().contains("ABC123"));
    }

    @Test
    void rawPromptInputFailsClosed() {
        final ProviderReadinessEvaluationResult result =
                new ProviderReadinessGuardService().evaluate(commandWithTraceId("rawPrompt:value"));

        assertEquals(ProviderReadinessDecision.NOT_READY, result.decision());
        assertEquals(ProviderReadinessDecisionReason.RAW_PROMPT_REJECTED, result.reason());
    }

    @Test
    void rawProviderResponseInputFailsClosed() {
        final ProviderReadinessEvaluationResult result =
                new ProviderReadinessGuardService().evaluate(commandWithSourceRequestId(
                        "rawProviderResponse:value"));

        assertEquals(ProviderReadinessDecision.NOT_READY, result.decision());
        assertEquals(ProviderReadinessDecisionReason.RAW_PROVIDER_RESPONSE_REJECTED, result.reason());
    }

    @Test
    void buySellMarketOrderInputFailsClosed() {
        final ProviderReadinessEvaluationResult buyResult =
                new ProviderReadinessGuardService().evaluate(commandWithProvider("BUY"));
        final ProviderReadinessEvaluationResult sellResult =
                new ProviderReadinessGuardService().evaluate(commandWithProvider("SELL"));
        final ProviderReadinessEvaluationResult marketOrderResult =
                new ProviderReadinessGuardService().evaluate(commandWithProvider("MARKET_ORDER"));

        assertEquals(ProviderReadinessDecisionReason.TRADING_INPUT_REJECTED, buyResult.reason());
        assertEquals(ProviderReadinessDecisionReason.TRADING_INPUT_REJECTED, sellResult.reason());
        assertEquals(ProviderReadinessDecisionReason.TRADING_INPUT_REJECTED, marketOrderResult.reason());
    }

    @Test
    void placeCancelMutateNqStateInputFailsClosed() {
        final ProviderReadinessEvaluationResult placeResult =
                new ProviderReadinessGuardService().evaluate(commandWithSourceRef("PLACE_ORDER"));
        final ProviderReadinessEvaluationResult cancelResult =
                new ProviderReadinessGuardService().evaluate(commandWithSourceRef("CANCEL_ORDER"));
        final ProviderReadinessEvaluationResult mutateResult =
                new ProviderReadinessGuardService().evaluate(commandWithSourceRef("MUTATE_NQ_STATE"));

        assertEquals(ProviderReadinessDecisionReason.NQ_MUTATION_REJECTED, placeResult.reason());
        assertEquals(ProviderReadinessDecisionReason.NQ_MUTATION_REJECTED, cancelResult.reason());
        assertEquals(ProviderReadinessDecisionReason.NQ_MUTATION_REJECTED, mutateResult.reason());
    }

    @Test
    void readyDoesNotEnableRealProviderHttpLiveOrTrading() {
        final ProviderReadinessEvaluationResult result =
                new ProviderReadinessGuardService().evaluate(validCommand());

        assertEquals(ProviderReadinessDecision.READY, result.decision());
        assertFalse(result.decision().enablesRealProvider());
        assertFalse(result.decision().enablesRealHttp());
        assertFalse(result.decision().enablesLive());
        assertFalse(result.decision().allowsTrading());
    }

    @Test
    void resultDoesNotExposeAuthorizationLiveOrTradingSignalFields() {
        final ProviderReadinessEvaluationResult result =
                new ProviderReadinessGuardService().evaluate(validCommand());
        final String rendered = result.toString();

        assertFalse(rendered.contains("realProviderEnabled"));
        assertFalse(rendered.contains("realHttpEnabled"));
        assertFalse(rendered.contains("liveEnabled"));
        assertFalse(rendered.contains("tradingPermission"));
        assertFalse(rendered.contains("providerAuthorization"));
        assertFalse(rendered.contains("tradingSignal"));
    }

    @Test
    void providerSdkHttpAgentAndLangGraphClassesAreNotIntroduced() throws IOException {
        final Path gatewayPackage =
                moduleRoot().resolve("src/main/java/com/guidinglight/decisionhub/usecase/qdr/gateway");

        for (final String fileName : B3_MAIN_FILES) {
            final String source = Files.readString(gatewayPackage.resolve(fileName));
            for (final Pattern pattern : FORBIDDEN_RUNTIME_PATTERNS) {
                assertFalse(pattern.matcher(source).find(), fileName + " must not introduce " + pattern);
            }
        }
    }

    @Test
    void policyEvaluationFailureFailsClosed() {
        final ProviderReadinessGuardService service =
                new ProviderReadinessGuardService(command -> {
                    throw new IllegalStateException("policy evaluator down");
                });

        final ProviderReadinessEvaluationResult result = service.evaluate(validCommand());

        assertEquals(ProviderReadinessDecision.NOT_READY, result.decision());
        assertEquals(ProviderReadinessDecisionReason.INTERNAL_ERROR, result.reason());
        assertFalse(result.toString().contains("policy evaluator down"));
    }

    @Test
    void policyReturningUnsafeReadyReasonFailsClosed() {
        final ProviderReadinessGuardService service =
                new ProviderReadinessGuardService(command -> ProviderReadinessEvaluationResult.failClosed(
                        ProviderReadinessDecision.READY,
                        ProviderReadinessDecisionReason.POLICY_DENIED,
                        POLICY_VERSION,
                        TRACE_ID,
                        SOURCE_REQUEST_ID,
                        PROVIDER_REF));

        final ProviderReadinessEvaluationResult result = service.evaluate(validCommand());

        assertEquals(ProviderReadinessDecision.NOT_READY, result.decision());
        assertEquals(ProviderReadinessDecisionReason.POLICY_EVIDENCE_REJECTED, result.reason());
    }

    private static ProviderReadinessEvaluationCommand validCommand() {
        return new ProviderReadinessEvaluationCommand(
                TENANT_ID,
                SOURCE_REF,
                PROVIDER_REF,
                GATEWAY_VERSION_REF,
                HASH,
                ProviderFailureClassification.NONE,
                latencyBudgetSummary(),
                ProviderTrustDecisionSummary.allowed("trust-ref-a"),
                ProviderReadinessSignal.ready("readiness-ref-a"),
                healthView(),
                POLICY_VERSION,
                TRACE_ID,
                SOURCE_REQUEST_ID,
                NOW,
                NOW);
    }

    private static ProviderReadinessEvaluationCommand commandWithTenant(final String tenantId) {
        final ProviderReadinessEvaluationCommand base = validCommand();
        return command(
                tenantId,
                base.sourceRef(),
                base.providerRef(),
                base.failureClassification(),
                base.latencyBudgetSummary(),
                base.trustDecisionSummary(),
                base.policyVersion(),
                base.traceId(),
                base.sourceRequestId());
    }

    private static ProviderReadinessEvaluationCommand commandWithProvider(final String providerRef) {
        final ProviderReadinessEvaluationCommand base = validCommand();
        return command(
                base.tenantId(),
                base.sourceRef(),
                providerRef,
                base.failureClassification(),
                base.latencyBudgetSummary(),
                base.trustDecisionSummary(),
                base.policyVersion(),
                base.traceId(),
                base.sourceRequestId());
    }

    private static ProviderReadinessEvaluationCommand commandWithPolicyVersion(
            final String policyVersion) {
        final ProviderReadinessEvaluationCommand base = validCommand();
        return command(
                base.tenantId(),
                base.sourceRef(),
                base.providerRef(),
                base.failureClassification(),
                base.latencyBudgetSummary(),
                base.trustDecisionSummary(),
                policyVersion,
                base.traceId(),
                base.sourceRequestId());
    }

    private static ProviderReadinessEvaluationCommand commandWithSourceRef(final String sourceRef) {
        final ProviderReadinessEvaluationCommand base = validCommand();
        return command(
                base.tenantId(),
                sourceRef,
                base.providerRef(),
                base.failureClassification(),
                base.latencyBudgetSummary(),
                base.trustDecisionSummary(),
                base.policyVersion(),
                base.traceId(),
                base.sourceRequestId());
    }

    private static ProviderReadinessEvaluationCommand commandWithTrustDecision(
            final ProviderTrustDecisionSummary trustDecision) {
        final ProviderReadinessEvaluationCommand base = validCommand();
        return command(
                base.tenantId(),
                base.sourceRef(),
                base.providerRef(),
                base.failureClassification(),
                base.latencyBudgetSummary(),
                trustDecision,
                base.policyVersion(),
                base.traceId(),
                base.sourceRequestId());
    }

    private static ProviderReadinessEvaluationCommand commandWithClassification(
            final ProviderFailureClassification classification) {
        final ProviderReadinessEvaluationCommand base = validCommand();
        return command(
                base.tenantId(),
                base.sourceRef(),
                base.providerRef(),
                classification,
                base.latencyBudgetSummary(),
                base.trustDecisionSummary(),
                base.policyVersion(),
                base.traceId(),
                base.sourceRequestId());
    }

    private static ProviderReadinessEvaluationCommand commandWithTraceId(final String traceId) {
        final ProviderReadinessEvaluationCommand base = validCommand();
        return command(
                base.tenantId(),
                base.sourceRef(),
                base.providerRef(),
                base.failureClassification(),
                base.latencyBudgetSummary(),
                base.trustDecisionSummary(),
                base.policyVersion(),
                traceId,
                base.sourceRequestId());
    }

    private static ProviderReadinessEvaluationCommand commandWithSourceRequestId(
            final String sourceRequestId) {
        final ProviderReadinessEvaluationCommand base = validCommand();
        return command(
                base.tenantId(),
                base.sourceRef(),
                base.providerRef(),
                base.failureClassification(),
                base.latencyBudgetSummary(),
                base.trustDecisionSummary(),
                base.policyVersion(),
                base.traceId(),
                sourceRequestId);
    }

    private static ProviderReadinessEvaluationCommand command(
            final String tenantId,
            final String sourceRef,
            final String providerRef,
            final ProviderFailureClassification classification,
            final ProviderLatencyBudgetSummary latencyBudget,
            final ProviderTrustDecisionSummary trustDecision,
            final String policyVersion,
            final String traceId,
            final String sourceRequestId) {
        return new ProviderReadinessEvaluationCommand(
                tenantId,
                sourceRef,
                providerRef,
                GATEWAY_VERSION_REF,
                HASH,
                classification,
                latencyBudget,
                trustDecision,
                ProviderReadinessSignal.ready("readiness-ref-a"),
                null,
                policyVersion,
                traceId,
                sourceRequestId,
                NOW,
                NOW);
    }

    private static ProviderHealthReadModelView healthView() {
        final ModelGatewayObservabilitySummary summary = new ModelGatewayObservabilitySummary(
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
        return new ProviderHealthReadModelService(List.of(summary))
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

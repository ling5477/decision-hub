package com.guidinglight.decisionhub.usecase.decision.integration1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import com.guidinglight.decisionhub.domain.decision.ForbiddenAction;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import com.guidinglight.decisionhub.usecase.decision.DecisionContext;
import com.guidinglight.decisionhub.usecase.decision.DecisionOrchestrator;
import com.guidinglight.decisionhub.usecase.decision.DecisionSignalProvider;
import com.guidinglight.decisionhub.usecase.decision.DecisionSignalResult;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionContextBuilder;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionOrchestrator;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionPolicyChecker;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionProviderBudgetGuard;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionProviderGuard;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionProviderHealthEvaluator;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionProviderLatencyRecorder;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionRiskReviewer;
import com.guidinglight.decisionhub.usecase.decision.DecisionOutputAssembler;
import com.guidinglight.decisionhub.usecase.decision.MockDecisionSignalProvider;
import com.guidinglight.decisionhub.usecase.decision.support.RecordingDecisionAuditReplayRepository;
import com.guidinglight.decisionhub.usecase.decision.integration1.support.DhDryRunTestSupportEntry;
import com.guidinglight.decisionhub.usecase.decision.integration1.support.DhDryRunTestSupportEntry.Exchange;
import com.guidinglight.decisionhub.usecase.decision.integration1.support.DhDryRunTestSupportEntry.InMemoryNonceStore;
import com.guidinglight.decisionhub.usecase.decision.integration1.support.DhDryRunTestSupportEntry.Result;
import com.guidinglight.decisionhub.usecase.decision.integration1.support.DhDryRunTestSupportEntry.Step;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * IMP1 DH dry-run test-support entry 验证。
 *
 * <p>本测试只覆盖 mock-only validation harness，不创建 runtime endpoint、Controller、fixture JSON、真实 HTTP、
 * real provider、Agent/LangGraph runtime 或 LIVE 能力。
 */
final class DhDryRunTestSupportEntryTest {

    private static final Instant NOW = Instant.parse("2026-07-03T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void validMockOnlyDryRunRequestPassesValidationChainInOrder() {
        final Result result = defaultEntry().validate(validExchange("nonce-imp1-valid"));

        assertTrue(result.accepted());
        assertEquals(200, result.statusCode());
        assertIterableEquals(List.of(Step.values()), result.steps());
        assertReadonlyOutput(result.output());
        assertEquals(DecisionAction.NO_TRADE, result.output().getAction());
        assertSafeSummary(result);
    }

    @Test
    void missingRequiredHeaderFailsClosed() {
        final Exchange exchange = withoutHeader(validExchange("nonce-imp1-missing-header"),
                DhDryRunTestSupportEntry.HEADER_SIGNATURE);

        final Result result = defaultEntry().validate(exchange);

        assertRejected(result, 401, "MISSING_CANONICAL_HEADER");
        assertTrue(result.steps().contains(Step.CANONICAL_HEADER_PRESENCE));
        assertFalse(result.steps().contains(Step.HMAC_VALUE_BASED_SIGNATURE_VALIDATION));
    }

    @Test
    void invalidTimestampFailsClosed() {
        final Exchange exchange =
                withHeader(validExchange("nonce-imp1-invalid-ts"),
                        DhDryRunTestSupportEntry.HEADER_TIMESTAMP,
                        "2026-07-03T08:00:00+08:00");

        final Result result = defaultEntry().validate(resigned(exchange));

        assertRejected(result, 401, "TIMESTAMP_INVALID");
        assertTrue(result.steps().contains(Step.TIMESTAMP_UTC_Z_VALIDATION));
    }

    @Test
    void nonceReplayFailsClosed() {
        final DhDryRunTestSupportEntry entry = defaultEntry();
        final Exchange exchange = validExchange("nonce-imp1-replay");

        assertTrue(entry.validate(exchange).accepted());
        final Result replay = entry.validate(exchange);

        assertRejected(replay, 409, "NONCE_REPLAY");
        assertTrue(replay.steps().contains(Step.NONCE_REPLAY_FAIL_CLOSED));
    }

    @Test
    void invalidHmacFailsClosed() {
        final Exchange exchange =
                withHeader(validExchange("nonce-imp1-bad-signature"),
                        DhDryRunTestSupportEntry.HEADER_SIGNATURE,
                        "bad-signature");

        final Result result = defaultEntry().validate(exchange);

        assertRejected(result, 401, "SIGNATURE_INVALID");
        assertTrue(result.steps().contains(Step.HMAC_VALUE_BASED_SIGNATURE_VALIDATION));
    }

    @Test
    void reviewGatedNqDryRunSourceIsDeniedBeforeRuntimePath() {
        final ObjectNode payload = (ObjectNode) DhDryRunTestSupportEntry.validPayload().deepCopy();
        payload.put("source", DhDryRunTestSupportEntry.REVIEW_GATED_SOURCE);
        final Exchange exchange =
                DhDryRunTestSupportEntry.signedExchange(
                        DhDryRunTestSupportEntry.REVIEW_GATED_SOURCE, payload, "nonce-imp1-source-denied", NOW);

        final Result result = defaultEntry().validate(exchange);

        assertRejected(result, 403, "SOURCE_DENIED");
        assertTrue(result.steps().contains(Step.SOURCE_ALLOWLIST_GUARD));
        assertFalse(result.steps().contains(Step.DECISION_ORCHESTRATOR_MOCK_ONLY_BOUNDARY));
    }

    @Test
    void forbiddenCredentialOrderAccountQuantityBuySellFieldsFailClosed() {
        for (Map.Entry<String, String> forbidden : forbiddenMutations().entrySet()) {
            final ObjectNode payload = (ObjectNode) DhDryRunTestSupportEntry.validPayload().deepCopy();
            payload.put(forbidden.getKey(), forbidden.getValue());
            final Result result =
                    defaultEntry()
                            .validate(
                                    DhDryRunTestSupportEntry.signedExchange(
                                            DhDryRunTestSupportEntry.TEST_SOURCE,
                                            payload,
                                            "nonce-imp1-forbidden-" + forbidden.getKey(),
                                            NOW));

            assertRejected(result, 400, "FORBIDDEN_FIELD");
            assertTrue(result.steps().contains(Step.FORBIDDEN_FIELDS_VALIDATION));
        }
    }

    @Test
    void longAndShortBiasRemainReadonlyBiasNotTradingAction() {
        for (DecisionAction action : List.of(DecisionAction.LONG_BIAS, DecisionAction.SHORT_BIAS)) {
            final Result result =
                    entryWithSignal(DecisionSignalResult.mock(action, List.of("MOCK_" + action.name())))
                            .validate(validExchange("nonce-imp1-" + action.name().toLowerCase()));

            assertTrue(result.accepted());
            assertEquals(action, result.output().getAction());
            assertReadonlyOutput(result.output());
            assertFalse(result.output().getReasonCodes().contains("BUY"));
            assertFalse(result.output().getReasonCodes().contains("SELL"));
        }
    }

    @Test
    void unknownInternalErrorNeverUpgradesToDirectionalBias() {
        final DecisionOrchestrator throwingOrchestrator =
                request -> {
                    throw new IllegalStateException("unknown mock-only failure");
                };

        final Result result =
                new DhDryRunTestSupportEntry(throwingOrchestrator, new InMemoryNonceStore(), CLOCK)
                        .validate(validExchange("nonce-imp1-unknown-error"));

        assertRejected(result, 500, "INTERNAL_FAIL_CLOSED");
        assertEquals(DecisionAction.ABSTAIN, result.output().getAction());
        assertFalse(result.output().getAction() == DecisionAction.LONG_BIAS);
        assertFalse(result.output().getAction() == DecisionAction.SHORT_BIAS);
    }

    @Test
    void providerGuardDisabledFailsClosedWithoutProviderRuntime() {
        final RecordingDecisionAuditReplayRepository repository = new RecordingDecisionAuditReplayRepository();
        final DhDryRunTestSupportEntry entry =
                new DhDryRunTestSupportEntry(
                        orchestrator(
                                repository,
                                DecisionSignalResult.mock(DecisionAction.LONG_BIAS, List.of("MOCK_LONG_BIAS")),
                                false),
                        new InMemoryNonceStore(),
                        CLOCK);

        final Result result = entry.validate(validExchange("nonce-imp1-provider-disabled"));

        assertTrue(result.accepted());
        assertEquals(200, result.statusCode());
        assertEquals("OK", result.errorCode());
        assertEquals(DecisionAction.ABSTAIN, result.output().getAction());
        assertEquals(ProviderSignalStatus.DISABLED, result.output().getProviderStatus());
        assertReadonlyOutput(result.output());
        assertTrue(result.steps().contains(Step.PROVIDER_GUARD_MOCK_ONLY_BOUNDARY));
        assertEquals(1, repository.providerCallCount());
        assertSafeSummary(result);
    }

    @Test
    void auditTraceReplaySummaryDoesNotRecordCredentialOrRawSecret() {
        final Result result = defaultEntry().validate(validExchange("nonce-imp1-safe-summary"));

        assertTrue(result.accepted());
        assertSafeSummary(result);
        assertFalse(result.safeSummary().conciseText().contains(DhDryRunTestSupportEntry.TEST_SECRET));
    }

    @Test
    void dryRunRuntimeEndpointControllerRealProviderAgentAndLangGraphAreNotAdded() throws Exception {
        assertNoProductionToken(List.of("dh-api/src/main/java", "dh-app/src/main/java"), "NQ_DRYRUN");
        assertNoProductionToken(List.of("dh-api/src/main/java", "dh-app/src/main/java"), "dry-run");
        assertNoProductionToken(List.of("dh-api/src/main/java", "dh-app/src/main/java"), "dryrun");
        assertNoProductionToken(List.of("dh-api/src/main/java", "dh-app/src/main/java"), "RealNqDryRun");
        assertNoProductionToken(List.of("dh-api/src/main/java", "dh-app/src/main/java"), "OpenAI");
        assertNoProductionToken(List.of("dh-api/src/main/java", "dh-app/src/main/java"), "Claude");
        assertNoProductionToken(List.of("dh-api/src/main/java", "dh-app/src/main/java"), "Gemini");
        assertNoProductionToken(List.of("dh-api/src/main/java", "dh-app/src/main/java"), "@PostMapping(\"/dry");
        assertNoProductionToken(List.of("dh-api/src/main/java", "dh-app/src/main/java"), "@RequestMapping(\"/dry");
    }

    private static DhDryRunTestSupportEntry defaultEntry() {
        return new DhDryRunTestSupportEntry(
                new DefaultDecisionOrchestrator(
                        new DefaultDecisionContextBuilder(),
                        new DefaultDecisionPolicyChecker(),
                        new MockDecisionSignalProvider(),
                        new DefaultDecisionRiskReviewer(),
                        new DecisionOutputAssembler(),
                        new RecordingDecisionAuditReplayRepository(),
                        CLOCK),
                new InMemoryNonceStore(),
                CLOCK);
    }

    private static DhDryRunTestSupportEntry entryWithSignal(final DecisionSignalResult signal) {
        return new DhDryRunTestSupportEntry(
                orchestrator(new RecordingDecisionAuditReplayRepository(), signal, true),
                new InMemoryNonceStore(),
                CLOCK);
    }

    private static DecisionOrchestrator orchestrator(
            final RecordingDecisionAuditReplayRepository repository,
            final DecisionSignalResult signal,
            final boolean providerEnabled) {
        return new DefaultDecisionOrchestrator(
                new DefaultDecisionContextBuilder(),
                new DefaultDecisionPolicyChecker(),
                new StaticSignalProvider(signal),
                new DefaultDecisionRiskReviewer(),
                new DecisionOutputAssembler(),
                repository,
                new DefaultDecisionProviderGuard(
                        new DefaultDecisionProviderHealthEvaluator(),
                        new DefaultDecisionProviderBudgetGuard(providerEnabled, 1L, 1L)),
                new DefaultDecisionProviderLatencyRecorder(),
                CLOCK);
    }

    private static Exchange validExchange(final String nonce) {
        return DhDryRunTestSupportEntry.signedExchange(
                DhDryRunTestSupportEntry.TEST_SOURCE,
                DhDryRunTestSupportEntry.validPayload(),
                nonce,
                NOW);
    }

    private static Exchange withoutHeader(final Exchange exchange, final String header) {
        final Map<String, String> headers = new LinkedHashMap<>(exchange.headers());
        headers.remove(header);
        return new Exchange(headers, exchange.body(), exchange.payload());
    }

    private static Exchange withHeader(final Exchange exchange, final String header, final String value) {
        final Map<String, String> headers = new LinkedHashMap<>(exchange.headers());
        headers.put(header, value);
        return new Exchange(headers, exchange.body(), exchange.payload());
    }

    private static Exchange resigned(final Exchange exchange) {
        final Map<String, String> headers = new LinkedHashMap<>(exchange.headers());
        headers.put(
                DhDryRunTestSupportEntry.HEADER_SIGNATURE,
                DhDryRunTestSupportEntry.sign(
                        headers, exchange.body(), DhDryRunTestSupportEntry.TEST_SECRET));
        return new Exchange(headers, exchange.body(), exchange.payload());
    }

    private static Map<String, String> forbiddenMutations() {
        final Map<String, String> fields = new LinkedHashMap<>();
        fields.put("apiSecret", "fake-value");
        fields.put("orderId", "order-1");
        fields.put("accountId", "account-1");
        fields.put("quantity", "1");
        fields.put("side", "BUY");
        fields.put("intent", "SELL");
        return fields;
    }

    private static void assertRejected(final Result result, final int statusCode, final String errorCode) {
        assertFalse(result.accepted());
        assertEquals(statusCode, result.statusCode());
        assertEquals(errorCode, result.errorCode());
        assertEquals(DecisionAction.ABSTAIN, result.output().getAction());
        assertTrue(result.steps().contains(Step.FAIL_CLOSED_RESPONSE_NORMALIZATION));
        assertReadonlyOutput(result.output());
        assertSafeSummary(result);
    }

    private static void assertReadonlyOutput(final DecisionOutput output) {
        assertEquals(DecisionType.READ_ONLY_RECOMMENDATION, output.getDecisionType());
        assertTrue(
                List.of(
                                DecisionAction.ABSTAIN,
                                DecisionAction.OBSERVE,
                                DecisionAction.NO_TRADE,
                                DecisionAction.LONG_BIAS,
                                DecisionAction.SHORT_BIAS)
                        .contains(output.getAction()));
        assertEquals(ForbiddenAction.mandatorySet(), output.getForbiddenActions());
        assertFalse(output.getReasonCodes().contains("PLACE_ORDER"));
        assertFalse(output.getReasonCodes().contains("CANCEL_ORDER"));
    }

    private static void assertSafeSummary(final Result result) {
        final String summary = result.safeSummary().conciseText().toLowerCase(java.util.Locale.ROOT);
        assertFalse(summary.contains("secret"));
        assertFalse(summary.contains("credential"));
        assertFalse(summary.contains("apikey"));
        assertFalse(summary.contains("api_key"));
        assertFalse(summary.contains("passphrase"));
        assertFalse(summary.contains("raw"));
    }

    private static void assertNoProductionToken(final List<String> roots, final String token) throws Exception {
        for (String root : roots) {
            final Path path = Path.of(root);
            if (!Files.exists(path)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(path)) {
                for (Path file : files.filter(Files::isRegularFile).filter(DhDryRunTestSupportEntryTest::isJavaFile).toList()) {
                    assertFalse(
                            Files.readString(file).contains(token),
                            "production Java source must not contain token " + token + " in " + file);
                }
            }
        }
    }

    private static boolean isJavaFile(final Path path) {
        return path.getFileName().toString().endsWith(".java");
    }

    private static final class StaticSignalProvider implements DecisionSignalProvider {
        private final DecisionSignalResult signal;

        private StaticSignalProvider(final DecisionSignalResult signal) {
            this.signal = signal;
        }

        @Override
        public DecisionSignalResult signal(final DecisionContext context) {
            return signal;
        }
    }
}

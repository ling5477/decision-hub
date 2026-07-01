package com.guidinglight.decisionhub.usecase.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderFailureClass;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderLatency;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import com.guidinglight.decisionhub.domain.decision.ForbiddenAction;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * K5 orchestrator-provider guard 集成测试，覆盖 fail-closed 输出与 provider call summary。
 */
final class DecisionOrchestratorProviderGuardTest {

    private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void healthyProviderWritesHealthBudgetLatencySummary() {
        final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();
        final CountingSignalProvider provider =
                new CountingSignalProvider(
                        DecisionSignalResult.mock(DecisionAction.NO_TRADE, List.of("MOCK_NO_TRADE")));

        final DecisionOutput output =
                orchestrator(repository, provider, new DefaultDecisionProviderGuard(), defaultLatency())
                        .decide(validRequest());

        assertEquals(DecisionAction.NO_TRADE, output.getAction());
        assertEquals(DecisionStatus.OBSERVATION_ONLY, output.getStatus());
        assertEquals(ProviderSignalStatus.MOCKED, output.getProviderStatus());
        assertEquals(DecisionType.READ_ONLY_RECOMMENDATION, output.getDecisionType());
        assertEquals(ForbiddenAction.mandatorySet(), output.getForbiddenActions());
        assertEquals(1, provider.calls);
        assertEquals(1, repository.providerCalls.size());
        final DecisionPersistenceRecords.ProviderCallRecord call = repository.providerCalls.get(0);
        assertEquals(ProviderSignalStatus.MOCKED, call.providerStatus());
        assertEquals(0L, call.latencyMs());
        assertNull(call.errorCode());
        assertEquals(true, call.signalJson().get("guardAllowed"));
        assertEquals("HEALTHY", call.signalJson().get("healthStatus"));
        assertEquals("WITHIN_BUDGET", call.signalJson().get("budgetStatus"));
        assertEquals("NONE", call.signalJson().get("failureClass"));
        assertEquals(0L, call.signalJson().get("latencyMs"));
    }

    @Test
    void disabledProviderFailsClosedBeforeProviderCall() {
        final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();
        final CountingSignalProvider provider =
                new CountingSignalProvider(
                        DecisionSignalResult.mock(DecisionAction.LONG_BIAS, List.of("MOCK_LONG_BIAS")));
        final DecisionProviderGuard guard =
                new DefaultDecisionProviderGuard(
                        new DefaultDecisionProviderHealthEvaluator(),
                        new DefaultDecisionProviderBudgetGuard(false, 1L, 1L));

        final DecisionOutput output =
                orchestrator(repository, provider, guard, defaultLatency()).decide(validRequest());

        assertEquals(0, provider.calls);
        assertEquals(DecisionAction.ABSTAIN, output.getAction());
        assertEquals(ProviderSignalStatus.DISABLED, output.getProviderStatus());
        assertEquals(DecisionProviderFailureClass.DISABLED.name(), repository.providerCalls.get(0).errorCode());
        assertEquals(false, repository.providerCalls.get(0).signalJson().get("guardAllowed"));
    }

    @Test
    void budgetExceededFailsClosedBeforeProviderCall() {
        final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();
        final CountingSignalProvider provider =
                new CountingSignalProvider(
                        DecisionSignalResult.mock(DecisionAction.LONG_BIAS, List.of("MOCK_LONG_BIAS")));
        final DecisionProviderGuard guard =
                new DefaultDecisionProviderGuard(
                        new DefaultDecisionProviderHealthEvaluator(),
                        new DefaultDecisionProviderBudgetGuard(true, 2L, 1L));

        final DecisionOutput output =
                orchestrator(repository, provider, guard, defaultLatency()).decide(validRequest());

        assertEquals(0, provider.calls);
        assertEquals(DecisionAction.ABSTAIN, output.getAction());
        assertEquals(ProviderSignalStatus.BUDGET_EXCEEDED, output.getProviderStatus());
        assertEquals("BUDGET_EXCEEDED", repository.providerCalls.get(0).errorCode());
        assertEquals("BUDGET_EXCEEDED", repository.providerCalls.get(0).signalJson().get("failureClass"));
    }

    @Test
    void timeoutSignalFailsClosedBeforeRiskReview() {
        final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();
        final CountingSignalProvider provider =
                new CountingSignalProvider(
                        DecisionSignalResult.failure(ProviderSignalStatus.TIMEOUT, "MOCK_PROVIDER_TIMEOUT"));

        final DecisionOutput output =
                orchestrator(repository, provider, new DefaultDecisionProviderGuard(), defaultLatency())
                        .decide(validRequest());

        assertEquals(1, provider.calls);
        assertEquals(DecisionAction.ABSTAIN, output.getAction());
        assertEquals(ProviderSignalStatus.TIMEOUT, output.getProviderStatus());
        assertEquals("TIMEOUT", repository.providerCalls.get(0).errorCode());
        assertTrue(output.getReasonCodes().contains("MOCK_PROVIDER_TIMEOUT"));
    }

    @Test
    void providerFailureFailsClosedBeforeRiskReview() {
        final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();
        final CountingSignalProvider provider =
                new CountingSignalProvider(
                        DecisionSignalResult.failure(ProviderSignalStatus.FAILED, "MOCK_PROVIDER_FAILED"));

        final DecisionOutput output =
                orchestrator(repository, provider, new DefaultDecisionProviderGuard(), defaultLatency())
                        .decide(validRequest());

        assertEquals(DecisionAction.ABSTAIN, output.getAction());
        assertEquals(ProviderSignalStatus.FAILED, output.getProviderStatus());
        assertEquals("FAILED", repository.providerCalls.get(0).errorCode());
    }

    @Test
    void untrustedProviderFailsClosedBeforeRiskReview() {
        final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();
        final CountingSignalProvider provider =
                new CountingSignalProvider(
                        DecisionSignalResult.failure(ProviderSignalStatus.UNTRUSTED, "MOCK_PROVIDER_UNTRUSTED"));

        final DecisionOutput output =
                orchestrator(repository, provider, new DefaultDecisionProviderGuard(), defaultLatency())
                        .decide(validRequest());

        assertEquals(DecisionAction.ABSTAIN, output.getAction());
        assertEquals(ProviderSignalStatus.UNTRUSTED, output.getProviderStatus());
        assertEquals("UNTRUSTED", repository.providerCalls.get(0).errorCode());
    }

    @Test
    void latencyTimeoutFailsClosedEvenWhenMockSignalIsHealthy() {
        final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();
        final CountingSignalProvider provider =
                new CountingSignalProvider(
                        DecisionSignalResult.mock(DecisionAction.LONG_BIAS, List.of("MOCK_LONG_BIAS")));
        final DecisionProviderLatencyRecorder timeoutLatency =
                (providerName, startedAt, endedAt) ->
                        new DecisionProviderLatency(providerName, 1500L, 1000L, true, endedAt);

        final DecisionOutput output =
                orchestrator(repository, provider, new DefaultDecisionProviderGuard(), timeoutLatency)
                        .decide(validRequest());

        assertEquals(DecisionAction.ABSTAIN, output.getAction());
        assertEquals(ProviderSignalStatus.TIMEOUT, output.getProviderStatus());
        assertEquals("TIMEOUT", repository.providerCalls.get(0).errorCode());
        assertEquals(1500L, repository.providerCalls.get(0).latencyMs());
        assertEquals(true, repository.providerCalls.get(0).signalJson().get("timedOut"));
    }

    @Test
    void providerExceptionFailsClosedWithoutLeakingMessage() {
        final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();

        final DecisionOutput output =
                orchestrator(
                        repository,
                        context -> {
                            throw new IllegalStateException("raw provider message must not leak");
                        },
                        new DefaultDecisionProviderGuard(),
                        defaultLatency())
                        .decide(validRequest());

        assertEquals(DecisionAction.ABSTAIN, output.getAction());
        assertEquals(ProviderSignalStatus.FAILED, output.getProviderStatus());
        assertEquals("FAILED", repository.providerCalls.get(0).errorCode());
        assertEquals("FAILED", repository.providerCalls.get(0).signalJson().get("failureClass"));
    }

    private static DecisionOrchestrator orchestrator(
            final DecisionAuditRepository repository,
            final DecisionSignalProvider provider,
            final DecisionProviderGuard guard,
            final DecisionProviderLatencyRecorder latencyRecorder) {
        return new DefaultDecisionOrchestrator(
                new DefaultDecisionContextBuilder(),
                new DefaultDecisionPolicyChecker(),
                provider,
                new DefaultDecisionRiskReviewer(),
                new DecisionOutputAssembler(),
                repository,
                guard,
                latencyRecorder,
                CLOCK);
    }

    private static DecisionProviderLatencyRecorder defaultLatency() {
        return new DefaultDecisionProviderLatencyRecorder();
    }

    private static DecisionRequest validRequest() {
        return DecisionRequest.readOnlyRecommendation(
                "req-1",
                "trace-1",
                "tenant-1",
                "codex-test",
                new DecisionSubject("BTC-USDT", "CRYPTO", "1h", "strategy-1", "research-1"),
                "context://safe",
                new DecisionContextSnapshot("snapshot-1", NOW, List.of("evidence://case-1")),
                NOW);
    }

    private static final class CountingSignalProvider implements DecisionSignalProvider {
        private final DecisionSignalResult result;
        private int calls;

        private CountingSignalProvider(final DecisionSignalResult result) {
            this.result = result;
        }

        @Override
        public DecisionSignalResult signal(final DecisionContext context) {
            calls++;
            return result;
        }
    }

    private static final class RecordingDecisionAuditRepository implements DecisionAuditRepository {
        private final List<DecisionPersistenceRecords.RequestRecord> requests = new ArrayList<>();
        private final List<DecisionPersistenceRecords.ContextSnapshotRecord> contextSnapshots =
                new ArrayList<>();
        private final List<DecisionPersistenceRecords.TraceStepRecord> traceSteps = new ArrayList<>();
        private final List<DecisionPersistenceRecords.ProviderCallRecord> providerCalls =
                new ArrayList<>();
        private final List<DecisionPersistenceRecords.OutputRecord> outputs = new ArrayList<>();
        private final List<DecisionPersistenceRecords.AuditEventRecord> auditEvents =
                new ArrayList<>();

        @Override
        public void saveRequest(final DecisionPersistenceRecords.RequestRecord record) {
            requests.add(record);
        }

        @Override
        public void saveContextSnapshot(final DecisionPersistenceRecords.ContextSnapshotRecord record) {
            contextSnapshots.add(record);
        }

        @Override
        public void saveTraceStep(final DecisionPersistenceRecords.TraceStepRecord record) {
            traceSteps.add(record);
        }

        @Override
        public void saveProviderCall(final DecisionPersistenceRecords.ProviderCallRecord record) {
            providerCalls.add(record);
        }

        @Override
        public void saveOutput(final DecisionPersistenceRecords.OutputRecord record) {
            outputs.add(record);
        }

        @Override
        public void saveAuditEvent(final DecisionPersistenceRecords.AuditEventRecord record) {
            auditEvents.add(record);
        }
    }
}

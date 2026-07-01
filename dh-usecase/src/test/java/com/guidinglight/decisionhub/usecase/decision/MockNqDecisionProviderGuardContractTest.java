package com.guidinglight.decisionhub.usecase.decision;

import static com.guidinglight.decisionhub.usecase.decision.support.MockNqDryRunAssertionSupport.assertStructuredReadOnlyOutput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import com.guidinglight.decisionhub.usecase.decision.support.MockNqDryRunFixtures;
import com.guidinglight.decisionhub.usecase.decision.support.RecordingDecisionAuditReplayRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * K6 mock NQ dry-run provider guard 合同测试，验证 disabled / budget / timeout 均 fail-closed。
 */
final class MockNqDecisionProviderGuardContractTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void disabledProviderFailsClosedBeforeAnyProviderCall() {
        final RecordingDecisionAuditReplayRepository repository =
                new RecordingDecisionAuditReplayRepository();
        final CountingSignalProvider provider =
                new CountingSignalProvider(
                        DecisionSignalResult.mock(DecisionAction.LONG_BIAS, List.of("MOCK_LONG_BIAS")));
        final DecisionProviderGuard guard =
                new DefaultDecisionProviderGuard(
                        new DefaultDecisionProviderHealthEvaluator(),
                        new DefaultDecisionProviderBudgetGuard(false, 1L, 1L));

        final DecisionOutput output =
                orchestrator(repository, provider, guard).decide(MockNqDryRunFixtures.providerBlockedRequest());

        assertEquals(0, provider.calls);
        assertEquals(DecisionAction.ABSTAIN, output.getAction());
        assertEquals(ProviderSignalStatus.DISABLED, output.getProviderStatus());
        assertStructuredReadOnlyOutput(output);
        assertEquals("DISABLED", repository.lastProviderCall().signalJson().get("failureClass"));
    }

    @Test
    void budgetExceededFailsClosedBeforeAnyProviderCall() {
        final RecordingDecisionAuditReplayRepository repository =
                new RecordingDecisionAuditReplayRepository();
        final CountingSignalProvider provider =
                new CountingSignalProvider(
                        DecisionSignalResult.mock(DecisionAction.LONG_BIAS, List.of("MOCK_LONG_BIAS")));
        final DecisionProviderGuard guard =
                new DefaultDecisionProviderGuard(
                        new DefaultDecisionProviderHealthEvaluator(),
                        new DefaultDecisionProviderBudgetGuard(true, 2L, 1L));

        final DecisionOutput output =
                orchestrator(repository, provider, guard).decide(MockNqDryRunFixtures.providerBlockedRequest());

        assertEquals(0, provider.calls);
        assertEquals(DecisionAction.ABSTAIN, output.getAction());
        assertEquals(ProviderSignalStatus.BUDGET_EXCEEDED, output.getProviderStatus());
        assertStructuredReadOnlyOutput(output);
        assertEquals("BUDGET_EXCEEDED", repository.lastProviderCall().errorCode());
    }

    @Test
    void providerTimeoutFailsClosedWithoutDirectionalBias() {
        final RecordingDecisionAuditReplayRepository repository =
                new RecordingDecisionAuditReplayRepository();
        final CountingSignalProvider provider =
                new CountingSignalProvider(
                        DecisionSignalResult.failure(ProviderSignalStatus.TIMEOUT, "MOCK_PROVIDER_TIMEOUT"));

        final DecisionOutput output =
                orchestrator(repository, provider, new DefaultDecisionProviderGuard())
                        .decide(MockNqDryRunFixtures.providerBlockedRequest());

        assertEquals(1, provider.calls);
        assertEquals(DecisionAction.ABSTAIN, output.getAction());
        assertEquals(ProviderSignalStatus.TIMEOUT, output.getProviderStatus());
        assertTrue(output.getReasonCodes().contains("MOCK_PROVIDER_TIMEOUT"));
        assertStructuredReadOnlyOutput(output);
    }

    private static DecisionOrchestrator orchestrator(
            final DecisionAuditRepository repository,
            final DecisionSignalProvider provider,
            final DecisionProviderGuard guard) {
        return new DefaultDecisionOrchestrator(
                new DefaultDecisionContextBuilder(),
                new DefaultDecisionPolicyChecker(),
                provider,
                new DefaultDecisionRiskReviewer(),
                new DecisionOutputAssembler(),
                repository,
                guard,
                new DefaultDecisionProviderLatencyRecorder(),
                CLOCK);
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
}

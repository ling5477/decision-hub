package com.guidinglight.decisionhub.usecase.decision;

import static com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.assertDecisionMatches;
import static com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.byCaseId;
import static com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.requiredText;
import static com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.requestFrom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.GoldenCase;
import com.guidinglight.decisionhub.usecase.decision.support.RecordingDecisionAuditReplayRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * K7 eval baseline 行为测试。
 *
 * <p>本测试用固定 Clock 和 mock-only provider/guard 复跑关键路径，证明 golden case 的 expectedDecision
 * 不是文档占位，而是当前 K1-K6 deterministic 行为的可执行基线。
 */
final class DecisionEvalBaselineTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void goldenCasesMatchDeterministicDecisionPipelineOutputs() {
        assertMatches("valid-no-trade", defaultOrchestrator());
        assertMatches("policy-blocked", defaultOrchestrator());
        assertMatches("no-evidence-abstain", defaultOrchestrator());
        assertMatches("forbidden-action-rejected", defaultOrchestrator());
        assertMatches("mock-nq-valid-dryrun", defaultOrchestrator());
        assertMatches("mock-nq-no-live-trade-guard", defaultOrchestrator());
        assertMatches("high-risk-abstain", orchestratorWithSignal(DecisionAction.LONG_BIAS, "MOCK_LONG_BIAS"));
        assertMatches(
                "provider-timeout-abstain",
                orchestratorWithProviderFailure(ProviderSignalStatus.TIMEOUT, "MOCK_PROVIDER_TIMEOUT"));
        assertMatches("provider-budget-exceeded-abstain", orchestratorWithBudgetExceeded());
        assertMatches("mock-nq-provider-blocked", orchestratorWithBudgetExceeded());
    }

    @Test
    void providerBudgetExceededDoesNotCallSignalProvider() {
        final GoldenCase goldenCase = byCaseId("provider-budget-exceeded-abstain");
        final CountingSignalProvider provider =
                new CountingSignalProvider(DecisionSignalResult.mock(DecisionAction.LONG_BIAS, List.of("UNUSED")));
        final DecisionOutput output =
                orchestrator(provider, budgetExceededGuard(), new RecordingDecisionAuditReplayRepository())
                        .decide(requestFrom(goldenCase.input()));

        assertEquals(0, provider.calls);
        assertDecisionMatches(goldenCase.caseId(), goldenCase.expectedDecision(), output);
    }

    @Test
    void replayGoldenCasesRespectTenantScope() {
        final GoldenCase found = byCaseId("replay-found-trace");
        final RecordingDecisionAuditReplayRepository foundRepository =
                new RecordingDecisionAuditReplayRepository();
        final DecisionRequest foundRequest = requestFrom(found.input());
        final DecisionOutput foundOutput = orchestrator(foundRepository).decide(foundRequest);
        final DecisionReplayView foundReplay = replay(foundRepository, found, foundRequest);

        assertDecisionMatches(found.caseId(), found.expectedDecision(), foundOutput);
        assertEquals(DecisionReplayStatus.FOUND, foundReplay.replayStatus());
        assertTrue(foundReplay.isComplete());
        assertEquals(foundRequest.getTraceId(), foundReplay.traceId());
        assertEquals(foundRequest.getTenantId(), foundReplay.tenantId());

        final GoldenCase mismatch = byCaseId("replay-tenant-mismatch-blocked");
        final RecordingDecisionAuditReplayRepository mismatchRepository =
                new RecordingDecisionAuditReplayRepository();
        final DecisionRequest mismatchRequest = requestFrom(mismatch.input());
        final DecisionOutput mismatchOutput = orchestrator(mismatchRepository).decide(mismatchRequest);
        final DecisionReplayView mismatchReplay = replay(mismatchRepository, mismatch, mismatchRequest);

        assertDecisionMatches(mismatch.caseId(), mismatch.expectedDecision(), mismatchOutput);
        assertEquals(DecisionReplayStatus.TENANT_MISMATCH, mismatchReplay.replayStatus());
        assertNull(mismatchReplay.request());
        assertNull(mismatchReplay.output());
    }

    private static void assertMatches(final String caseId, final DecisionOrchestrator orchestrator) {
        final GoldenCase goldenCase = byCaseId(caseId);
        final DecisionOutput output = orchestrator.decide(requestFrom(goldenCase.input()));

        assertDecisionMatches(goldenCase.caseId(), goldenCase.expectedDecision(), output);
    }

    private static DecisionReplayView replay(
            final RecordingDecisionAuditReplayRepository repository,
            final GoldenCase goldenCase,
            final DecisionRequest request) {
        final JsonNode expectedReplay = goldenCase.expectedReplay();
        return new DefaultDecisionReplayQueryService(repository)
                .replay(
                        requiredText(expectedReplay, "queryTenantId"),
                        request.getRequestId(),
                        request.getTraceId(),
                        request.getRequestId());
    }

    private static DecisionOrchestrator defaultOrchestrator() {
        return orchestrator(new MockDecisionSignalProvider(), new DefaultDecisionProviderGuard(), new RecordingDecisionAuditReplayRepository());
    }

    private static DecisionOrchestrator orchestratorWithSignal(
            final DecisionAction action, final String reasonCode) {
        return orchestrator(
                new MockDecisionSignalProvider(ProviderSignalStatus.MOCKED, action, List.of(reasonCode)),
                new DefaultDecisionProviderGuard(),
                new RecordingDecisionAuditReplayRepository());
    }

    private static DecisionOrchestrator orchestratorWithProviderFailure(
            final ProviderSignalStatus status, final String reasonCode) {
        return orchestrator(
                context -> DecisionSignalResult.failure(status, reasonCode),
                new DefaultDecisionProviderGuard(),
                new RecordingDecisionAuditReplayRepository());
    }

    private static DecisionOrchestrator orchestratorWithBudgetExceeded() {
        return orchestrator(
                new MockDecisionSignalProvider(ProviderSignalStatus.MOCKED, DecisionAction.LONG_BIAS, List.of("UNUSED")),
                budgetExceededGuard(),
                new RecordingDecisionAuditReplayRepository());
    }

    private static DecisionProviderGuard budgetExceededGuard() {
        return new DefaultDecisionProviderGuard(
                new DefaultDecisionProviderHealthEvaluator(),
                new DefaultDecisionProviderBudgetGuard(true, 2L, 1L));
    }

    private static DecisionOrchestrator orchestrator(
            final DecisionAuditRepository repository) {
        return orchestrator(new MockDecisionSignalProvider(), new DefaultDecisionProviderGuard(), repository);
    }

    private static DecisionOrchestrator orchestrator(
            final DecisionSignalProvider provider,
            final DecisionProviderGuard guard,
            final DecisionAuditRepository repository) {
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

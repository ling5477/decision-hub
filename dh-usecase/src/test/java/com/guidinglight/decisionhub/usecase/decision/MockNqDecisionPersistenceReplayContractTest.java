package com.guidinglight.decisionhub.usecase.decision;

import static com.guidinglight.decisionhub.usecase.decision.support.MockNqDryRunAssertionSupport.assertMockNqRequestIsReadOnly;
import static com.guidinglight.decisionhub.usecase.decision.support.MockNqDryRunAssertionSupport.assertStructuredReadOnlyOutput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.usecase.decision.support.MockNqDryRunFixtures;
import com.guidinglight.decisionhub.usecase.decision.support.RecordingDecisionAuditReplayRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

/**
 * K6 persistence/replay 合同测试，验证 mock NQ dry-run 写入后能通过 K4 read model 只读回放。
 */
final class MockNqDecisionPersistenceReplayContractTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void mockNqDryRunWritesAuditAndCanBeReplayedByK4ReadModel() {
        final RecordingDecisionAuditReplayRepository repository =
                new RecordingDecisionAuditReplayRepository();
        final DecisionRequest request = MockNqDryRunFixtures.validDryRunRequest();
        final DecisionOutput output = orchestrator(repository).decide(request);

        final DecisionReplayView replay =
                new DefaultDecisionReplayQueryService(repository)
                        .replay(request.getTenantId(), request.getRequestId(), request.getTraceId(), request.getRequestId());

        assertMockNqRequestIsReadOnly(request);
        assertStructuredReadOnlyOutput(output);
        assertEquals(1, repository.providerCallCount());
        assertEquals(1, repository.outputCount());
        assertEquals(1, repository.auditEventCount());
        assertEquals(DecisionReplayStatus.FOUND, replay.replayStatus());
        assertTrue(replay.isComplete());
        assertNotNull(replay.request());
        assertNotNull(replay.context());
        assertNotNull(replay.output());
        assertFalse(replay.timeline().traceSteps().isEmpty());
        assertFalse(replay.timeline().providerCalls().isEmpty());
        assertFalse(replay.timeline().auditEvents().isEmpty());
        assertEquals(request.getRequestId(), replay.requestId());
        assertEquals(request.getTraceId(), replay.traceId());
        assertEquals(output.getAction(), replay.output().action());
        assertEquals(output.getDecisionType(), replay.output().decisionType());
    }

    @Test
    void replayReadModelDoesNotReturnCrossTenantData() {
        final RecordingDecisionAuditReplayRepository repository =
                new RecordingDecisionAuditReplayRepository();
        final DecisionRequest request = MockNqDryRunFixtures.validDryRunRequest();
        orchestrator(repository).decide(request);

        final DecisionReplayView replay =
                new DefaultDecisionReplayQueryService(repository)
                        .replay("tenant-other", request.getRequestId(), request.getTraceId(), request.getRequestId());

        assertEquals(DecisionReplayStatus.TENANT_MISMATCH, replay.replayStatus());
        assertEquals("tenant-other", replay.tenantId());
        assertEquals(null, replay.request());
        assertEquals(null, replay.output());
    }

    private static DecisionOrchestrator orchestrator(
            final RecordingDecisionAuditReplayRepository repository) {
        return new DefaultDecisionOrchestrator(
                new DefaultDecisionContextBuilder(),
                new DefaultDecisionPolicyChecker(),
                new MockDecisionSignalProvider(),
                new DefaultDecisionRiskReviewer(),
                new DecisionOutputAssembler(),
                repository,
                CLOCK);
    }
}

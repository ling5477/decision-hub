package com.guidinglight.decisionhub.usecase.qdr.readmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.qdr.DecisionRunStatus;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * stage-qdr-2 B2 read model service 测试。
 *
 * <p>覆盖 usecase 层输入 fail-closed、tenant-bound 查询委托与 trace 不存在时不读取 timeline。
 */
final class DecisionReadModelServiceTest {

    private static final String RUN_ID = "00000000-0000-0000-0000-000000000002";
    private static final Instant NOW = Instant.parse("2026-07-06T00:00:00Z");

    @Test
    void invalidDecisionRunIdFailsBeforePortCall() {
        final RecordingPort port = new RecordingPort();
        final DecisionReadModelService service = new DecisionReadModelService(port);

        final IllegalArgumentException error =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                service.findDecisionRunDetail(
                                        new DecisionRunReadQuery("tenant-a", "not-a-uuid", "user-a", "trace-a")));
        assertTrue(error.getMessage().contains("decisionRunId must be UUID"));
        assertEquals(0, port.detailInvocations);
    }

    @Test
    void detailQueryDelegatesTenantBoundContract() {
        final RecordingPort port = new RecordingPort();
        port.detail = Optional.of(validDetail());
        final DecisionReadModelService service = new DecisionReadModelService(port);

        final Optional<DecisionRunDetailView> result =
                service.findDecisionRunDetail(
                        new DecisionRunReadQuery("tenant-a", RUN_ID, "user-a", "trace-a"));

        assertEquals(Optional.of(validDetail()), result);
        assertEquals("tenant-a", port.lastDetailQuery.tenantId());
        assertEquals(RUN_ID, port.lastDetailQuery.decisionRunId());
    }

    @Test
    void traceQueryReturnsEmptyWhenRunMissingAndDoesNotReadTimeline() {
        final RecordingPort port = new RecordingPort();
        final DecisionReadModelService service = new DecisionReadModelService(port);

        final Optional<DecisionTraceTimelineView> result =
                service.findDecisionTrace(
                        new DecisionTraceReadQuery("tenant-a", RUN_ID, "user-a", "trace-a"));

        assertTrue(result.isEmpty());
        assertEquals(1, port.detailInvocations);
        assertEquals(0, port.traceInvocations);
    }

    @Test
    void portFailurePropagatesAsUnavailable() {
        final RecordingPort port = new RecordingPort();
        port.failure = new DecisionReadModelUnavailableException("synthetic failure", null);
        final DecisionReadModelService service = new DecisionReadModelService(port);

        final DecisionReadModelUnavailableException error =
                assertThrows(
                        DecisionReadModelUnavailableException.class,
                        () ->
                                service.findDecisionRunDetail(
                                        new DecisionRunReadQuery("tenant-a", RUN_ID, "user-a", "trace-a")));
        assertTrue(error.getMessage().contains("synthetic failure"));
    }

    private static DecisionRunDetailView validDetail() {
        return new DecisionRunDetailView(
                "00000000-0000-0000-0000-000000000001",
                RUN_ID,
                "tenant-a",
                "trace-a",
                "request-a",
                "request-key-a",
                "QUANT_DECISION_REVIEW",
                "NQ_DRYRUN",
                "snapshot-a",
                1,
                DecisionRunStatus.SUCCEEDED,
                NOW,
                NOW.plusMillis(10),
                10L,
                null,
                null,
                "quant-signal://00000000-0000-0000-0000-000000000003;signalType=UNKNOWN",
                "quant-decision://00000000-0000-0000-0000-000000000004;action=OBSERVE",
                "decision-output://request-a;action=OBSERVE",
                NOW);
    }

    private static final class RecordingPort implements DecisionReadModelQueryPort {
        private Optional<DecisionRunDetailView> detail = Optional.empty();
        private DecisionTraceTimelineView timeline;
        private DecisionReadModelUnavailableException failure;
        private DecisionRunReadQuery lastDetailQuery;
        private int detailInvocations;
        private int traceInvocations;

        @Override
        public Optional<DecisionRunDetailView> findDecisionRunDetail(final DecisionRunReadQuery query) {
            detailInvocations++;
            lastDetailQuery = query;
            if (failure != null) {
                throw failure;
            }
            return detail;
        }

        @Override
        public DecisionTraceTimelineView getDecisionTrace(final DecisionTraceReadQuery query) {
            traceInvocations++;
            if (failure != null) {
                throw failure;
            }
            return timeline;
        }

        @Override
        public Optional<DecisionEvidenceView> findDecisionEvidence(final DecisionEvidenceReadQuery query) {
            if (failure != null) {
                throw failure;
            }
            return Optional.empty();
        }
    }
}

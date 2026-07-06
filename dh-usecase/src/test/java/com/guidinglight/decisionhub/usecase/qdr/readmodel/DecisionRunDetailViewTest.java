package com.guidinglight.decisionhub.usecase.qdr.readmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.domain.qdr.DecisionRunStatus;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * stage-qdr-2 B1 decision run detail projection 的只读与脱敏校验。
 */
class DecisionRunDetailViewTest {

    private static final Instant NOW = Instant.parse("2026-07-06T00:00:00Z");

    @Test
    void validViewPassesAndTrimsFields() {
        final DecisionRunDetailView view = validView();

        assertEquals("tenant-1", view.tenantId());
        assertEquals("decision-run-1", view.decisionRunId());
        assertEquals(DecisionRunStatus.SUCCEEDED, view.status());
        assertEquals("signal summary ref://signal-1", view.quantSignalSummary());
    }

    @Test
    void missingTenantIdFailsClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new DecisionRunDetailView(
                                "decision-request-1",
                                "decision-run-1",
                                " ",
                                "trace-1",
                                "request-1",
                                "request-key-1",
                                "QUANT_DECISION_REVIEW",
                                "NQ_DRYRUN",
                                "snapshot-1",
                                1,
                                DecisionRunStatus.SUCCEEDED,
                                NOW,
                                NOW,
                                12L,
                                null,
                                null,
                                "signal summary",
                                "decision summary",
                                "output summary",
                                NOW));
    }

    @Test
    void missingDecisionRunIdFailsClosed() {
        assertThrows(
                NullPointerException.class,
                () ->
                        new DecisionRunDetailView(
                                "decision-request-1",
                                null,
                                "tenant-1",
                                "trace-1",
                                "request-1",
                                "request-key-1",
                                "QUANT_DECISION_REVIEW",
                                "NQ_DRYRUN",
                                "snapshot-1",
                                1,
                                DecisionRunStatus.SUCCEEDED,
                                NOW,
                                NOW,
                                12L,
                                null,
                                null,
                                "signal summary",
                                "decision summary",
                                "output summary",
                                NOW));
    }

    @Test
    void illegalExecutableActionSummaryFailsClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new DecisionRunDetailView(
                                "decision-request-1",
                                "decision-run-1",
                                "tenant-1",
                                "trace-1",
                                "request-1",
                                "request-key-1",
                                "QUANT_DECISION_REVIEW",
                                "NQ_DRYRUN",
                                "snapshot-1",
                                1,
                                DecisionRunStatus.SUCCEEDED,
                                NOW,
                                NOW,
                                12L,
                                null,
                                null,
                                "signal summary",
                                "action=BUY",
                                "output summary",
                                NOW));
    }

    @Test
    void recordComponentsDoNotExposeExecutableActionSemantics() {
        final Set<String> forbidden = Set.of("BUY", "SELL", "PLACE_ORDER", "CANCEL_ORDER");

        Arrays.stream(DecisionRunDetailView.class.getRecordComponents())
                .map(RecordComponent::getName)
                .map(String::toUpperCase)
                .forEach(name -> forbidden.forEach(word -> assertFalse(name.contains(word))));
    }

    private static DecisionRunDetailView validView() {
        return new DecisionRunDetailView(
                "decision-request-1",
                "decision-run-1",
                "tenant-1",
                "trace-1",
                "request-1",
                "request-key-1",
                "QUANT_DECISION_REVIEW",
                "NQ_DRYRUN",
                "snapshot-1",
                1,
                DecisionRunStatus.SUCCEEDED,
                NOW,
                NOW,
                12L,
                null,
                null,
                "signal summary ref://signal-1",
                "decision summary ref://decision-1",
                "output summary ref://output-1",
                NOW);
    }
}

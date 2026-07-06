package com.guidinglight.decisionhub.usecase.qdr.readmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * stage-qdr-2 B1 trace timeline projection 的只读 contract 回归。
 */
class DecisionTraceTimelineViewTest {

    private static final Instant NOW = Instant.parse("2026-07-06T00:00:00Z");

    @Test
    void validTimelineKeepsRepositoryProvidedStepOrder() {
        final DecisionTraceStepView second = step(2);
        final DecisionTraceStepView first = step(1);

        final DecisionTraceTimelineView view =
                new DecisionTraceTimelineView(
                        "decision-run-1", "tenant-1", "trace-1", "request-1", List.of(second, first));

        assertEquals(List.of(second, first), view.steps());
    }

    @Test
    void emptyStepsAreAllowedForNotMaterializedTrace() {
        final DecisionTraceTimelineView view =
                new DecisionTraceTimelineView(
                        "decision-run-1", "tenant-1", "trace-1", "request-1", List.of());

        assertTrue(view.steps().isEmpty());
    }

    @Test
    void nullStepsNormalizeToEmptyReadOnlyList() {
        final DecisionTraceTimelineView view =
                new DecisionTraceTimelineView("decision-run-1", "tenant-1", "trace-1", "request-1", null);

        assertTrue(view.steps().isEmpty());
    }

    @Test
    void timelineContractDoesNotExposeReplayTriggerField() {
        final boolean hasReplayField =
                Arrays.stream(DecisionTraceTimelineView.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .anyMatch(name -> name.toLowerCase().contains("replay"));

        assertFalse(hasReplayField);
    }

    private static DecisionTraceStepView step(final int stepNo) {
        return new DecisionTraceStepView(
                "step-" + stepNo,
                stepNo,
                "step-name-" + stepNo,
                "VALIDATION",
                "SUCCEEDED",
                NOW,
                NOW,
                3L,
                null,
                null,
                "{\"summaryRef\":\"input-" + stepNo + "\"}",
                "{\"summaryRef\":\"output-" + stepNo + "\"}",
                "provider-call-log://call-" + stepNo,
                "audit://event-" + stepNo);
    }
}

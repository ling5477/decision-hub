package com.guidinglight.decisionhub.usecase.qdr.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackExecutionScope;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Stage-QDR-10 explicit scope 与 bounded query contract 回归。 */
class DecisionFeedbackEvidenceQueryTest {

    private static final Instant FROM = Instant.parse("2026-05-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-07-30T00:00:00Z");

    @Test
    void derivesExactDecisionAndFeedbackQueriesFromOneTrustedScope() {
        final DecisionFeedbackEvidenceQuery query = query(100);

        assertEquals("tenant-a", query.decisionEvidenceQuery().tenantId());
        assertEquals("trace-a", query.decisionEvidenceQuery().traceId());
        assertEquals("request-a", query.decisionEvidenceQuery().requestId());
        assertEquals("decision-a", query.decisionEvidenceQuery().decisionId());
        assertEquals("run-a", query.decisionEvidenceQuery().decisionRunId());
        assertEquals(DecisionEvidencePolicy.CORE_DECISION, query.decisionEvidenceQuery().policy());
        assertEquals("tenant-a", query.feedbackEvidenceQuery().tenantId());
        assertEquals(FeedbackEnvironment.TEST, query.feedbackEvidenceQuery().environment());
        assertEquals("decision-a", query.feedbackEvidenceQuery().decisionId());
        assertEquals("trace-a", query.feedbackEvidenceQuery().traceId());
        assertEquals(100, query.feedbackEvidenceQuery().effectivePageSize());
        assertFalse(List.of(query.decisionEnvironmentProvenanceQuery().getClass().getRecordComponents())
                .stream()
                .anyMatch(component -> component.getName().equals("environment")));
        assertEquals(FROM, query.boundedPolicy().fromObservedAt());
        assertEquals(TO, query.boundedPolicy().toObservedAt());
        assertEquals(100, query.boundedPolicy().maxFeedbackItems());
        assertEquals(BoundedEvidencePolicy.POLICY_ID, query.boundedPolicy().policyId());
        assertEquals(BoundedEvidencePolicy.POLICY_VERSION, query.boundedPolicy().policyVersion());
    }

    @Test
    void rejectsMissingScopeUnknownEnvironmentAndMissingRun() {
        assertThrows(IllegalArgumentException.class, () -> new DecisionFeedbackEvidenceQuery(
                null, "trace-a", "request-a", "decision-a", "run-a", FROM, TO, 100));
        assertThrows(RuntimeException.class, () -> FeedbackEnvironment.fromWire("PROD"));
        assertThrows(IllegalArgumentException.class, () -> new DecisionFeedbackEvidenceQuery(
                new FeedbackExecutionScope("tenant-a", FeedbackEnvironment.TEST),
                "trace-a", "request-a", "decision-a", null, FROM, TO, 100));
    }

    @Test
    void enforcesNinetyDayAndSinglePageBounds() {
        assertEquals(1, query(1).maxFeedbackItems());
        assertEquals(100, query(100).maxFeedbackItems());
        assertThrows(IllegalArgumentException.class, () -> query(0));
        assertThrows(IllegalArgumentException.class, () -> query(101));
        assertThrows(IllegalArgumentException.class, () -> new DecisionFeedbackEvidenceQuery(
                new FeedbackExecutionScope("tenant-a", FeedbackEnvironment.TEST),
                "trace-a",
                "request-a",
                "decision-a",
                "run-a",
                FROM,
                TO.plusSeconds(1),
                100));
    }

    private static DecisionFeedbackEvidenceQuery query(final int maxFeedbackItems) {
        return new DecisionFeedbackEvidenceQuery(
                new FeedbackExecutionScope("tenant-a", FeedbackEnvironment.TEST),
                "trace-a",
                "request-a",
                "decision-a",
                "run-a",
                FROM,
                TO,
                maxFeedbackItems);
    }
}

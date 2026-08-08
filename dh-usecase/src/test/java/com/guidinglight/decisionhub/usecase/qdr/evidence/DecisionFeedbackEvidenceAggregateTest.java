package com.guidinglight.decisionhub.usecase.qdr.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackExecutionScope;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Consolidated aggregate 的 immutability、ordering 与 fail-closed 状态回归。 */
class DecisionFeedbackEvidenceAggregateTest {

    @Test
    void completeAggregateDefensivelyCopiesAndSortsFeedback() {
        final DecisionFeedbackEvidenceQuery query = DecisionFeedbackEvidenceTestFixtures.query(100);
        final List<HistoricalFeedbackEvidenceView> feedback = new ArrayList<>(List.of(
                DecisionFeedbackEvidenceTestFixtures.feedback("observation-1", "b".repeat(64), 1),
                DecisionFeedbackEvidenceTestFixtures.feedback("observation-2", "a".repeat(64), 2)));

        final DecisionFeedbackEvidenceAggregate aggregate = new DecisionFeedbackEvidenceAggregate(
                query.executionScope(),
                query.decisionCorrelation(),
                DecisionFeedbackEvidenceTestFixtures.completeDecision(query),
                feedback,
                EvidenceCompleteness.COMPLETE,
                List.of());
        feedback.clear();

        assertEquals(2, aggregate.feedbackEvidence().size());
        assertEquals("a".repeat(64), aggregate.feedbackEvidence().getFirst().attributionId());
        assertThrows(UnsupportedOperationException.class, () -> aggregate.feedbackEvidence().clear());
        assertTrue(aggregate.isUsable());
    }

    @Test
    void partialOnlyAllowsAbsentOptionalFeedback() {
        final DecisionFeedbackEvidenceQuery query = DecisionFeedbackEvidenceTestFixtures.query(100);
        final DecisionFeedbackEvidenceAggregate aggregate = new DecisionFeedbackEvidenceAggregate(
                query.executionScope(),
                query.decisionCorrelation(),
                DecisionFeedbackEvidenceTestFixtures.completeDecision(query),
                List.of(),
                EvidenceCompleteness.PARTIAL,
                List.of(new DecisionFeedbackEvidenceFinding(
                        DecisionFeedbackEvidenceFinding.Code.OPTIONAL_FEEDBACK_ABSENT,
                        DecisionFeedbackEvidenceFinding.Severity.INFO,
                        query.decisionId(),
                        "允许缺失的 feedback evidence 不存在")));

        assertTrue(aggregate.isUsable());
        assertThrows(IllegalArgumentException.class, () -> new DecisionFeedbackEvidenceAggregate(
                query.executionScope(),
                query.decisionCorrelation(),
                DecisionFeedbackEvidenceTestFixtures.completeDecision(query),
                List.of(DecisionFeedbackEvidenceTestFixtures.feedback(
                        "observation-1", "a".repeat(64), 1)),
                EvidenceCompleteness.PARTIAL,
                List.of()));
    }

    @Test
    void inconsistentAndNotFoundRequireBlockingFinding() {
        final DecisionFeedbackEvidenceQuery query = DecisionFeedbackEvidenceTestFixtures.query(100);
        assertThrows(IllegalArgumentException.class, () -> new DecisionFeedbackEvidenceAggregate(
                query.executionScope(),
                query.decisionCorrelation(),
                DecisionFeedbackEvidenceTestFixtures.completeDecision(query),
                List.of(),
                EvidenceCompleteness.INCONSISTENT,
                List.of()));

        final DecisionFeedbackEvidenceAggregate failed = new DecisionFeedbackEvidenceAggregate(
                query.executionScope(),
                query.decisionCorrelation(),
                DecisionFeedbackEvidenceTestFixtures.completeDecision(query),
                List.of(),
                EvidenceCompleteness.INCONSISTENT,
                List.of(new DecisionFeedbackEvidenceFinding(
                        DecisionFeedbackEvidenceFinding.Code.FEEDBACK_SOURCE_FAILED,
                        DecisionFeedbackEvidenceFinding.Severity.BLOCKER,
                        "feedback-source",
                        "历史 feedback evidence 读取失败")));
        assertFalse(failed.isUsable());
    }

    @Test
    void usableAggregateRejectsCrossEnvironmentFeedback() {
        final DecisionFeedbackEvidenceQuery query = DecisionFeedbackEvidenceTestFixtures.query(100);
        final HistoricalFeedbackEvidenceView foreign = DecisionFeedbackEvidenceTestFixtures.feedback(
                "tenant-a",
                FeedbackEnvironment.DEV,
                "decision-a",
                "trace-a",
                "observation-1",
                "a".repeat(64),
                1,
                "evidence:item-1");

        assertThrows(IllegalArgumentException.class, () -> new DecisionFeedbackEvidenceAggregate(
                new FeedbackExecutionScope("tenant-a", FeedbackEnvironment.TEST),
                query.decisionCorrelation(),
                DecisionFeedbackEvidenceTestFixtures.completeDecision(query),
                List.of(foreign),
                EvidenceCompleteness.COMPLETE,
                List.of()));
    }
}

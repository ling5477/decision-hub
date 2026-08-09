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
                DecisionEnvironmentProvenance.proven(FeedbackEnvironment.TEST, "guard-a"),
                query.decisionCorrelation(),
                DecisionFeedbackEvidenceTestFixtures.completeDecision(query),
                feedback,
                query.boundedPolicy(),
                false,
                EvidenceCompleteness.COMPLETE_WITHIN_BOUNDS,
                List.of());
        feedback.clear();

        assertEquals(2, aggregate.feedbackEvidence().size());
        assertEquals("a".repeat(64), aggregate.feedbackEvidence().getFirst().attributionId());
        assertThrows(UnsupportedOperationException.class, () -> aggregate.feedbackEvidence().clear());
        assertTrue(aggregate.isUsableWithinBounds());
        assertEquals(query.fromObservedAt(), aggregate.boundedPolicy().fromObservedAt());
        assertEquals(query.toObservedAt(), aggregate.boundedPolicy().toObservedAt());
        assertEquals(query.maxFeedbackItems(), aggregate.boundedPolicy().maxFeedbackItems());
        assertEquals(BoundedEvidencePolicy.POLICY_ID, aggregate.boundedPolicy().policyId());
        assertFalse(aggregate.overflowDetected());
    }

    @Test
    void partialOnlyAllowsAbsentOptionalFeedback() {
        final DecisionFeedbackEvidenceQuery query = DecisionFeedbackEvidenceTestFixtures.query(100);
        final DecisionFeedbackEvidenceAggregate aggregate = new DecisionFeedbackEvidenceAggregate(
                query.executionScope(),
                DecisionEnvironmentProvenance.proven(FeedbackEnvironment.TEST, "guard-a"),
                query.decisionCorrelation(),
                DecisionFeedbackEvidenceTestFixtures.completeDecision(query),
                List.of(),
                query.boundedPolicy(),
                false,
                EvidenceCompleteness.PARTIAL_WITHIN_BOUNDS,
                List.of(new DecisionFeedbackEvidenceFinding(
                        DecisionFeedbackEvidenceFinding.Code.OPTIONAL_FEEDBACK_ABSENT,
                        DecisionFeedbackEvidenceFinding.Severity.INFO,
                        query.decisionId(),
                        "允许缺失的 feedback evidence 不存在")));

        assertTrue(aggregate.isUsableWithinBounds());
        assertThrows(IllegalArgumentException.class, () -> new DecisionFeedbackEvidenceAggregate(
                query.executionScope(),
                DecisionEnvironmentProvenance.proven(FeedbackEnvironment.TEST, "guard-a"),
                query.decisionCorrelation(),
                DecisionFeedbackEvidenceTestFixtures.completeDecision(query),
                List.of(DecisionFeedbackEvidenceTestFixtures.feedback(
                        "observation-1", "a".repeat(64), 1)),
                query.boundedPolicy(),
                false,
                EvidenceCompleteness.PARTIAL_WITHIN_BOUNDS,
                List.of()));
    }

    @Test
    void inconsistentAndNotFoundRequireBlockingFinding() {
        final DecisionFeedbackEvidenceQuery query = DecisionFeedbackEvidenceTestFixtures.query(100);
        assertThrows(IllegalArgumentException.class, () -> new DecisionFeedbackEvidenceAggregate(
                query.executionScope(),
                DecisionEnvironmentProvenance.notEvaluated(),
                query.decisionCorrelation(),
                DecisionFeedbackEvidenceTestFixtures.completeDecision(query),
                List.of(),
                query.boundedPolicy(),
                false,
                EvidenceCompleteness.INCONSISTENT,
                List.of()));

        final DecisionFeedbackEvidenceAggregate failed = new DecisionFeedbackEvidenceAggregate(
                query.executionScope(),
                DecisionEnvironmentProvenance.notEvaluated(),
                query.decisionCorrelation(),
                DecisionFeedbackEvidenceTestFixtures.completeDecision(query),
                List.of(),
                query.boundedPolicy(),
                false,
                EvidenceCompleteness.INCONSISTENT,
                List.of(new DecisionFeedbackEvidenceFinding(
                        DecisionFeedbackEvidenceFinding.Code.FEEDBACK_SOURCE_FAILED,
                        DecisionFeedbackEvidenceFinding.Severity.BLOCKER,
                        "feedback-source",
                        "历史 feedback evidence 读取失败")));
        assertFalse(failed.isUsableWithinBounds());

        assertThrows(IllegalArgumentException.class, () -> new DecisionFeedbackEvidenceAggregate(
                query.executionScope(),
                DecisionEnvironmentProvenance.notEvaluated(),
                query.decisionCorrelation(),
                DecisionFeedbackEvidenceTestFixtures.completeDecision(query),
                List.of(DecisionFeedbackEvidenceTestFixtures.feedback(
                        "observation-1", "a".repeat(64), 1)),
                query.boundedPolicy(),
                false,
                EvidenceCompleteness.INCONSISTENT,
                List.of(new DecisionFeedbackEvidenceFinding(
                        DecisionFeedbackEvidenceFinding.Code.ENVIRONMENT_MISMATCH,
                        DecisionFeedbackEvidenceFinding.Severity.BLOCKER,
                        "feedback-source",
                        "拒绝的 feedback evidence 不得被聚合结果暴露"))));
        assertThrows(IllegalArgumentException.class, () -> new DecisionFeedbackEvidenceAggregate(
                query.executionScope(),
                DecisionEnvironmentProvenance.notEvaluated(),
                query.decisionCorrelation(),
                DecisionFeedbackEvidenceTestFixtures.completeDecision(query),
                List.of(DecisionFeedbackEvidenceTestFixtures.feedback(
                        "observation-1", "a".repeat(64), 1)),
                query.boundedPolicy(),
                false,
                EvidenceCompleteness.NOT_FOUND,
                List.of(new DecisionFeedbackEvidenceFinding(
                        DecisionFeedbackEvidenceFinding.Code.DECISION_ROOT_NOT_FOUND,
                        DecisionFeedbackEvidenceFinding.Severity.BLOCKER,
                        "decision-a",
                        "缺失 decision 的 aggregate 不得暴露 feedback evidence"))));
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
                DecisionEnvironmentProvenance.proven(FeedbackEnvironment.TEST, "guard-a"),
                query.decisionCorrelation(),
                DecisionFeedbackEvidenceTestFixtures.completeDecision(query),
                List.of(foreign),
                query.boundedPolicy(),
                false,
                EvidenceCompleteness.COMPLETE_WITHIN_BOUNDS,
                List.of()));
    }

    @Test
    void aggregateIdentityIncludesEveryBoundAndPolicyField() {
        final DecisionFeedbackEvidenceQuery firstQuery = DecisionFeedbackEvidenceTestFixtures.query(100);
        final DecisionFeedbackEvidenceQuery differentFrom = new DecisionFeedbackEvidenceQuery(
                firstQuery.executionScope(),
                firstQuery.traceId(),
                firstQuery.requestId(),
                firstQuery.decisionId(),
                firstQuery.decisionRunId(),
                firstQuery.fromObservedAt().plusSeconds(1),
                firstQuery.toObservedAt(),
                firstQuery.maxFeedbackItems());
        final DecisionFeedbackEvidenceQuery differentTo = new DecisionFeedbackEvidenceQuery(
                firstQuery.executionScope(),
                firstQuery.traceId(),
                firstQuery.requestId(),
                firstQuery.decisionId(),
                firstQuery.decisionRunId(),
                firstQuery.fromObservedAt(),
                firstQuery.toObservedAt().minusSeconds(1),
                firstQuery.maxFeedbackItems());
        final DecisionFeedbackEvidenceQuery differentMaximum = new DecisionFeedbackEvidenceQuery(
                firstQuery.executionScope(),
                firstQuery.traceId(),
                firstQuery.requestId(),
                firstQuery.decisionId(),
                firstQuery.decisionRunId(),
                firstQuery.fromObservedAt(),
                firstQuery.toObservedAt(),
                99);
        final List<HistoricalFeedbackEvidenceView> feedback = List.of(
                DecisionFeedbackEvidenceTestFixtures.feedback("observation-1", "a".repeat(64), 1));

        final DecisionFeedbackEvidenceAggregate first = complete(firstQuery, feedback);
        final DecisionFeedbackEvidenceAggregate changedFrom = complete(differentFrom, feedback);
        final DecisionFeedbackEvidenceAggregate changedTo = complete(differentTo, feedback);
        final DecisionFeedbackEvidenceAggregate changedMaximum = complete(differentMaximum, feedback);

        assertFalse(first.equals(changedFrom));
        assertFalse(first.equals(changedTo));
        assertFalse(first.equals(changedMaximum));
        assertThrows(IllegalArgumentException.class, () -> new BoundedEvidencePolicy(
                firstQuery.fromObservedAt(),
                firstQuery.toObservedAt(),
                firstQuery.maxFeedbackItems(),
                BoundedEvidencePolicy.POLICY_ID,
                "2",
                BoundedEvidencePolicy.TimeWindowSemantics.CLOSED_INTERVAL,
                firstQuery.boundedPolicy().orderingPolicy(),
                BoundedEvidencePolicy.OverflowBehavior.FAIL_CLOSED));
    }

    @Test
    void usableAggregateRequiresMatchingProvenDecisionEnvironmentAndNoOverflow() {
        final DecisionFeedbackEvidenceQuery query = DecisionFeedbackEvidenceTestFixtures.query(100);
        final List<HistoricalFeedbackEvidenceView> feedback = List.of(
                DecisionFeedbackEvidenceTestFixtures.feedback("observation-1", "a".repeat(64), 1));

        assertThrows(IllegalArgumentException.class, () -> new DecisionFeedbackEvidenceAggregate(
                query.executionScope(),
                DecisionEnvironmentProvenance.missing(),
                query.decisionCorrelation(),
                DecisionFeedbackEvidenceTestFixtures.completeDecision(query),
                feedback,
                query.boundedPolicy(),
                false,
                EvidenceCompleteness.COMPLETE_WITHIN_BOUNDS,
                List.of()));
        assertThrows(IllegalArgumentException.class, () -> new DecisionFeedbackEvidenceAggregate(
                query.executionScope(),
                DecisionEnvironmentProvenance.proven(FeedbackEnvironment.TEST, "guard-a"),
                query.decisionCorrelation(),
                DecisionFeedbackEvidenceTestFixtures.completeDecision(query),
                feedback,
                query.boundedPolicy(),
                true,
                EvidenceCompleteness.COMPLETE_WITHIN_BOUNDS,
                List.of()));
    }

    private static DecisionFeedbackEvidenceAggregate complete(
            final DecisionFeedbackEvidenceQuery query,
            final List<HistoricalFeedbackEvidenceView> feedback) {
        return new DecisionFeedbackEvidenceAggregate(
                query.executionScope(),
                DecisionEnvironmentProvenance.proven(FeedbackEnvironment.TEST, "guard-a"),
                query.decisionCorrelation(),
                DecisionFeedbackEvidenceTestFixtures.completeDecision(query),
                feedback,
                query.boundedPolicy(),
                false,
                EvidenceCompleteness.COMPLETE_WITHIN_BOUNDS,
                List.of());
    }
}

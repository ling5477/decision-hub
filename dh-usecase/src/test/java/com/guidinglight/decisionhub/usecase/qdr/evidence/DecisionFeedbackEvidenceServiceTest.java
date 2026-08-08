package com.guidinglight.decisionhub.usecase.qdr.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionEvidence;
import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionDimension;
import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionImpact;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackExecutionScope;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackStatus;
import com.guidinglight.decisionhub.domain.qdr.feedback.ObservedDecisionOutcome;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeSource;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceFinding.Code;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceStatus;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceType;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidencePage;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceQuery;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.RedactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Consolidated composition、correlation、bounded overflow 与 security 回归。 */
class DecisionFeedbackEvidenceServiceTest {

    private DecisionEvidenceAggregate decisionResult;
    private HistoricalFeedbackEvidencePage feedbackResult;
    private boolean feedbackFailure;
    private int feedbackReads;
    private HistoricalFeedbackEvidenceQuery lastFeedbackQuery;
    private DecisionFeedbackEvidenceService service;
    private DecisionFeedbackEvidenceQuery query;

    @BeforeEach
    void setUp() {
        query = DecisionFeedbackEvidenceTestFixtures.query(100);
        decisionResult = DecisionFeedbackEvidenceTestFixtures.completeDecision(query);
        feedbackResult = HistoricalFeedbackEvidencePage.terminal(List.of());
        service = new DecisionFeedbackEvidenceService(
                (requestedQuery, requestedPolicy) -> decisionResult,
                requestedQuery -> {
                    feedbackReads++;
                    lastFeedbackQuery = requestedQuery;
                    if (feedbackFailure) {
                        throw new IllegalStateException("synthetic sql secret");
                    }
                    return feedbackResult;
                });
    }

    @Test
    void completeEvidenceIsReadOnceAndDeterministicallyOrdered() {
        final HistoricalFeedbackEvidenceView older =
                DecisionFeedbackEvidenceTestFixtures.feedback("observation-1", "b".repeat(64), 1);
        final HistoricalFeedbackEvidenceView newer =
                DecisionFeedbackEvidenceTestFixtures.feedback("observation-2", "a".repeat(64), 2);
        feedbackResult = HistoricalFeedbackEvidencePage.terminal(List.of(newer, older));

        final DecisionFeedbackEvidenceAggregate left = service.aggregate(query);
        final DecisionFeedbackEvidenceAggregate right = service.aggregate(query);

        assertEquals(EvidenceCompleteness.COMPLETE, left.completeness());
        assertEquals(List.of(newer, older), left.feedbackEvidence());
        assertEquals(left, right);
        assertEquals(2, feedbackReads);
        assertEquals("tenant-a", lastFeedbackQuery.tenantId());
        assertEquals(FeedbackEnvironment.TEST, lastFeedbackQuery.environment());
        assertEquals("decision-a", lastFeedbackQuery.decisionId());
        assertEquals("trace-a", lastFeedbackQuery.traceId());
    }

    @Test
    void absentOptionalFeedbackIsPartial() {
        feedbackResult = HistoricalFeedbackEvidencePage.terminal(List.of());

        final DecisionFeedbackEvidenceAggregate result = service.aggregate(query);

        assertEquals(EvidenceCompleteness.PARTIAL, result.completeness());
        assertTrue(result.isUsable());
        assertFinding(result, Code.OPTIONAL_FEEDBACK_ABSENT);
    }

    @Test
    void missingMandatoryRequestOrRunIsNotFoundAndSkipsFeedbackRead() {
        decisionResult = query.decisionEvidenceQuery().policy().evaluate(
                query.decisionEvidenceQuery(), List.of());

        final DecisionFeedbackEvidenceAggregate result = service.aggregate(query);

        assertEquals(EvidenceCompleteness.NOT_FOUND, result.completeness());
        assertFalse(result.isUsable());
        assertFinding(result, Code.DECISION_ROOT_NOT_FOUND);
        assertEquals(0, feedbackReads);
    }

    @Test
    void missingNonRootMandatoryEvidenceIsInconsistent() {
        final DecisionEvidenceAggregate complete = DecisionFeedbackEvidenceTestFixtures.completeDecision(query);
        final List<DecisionEvidenceRef> withoutTrace = complete.evidenceRefs().stream()
                .filter(ref -> ref.evidenceType() != DecisionEvidencePolicy.EvidenceType.TRACE_STEP)
                .toList();
        decisionResult = DecisionEvidenceAggregate.evaluate(query.decisionEvidenceQuery(), withoutTrace);

        final DecisionFeedbackEvidenceAggregate result = service.aggregate(query);

        assertEquals(EvidenceCompleteness.INCONSISTENT, result.completeness());
        assertFinding(result, Code.DECISION_EVIDENCE_INCOMPLETE);
        assertEquals(0, feedbackReads);
    }

    @Test
    void requestAndRunIdentityMismatchFailClosed() {
        final DecisionEvidenceAggregate complete = DecisionFeedbackEvidenceTestFixtures.completeDecision(query);
        final List<DecisionEvidenceRef> mismatched = complete.evidenceRefs().stream()
                .map(ref -> {
                    if (ref.evidenceType() == DecisionEvidencePolicy.EvidenceType.RUN) {
                        return DecisionFeedbackEvidenceTestFixtures.ref(
                                query,
                                DecisionEvidencePolicy.EvidenceType.RUN,
                                "v6-run:run-other");
                    }
                    return ref;
                })
                .toList();
        decisionResult = DecisionEvidenceAggregate.evaluate(query.decisionEvidenceQuery(), mismatched);

        final DecisionFeedbackEvidenceAggregate result = service.aggregate(query);

        assertEquals(EvidenceCompleteness.INCONSISTENT, result.completeness());
        assertFinding(result, Code.RUN_MISMATCH);
    }

    @Test
    void overflowFailsClosedWithoutFollowingCursor() {
        final HistoricalFeedbackEvidenceQuery feedbackQuery = query.feedbackEvidenceQuery();
        final HistoricalFeedbackEvidenceQuery.Cursor cursor = new HistoricalFeedbackEvidenceQuery.Cursor(
                1,
                query.executionScope().tenantId(),
                query.executionScope().environment(),
                feedbackQuery.filterFingerprint(),
                DecisionFeedbackEvidenceTestFixtures.OBSERVED_AT,
                "a".repeat(64));
        feedbackResult = new HistoricalFeedbackEvidencePage(
                List.of(DecisionFeedbackEvidenceTestFixtures.feedback(
                        "observation-1", "a".repeat(64), 1)),
                true,
                Optional.of(cursor));

        final DecisionFeedbackEvidenceAggregate result = service.aggregate(query);

        assertEquals(EvidenceCompleteness.INCONSISTENT, result.completeness());
        assertFinding(result, Code.FEEDBACK_RESULT_LIMIT_EXCEEDED);
        assertEquals(1, feedbackReads);
    }

    @Test
    void crossTenantEnvironmentDecisionAndTraceFailClosed() {
        final HistoricalFeedbackEvidenceView foreign = DecisionFeedbackEvidenceTestFixtures.feedback(
                "tenant-b",
                FeedbackEnvironment.DEV,
                "decision-b",
                "trace-b",
                "observation-1",
                "a".repeat(64),
                1,
                "evidence:item-1");
        feedbackResult = HistoricalFeedbackEvidencePage.terminal(List.of(foreign));

        final DecisionFeedbackEvidenceAggregate result = service.aggregate(query);

        assertEquals(EvidenceCompleteness.INCONSISTENT, result.completeness());
        List.of(Code.TENANT_MISMATCH, Code.ENVIRONMENT_MISMATCH, Code.DECISION_MISMATCH, Code.TRACE_MISMATCH)
                .forEach(code -> assertFinding(result, code));
    }

    @Test
    void duplicateAndOutOfOrderFeedbackFailClosed() {
        final HistoricalFeedbackEvidenceView older =
                DecisionFeedbackEvidenceTestFixtures.feedback("observation-1", "a".repeat(64), 1);
        final HistoricalFeedbackEvidenceView newerSameIdentity =
                DecisionFeedbackEvidenceTestFixtures.feedback("observation-1", "a".repeat(64), 2);
        feedbackResult = HistoricalFeedbackEvidencePage.terminal(List.of(older, newerSameIdentity));

        final DecisionFeedbackEvidenceAggregate result = service.aggregate(query);

        assertEquals(EvidenceCompleteness.INCONSISTENT, result.completeness());
        assertFinding(result, Code.FEEDBACK_IDENTITY_CONFLICT);
        assertFinding(result, Code.FEEDBACK_ORDER_INVALID);
    }

    @Test
    void unsafeFeedbackReferenceIsRejected() {
        feedbackResult = HistoricalFeedbackEvidencePage.terminal(List.of(
                DecisionFeedbackEvidenceTestFixtures.feedback(
                        "tenant-a",
                        FeedbackEnvironment.TEST,
                        "decision-a",
                        "trace-a",
                        "observation-1",
                        "a".repeat(64),
                        1,
                        "raw_prompt:item-1")));

        final DecisionFeedbackEvidenceAggregate result = service.aggregate(query);

        assertEquals(EvidenceCompleteness.INCONSISTENT, result.completeness());
        assertFinding(result, Code.UNSAFE_EVIDENCE_REJECTED);
    }

    @Test
    void feedbackSourceFailureIsSanitizedAndFailClosed() {
        feedbackFailure = true;

        final DecisionFeedbackEvidenceAggregate result = service.aggregate(query);

        assertEquals(EvidenceCompleteness.INCONSISTENT, result.completeness());
        assertFinding(result, Code.FEEDBACK_SOURCE_FAILED);
        assertFalse(result.toString().contains("synthetic"));
    }

    private static void assertFinding(
            final DecisionFeedbackEvidenceAggregate aggregate, final Code code) {
        assertTrue(aggregate.findings().stream().anyMatch(finding -> finding.code() == code), code.name());
    }
}

/** Allowlisted Stage-QDR-10 tests 共用的 deterministic fixture。 */
final class DecisionFeedbackEvidenceTestFixtures {

    static final Instant OBSERVED_AT = Instant.parse("2026-07-30T00:00:00Z");

    private DecisionFeedbackEvidenceTestFixtures() {}

    static DecisionFeedbackEvidenceQuery query(final int maxItems) {
        return new DecisionFeedbackEvidenceQuery(
                new FeedbackExecutionScope("tenant-a", FeedbackEnvironment.TEST),
                "trace-a",
                "request-a",
                "decision-a",
                "run-a",
                OBSERVED_AT.minusSeconds(90L * 24 * 60 * 60),
                OBSERVED_AT,
                maxItems);
    }

    static DecisionEvidenceAggregate completeDecision(final DecisionFeedbackEvidenceQuery query) {
        final List<DecisionEvidenceRef> refs = DecisionEvidencePolicy.CORE_DECISION
                .mandatoryEvidenceTypes()
                .stream()
                .map(type -> ref(query, type, refId(query, type)))
                .toList();
        return DecisionEvidenceAggregate.evaluate(query.decisionEvidenceQuery(), refs);
    }

    static DecisionEvidenceRef ref(
            final DecisionFeedbackEvidenceQuery query,
            final DecisionEvidencePolicy.EvidenceType type,
            final String refId) {
        return new DecisionEvidenceRef(
                new DecisionEvidence(refId, type.name(), "SAFE_" + type.name()),
                query.decisionCorrelation(),
                type,
                refId,
                null,
                "TEST_" + type.name(),
                true,
                RedactionStatus.SUMMARY_ONLY);
    }

    static HistoricalFeedbackEvidenceView feedback(
            final String observationId,
            final String attributionId,
            final int observedAtOffsetSeconds) {
        return feedback(
                "tenant-a",
                FeedbackEnvironment.TEST,
                "decision-a",
                "trace-a",
                observationId,
                attributionId,
                observedAtOffsetSeconds,
                "evidence:item-1");
    }

    static HistoricalFeedbackEvidenceView feedback(
            final String tenantId,
            final FeedbackEnvironment environment,
            final String decisionId,
            final String traceId,
            final String observationId,
            final String attributionId,
            final int observedAtOffsetSeconds,
            final String evidenceRef) {
        final Instant observedAt = OBSERVED_AT.plusSeconds(observedAtOffsetSeconds);
        return new HistoricalFeedbackEvidenceView(
                tenantId,
                environment,
                decisionId,
                traceId,
                observationId,
                attributionId,
                observedAt,
                OutcomeSource.STRUCTURED_TEST_FIXTURE,
                ObservedDecisionOutcome.SUCCEEDED,
                observedAt.plusSeconds(1),
                "policy-a",
                "v1",
                FeedbackStatus.ATTRIBUTED,
                BigDecimal.ONE,
                List.of(new HistoricalFeedbackEvidenceView.Contribution(
                        AttributionDimension.EVIDENCE_QUALITY,
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        AttributionImpact.POSITIVE,
                        BigDecimal.ONE,
                        "TEST",
                        evidenceRef,
                        0)),
                List.of(new HistoricalFeedbackEvidenceView.Reference(
                        ReferenceType.EVIDENCE, evidenceRef, ReferenceStatus.ACTIVE)));
    }

    private static String refId(
            final DecisionFeedbackEvidenceQuery query,
            final DecisionEvidencePolicy.EvidenceType type) {
        return switch (type) {
            case REQUEST -> "v5-request:" + query.requestId();
            case RUN -> "v6-run:" + query.decisionRunId();
            default -> "safe-" + type.name().toLowerCase(Locale.ROOT);
        };
    }
}

package com.guidinglight.decisionhub.usecase.qdr.evidence;

import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceQuery;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Immutable policy describing exactly which bounded feedback evidence was evaluated. */
public record BoundedEvidencePolicy(
        Instant fromObservedAt,
        Instant toObservedAt,
        int maxFeedbackItems,
        String policyId,
        String policyVersion,
        TimeWindowSemantics timeWindowSemantics,
        String orderingPolicy,
        OverflowBehavior overflowBehavior) {

    /** Stable Stage-QDR-10 bounded evidence policy identity. */
    public static final String POLICY_ID = "QDR10-DECISION-FEEDBACK-EVIDENCE";
    /** Policy version changes whenever bounded completeness semantics change. */
    public static final String POLICY_VERSION = "1";
    private static final Duration MAX_RANGE = Duration.ofDays(90);

    /** Enforces a closed, bounded, deterministic and fail-closed policy. */
    public BoundedEvidencePolicy {
        fromObservedAt = Objects.requireNonNull(fromObservedAt, "fromObservedAt");
        toObservedAt = Objects.requireNonNull(toObservedAt, "toObservedAt");
        if (fromObservedAt.isAfter(toObservedAt)
                || Duration.between(fromObservedAt, toObservedAt).compareTo(MAX_RANGE) > 0) {
            throw new IllegalArgumentException("bounded evidence range must be within 90 days");
        }
        if (maxFeedbackItems < 1
                || maxFeedbackItems > HistoricalFeedbackEvidenceQuery.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("maxFeedbackItems must be within [1,100]");
        }
        policyId = DecisionEvidencePolicy.requireSafeText(policyId, "policyId");
        policyVersion = DecisionEvidencePolicy.requireSafeText(policyVersion, "policyVersion");
        timeWindowSemantics = Objects.requireNonNull(timeWindowSemantics, "timeWindowSemantics");
        orderingPolicy = DecisionEvidencePolicy.requireSafeText(orderingPolicy, "orderingPolicy");
        overflowBehavior = Objects.requireNonNull(overflowBehavior, "overflowBehavior");
        if (!POLICY_ID.equals(policyId)
                || !POLICY_VERSION.equals(policyVersion)
                || timeWindowSemantics != TimeWindowSemantics.CLOSED_INTERVAL
                || !HistoricalFeedbackEvidenceQuery.ORDERING_VERSION.equals(orderingPolicy)
                || overflowBehavior != OverflowBehavior.FAIL_CLOSED) {
            throw new IllegalArgumentException("bounded evidence policy identity is unsupported");
        }
    }

    /** Creates the policy from the caller-declared query without recalculation or current time. */
    public static BoundedEvidencePolicy from(final DecisionFeedbackEvidenceQuery query) {
        final DecisionFeedbackEvidenceQuery checked = Objects.requireNonNull(query, "query");
        return new BoundedEvidencePolicy(
                checked.fromObservedAt(),
                checked.toObservedAt(),
                checked.maxFeedbackItems(),
                POLICY_ID,
                POLICY_VERSION,
                TimeWindowSemantics.CLOSED_INTERVAL,
                HistoricalFeedbackEvidenceQuery.ORDERING_VERSION,
                OverflowBehavior.FAIL_CLOSED);
    }

    /** Both endpoints are included by the PostgreSQL historical evidence query. */
    public enum TimeWindowSemantics {
        CLOSED_INTERVAL
    }

    /** Any evidence beyond the declared cap makes the aggregate unusable. */
    public enum OverflowBehavior {
        FAIL_CLOSED
    }
}

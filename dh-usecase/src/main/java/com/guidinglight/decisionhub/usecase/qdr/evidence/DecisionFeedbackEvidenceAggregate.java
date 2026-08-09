package com.guidinglight.decisionhub.usecase.qdr.evidence;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackExecutionScope;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceView;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Immutable, bounded and side-effect-free decision/feedback evidence aggregate。 */
public record DecisionFeedbackEvidenceAggregate(
        FeedbackExecutionScope executionScope,
        DecisionEnvironmentProvenance decisionEnvironmentProvenance,
        DecisionEvidenceCorrelation decisionCorrelation,
        DecisionEvidenceAggregate decisionEvidence,
        List<HistoricalFeedbackEvidenceView> feedbackEvidence,
        BoundedEvidencePolicy boundedPolicy,
        boolean overflowDetected,
        EvidenceCompleteness completeness,
        List<DecisionFeedbackEvidenceFinding> findings) {

    private static final Comparator<HistoricalFeedbackEvidenceView> FEEDBACK_ORDER =
            Comparator.comparing(
                            HistoricalFeedbackEvidenceView::observedAt,
                            Comparator.reverseOrder())
                    .thenComparing(
                            HistoricalFeedbackEvidenceView::attributionId,
                            Comparator.reverseOrder());
    private static final Comparator<DecisionFeedbackEvidenceFinding> FINDING_ORDER =
            Comparator.comparing(
                            (DecisionFeedbackEvidenceFinding finding) -> finding.code().name())
                    .thenComparing(finding -> Objects.toString(finding.safeRef(), ""))
                    .thenComparing(DecisionFeedbackEvidenceFinding::summary);

    /** Defensive copy、稳定排序并保护可用状态的不变量。 */
    public DecisionFeedbackEvidenceAggregate {
        executionScope = Objects.requireNonNull(executionScope, "executionScope");
        decisionEnvironmentProvenance = Objects.requireNonNull(
                decisionEnvironmentProvenance, "decisionEnvironmentProvenance");
        decisionCorrelation = Objects.requireNonNull(decisionCorrelation, "decisionCorrelation");
        decisionEvidence = Objects.requireNonNull(decisionEvidence, "decisionEvidence");
        feedbackEvidence = Objects.requireNonNullElse(feedbackEvidence, List.<HistoricalFeedbackEvidenceView>of())
                .stream()
                .map(view -> Objects.requireNonNull(view, "feedbackEvidence item"))
                .sorted(FEEDBACK_ORDER)
                .toList();
        boundedPolicy = Objects.requireNonNull(boundedPolicy, "boundedPolicy");
        completeness = Objects.requireNonNull(completeness, "completeness");
        findings = Objects.requireNonNullElse(findings, List.<DecisionFeedbackEvidenceFinding>of())
                .stream()
                .map(finding -> Objects.requireNonNull(finding, "finding"))
                .sorted(FINDING_ORDER)
                .toList();

        final FeedbackExecutionScope checkedExecutionScope = executionScope;
        final DecisionEnvironmentProvenance checkedProvenance = decisionEnvironmentProvenance;
        final DecisionEvidenceCorrelation checkedDecisionCorrelation = decisionCorrelation;

        if (!checkedExecutionScope.tenantId().equals(checkedDecisionCorrelation.tenantId())) {
            throw new IllegalArgumentException("execution scope tenant must own decision correlation");
        }
        final boolean hasBlockingFinding = findings.stream().anyMatch(finding ->
                finding.severity() == DecisionFeedbackEvidenceFinding.Severity.ERROR
                        || finding.severity() == DecisionFeedbackEvidenceFinding.Severity.BLOCKER);
        if (feedbackEvidence.size() > boundedPolicy.maxFeedbackItems()) {
            throw new IllegalArgumentException("aggregate feedback exceeds bounded policy");
        }
        if (completeness == EvidenceCompleteness.COMPLETE_WITHIN_BOUNDS
                || completeness == EvidenceCompleteness.PARTIAL_WITHIN_BOUNDS) {
            if (decisionEvidence.status() != DecisionEvidenceStatus.COMPLETE
                    || !decisionCorrelation.matches(decisionEvidence.correlation())
                    || hasBlockingFinding) {
                throw new IllegalArgumentException("usable aggregate requires complete consistent decision evidence");
            }
            if (!checkedProvenance.isProven()
                    || checkedProvenance.environment() != checkedExecutionScope.environment()) {
                throw new IllegalArgumentException("usable aggregate requires exact decision environment provenance");
            }
            if (overflowDetected) {
                throw new IllegalArgumentException("usable aggregate cannot contain bounded overflow");
            }
            final boolean feedbackScopeMismatch = feedbackEvidence.stream().anyMatch(view ->
                    !checkedExecutionScope.tenantId().equals(view.tenantId())
                            || checkedExecutionScope.environment() != view.environment()
                            || !checkedDecisionCorrelation.decisionId().equals(view.decisionId())
                            || !checkedDecisionCorrelation.traceId().equals(view.traceId()));
            if (feedbackScopeMismatch) {
                throw new IllegalArgumentException("usable aggregate cannot contain cross-scope feedback");
            }
        }
        if (completeness == EvidenceCompleteness.COMPLETE_WITHIN_BOUNDS
                && feedbackEvidence.isEmpty()) {
            throw new IllegalArgumentException("COMPLETE_WITHIN_BOUNDS requires feedback evidence");
        }
        if (completeness == EvidenceCompleteness.PARTIAL_WITHIN_BOUNDS
                && !feedbackEvidence.isEmpty()) {
            throw new IllegalArgumentException(
                    "PARTIAL_WITHIN_BOUNDS only represents absent optional feedback");
        }
        if ((completeness == EvidenceCompleteness.INCONSISTENT
                        || completeness == EvidenceCompleteness.NOT_FOUND)
                && !hasBlockingFinding) {
            throw new IllegalArgumentException("fail-closed aggregate requires a blocking finding");
        }
        if ((completeness == EvidenceCompleteness.INCONSISTENT
                        || completeness == EvidenceCompleteness.NOT_FOUND)
                && !feedbackEvidence.isEmpty()) {
            throw new IllegalArgumentException("fail-closed aggregate cannot expose rejected feedback");
        }
    }

    /** Only policy-qualified COMPLETE/PARTIAL states are consumable. */
    public boolean isUsableWithinBounds() {
        return completeness == EvidenceCompleteness.COMPLETE_WITHIN_BOUNDS
                || completeness == EvidenceCompleteness.PARTIAL_WITHIN_BOUNDS;
    }
}

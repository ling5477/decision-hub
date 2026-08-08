package com.guidinglight.decisionhub.usecase.qdr.evidence;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackExecutionScope;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceView;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Immutable, bounded and side-effect-free decision/feedback evidence aggregate。 */
public record DecisionFeedbackEvidenceAggregate(
        FeedbackExecutionScope executionScope,
        DecisionEvidenceCorrelation decisionCorrelation,
        DecisionEvidenceAggregate decisionEvidence,
        List<HistoricalFeedbackEvidenceView> feedbackEvidence,
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
        decisionCorrelation = Objects.requireNonNull(decisionCorrelation, "decisionCorrelation");
        decisionEvidence = Objects.requireNonNull(decisionEvidence, "decisionEvidence");
        feedbackEvidence = Objects.requireNonNullElse(feedbackEvidence, List.<HistoricalFeedbackEvidenceView>of())
                .stream()
                .map(view -> Objects.requireNonNull(view, "feedbackEvidence item"))
                .sorted(FEEDBACK_ORDER)
                .toList();
        completeness = Objects.requireNonNull(completeness, "completeness");
        findings = Objects.requireNonNullElse(findings, List.<DecisionFeedbackEvidenceFinding>of())
                .stream()
                .map(finding -> Objects.requireNonNull(finding, "finding"))
                .sorted(FINDING_ORDER)
                .toList();

        final FeedbackExecutionScope checkedExecutionScope = executionScope;
        final DecisionEvidenceCorrelation checkedDecisionCorrelation = decisionCorrelation;

        if (!checkedExecutionScope.tenantId().equals(checkedDecisionCorrelation.tenantId())) {
            throw new IllegalArgumentException("execution scope tenant must own decision correlation");
        }
        final boolean hasBlockingFinding = findings.stream().anyMatch(finding ->
                finding.severity() == DecisionFeedbackEvidenceFinding.Severity.ERROR
                        || finding.severity() == DecisionFeedbackEvidenceFinding.Severity.BLOCKER);
        if (completeness == EvidenceCompleteness.COMPLETE
                || completeness == EvidenceCompleteness.PARTIAL) {
            if (decisionEvidence.status() != DecisionEvidenceStatus.COMPLETE
                    || !decisionCorrelation.matches(decisionEvidence.correlation())
                    || hasBlockingFinding) {
                throw new IllegalArgumentException("usable aggregate requires complete consistent decision evidence");
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
        if (completeness == EvidenceCompleteness.COMPLETE && feedbackEvidence.isEmpty()) {
            throw new IllegalArgumentException("COMPLETE aggregate requires feedback evidence");
        }
        if (completeness == EvidenceCompleteness.PARTIAL && !feedbackEvidence.isEmpty()) {
            throw new IllegalArgumentException("PARTIAL aggregate only represents absent optional feedback");
        }
        if ((completeness == EvidenceCompleteness.INCONSISTENT
                        || completeness == EvidenceCompleteness.NOT_FOUND)
                && !hasBlockingFinding) {
            throw new IllegalArgumentException("fail-closed aggregate requires a blocking finding");
        }
    }

    /** COMPLETE/PARTIAL 是唯一可消费状态；其余状态必须 fail-closed。 */
    public boolean isUsable() {
        return completeness == EvidenceCompleteness.COMPLETE
                || completeness == EvidenceCompleteness.PARTIAL;
    }
}

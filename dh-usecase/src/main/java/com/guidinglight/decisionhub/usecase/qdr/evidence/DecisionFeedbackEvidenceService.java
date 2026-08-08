package com.guidinglight.decisionhub.usecase.qdr.evidence;

import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceFinding.Code;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceFinding.Severity;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidencePage;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceQuery;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceReadService;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceView;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Stage-QDR-10 internal-only consolidated read service。
 *
 * <p>该服务只组合既有 decision evidence reader 与 bounded historical feedback reader。它不创建
 * execution scope、不推断 environment、不写库、不执行 feedback learning，也不调用 API、HTTP、Provider、
 * NQ、Agent、LangGraph 或交易执行能力。
 */
public final class DecisionFeedbackEvidenceService {

    private static final Comparator<HistoricalFeedbackEvidenceView> FEEDBACK_ORDER =
            Comparator.comparing(
                            HistoricalFeedbackEvidenceView::observedAt,
                            Comparator.reverseOrder())
                    .thenComparing(
                            HistoricalFeedbackEvidenceView::attributionId,
                            Comparator.reverseOrder());

    private final BiFunction<DecisionEvidenceQuery, DecisionEvidencePolicy, DecisionEvidenceAggregate>
            decisionEvidenceReader;
    private final Function<HistoricalFeedbackEvidenceQuery, HistoricalFeedbackEvidencePage>
            feedbackEvidenceReader;

    /** 创建只依赖现有只读 service 的 consolidated reader。 */
    public DecisionFeedbackEvidenceService(
            final DecisionEvidenceAggregateService decisionEvidenceService,
            final HistoricalFeedbackEvidenceReadService feedbackEvidenceService) {
        this(
                Objects.requireNonNull(decisionEvidenceService, "decisionEvidenceService")::aggregate,
                Objects.requireNonNull(feedbackEvidenceService, "feedbackEvidenceService")::read);
    }

    /** Package-private pure seam，仅供同包 contract tests 注入无副作用 reader。 */
    DecisionFeedbackEvidenceService(
            final BiFunction<DecisionEvidenceQuery, DecisionEvidencePolicy, DecisionEvidenceAggregate>
                    decisionEvidenceReader,
            final Function<HistoricalFeedbackEvidenceQuery, HistoricalFeedbackEvidencePage>
                    feedbackEvidenceReader) {
        this.decisionEvidenceReader =
                Objects.requireNonNull(decisionEvidenceReader, "decisionEvidenceReader");
        this.feedbackEvidenceReader =
                Objects.requireNonNull(feedbackEvidenceReader, "feedbackEvidenceReader");
    }

    /** 聚合一次 bounded、deterministic、tenant/environment-bound evidence query。 */
    public DecisionFeedbackEvidenceAggregate aggregate(final DecisionFeedbackEvidenceQuery query) {
        if (query == null) {
            throw new IllegalArgumentException(Code.EXECUTION_SCOPE_REQUIRED.name());
        }

        final DecisionEvidenceAggregate decisionEvidence = readDecisionEvidence(query);
        final List<DecisionFeedbackEvidenceFinding> decisionFindings =
                validateDecisionEvidence(query, decisionEvidence);
        if (!decisionFindings.isEmpty()) {
            final EvidenceCompleteness completeness = rootMissing(decisionEvidence)
                            && decisionEvidence.status() != DecisionEvidenceStatus.INVALID
                    ? EvidenceCompleteness.NOT_FOUND
                    : EvidenceCompleteness.INCONSISTENT;
            return result(query, decisionEvidence, List.of(), completeness, decisionFindings);
        }

        final HistoricalFeedbackEvidencePage page;
        try {
            page = Objects.requireNonNull(
                    feedbackEvidenceReader.apply(query.feedbackEvidenceQuery()),
                    "historical feedback page");
        } catch (final RuntimeException error) {
            return result(
                    query,
                    decisionEvidence,
                    List.of(),
                    EvidenceCompleteness.INCONSISTENT,
                    List.of(blocker(
                            Code.FEEDBACK_SOURCE_FAILED,
                            "feedback-source",
                            "历史 feedback evidence 读取失败")));
        }

        final List<HistoricalFeedbackEvidenceView> feedback = List.copyOf(page.items());
        final List<DecisionFeedbackEvidenceFinding> feedbackFindings =
                validateFeedbackEvidence(query, page, feedback);
        if (!feedbackFindings.isEmpty()) {
            return result(
                    query,
                    decisionEvidence,
                    feedback,
                    EvidenceCompleteness.INCONSISTENT,
                    feedbackFindings);
        }
        if (feedback.isEmpty()) {
            return result(
                    query,
                    decisionEvidence,
                    feedback,
                    EvidenceCompleteness.PARTIAL,
                    List.of(new DecisionFeedbackEvidenceFinding(
                            Code.OPTIONAL_FEEDBACK_ABSENT,
                            Severity.INFO,
                            query.decisionId(),
                            "允许缺失的 feedback evidence 不存在")));
        }
        return result(
                query,
                decisionEvidence,
                feedback,
                EvidenceCompleteness.COMPLETE,
                List.of());
    }

    private DecisionEvidenceAggregate readDecisionEvidence(
            final DecisionFeedbackEvidenceQuery query) {
        try {
            return Objects.requireNonNull(
                    decisionEvidenceReader.apply(
                            query.decisionEvidenceQuery(), DecisionEvidencePolicy.CORE_DECISION),
                    "decision evidence aggregate");
        } catch (final RuntimeException error) {
            final DecisionEvidenceQuery decisionQuery = query.decisionEvidenceQuery();
            final DecisionEvidenceAggregate incomplete =
                    decisionQuery.policy().evaluate(decisionQuery, List.of());
            final List<DecisionEvidenceFinding> findings = new ArrayList<>(incomplete.findings());
            findings.add(new DecisionEvidenceFinding(
                    "SOURCE_READ_FAILED",
                    DecisionEvidenceFinding.Severity.BLOCKER,
                    null,
                    "decision-evidence-source",
                    "决策证据来源读取失败"));
            return new DecisionEvidenceAggregate(
                    incomplete.correlation(),
                    List.of(),
                    DecisionEvidenceStatus.INVALID,
                    findings,
                    incomplete.missingMandatoryEvidence());
        }
    }

    private static List<DecisionFeedbackEvidenceFinding> validateDecisionEvidence(
            final DecisionFeedbackEvidenceQuery query,
            final DecisionEvidenceAggregate decisionEvidence) {
        final List<DecisionFeedbackEvidenceFinding> findings = new ArrayList<>();
        addCorrelationFindings(
                query.decisionCorrelation(), decisionEvidence.correlation(), findings, "decision-aggregate");
        for (final DecisionEvidenceRef ref : decisionEvidence.evidenceRefs()) {
            addCorrelationFindings(query.decisionCorrelation(), ref.correlation(), findings, ref.refId());
        }

        final List<DecisionEvidenceRef> requestRefs = refsOfType(
                decisionEvidence, DecisionEvidencePolicy.EvidenceType.REQUEST);
        final List<DecisionEvidenceRef> runRefs = refsOfType(
                decisionEvidence, DecisionEvidencePolicy.EvidenceType.RUN);
        if (!requestRefs.isEmpty()
                && requestRefs.stream().noneMatch(ref ->
                        ref.refId().equals("v5-request:" + query.requestId()))) {
            findings.add(blocker(
                    Code.REQUEST_MISMATCH, query.requestId(), "decision REQUEST identity 不一致"));
        }
        if (!runRefs.isEmpty()
                && runRefs.stream().noneMatch(ref ->
                        ref.refId().equals("v6-run:" + query.decisionRunId()))) {
            findings.add(blocker(
                    Code.RUN_MISMATCH, query.decisionRunId(), "decision RUN identity 不一致"));
        }

        if (decisionEvidence.status() == DecisionEvidenceStatus.INVALID) {
            findings.add(blocker(
                    Code.DECISION_EVIDENCE_INVALID,
                    query.decisionId(),
                    "decision evidence 不满足安全或关联边界"));
        } else if (rootMissing(decisionEvidence)) {
            findings.add(blocker(
                    Code.DECISION_ROOT_NOT_FOUND,
                    query.decisionId(),
                    "强制 REQUEST 或 RUN decision root 不存在"));
        } else if (decisionEvidence.status() != DecisionEvidenceStatus.COMPLETE
                || !decisionEvidence.missingMandatoryEvidence().isEmpty()) {
            findings.add(blocker(
                    Code.DECISION_EVIDENCE_INCOMPLETE,
                    query.decisionId(),
                    "非 root 强制 decision evidence 不完整"));
        }
        return findings;
    }

    private static List<DecisionFeedbackEvidenceFinding> validateFeedbackEvidence(
            final DecisionFeedbackEvidenceQuery query,
            final HistoricalFeedbackEvidencePage page,
            final List<HistoricalFeedbackEvidenceView> feedback) {
        final List<DecisionFeedbackEvidenceFinding> findings = new ArrayList<>();
        if (page.hasNext()) {
            findings.add(blocker(
                    Code.FEEDBACK_RESULT_LIMIT_EXCEEDED,
                    query.decisionId(),
                    "feedback evidence 超出单页上限"));
        }
        if (!feedback.equals(feedback.stream().sorted(FEEDBACK_ORDER).toList())) {
            findings.add(blocker(
                    Code.FEEDBACK_ORDER_INVALID,
                    query.decisionId(),
                    "feedback evidence 顺序不稳定"));
        }

        final Set<String> identities = new HashSet<>();
        for (final HistoricalFeedbackEvidenceView view : feedback) {
            if (!query.executionScope().tenantId().equals(view.tenantId())) {
                findings.add(blocker(Code.TENANT_MISMATCH, view.attributionId(), "feedback tenant 不一致"));
            }
            if (query.executionScope().environment() != view.environment()) {
                findings.add(blocker(
                        Code.ENVIRONMENT_MISMATCH,
                        view.attributionId(),
                        "feedback environment 不一致"));
            }
            if (!query.decisionId().equals(view.decisionId())) {
                findings.add(blocker(
                        Code.DECISION_MISMATCH, view.attributionId(), "feedback decision 不一致"));
            }
            if (!query.traceId().equals(view.traceId())) {
                findings.add(blocker(Code.TRACE_MISMATCH, view.attributionId(), "feedback trace 不一致"));
            }
            final String identity = view.observationId() + ':' + view.attributionId();
            if (!identities.add(identity)) {
                findings.add(blocker(
                        Code.FEEDBACK_IDENTITY_CONFLICT,
                        view.attributionId(),
                        "feedback observation/attribution identity 重复"));
            }
            if (!isSafe(view)) {
                findings.add(blocker(
                        Code.UNSAFE_EVIDENCE_REJECTED,
                        view.attributionId(),
                        "不安全 feedback evidence 已被拒绝"));
            }
        }
        return findings;
    }

    private static boolean isSafe(final HistoricalFeedbackEvidenceView view) {
        try {
            DecisionEvidencePolicy.requireSafeText(view.tenantId(), "feedback.tenantId");
            DecisionEvidencePolicy.requireSafeText(view.decisionId(), "feedback.decisionId");
            DecisionEvidencePolicy.requireSafeText(view.traceId(), "feedback.traceId");
            DecisionEvidencePolicy.requireSafeText(view.observationId(), "feedback.observationId");
            DecisionEvidencePolicy.requireSafeText(view.attributionId(), "feedback.attributionId");
            DecisionEvidencePolicy.requireSafeText(view.policyId(), "feedback.policyId");
            DecisionEvidencePolicy.requireSafeText(view.policyVersion(), "feedback.policyVersion");
            view.contributions().forEach(contribution -> {
                DecisionEvidencePolicy.requireSafeText(
                        contribution.reasonCode(), "feedback.reasonCode");
                DecisionEvidencePolicy.requireSafeText(
                        contribution.evidenceReference(), "feedback.evidenceReference");
            });
            view.references().forEach(reference -> DecisionEvidencePolicy.requireSafeText(
                    reference.value(), "feedback.reference"));
            return true;
        } catch (final RuntimeException error) {
            return false;
        }
    }

    private static void addCorrelationFindings(
            final DecisionEvidenceCorrelation expected,
            final DecisionEvidenceCorrelation actual,
            final List<DecisionFeedbackEvidenceFinding> findings,
            final String safeRef) {
        if (!expected.tenantId().equals(actual.tenantId())) {
            findings.add(blocker(Code.TENANT_MISMATCH, safeRef, "decision tenant 不一致"));
        }
        if (!expected.traceId().equals(actual.traceId())) {
            findings.add(blocker(Code.TRACE_MISMATCH, safeRef, "decision trace 不一致"));
        }
        if (!expected.requestId().equals(actual.requestId())) {
            findings.add(blocker(Code.REQUEST_MISMATCH, safeRef, "decision request 不一致"));
        }
        if (!expected.decisionId().equals(actual.decisionId())) {
            findings.add(blocker(Code.DECISION_MISMATCH, safeRef, "decision identity 不一致"));
        }
    }

    private static List<DecisionEvidenceRef> refsOfType(
            final DecisionEvidenceAggregate aggregate,
            final DecisionEvidencePolicy.EvidenceType type) {
        return aggregate.evidenceRefs().stream()
                .filter(ref -> ref.evidenceType() == type)
                .toList();
    }

    private static boolean rootMissing(final DecisionEvidenceAggregate aggregate) {
        return aggregate.missingMandatoryEvidence().contains(DecisionEvidencePolicy.EvidenceType.REQUEST)
                || aggregate.missingMandatoryEvidence().contains(DecisionEvidencePolicy.EvidenceType.RUN)
                || refsOfType(aggregate, DecisionEvidencePolicy.EvidenceType.REQUEST).isEmpty()
                || refsOfType(aggregate, DecisionEvidencePolicy.EvidenceType.RUN).isEmpty();
    }

    private static DecisionFeedbackEvidenceFinding blocker(
            final Code code, final String safeRef, final String summary) {
        return new DecisionFeedbackEvidenceFinding(code, Severity.BLOCKER, safeRef, summary);
    }

    private static DecisionFeedbackEvidenceAggregate result(
            final DecisionFeedbackEvidenceQuery query,
            final DecisionEvidenceAggregate decisionEvidence,
            final List<HistoricalFeedbackEvidenceView> feedback,
            final EvidenceCompleteness completeness,
            final List<DecisionFeedbackEvidenceFinding> findings) {
        return new DecisionFeedbackEvidenceAggregate(
                query.executionScope(),
                query.decisionCorrelation(),
                decisionEvidence,
                feedback,
                completeness,
                findings);
    }
}

package com.guidinglight.decisionhub.usecase.qdr.evidence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Stage-QDR-6 B2 的 evidence 完整度、冲突与稳定顺序评估器。
 *
 * <p>评估器复用 B1 policy，不读取任何 port。来源读取、映射或 correlation 失败由调用方提供结构化
 * finding；ERROR/BLOCKER finding 会强制 aggregate 为 INVALID，避免部分成功掩盖来源失败。
 */
public final class DecisionEvidenceConsistencyEvaluator {

    private static final Comparator<DecisionEvidenceRef> REF_ORDER = Comparator
            .comparing((DecisionEvidenceRef ref) -> ref.evidenceType().name())
            .thenComparing(DecisionEvidenceRef::refId)
            .thenComparing(DecisionEvidenceRef::sourceType)
            .thenComparing(ref -> Objects.toString(ref.contentHash(), ""));

    private static final Comparator<DecisionEvidenceFinding> FINDING_ORDER = Comparator
            .comparing(DecisionEvidenceFinding::code)
            .thenComparing(finding -> finding.evidenceType() == null
                    ? ""
                    : finding.evidenceType().name())
            .thenComparing(finding -> Objects.toString(finding.safeRef(), ""))
            .thenComparing(DecisionEvidenceFinding::message);

    /**
     * 合并 B1 policy 结果与 B2 来源 findings，并稳定排序输出。
     *
     * @param query 已校验 evidence query。
     * @param evidenceRefs 已投影的安全 evidence refs。
     * @param sourceFindings 来源读取、映射和 correlation findings。
     * @return fail-closed 且顺序稳定的 aggregate。
     */
    public DecisionEvidenceAggregate evaluate(
            final DecisionEvidenceQuery query,
            final List<DecisionEvidenceRef> evidenceRefs,
            final List<DecisionEvidenceFinding> sourceFindings) {
        final DecisionEvidenceQuery checkedQuery = Objects.requireNonNull(query, "query");
        final List<DecisionEvidenceRef> orderedRefs = Objects.requireNonNullElse(evidenceRefs, List.<DecisionEvidenceRef>of())
                .stream()
                .sorted(REF_ORDER)
                .toList();
        final DecisionEvidenceAggregate policyResult = checkedQuery.policy().evaluate(checkedQuery, orderedRefs);
        final List<DecisionEvidenceFinding> combined = new ArrayList<>(policyResult.findings());
        combined.addAll(Objects.requireNonNullElse(sourceFindings, List.of()));
        final boolean sourceInvalid = Objects.requireNonNullElse(sourceFindings, List.<DecisionEvidenceFinding>of())
                .stream()
                .anyMatch(finding ->
                finding.severity() == DecisionEvidenceFinding.Severity.ERROR
                        || finding.severity() == DecisionEvidenceFinding.Severity.BLOCKER);
        final DecisionEvidenceStatus status = sourceInvalid
                ? DecisionEvidenceStatus.INVALID
                : policyResult.status();
        return new DecisionEvidenceAggregate(
                policyResult.correlation(),
                orderedRefs,
                status,
                combined.stream().sorted(FINDING_ORDER).toList(),
                policyResult.missingMandatoryEvidence());
    }
}

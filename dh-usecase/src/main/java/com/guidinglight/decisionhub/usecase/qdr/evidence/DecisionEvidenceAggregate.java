package com.guidinglight.decisionhub.usecase.qdr.evidence;

import java.util.List;
import java.util.Objects;

/**
 * B1 evidence correlation 的只读聚合合同。
 *
 * <p>aggregate 只表达调用方已经提供的 evidence refs、完整度和安全 finding，不包含原始 payload、prompt、
 * provider response、credential 或执行授权。调用方应通过 {@link #evaluate(DecisionEvidenceQuery, List)}
 * 取得状态，不能用该对象执行任何 repository 读取或 replay。
 *
 * @param correlation              aggregate 的严格四键关联。
 * @param evidenceRefs             调用方传入的 typed safe refs。
 * @param status                   fail-closed 评估状态。
 * @param findings                 仅安全 code/ref/message 的 finding 列表。
 * @param missingMandatoryEvidence 当前 policy 缺失的强制 evidence 类型。
 */
public record DecisionEvidenceAggregate(
        DecisionEvidenceCorrelation correlation,
        List<DecisionEvidenceRef> evidenceRefs,
        DecisionEvidenceStatus status,
        List<DecisionEvidenceFinding> findings,
        List<DecisionEvidencePolicy.EvidenceType> missingMandatoryEvidence) {

    /** 复制 aggregate 集合，防止调用方在构造后篡改 evidence、finding 或缺失类型。 */
    public DecisionEvidenceAggregate {
        correlation = Objects.requireNonNull(correlation, "correlation");
        evidenceRefs = List.copyOf(Objects.requireNonNullElse(evidenceRefs, List.of()));
        status = Objects.requireNonNull(status, "status");
        findings = List.copyOf(Objects.requireNonNullElse(findings, List.of()));
        missingMandatoryEvidence =
                List.copyOf(Objects.requireNonNullElse(missingMandatoryEvidence, List.of()));
        if (status == DecisionEvidenceStatus.COMPLETE && !missingMandatoryEvidence.isEmpty()) {
            throw new IllegalArgumentException("COMPLETE aggregate cannot declare missing mandatory evidence");
        }
        if (status == DecisionEvidenceStatus.INCOMPLETE && missingMandatoryEvidence.isEmpty()) {
            throw new IllegalArgumentException("INCOMPLETE aggregate must declare missing mandatory evidence");
        }
    }

    /**
     * 使用 query 指定的固定 policy 评估已提供 evidence refs。
     *
     * @param query tenant-bound evidence query。
     * @param evidenceRefs 待评估的 typed safe refs。
     * @return 纯 contract evaluation 结果。
     */
    public static DecisionEvidenceAggregate evaluate(
            final DecisionEvidenceQuery query, final List<DecisionEvidenceRef> evidenceRefs) {
        final DecisionEvidenceQuery checkedQuery = Objects.requireNonNull(query, "query");
        return checkedQuery.policy().evaluate(checkedQuery, evidenceRefs);
    }

    /**
     * 指示 aggregate 是否已经通过当前 profile 的完整度与安全校验。
     *
     * @return 仅在 status 为 COMPLETE 时返回 true。
     */
    public boolean isComplete() {
        return status == DecisionEvidenceStatus.COMPLETE;
    }
}

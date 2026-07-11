package com.guidinglight.decisionhub.usecase.qdr.evidence;

import java.util.Objects;

/**
 * Stage-QDR-6 B2 的 correlation 解析与来源一致性校验器。
 *
 * <p>该类只比较 tenantId、traceId、requestId、decisionId 四个稳定键，不推断缺失 ID，
 * 也不把当前 V5 的 ID 映射升级为永久 schema 规则。任一来源缺键或不一致都返回 false，
 * 由聚合服务转换为结构化 fail-closed finding。
 */
public final class DecisionEvidenceCorrelationResolver {

    /**
     * 从已校验 query 解析唯一主关联键。
     *
     * @param query evidence 查询合同。
     * @return tenant-bound 四键 correlation。
     */
    public DecisionEvidenceCorrelation resolve(final DecisionEvidenceQuery query) {
        return Objects.requireNonNull(query, "query").correlation();
    }

    /**
     * 校验完整四键来源是否严格匹配。
     *
     * @param expected 查询主关联键。
     * @param tenantId 来源 tenantId。
     * @param traceId 来源 traceId。
     * @param requestId 来源 requestId。
     * @param decisionId 来源 decisionId。
     * @return 所有键均非空且完全一致时返回 true。
     */
    public boolean matches(
            final DecisionEvidenceCorrelation expected,
            final String tenantId,
            final String traceId,
            final String requestId,
            final String decisionId) {
        final DecisionEvidenceCorrelation checked = Objects.requireNonNull(expected, "expected");
        return checked.tenantId().equals(tenantId)
                && checked.traceId().equals(traceId)
                && checked.requestId().equals(requestId)
                && checked.decisionId().equals(decisionId);
    }

    /**
     * 校验不携带 decisionId 的 V6/V8 来源。
     *
     * <p>这些来源必须匹配 tenant/trace/request；decisionId 仍由已经通过完整校验的 V5/V9 来源证明，
     * 本方法不会从 requestId 推导 decisionId。
     *
     * @param expected 查询主关联键。
     * @param tenantId 来源 tenantId。
     * @param traceId 来源 traceId。
     * @param requestId 来源 requestId。
     * @return 三个可用关联键完全一致时返回 true。
     */
    public boolean matchesWithoutDecision(
            final DecisionEvidenceCorrelation expected,
            final String tenantId,
            final String traceId,
            final String requestId) {
        final DecisionEvidenceCorrelation checked = Objects.requireNonNull(expected, "expected");
        return checked.tenantId().equals(tenantId)
                && checked.traceId().equals(traceId)
                && checked.requestId().equals(requestId);
    }
}

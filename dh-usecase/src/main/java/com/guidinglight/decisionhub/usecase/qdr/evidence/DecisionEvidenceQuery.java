package com.guidinglight.decisionhub.usecase.qdr.evidence;

/**
 * B1 evidence aggregate 的查询合同。
 *
 * <p>四个主关联键始终必填；其余 selector 只能收窄后续 B2 的只读范围，不能替代 tenant、trace、request 或
 * decision 的关联键。本对象本身不声明 Repository、JDBC 或任何外部客户端。
 *
 * @param tenantId       租户边界。
 * @param traceId        可审计链路 ID。
 * @param requestId      外部请求 ID。
 * @param decisionId     决策 ID。
 * @param decisionRunId  可选内部 decision run selector。
 * @param caseId         可选 QDR replay case selector。
 * @param evaluationId   可选 QDR evaluation selector。
 * @param verdictId      可选 regression verdict selector。
 * @param providerRef    可选 provider safe ref selector。
 * @param policy         固定的 evidence completeness profile。
 */
public record DecisionEvidenceQuery(
        String tenantId,
        String traceId,
        String requestId,
        String decisionId,
        String decisionRunId,
        String caseId,
        String evaluationId,
        String verdictId,
        String providerRef,
        DecisionEvidencePolicy policy) {

    /** 校验主关联键和可选 safe selector，禁止 selector 替代主关联键。 */
    public DecisionEvidenceQuery {
        tenantId = DecisionEvidencePolicy.requireSafeText(tenantId, "tenantId");
        traceId = DecisionEvidencePolicy.requireSafeText(traceId, "traceId");
        requestId = DecisionEvidencePolicy.requireSafeText(requestId, "requestId");
        decisionId = DecisionEvidencePolicy.requireSafeText(decisionId, "decisionId");
        decisionRunId = DecisionEvidencePolicy.optionalSafeText(decisionRunId, "decisionRunId");
        caseId = DecisionEvidencePolicy.optionalSafeText(caseId, "caseId");
        evaluationId = DecisionEvidencePolicy.optionalSafeText(evaluationId, "evaluationId");
        verdictId = DecisionEvidencePolicy.optionalSafeText(verdictId, "verdictId");
        providerRef = DecisionEvidencePolicy.optionalSafeText(providerRef, "providerRef");
        policy = DecisionEvidencePolicy.requirePolicy(policy);
    }

    /**
     * 返回本查询对应的严格主关联键。
     *
     * @return 包含 tenantId、traceId、requestId 与 decisionId 的 correlation。
     */
    public DecisionEvidenceCorrelation correlation() {
        return new DecisionEvidenceCorrelation(tenantId, traceId, requestId, decisionId);
    }
}

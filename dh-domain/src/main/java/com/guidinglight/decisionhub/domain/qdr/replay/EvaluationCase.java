package com.guidinglight.decisionhub.domain.qdr.replay;

/**
 * QDR evaluation case 合同。
 *
 * <p>该对象只绑定 replay case 与结构化 expected/actual summary，不执行真实 replay，不接 HTTP 或真实 provider。
 */
public record EvaluationCase(
        String tenantId,
        String evaluationId,
        String caseId,
        String policyVersion,
        String modelVersionRef,
        String modelGatewayVersionRef,
        ExpectedDecisionSummary expectedSummary,
        ExpectedDecisionSummary actualSummary,
        RegressionVerdict verdict,
        ReplayOutputRef outputRef) {

    /**
     * 规范化 evaluation case 标识字段；跨 tenant 和缺失字段由 usecase 合同服务 fail-closed。
     */
    public EvaluationCase {
        tenantId = trimToNull(tenantId);
        evaluationId = trimToNull(evaluationId);
        caseId = trimToNull(caseId);
        policyVersion = trimToNull(policyVersion);
        modelVersionRef = trimToNull(modelVersionRef);
        modelGatewayVersionRef = trimToNull(modelGatewayVersionRef);
    }

    private static String trimToNull(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

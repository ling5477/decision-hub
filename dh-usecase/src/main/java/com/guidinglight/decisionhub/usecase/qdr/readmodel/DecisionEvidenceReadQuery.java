package com.guidinglight.decisionhub.usecase.qdr.readmodel;

/**
 * stage-qdr-2 B1 read query：用于读取 decision run 证据引用集合。
 *
 * <p>默认返回脱敏摘要与引用，不返回 raw prompt/credential/provider-response。
 */
public record DecisionEvidenceReadQuery(
        String tenantId, String decisionRunId, String requesterId, String traceId) {

    public DecisionEvidenceReadQuery {
        tenantId = ReadModelValidation.requireText(tenantId, "tenantId");
        decisionRunId = ReadModelValidation.requireText(decisionRunId, "decisionRunId");
        requesterId = ReadModelValidation.optionalText(requesterId, "requesterId");
        traceId = ReadModelValidation.optionalText(traceId, "traceId");
    }
}

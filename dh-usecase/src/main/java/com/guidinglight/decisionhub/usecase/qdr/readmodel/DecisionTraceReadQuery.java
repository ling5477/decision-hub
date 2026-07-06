package com.guidinglight.decisionhub.usecase.qdr.readmodel;

/**
 * stage-qdr-2 B1 read query：用于读取 decision run 的 trace timeline。
 *
 * <p>查询默认只读，不触发 replay execution 或外部 HTTP，并必须 tenant-bound。
 */
public record DecisionTraceReadQuery(
        String tenantId, String decisionRunId, String requesterId, String traceId) {

    public DecisionTraceReadQuery {
        tenantId = ReadModelValidation.requireText(tenantId, "tenantId");
        decisionRunId = ReadModelValidation.requireText(decisionRunId, "decisionRunId");
        requesterId = ReadModelValidation.optionalText(requesterId, "requesterId");
        traceId = ReadModelValidation.optionalText(traceId, "traceId");
    }
}

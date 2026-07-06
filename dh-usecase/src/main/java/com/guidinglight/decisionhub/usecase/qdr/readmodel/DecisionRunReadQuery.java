package com.guidinglight.decisionhub.usecase.qdr.readmodel;

/**
 * stage-qdr-2 B1 read query：用于读取单条 decision run 只读详情。
 *
 * <p>查询必须 tenant-bound，不允许跨租户查询，不允许空白 tenantId/decisionRunId。任何
 * replay / provider 触发一律由更下层实现约束，本 contract 仅描述查询意图。
 */
public record DecisionRunReadQuery(String tenantId, String decisionRunId, String requesterId, String traceId) {

    public DecisionRunReadQuery {
        tenantId = ReadModelValidation.requireText(tenantId, "tenantId");
        decisionRunId = ReadModelValidation.requireText(decisionRunId, "decisionRunId");
        requesterId = ReadModelValidation.optionalText(requesterId, "requesterId");
        traceId = ReadModelValidation.optionalText(traceId, "traceId");
    }
}

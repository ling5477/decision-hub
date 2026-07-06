package com.guidinglight.decisionhub.usecase.qdr.readmodel;

import java.util.List;

/**
 * stage-qdr-2 B1 decision run trace timeline 只读视图。
 *
 * <p>该 DTO 只是查询结果 projection，允许空 steps 表示 trace 尚未物化；构造过程不排序、不重放、
 * 不调用 provider，也不触发外部 HTTP。
 */
public record DecisionTraceTimelineView(
        String decisionRunId, String tenantId, String traceId, String requestId, List<DecisionTraceStepView> steps) {

    public DecisionTraceTimelineView {
        decisionRunId = ReadModelValidation.requireText(decisionRunId, "decisionRunId");
        tenantId = ReadModelValidation.requireText(tenantId, "tenantId");
        traceId = ReadModelValidation.requireText(traceId, "traceId");
        requestId = ReadModelValidation.requireText(requestId, "requestId");
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}

package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;

import java.util.Map;

/**
 * Prompt render 上下文。
 *
 * <p>上下文必须 tenant-bound，并且只承载本地 deterministic render 所需的脱敏输入；B1 不写 persistence、
 * 不调用 provider。
 *
 * @param tenantId  tenant 边界。
 * @param traceId   traceId。
 * @param requestId requestId。
 * @param inputs    render input map。
 */
public record PromptRenderContext(
        String tenantId, String traceId, String requestId, Map<String, String> inputs) {

    /**
     * 校验 render context 并复制 input map。
     */
    public PromptRenderContext {
        tenantId = PromptModelSafetyRules.requireText(tenantId, "tenantId");
        traceId = PromptModelSafetyRules.requireText(traceId, "traceId");
        requestId = PromptModelSafetyRules.requireText(requestId, "requestId");
        inputs = Map.copyOf(inputs == null ? Map.of() : inputs);
    }
}

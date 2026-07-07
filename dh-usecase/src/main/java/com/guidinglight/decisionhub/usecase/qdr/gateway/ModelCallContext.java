package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Model gateway 调用上下文。
 *
 * <p>字段在 record 中保持原样，便于 gateway service 对缺失值返回结构化 fail-closed 结果，而不是在
 * 构造阶段抛出带调用方上下文的异常。
 *
 * @param tenantId          tenant 边界。
 * @param traceId           traceId。
 * @param requestId         requestId。
 * @param decisionRunId     decision run id。
 * @param promptVersionId   prompt version id。
 * @param modelVersionId    model version id。
 * @param providerProfileId provider profile id。
 */
public record ModelCallContext(
        String tenantId,
        String traceId,
        String requestId,
        String decisionRunId,
        String promptVersionId,
        String modelVersionId,
        String providerProfileId) {
}

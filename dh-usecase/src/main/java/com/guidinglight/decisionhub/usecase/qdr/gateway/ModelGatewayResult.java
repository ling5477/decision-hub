package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.Objects;

/**
 * Model gateway 结构化结果。
 *
 * <p>成功结果只返回 redacted summary、refs 与 structured readonly decision；失败结果只返回固定
 * failure code/message。该 record 没有 raw prompt 或 raw provider response 字段。
 *
 * @param success                  true 表示 mock gateway 成功。
 * @param failure                  fail-closed failure。
 * @param decision                 structured readonly decision。
 * @param tenantId                 tenant ref。
 * @param traceId                  trace ref。
 * @param requestId                request ref。
 * @param decisionRunId            decision run ref。
 * @param promptVersionId          prompt version ref。
 * @param modelVersionId           model version ref。
 * @param providerProfileId        provider profile ref。
 * @param providerTrustDecisionRef provider trust decision ref。
 * @param modelCallRef             planned model call ref。
 * @param auditRef                 planned audit ref。
 * @param redactedSummary          safe summary。
 * @param usage                    usage summary。
 */
public record ModelGatewayResult(
        boolean success,
        ModelGatewayFailure failure,
        ModelGatewayDecision decision,
        String tenantId,
        String traceId,
        String requestId,
        String decisionRunId,
        String promptVersionId,
        String modelVersionId,
        String providerProfileId,
        String providerTrustDecisionRef,
        String modelCallRef,
        String auditRef,
        String redactedSummary,
        ModelGatewayUsage usage) {

    /**
     * 校验 result 成功/失败互斥形态。
     */
    public ModelGatewayResult {
        if (success) {
            decision = Objects.requireNonNull(decision, "decision");
            providerTrustDecisionRef =
                    Objects.requireNonNull(providerTrustDecisionRef, "providerTrustDecisionRef");
            modelCallRef = Objects.requireNonNull(modelCallRef, "modelCallRef");
            auditRef = Objects.requireNonNull(auditRef, "auditRef");
            redactedSummary = Objects.requireNonNull(redactedSummary, "redactedSummary");
            usage = Objects.requireNonNull(usage, "usage");
            failure = null;
        } else {
            failure = Objects.requireNonNull(failure, "failure");
            decision = null;
        }
    }

    /**
     * 创建 fail-closed result。
     *
     * @param code    failure code。
     * @param request gateway request，可为空。
     * @return fail-closed structured result。
     */
    public static ModelGatewayResult failure(
            final ModelGatewayFailureCode code, final ModelGatewayRequest request) {
        final ModelCallContext context = request == null ? null : request.context();
        return new ModelGatewayResult(
                false,
                ModelGatewayFailure.of(code),
                null,
                context == null ? null : context.tenantId(),
                context == null ? null : context.traceId(),
                context == null ? null : context.requestId(),
                context == null ? null : context.decisionRunId(),
                context == null ? null : context.promptVersionId(),
                context == null ? null : context.modelVersionId(),
                context == null ? null : context.providerProfileId(),
                null,
                null,
                null,
                null,
                null);
    }

    /**
     * 创建 success result。
     *
     * @param request                  gateway request。
     * @param decision                 structured decision。
     * @param providerTrustDecisionRef trust decision ref。
     * @param modelCallRef             model call ref。
     * @param auditRef                 audit ref。
     * @param redactedSummary          safe summary。
     * @param usage                    usage summary。
     * @return success result。
     */
    public static ModelGatewayResult success(
            final ModelGatewayRequest request,
            final ModelGatewayDecision decision,
            final String providerTrustDecisionRef,
            final String modelCallRef,
            final String auditRef,
            final String redactedSummary,
            final ModelGatewayUsage usage) {
        final ModelCallContext context = request.context();
        return new ModelGatewayResult(
                true,
                null,
                decision,
                context.tenantId(),
                context.traceId(),
                context.requestId(),
                context.decisionRunId(),
                context.promptVersionId(),
                context.modelVersionId(),
                context.providerProfileId(),
                providerTrustDecisionRef,
                modelCallRef,
                auditRef,
                redactedSummary,
                usage);
    }
}

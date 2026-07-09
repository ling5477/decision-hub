package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Provider trust decision 的安全摘要。
 *
 * <p>`ALLOWED` 仅表示 mock / policy context 中通过检查，不代表真实 provider authorization、LIVE
 * permission 或交易权限。
 *
 * @param decision    trust decision。
 * @param decisionRef safe decision ref，不含原始 provider 或 credential 材料。
 */
public record ProviderTrustDecisionSummary(Decision decision, String decisionRef) {

    /**
     * Trust decision 支持值。
     */
    public enum Decision {
        /** mock / policy context 下允许继续。 */
        ALLOWED,
        /** policy 拒绝，必须 fail-closed。 */
        DENIED,
        /** 降级 evidence，不可执行。 */
        DEGRADED,
        /** 跳过 provider evidence，不可执行。 */
        SKIPPED
    }

    /**
     * 创建 allowed trust summary。
     *
     * @param decisionRef safe decision ref。
     * @return trust decision summary。
     */
    public static ProviderTrustDecisionSummary allowed(final String decisionRef) {
        return new ProviderTrustDecisionSummary(Decision.ALLOWED, decisionRef);
    }

    /**
     * 创建 denied trust summary。
     *
     * @param decisionRef safe decision ref。
     * @return trust decision summary。
     */
    public static ProviderTrustDecisionSummary denied(final String decisionRef) {
        return new ProviderTrustDecisionSummary(Decision.DENIED, decisionRef);
    }

    /**
     * 创建 degraded trust summary。
     *
     * @param decisionRef safe decision ref。
     * @return trust decision summary。
     */
    public static ProviderTrustDecisionSummary degraded(final String decisionRef) {
        return new ProviderTrustDecisionSummary(Decision.DEGRADED, decisionRef);
    }

    /**
     * 创建 skipped trust summary。
     *
     * @param decisionRef safe decision ref。
     * @return trust decision summary。
     */
    public static ProviderTrustDecisionSummary skipped(final String decisionRef) {
        return new ProviderTrustDecisionSummary(Decision.SKIPPED, decisionRef);
    }
}

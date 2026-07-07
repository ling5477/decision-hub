package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.Objects;

/**
 * ProviderTrustPolicy 结构化判定。
 *
 * <p>decisionRef 是 safe ref，不含 provider credential、endpoint、raw prompt 或 raw provider response。
 *
 * @param allowed     true 表示允许进入 provider call。
 * @param failureCode 拒绝时的 fail-closed code。
 * @param decisionRef safe trust decision ref。
 */
public record ModelProviderTrustDecision(
        boolean allowed, ModelGatewayFailureCode failureCode, String decisionRef) {

    /**
     * 校验 trust decision 形态。
     */
    public ModelProviderTrustDecision {
        if (allowed) {
            failureCode = null;
            decisionRef = Objects.requireNonNull(decisionRef, "decisionRef");
        } else {
            failureCode = Objects.requireNonNullElse(failureCode, ModelGatewayFailureCode.POLICY_DENIED);
            decisionRef = Objects.requireNonNullElse(decisionRef, "provider-trust-denied");
        }
    }

    /**
     * 创建允许结果。
     *
     * @param decisionRef safe decision ref。
     * @return allow decision。
     */
    public static ModelProviderTrustDecision allowed(final String decisionRef) {
        return new ModelProviderTrustDecision(true, null, decisionRef);
    }

    /**
     * 创建拒绝结果。
     *
     * @param failureCode failure code。
     * @param decisionRef safe decision ref。
     * @return denied decision。
     */
    public static ModelProviderTrustDecision denied(
            final ModelGatewayFailureCode failureCode, final String decisionRef) {
        return new ModelProviderTrustDecision(false, failureCode, decisionRef);
    }
}

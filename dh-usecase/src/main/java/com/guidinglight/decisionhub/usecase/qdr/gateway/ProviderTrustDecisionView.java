package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.Objects;

/**
 * Provider trust decision 的安全只读视图。
 *
 * <p>`ALLOWED` 只表示 mock / policy context 下通过检查，不能解释为 provider 授权、交易许可或上线许可。
 *
 * @param decision    trust decision。
 * @param decisionRef safe decision ref。
 * @param boundaryRef 固定边界 ref，说明该 view 只属于内部 read model。
 */
public record ProviderTrustDecisionView(
        ProviderTrustDecisionSummary.Decision decision, String decisionRef, String boundaryRef) {

    /** 校验 trust decision view 的 safe ref 边界。 */
    public ProviderTrustDecisionView {
        decision = Objects.requireNonNull(decision, "decision");
        final ProviderTrustDecisionSummary checked =
                new ModelGatewayObservabilityContractService()
                        .validate(new ProviderTrustDecisionSummary(decision, decisionRef));
        decisionRef = checked.decisionRef();
        boundaryRef = new ModelGatewayObservabilityContractService()
                .validate(ProviderTrustDecisionSummary.skipped(boundaryRef))
                .decisionRef();
    }

    static ProviderTrustDecisionView from(final ProviderTrustDecisionSummary summary) {
        final ProviderTrustDecisionSummary checked =
                new ModelGatewayObservabilityContractService().validate(summary);
        return new ProviderTrustDecisionView(
                checked.decision(), checked.decisionRef(), "internal-read-model-only");
    }
}

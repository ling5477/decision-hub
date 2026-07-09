package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.Objects;

/**
 * B4 report 中的 trust decision 摘要。
 *
 * <p>`ALLOWED` 仅表示 mock / policy context 下通过检查，不能解释为 provider 授权、上线许可、LIVE
 * permission、trading permission 或 NQ execution permission。
 *
 * @param decision    trust decision。
 * @param decisionRef trust decision safe ref。
 * @param boundaryRef 固定 report boundary ref。
 */
public record ProviderTrustDecisionReport(
        ProviderTrustDecisionSummary.Decision decision, String decisionRef, String boundaryRef) {

    /**
     * 校验 trust decision report 的 safe ref 边界。
     */
    public ProviderTrustDecisionReport {
        decision = Objects.requireNonNull(decision, "decision");
        final ProviderTrustDecisionSummary checked = new ModelGatewayObservabilityContractService()
                .validate(new ProviderTrustDecisionSummary(decision, decisionRef));
        decisionRef = checked.decisionRef();
        boundaryRef = ObservabilityReportSafety.requireSafeText(boundaryRef, "boundaryRef");
    }

    static ProviderTrustDecisionReport from(final ProviderTrustDecisionSummary summary) {
        final ProviderTrustDecisionSummary checked =
                new ModelGatewayObservabilityContractService().validate(summary);
        return new ProviderTrustDecisionReport(
                checked.decision(), checked.decisionRef(), "internal-report-only");
    }
}

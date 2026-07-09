package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.List;

/**
 * Provider readiness signal 的安全只读视图。
 *
 * <p>`READY` 只表示内部 readiness evidence 满足后续评估条件，不开启真实 provider、HTTP 或 LIVE。
 *
 * @param status       readiness status。
 * @param readinessRef readiness safe ref。
 * @param findings     readiness finding 列表。
 */
public record ProviderReadinessSignalView(
        ProviderReadinessStatus status, String readinessRef, List<ProviderReadinessFinding> findings) {

    /** 校验 readiness signal view 的 redaction 与 runtime 边界。 */
    public ProviderReadinessSignalView {
        final ProviderReadinessSignal checked = new ModelGatewayObservabilityContractService()
                .validate(new ProviderReadinessSignal(status, readinessRef, findings));
        status = checked.status();
        readinessRef = checked.readinessRef();
        findings = checked.findings();
    }

    static ProviderReadinessSignalView from(final ProviderReadinessSignal signal) {
        final ProviderReadinessSignal checked =
                new ModelGatewayObservabilityContractService().validate(signal);
        return new ProviderReadinessSignalView(
                checked.status(), checked.readinessRef(), checked.findings());
    }
}

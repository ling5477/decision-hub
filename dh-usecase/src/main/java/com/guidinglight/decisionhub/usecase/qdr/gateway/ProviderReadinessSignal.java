package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.List;

/**
 * Provider readiness 的非执行信号。
 *
 * <p>`READY` 仅表示满足未来 readiness 条件，不开启真实 provider、HTTP、LIVE 或交易权限。字段合法性由
 * {@link ModelGatewayObservabilityContractService} 统一校验。
 *
 * @param status       readiness status。
 * @param readinessRef safe readiness ref。
 * @param findings     readiness findings。
 */
public record ProviderReadinessSignal(
        ProviderReadinessStatus status, String readinessRef, List<ProviderReadinessFinding> findings) {

    /**
     * 复制 finding 列表，避免调用方后续修改。
     */
    public ProviderReadinessSignal {
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    /**
     * 创建 ready signal。
     *
     * @param readinessRef safe readiness ref。
     * @return readiness signal。
     */
    public static ProviderReadinessSignal ready(final String readinessRef) {
        return new ProviderReadinessSignal(ProviderReadinessStatus.READY, readinessRef, List.of());
    }

    /**
     * 创建 not-ready signal。
     *
     * @param readinessRef safe readiness ref。
     * @param findings     readiness findings。
     * @return readiness signal。
     */
    public static ProviderReadinessSignal notReady(
            final String readinessRef, final List<ProviderReadinessFinding> findings) {
        return new ProviderReadinessSignal(ProviderReadinessStatus.NOT_READY, readinessRef, findings);
    }

    /**
     * 创建 degraded signal。
     *
     * @param readinessRef safe readiness ref。
     * @param findings     readiness findings。
     * @return readiness signal。
     */
    public static ProviderReadinessSignal degraded(
            final String readinessRef, final List<ProviderReadinessFinding> findings) {
        return new ProviderReadinessSignal(ProviderReadinessStatus.DEGRADED, readinessRef, findings);
    }

    /**
     * 创建 skipped signal。
     *
     * @param readinessRef safe readiness ref。
     * @param findings     readiness findings。
     * @return readiness signal。
     */
    public static ProviderReadinessSignal skipped(
            final String readinessRef, final List<ProviderReadinessFinding> findings) {
        return new ProviderReadinessSignal(ProviderReadinessStatus.SKIPPED, readinessRef, findings);
    }
}

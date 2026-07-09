package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Provider latency / budget 的安全只读视图。
 *
 * <p>只展示本地 summary 数值，不表示真实 provider billing、HTTP SLA 或 execution permission。
 *
 * @param p50Ms           p50 延迟毫秒。
 * @param p95Ms           p95 延迟毫秒。
 * @param p99Ms           p99 延迟毫秒。
 * @param timeoutMs       timeout budget 毫秒。
 * @param budgetUsedRatio 预算使用比例，范围 0..1。
 * @param sampleCount     样本数。
 */
public record ProviderLatencyBudgetView(
        long p50Ms,
        long p95Ms,
        long p99Ms,
        long timeoutMs,
        double budgetUsedRatio,
        long sampleCount) {

    /** 校验 latency / budget view 的数值边界。 */
    public ProviderLatencyBudgetView {
        new ModelGatewayObservabilityContractService()
                .validate(new ProviderLatencyBudgetSummary(
                        p50Ms, p95Ms, p99Ms, timeoutMs, budgetUsedRatio, sampleCount));
    }

    static ProviderLatencyBudgetView from(final ProviderLatencyBudgetSummary summary) {
        final ProviderLatencyBudgetSummary checked =
                new ModelGatewayObservabilityContractService().validate(summary);
        return new ProviderLatencyBudgetView(
                checked.p50Ms(),
                checked.p95Ms(),
                checked.p99Ms(),
                checked.timeoutMs(),
                checked.budgetUsedRatio(),
                checked.sampleCount());
    }
}

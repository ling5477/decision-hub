package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * B4 report 中的 latency / budget 摘要。
 *
 * <p>字段只表示内部观测数值，不表示真实 provider billing、真实 HTTP SLA 或 execution permission。
 *
 * @param p50Ms           p50 延迟毫秒。
 * @param p95Ms           p95 延迟毫秒。
 * @param p99Ms           p99 延迟毫秒。
 * @param timeoutMs       timeout budget 毫秒。
 * @param budgetUsedRatio 预算使用比例。
 * @param sampleCount     样本数。
 * @param summaryRef      latency / budget safe summary ref。
 */
public record ProviderLatencyBudgetReport(
        long p50Ms,
        long p95Ms,
        long p99Ms,
        long timeoutMs,
        double budgetUsedRatio,
        long sampleCount,
        String summaryRef) {

    /**
     * 校验 latency / budget report 的数值与 safe ref 边界。
     */
    public ProviderLatencyBudgetReport {
        new ModelGatewayObservabilityContractService()
                .validate(new ProviderLatencyBudgetSummary(
                        p50Ms, p95Ms, p99Ms, timeoutMs, budgetUsedRatio, sampleCount));
        summaryRef = ObservabilityReportSafety.requireSafeText(summaryRef, "summaryRef");
    }

    static ProviderLatencyBudgetReport from(final ProviderLatencyBudgetSummary summary) {
        final ProviderLatencyBudgetSummary checked =
                new ModelGatewayObservabilityContractService().validate(summary);
        return new ProviderLatencyBudgetReport(
                checked.p50Ms(),
                checked.p95Ms(),
                checked.p99Ms(),
                checked.timeoutMs(),
                checked.budgetUsedRatio(),
                checked.sampleCount(),
                "latency-budget:summary");
    }
}

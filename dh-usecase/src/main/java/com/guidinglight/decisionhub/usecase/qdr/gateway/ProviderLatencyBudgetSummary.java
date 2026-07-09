package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Provider observability 的安全延迟 / 预算摘要。
 *
 * <p>字段只保存数值 summary，不保存 call payload、prompt 正文或 provider 原始输出；具体合法性由
 * {@link ModelGatewayObservabilityContractService} 统一校验并 fail-closed。
 *
 * @param p50Ms           p50 延迟毫秒。
 * @param p95Ms           p95 延迟毫秒。
 * @param p99Ms           p99 延迟毫秒。
 * @param timeoutMs       timeout budget 毫秒。
 * @param budgetUsedRatio 预算使用比例，B1 要求为有限且 0..1。
 * @param sampleCount     样本数。
 */
public record ProviderLatencyBudgetSummary(
        long p50Ms,
        long p95Ms,
        long p99Ms,
        long timeoutMs,
        double budgetUsedRatio,
        long sampleCount) {
}

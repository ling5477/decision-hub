package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * B4 report 的 provider health section。
 *
 * <p>该 section 复用 B2 read model safe view，只展示 tenant-bound observability evidence，不表示 provider
 * authorization、LIVE permission、trading signal 或 execution approval。
 *
 * @param tenantId               tenant 边界。
 * @param providerRef            provider safe ref。
 * @param modelGatewayVersionRef model gateway version safe ref。
 * @param readinessStatus        readiness / health status。
 * @param failureSummary         failure classification summary。
 * @param latencyBudgetReport    latency / budget report。
 * @param lastObservedAt         最近观察时间。
 * @param safeRefs               safe ref 列表。
 * @param redactedSummary        脱敏摘要。
 */
public record ProviderHealthReportSection(
        String tenantId,
        String providerRef,
        String modelGatewayVersionRef,
        ProviderReadinessStatus readinessStatus,
        ProviderFailureClassificationSummary failureSummary,
        ProviderLatencyBudgetReport latencyBudgetReport,
        Instant lastObservedAt,
        List<String> safeRefs,
        String redactedSummary) {

    /**
     * 校验 provider health section 的 tenant-bound read-only 边界。
     */
    public ProviderHealthReportSection {
        tenantId = ObservabilityReportSafety.requireSafeText(tenantId, "tenantId");
        providerRef = ObservabilityReportSafety.requireSafeText(providerRef, "providerRef");
        modelGatewayVersionRef =
                ObservabilityReportSafety.requireSafeText(modelGatewayVersionRef, "modelGatewayVersionRef");
        readinessStatus = Objects.requireNonNull(readinessStatus, "readinessStatus");
        failureSummary = Objects.requireNonNull(failureSummary, "failureSummary");
        latencyBudgetReport = Objects.requireNonNull(latencyBudgetReport, "latencyBudgetReport");
        lastObservedAt = ObservabilityReportSafety.requireInstant(lastObservedAt, "lastObservedAt");
        safeRefs = ObservabilityReportSafety.requireSafeTexts(safeRefs, "safeRefs");
        redactedSummary = ObservabilityReportSafety.requireSafeText(redactedSummary, "redactedSummary");
    }

    static ProviderHealthReportSection from(final ProviderHealthReadModelView view) {
        final ProviderHealthReadModelView checked = Objects.requireNonNull(view, "view");
        return new ProviderHealthReportSection(
                checked.tenantId(),
                checked.providerRef(),
                checked.modelGatewayVersionRef(),
                checked.readinessSignal().status(),
                ProviderFailureClassificationSummary.from(checked.failureClassification().classification()),
                new ProviderLatencyBudgetReport(
                        checked.latencyBudget().p50Ms(),
                        checked.latencyBudget().p95Ms(),
                        checked.latencyBudget().p99Ms(),
                        checked.latencyBudget().timeoutMs(),
                        checked.latencyBudget().budgetUsedRatio(),
                        checked.latencyBudget().sampleCount(),
                        "latency-budget:provider-health"),
                checked.lastObservedAt(),
                checked.safeRefs(),
                checked.redactedSummary());
    }
}

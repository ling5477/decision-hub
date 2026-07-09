package com.guidinglight.decisionhub.usecase.qdr.gateway;

import com.guidinglight.decisionhub.usecase.qdr.model.QdrPersistenceSafety;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Model gateway call observability 的内部只读视图。
 *
 * <p>该 view 只暴露 gateway call 的 safe refs、hash、enum 和 redacted summary，不暴露 raw material、
 * credential-like material、provider payload 或交易执行语义。
 *
 * @param tenantId                tenant 边界。
 * @param providerRef             provider safe ref。
 * @param modelGatewayVersionRef  model gateway version safe ref。
 * @param traceId                 traceId。
 * @param sourceRequestId         source request safe ref。
 * @param providerSummaryHash     provider summary SHA-256 hash。
 * @param failureClassification   failure classification view。
 * @param latencyBudget           latency / budget view。
 * @param trustDecision           trust decision view。
 * @param readinessSignal         readiness signal view。
 * @param observedAt              观察时间。
 * @param safeRefs                safe ref 列表。
 * @param redactedSummary         脱敏摘要。
 */
public record ModelGatewayCallObservabilityView(
        String tenantId,
        String providerRef,
        String modelGatewayVersionRef,
        String traceId,
        String sourceRequestId,
        String providerSummaryHash,
        ProviderFailureClassificationView failureClassification,
        ProviderLatencyBudgetView latencyBudget,
        ProviderTrustDecisionView trustDecision,
        ProviderReadinessSignalView readinessSignal,
        Instant observedAt,
        List<String> safeRefs,
        String redactedSummary) {

    /** 校验 observability view 仍处于 B2 redaction / read-only 边界内。 */
    public ModelGatewayCallObservabilityView {
        tenantId = QdrPersistenceSafety.requireSafeText(tenantId, "tenantId");
        providerRef = QdrPersistenceSafety.requireSafeText(providerRef, "providerRef");
        modelGatewayVersionRef =
                QdrPersistenceSafety.requireSafeText(modelGatewayVersionRef, "modelGatewayVersionRef");
        traceId = QdrPersistenceSafety.requireSafeText(traceId, "traceId");
        sourceRequestId = QdrPersistenceSafety.requireSafeText(sourceRequestId, "sourceRequestId");
        providerSummaryHash =
                QdrPersistenceSafety.requireSha256Hex(providerSummaryHash, "providerSummaryHash");
        failureClassification = Objects.requireNonNull(failureClassification, "failureClassification");
        latencyBudget = Objects.requireNonNull(latencyBudget, "latencyBudget");
        trustDecision = Objects.requireNonNull(trustDecision, "trustDecision");
        readinessSignal = Objects.requireNonNull(readinessSignal, "readinessSignal");
        observedAt = QdrPersistenceSafety.requireInstant(observedAt, "observedAt");
        safeRefs = safeRefs(Objects.requireNonNullElse(safeRefs, List.of()));
        redactedSummary = QdrPersistenceSafety.requireSafeText(redactedSummary, "redactedSummary");
    }

    static ModelGatewayCallObservabilityView from(final ModelGatewayObservabilitySummary summary) {
        final ModelGatewayObservabilitySummary checked =
                new ModelGatewayObservabilityContractService().validate(summary);
        return new ModelGatewayCallObservabilityView(
                checked.tenantId(),
                checked.providerRef(),
                checked.modelGatewayVersionRef(),
                checked.traceId(),
                checked.sourceRequestId(),
                checked.providerSummaryHash(),
                ProviderFailureClassificationView.from(
                        checked.failureClassification(),
                        "failure:" + checked.failureClassification().name().toLowerCase()),
                ProviderLatencyBudgetView.from(checked.latencyBudgetSummary()),
                ProviderTrustDecisionView.from(checked.trustDecisionSummary()),
                ProviderReadinessSignalView.from(checked.readinessSignal()),
                checked.createdAt(),
                safeRefs(checked),
                redactedSummary(checked));
    }

    private static List<String> safeRefs(final ModelGatewayObservabilitySummary summary) {
        return List.of(
                "trace:" + summary.traceId(),
                "sourceRequest:" + summary.sourceRequestId(),
                "provider:" + summary.providerRef(),
                "modelGatewayVersion:" + summary.modelGatewayVersionRef(),
                "providerSummaryHash:" + summary.providerSummaryHash().substring(0, 16));
    }

    private static List<String> safeRefs(final List<String> values) {
        return values.stream()
                .map(value -> QdrPersistenceSafety.requireSafeText(value, "safeRefs"))
                .toList();
    }

    private static String redactedSummary(final ModelGatewayObservabilitySummary summary) {
        return "providerRef="
                + summary.providerRef()
                + ";readiness="
                + summary.readinessSignal().status().name()
                + ";failure="
                + summary.failureClassification().name();
    }
}

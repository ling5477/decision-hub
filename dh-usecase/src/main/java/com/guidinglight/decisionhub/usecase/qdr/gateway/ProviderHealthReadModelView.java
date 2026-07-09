package com.guidinglight.decisionhub.usecase.qdr.gateway;

import com.guidinglight.decisionhub.usecase.qdr.model.QdrPersistenceSafety;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Provider health / gateway call observability 的内部只读聚合 view。
 *
 * <p>该 view 只用于 Stage-QDR-5 B2 internal evidence，不是 provider authorization、LIVE permission、
 * trading signal 或 execution approval。
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
 * @param readinessFinding        readiness finding safe summary。
 * @param lastObservedAt          最近观察时间。
 * @param createdAt               创建时间。
 * @param updatedAt               更新时间；B2 内部 projection 等同于 lastObservedAt。
 * @param safeRefs                safe ref 列表。
 * @param redactedSummary         脱敏摘要。
 * @param gatewayCallObservability gateway call observability view。
 */
public record ProviderHealthReadModelView(
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
        String readinessFinding,
        Instant lastObservedAt,
        Instant createdAt,
        Instant updatedAt,
        List<String> safeRefs,
        String redactedSummary,
        ModelGatewayCallObservabilityView gatewayCallObservability) {

    /** 校验 provider health read model view 的 redaction 与 read-only 边界。 */
    public ProviderHealthReadModelView {
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
        readinessFinding = QdrPersistenceSafety.requireSafeText(readinessFinding, "readinessFinding");
        lastObservedAt = QdrPersistenceSafety.requireInstant(lastObservedAt, "lastObservedAt");
        createdAt = QdrPersistenceSafety.requireInstant(createdAt, "createdAt");
        updatedAt = QdrPersistenceSafety.requireInstant(updatedAt, "updatedAt");
        safeRefs = safeRefs(Objects.requireNonNullElse(safeRefs, List.of()));
        redactedSummary = QdrPersistenceSafety.requireSafeText(redactedSummary, "redactedSummary");
        gatewayCallObservability =
                Objects.requireNonNull(gatewayCallObservability, "gatewayCallObservability");
    }

    static ProviderHealthReadModelView from(final ModelGatewayObservabilitySummary summary) {
        final ModelGatewayCallObservabilityView callView =
                ModelGatewayCallObservabilityView.from(summary);
        final ProviderHealthSummary healthSummary = new ModelGatewayObservabilityContractService()
                .validate(new ProviderHealthSummary(
                        callView.tenantId(),
                        callView.providerRef(),
                        callView.modelGatewayVersionRef(),
                        callView.readinessSignal().status(),
                        callView.failureClassification().classification(),
                        summary.latencyBudgetSummary(),
                        callView.observedAt()));
        return new ProviderHealthReadModelView(
                healthSummary.tenantId(),
                healthSummary.providerRef(),
                healthSummary.modelGatewayVersionRef(),
                callView.traceId(),
                callView.sourceRequestId(),
                callView.providerSummaryHash(),
                callView.failureClassification(),
                callView.latencyBudget(),
                callView.trustDecision(),
                callView.readinessSignal(),
                readinessFinding(callView.readinessSignal()),
                healthSummary.lastObservedAt(),
                summary.createdAt(),
                healthSummary.lastObservedAt(),
                callView.safeRefs(),
                callView.redactedSummary(),
                callView);
    }

    private static String readinessFinding(final ProviderReadinessSignalView signal) {
        if (signal.findings().isEmpty()) {
            return "none";
        }
        return signal.findings().stream()
                .map(ProviderReadinessFinding::code)
                .findFirst()
                .orElse("none");
    }

    private static List<String> safeRefs(final List<String> values) {
        return values.stream()
                .map(value -> QdrPersistenceSafety.requireSafeText(value, "safeRefs"))
                .toList();
    }
}

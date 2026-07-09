package com.guidinglight.decisionhub.usecase.qdr.gateway;

import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;
import com.guidinglight.decisionhub.usecase.qdr.model.QdrPersistenceSafety;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * Stage-QDR-5 B1 model gateway observability contract validator。
 *
 * <p>本 service 只做本地合同校验和 fail-closed guard，不落库、不接 provider、不发 HTTP、不读取凭证、不启动
 * Agent / LangGraph，也不产生交易信号。
 */
public final class ModelGatewayObservabilityContractService {

    /**
     * 校验 model gateway observability summary。
     *
     * @param summary observability summary。
     * @return 原 summary，表示合同有效。
     */
    public ModelGatewayObservabilitySummary validate(
            final ModelGatewayObservabilitySummary summary) {
        try {
            final ModelGatewayObservabilitySummary checked =
                    Objects.requireNonNull(summary, "summary");
            requireSafeRef(checked.tenantId(), "tenantId");
            requireSafeRef(checked.traceId(), "traceId");
            requireSafeRef(checked.sourceRequestId(), "sourceRequestId");
            requireSafeRef(checked.modelGatewayVersionRef(), "modelGatewayVersionRef");
            requireSafeRef(checked.providerRef(), "providerRef");
            requireSafeHash(checked.providerSummaryHash(), "providerSummaryHash");
            validate(checked.latencyBudgetSummary());
            requireClassification(checked.failureClassification());
            validate(checked.trustDecisionSummary());
            validate(checked.readinessSignal());
            requireInstant(checked.createdAt(), "createdAt");
            return checked;
        } catch (final RuntimeException error) {
            throw failClosed("OBSERVABILITY_CONTRACT_REJECTED");
        }
    }

    /**
     * 校验 provider health summary。
     *
     * @param summary provider health summary。
     * @return 原 summary，表示合同有效。
     */
    public ProviderHealthSummary validate(final ProviderHealthSummary summary) {
        try {
            final ProviderHealthSummary checked = Objects.requireNonNull(summary, "summary");
            requireSafeRef(checked.tenantId(), "tenantId");
            requireSafeRef(checked.providerRef(), "providerRef");
            requireSafeRef(checked.modelGatewayVersionRef(), "modelGatewayVersionRef");
            requireReadinessStatus(checked.status());
            requireClassification(checked.failureClassification());
            validate(checked.latencyBudgetSummary());
            requireInstant(checked.lastObservedAt(), "lastObservedAt");
            return checked;
        } catch (final RuntimeException error) {
            throw failClosed("PROVIDER_HEALTH_CONTRACT_REJECTED");
        }
    }

    /**
     * 校验 latency / budget summary。
     *
     * @param summary latency / budget summary。
     * @return 原 summary。
     */
    public ProviderLatencyBudgetSummary validate(final ProviderLatencyBudgetSummary summary) {
        try {
            final ProviderLatencyBudgetSummary checked = Objects.requireNonNull(summary, "summary");
            requireNonNegative(checked.p50Ms(), "p50Ms");
            requireNonNegative(checked.p95Ms(), "p95Ms");
            requireNonNegative(checked.p99Ms(), "p99Ms");
            requireNonNegative(checked.timeoutMs(), "timeoutMs");
            requireNonNegative(checked.sampleCount(), "sampleCount");
            if (!Double.isFinite(checked.budgetUsedRatio())
                    || checked.budgetUsedRatio() < 0.0d
                    || checked.budgetUsedRatio() > 1.0d) {
                throw new IllegalArgumentException("budgetUsedRatio must be finite 0..1");
            }
            return checked;
        } catch (final RuntimeException error) {
            throw failClosed("LATENCY_BUDGET_CONTRACT_REJECTED");
        }
    }

    /**
     * 校验 trust decision summary。
     *
     * @param summary trust decision summary。
     * @return 原 summary。
     */
    public ProviderTrustDecisionSummary validate(final ProviderTrustDecisionSummary summary) {
        try {
            final ProviderTrustDecisionSummary checked = Objects.requireNonNull(summary, "summary");
            Objects.requireNonNull(checked.decision(), "decision");
            requireSafeRef(checked.decisionRef(), "decisionRef");
            return checked;
        } catch (final RuntimeException error) {
            throw failClosed("TRUST_DECISION_CONTRACT_REJECTED");
        }
    }

    /**
     * 校验 readiness signal。
     *
     * @param signal readiness signal。
     * @return 原 signal。
     */
    public ProviderReadinessSignal validate(final ProviderReadinessSignal signal) {
        try {
            final ProviderReadinessSignal checked = Objects.requireNonNull(signal, "signal");
            requireReadinessStatus(checked.status());
            requireSafeRef(checked.readinessRef(), "readinessRef");
            checked.findings().forEach(this::validateFinding);
            return checked;
        } catch (final RuntimeException error) {
            throw failClosed("READINESS_SIGNAL_CONTRACT_REJECTED");
        }
    }

    private void validateFinding(final ProviderReadinessFinding finding) {
        final ProviderReadinessFinding checked = Objects.requireNonNull(finding, "finding");
        Objects.requireNonNull(checked.severity(), "severity");
        requireSafeRef(checked.code(), "findingCode");
        requireSafeRef(checked.summaryRef(), "findingSummaryRef");
    }

    private static ProviderFailureClassification requireClassification(
            final ProviderFailureClassification classification) {
        return Objects.requireNonNull(classification, "failureClassification");
    }

    private static ProviderReadinessStatus requireReadinessStatus(
            final ProviderReadinessStatus status) {
        return Objects.requireNonNull(status, "readinessStatus");
    }

    private static Instant requireInstant(final Instant value, final String field) {
        return Objects.requireNonNull(value, field);
    }

    private static long requireNonNegative(final long value, final String field) {
        if (value < 0L) {
            throw new IllegalArgumentException(field + " must be non-negative");
        }
        return value;
    }

    private static String requireSafeHash(final String value, final String field) {
        return requireSafeRef(QdrPersistenceSafety.requireSha256Hex(value, field), field);
    }

    private static String requireSafeRef(final String value, final String field) {
        final String checked = QdrPersistenceSafety.requireSafeText(value, field);
        rejectRawMaterial(field, checked);
        rejectRuntimeEnablement(field, checked);
        rejectTradingAction(field, checked);
        return checked;
    }

    private static void rejectRawMaterial(final String field, final String value) {
        final String normalized = normalize(value);
        if (normalized.contains("rawprompt")
                || normalized.contains("raw_prompt")
                || normalized.contains("prompttext")
                || normalized.contains("rawproviderresponse")
                || normalized.contains("raw_provider_response")
                || normalized.contains("providerraw")) {
            throw new IllegalArgumentException(field + " rejected by raw material boundary");
        }
    }

    private static void rejectRuntimeEnablement(final String field, final String value) {
        final String normalized = normalize(value);
        if (normalized.contains("enablerealprovider")
                || normalized.contains("realproviderenabled")
                || normalized.contains("enablerealhttp")
                || normalized.contains("realhttpenabled")
                || normalized.contains("enablelive")
                || normalized.contains("liveenabled")
                || normalized.contains("livepermission")
                || normalized.contains("providerauthorization")
                || normalized.contains("tradingsignal")) {
            throw new IllegalArgumentException(field + " rejected by readiness boundary");
        }
    }

    private static void rejectTradingAction(final String field, final String value) {
        if (PromptModelSafetyRules.containsExecutableTradingInstruction(value)
                || value.contains("MUTATE_NQ_STATE")) {
            throw new IllegalArgumentException(field + " rejected by trading boundary");
        }
    }

    private static ModelGatewayObservabilityContractException failClosed(final String code) {
        return new ModelGatewayObservabilityContractException(code);
    }

    private static String normalize(final String value) {
        return value.toLowerCase(Locale.ROOT).replace(" ", "");
    }
}

package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Stage-QDR-5 B4 Observability Report / Acceptance Support service。
 *
 * <p>Service 只生成内部 report 与 acceptance support，复用 B1 observability summary、B2 provider health
 * read model safe view 和 B3 readiness evaluation result；不读取 raw provider response、不读取 credential、
 * 不调用真实 provider、不调用 HTTP、不调用 NQ、不生成交易信号。任何输入、合同或内部异常都 fail-closed，
 * 并且 `PASS` 永远不解释为 provider authorization、LIVE permission 或 trading permission。
 */
public final class ObservabilityReportService {

    private static final String ZERO_HASH =
            "0000000000000000000000000000000000000000000000000000000000000000";
    private static final String UNAVAILABLE_TENANT = "tenant-unavailable";
    private static final String UNAVAILABLE_SOURCE = "source-unavailable";
    private static final String UNAVAILABLE_PROVIDER = "provider-unavailable";
    private static final String UNAVAILABLE_GATEWAY = "gateway-unavailable";
    private static final String UNAVAILABLE_TRACE = "trace-unavailable";
    private static final String UNAVAILABLE_REQUEST = "request-unavailable";
    private static final String FAIL_CLOSED_POLICY = "policy:fail-closed";
    private static final String SECURITY_BOUNDARY_REF = "internal-readonly-report-only";

    private final ModelGatewayObservabilityContractService contractService;

    /**
     * 创建使用默认 B1 contract validator 的 report service。
     */
    public ObservabilityReportService() {
        this(new ModelGatewayObservabilityContractService());
    }

    /**
     * 创建 observability report service。
     *
     * @param contractService B1/B2 safe contract validator。
     */
    public ObservabilityReportService(final ModelGatewayObservabilityContractService contractService) {
        this.contractService = Objects.requireNonNull(contractService, "contractService");
    }

    /**
     * 生成 Stage-QDR-5 B4 内部 observability report。
     *
     * <p>入参必须有 tenantId；缺 tenant、缺 provider、缺 B1/B2 evidence 或 unsafe 输入都会输出 `FAIL`。
     * 缺 B3 readiness decision 时输出 `SKIPPED`，不能输出 `PASS`。生成失败不会抛出含敏感上下文的异常，
     * 而是返回 fail-closed report。
     *
     * @param command B4 report command。
     * @return internal report / acceptance support。
     */
    public ModelGatewayObservabilityReport generate(final ObservabilityReportCommand command) {
        try {
            final NormalizedReportInput input = normalize(command);
            if (input.readinessEvaluationResult() == null) {
                return buildReport(input, ProviderReadinessReportSection.skipped(input.observedAt()));
            }
            return buildReport(
                    input,
                    ProviderReadinessReportSection.from(
                            input.readinessEvaluationResult(), input.observedAt()));
        } catch (final RuntimeException error) {
            return failClosedReport();
        }
    }

    private NormalizedReportInput normalize(final ObservabilityReportCommand command) {
        final ObservabilityReportCommand checked = Objects.requireNonNull(command, "command");
        final ModelGatewayObservabilitySummary summary =
                contractService.validate(Objects.requireNonNull(
                        checked.observabilitySummary(), "observabilitySummary"));
        final ProviderHealthReadModelView healthView =
                Objects.requireNonNull(checked.providerHealthView(), "providerHealthView");
        final String tenantId = ObservabilityReportSafety.requireSafeText(
                firstNonBlank(checked.tenantId(), summary.tenantId()), "tenantId");
        final String sourceRef =
                ObservabilityReportSafety.requireSafeText(checked.sourceRef(), "sourceRef");
        requireMatch(tenantId, summary.tenantId(), "summary tenant");
        requireMatch(tenantId, healthView.tenantId(), "health tenant");
        requireMatch(summary.providerRef(), healthView.providerRef(), "provider");
        requireMatch(
                summary.modelGatewayVersionRef(),
                healthView.modelGatewayVersionRef(),
                "model gateway version");
        requireMatch(summary.providerSummaryHash(), healthView.providerSummaryHash(), "provider summary hash");

        final ProviderReadinessEvaluationResult readinessResult = checked.readinessEvaluationResult();
        if (readinessResult != null) {
            requireMatch(summary.providerRef(), readinessResult.providerRef(), "readiness provider");
            requireMatch(summary.traceId(), readinessResult.traceId(), "readiness trace");
            requireMatch(
                    summary.sourceRequestId(),
                    readinessResult.sourceRequestId(),
                    "readiness source request");
        }

        final Instant createdAt = firstNonNull(checked.createdAt(), summary.createdAt());
        final Instant observedAt = firstNonNull(checked.observedAt(), healthView.lastObservedAt());
        return new NormalizedReportInput(
                tenantId,
                sourceRef,
                summary,
                healthView,
                readinessResult,
                ObservabilityReportSafety.requireInstant(createdAt, "createdAt"),
                ObservabilityReportSafety.requireInstant(observedAt, "observedAt"));
    }

    private ModelGatewayObservabilityReport buildReport(
            final NormalizedReportInput input,
            final ProviderReadinessReportSection readinessSection) {
        final ProviderHealthReportSection healthSection =
                ProviderHealthReportSection.from(input.providerHealthView());
        final ProviderFailureClassificationSummary failureSummary =
                ProviderFailureClassificationSummary.from(input.summary().failureClassification());
        final ProviderLatencyBudgetReport latencyReport =
                ProviderLatencyBudgetReport.from(input.summary().latencyBudgetSummary());
        final ProviderTrustDecisionReport trustReport =
                ProviderTrustDecisionReport.from(input.summary().trustDecisionSummary());
        final ProviderReadinessAcceptanceStatus acceptanceStatus =
                acceptanceStatus(input, readinessSection);
        final ProviderReadinessAcceptanceSummary acceptanceSummary =
                new ProviderReadinessAcceptanceSummary(
                        acceptanceStatus,
                        readinessSection.readinessDecision(),
                        readinessSection.readinessFinding(),
                        readinessSection.readinessPolicyVersion(),
                        input.summary().traceId(),
                        input.summary().sourceRequestId(),
                        input.summary().providerRef(),
                        redactedAcceptanceSummary(acceptanceStatus));
        final StageQdr5AcceptanceEvidence acceptanceEvidence =
                new StageQdr5AcceptanceEvidence(
                        input.tenantId(),
                        input.summary().providerRef(),
                        input.summary().traceId(),
                        input.summary().sourceRequestId(),
                        acceptanceStatus,
                        "stage-qdr5:b4:acceptance:" + acceptanceStatus.name().toLowerCase(),
                        SECURITY_BOUNDARY_REF,
                        input.createdAt());
        return new ModelGatewayObservabilityReport(
                input.tenantId(),
                input.sourceRef(),
                input.summary().providerRef(),
                input.summary().modelGatewayVersionRef(),
                input.summary().providerSummaryHash(),
                input.summary().traceId(),
                input.summary().sourceRequestId(),
                healthSection,
                readinessSection,
                failureSummary,
                latencyReport,
                trustReport,
                acceptanceSummary,
                acceptanceEvidence,
                input.createdAt(),
                input.observedAt(),
                "stage-qdr5:b4:observability-report:" + acceptanceStatus.name().toLowerCase());
    }

    private ProviderReadinessAcceptanceStatus acceptanceStatus(
            final NormalizedReportInput input,
            final ProviderReadinessReportSection readinessSection) {
        if (readinessSection.readinessDecision() == ProviderReadinessDecision.SKIPPED
                || readinessSection.readinessStatus() == ProviderReadinessStatus.SKIPPED
                || input.summary().trustDecisionSummary().decision()
                == ProviderTrustDecisionSummary.Decision.SKIPPED) {
            return ProviderReadinessAcceptanceStatus.SKIPPED;
        }
        if (readinessSection.readinessDecision() == ProviderReadinessDecision.READY
                && readinessSection.readinessStatus() == ProviderReadinessStatus.READY
                && input.providerHealthView().readinessSignal().status() == ProviderReadinessStatus.READY
                && input.summary().failureClassification() == ProviderFailureClassification.NONE
                && input.summary().trustDecisionSummary().decision()
                == ProviderTrustDecisionSummary.Decision.ALLOWED) {
            return ProviderReadinessAcceptanceStatus.PASS;
        }
        if (readinessSection.readinessDecision() == ProviderReadinessDecision.DEGRADED
                || readinessSection.readinessStatus() == ProviderReadinessStatus.DEGRADED
                || input.providerHealthView().readinessSignal().status() == ProviderReadinessStatus.DEGRADED
                || input.summary().trustDecisionSummary().decision()
                == ProviderTrustDecisionSummary.Decision.DEGRADED) {
            return ProviderReadinessAcceptanceStatus.WARN;
        }
        return ProviderReadinessAcceptanceStatus.FAIL;
    }

    private ModelGatewayObservabilityReport failClosedReport() {
        final Instant now = Instant.EPOCH;
        final ProviderFailureClassificationSummary failureSummary =
                ProviderFailureClassificationSummary.from(ProviderFailureClassification.UNKNOWN);
        final ProviderLatencyBudgetReport latencyReport =
                new ProviderLatencyBudgetReport(0L, 0L, 0L, 0L, 0.0d, 0L, "latency-budget:fail-closed");
        final ProviderTrustDecisionReport trustReport =
                ProviderTrustDecisionReport.from(ProviderTrustDecisionSummary.skipped("trust:fail-closed"));
        final ProviderReadinessEvidenceView evidence = new ProviderReadinessEvidenceView(
                ProviderReadinessSeverity.BLOCKING,
                "report-generation-fail-closed",
                "provider-readiness:report-generation-fail-closed");
        final ProviderReadinessReportSection readinessSection =
                new ProviderReadinessReportSection(
                        ProviderReadinessStatus.NOT_READY,
                        ProviderReadinessDecision.NOT_READY,
                        ProviderReadinessDecisionReason.INTERNAL_ERROR,
                        evidence.code(),
                        FAIL_CLOSED_POLICY,
                        List.of(evidence),
                        now);
        final ProviderHealthReportSection healthSection =
                new ProviderHealthReportSection(
                        UNAVAILABLE_TENANT,
                        UNAVAILABLE_PROVIDER,
                        UNAVAILABLE_GATEWAY,
                        ProviderReadinessStatus.NOT_READY,
                        failureSummary,
                        latencyReport,
                        now,
                        List.of("safe-ref:fail-closed"),
                        "provider-health:fail-closed");
        final ProviderReadinessAcceptanceSummary acceptanceSummary =
                new ProviderReadinessAcceptanceSummary(
                        ProviderReadinessAcceptanceStatus.FAIL,
                        ProviderReadinessDecision.NOT_READY,
                        evidence.code(),
                        FAIL_CLOSED_POLICY,
                        UNAVAILABLE_TRACE,
                        UNAVAILABLE_REQUEST,
                        UNAVAILABLE_PROVIDER,
                        "stage-qdr5:b4:acceptance-fail-closed");
        final StageQdr5AcceptanceEvidence acceptanceEvidence =
                new StageQdr5AcceptanceEvidence(
                        UNAVAILABLE_TENANT,
                        UNAVAILABLE_PROVIDER,
                        UNAVAILABLE_TRACE,
                        UNAVAILABLE_REQUEST,
                        ProviderReadinessAcceptanceStatus.FAIL,
                        "stage-qdr5:b4:acceptance:fail",
                        SECURITY_BOUNDARY_REF,
                        now);
        return new ModelGatewayObservabilityReport(
                UNAVAILABLE_TENANT,
                UNAVAILABLE_SOURCE,
                UNAVAILABLE_PROVIDER,
                UNAVAILABLE_GATEWAY,
                ZERO_HASH,
                UNAVAILABLE_TRACE,
                UNAVAILABLE_REQUEST,
                healthSection,
                readinessSection,
                failureSummary,
                latencyReport,
                trustReport,
                acceptanceSummary,
                acceptanceEvidence,
                now,
                now,
                "stage-qdr5:b4:observability-report:fail-closed");
    }

    private static String redactedAcceptanceSummary(final ProviderReadinessAcceptanceStatus status) {
        return "stage-qdr5:b4:acceptance-" + status.name().toLowerCase();
    }

    private static void requireMatch(
            final String expected, final String actual, final String field) {
        final String checkedExpected = ObservabilityReportSafety.requireSafeText(expected, field + " expected");
        final String checkedActual = ObservabilityReportSafety.requireSafeText(actual, field + " actual");
        if (!checkedExpected.equals(checkedActual)) {
            throw new IllegalArgumentException(field + " mismatch");
        }
    }

    private static String firstNonBlank(final String first, final String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private static Instant firstNonNull(final Instant first, final Instant second) {
        return first == null ? second : first;
    }

    private record NormalizedReportInput(
            String tenantId,
            String sourceRef,
            ModelGatewayObservabilitySummary summary,
            ProviderHealthReadModelView providerHealthView,
            ProviderReadinessEvaluationResult readinessEvaluationResult,
            Instant createdAt,
            Instant observedAt) {
    }
}

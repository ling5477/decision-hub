package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Stage-QDR-5 B2 provider health / gateway call observability 内部 read model service。
 *
 * <p>Service 只投影已经加载的 B1 observability summary 或既有 {@link ModelGatewayCallRecord} 脱敏
 * metadata；不新增 repository/JDBC，不调用 provider，不发 HTTP，不访问 NQ，不启动 Agent/LangGraph，
 * 不生成交易信号。任何 query、projection 或 source 校验失败都会 fail-closed。
 */
public final class ProviderHealthReadModelService {

    private static final String READ_MODEL_REJECTED = "PROVIDER_HEALTH_READ_MODEL_REJECTED";

    private final List<ModelGatewayObservabilitySummary> summaries;
    private final ModelGatewayObservabilityContractService contractService;

    /**
     * 创建 provider health read model service。
     *
     * @param summaries 已加载的 B1 observability summaries；调用方负责决定来源，本 service 不查库。
     */
    public ProviderHealthReadModelService(
            final Collection<ModelGatewayObservabilitySummary> summaries) {
        this(summaries, new ModelGatewayObservabilityContractService());
    }

    /**
     * 创建 provider health read model service。
     *
     * @param summaries       已加载的 B1 observability summaries。
     * @param contractService B1 observability contract validator。
     */
    public ProviderHealthReadModelService(
            final Collection<ModelGatewayObservabilitySummary> summaries,
            final ModelGatewayObservabilityContractService contractService) {
        this.summaries = List.copyOf(Objects.requireNonNull(summaries, "summaries"));
        this.contractService = Objects.requireNonNull(contractService, "contractService");
    }

    /**
     * 从既有 ModelGatewayCallRecord 构建内部 read model service。
     *
     * <p>该入口只复用已有 gateway call 脱敏 metadata，不扩展 production repository / JDBC，也不读取 raw
     * material。
     *
     * @param records 已加载的 gateway call records。
     * @return provider health read model service。
     */
    public static ProviderHealthReadModelService fromGatewayCalls(
            final Collection<ModelGatewayCallRecord> records) {
        final List<ModelGatewayObservabilitySummary> projected = Objects.requireNonNull(records, "records")
                .stream()
                .map(ProviderHealthReadModelService::fromGatewayCall)
                .toList();
        return new ProviderHealthReadModelService(projected);
    }

    /**
     * 查询 provider health / gateway call observability read model。
     *
     * @param query tenant-bound query。
     * @return matching views；跨租户或无命中返回空列表。
     */
    public List<ProviderHealthReadModelView> findProviderHealth(
            final ProviderHealthReadModelQuery query) {
        try {
            final ProviderHealthReadModelQuery checked = Objects.requireNonNull(query, "query");
            final List<ModelGatewayObservabilitySummary> checkedSummaries = summaries.stream()
                    .map(contractService::validate)
                    .toList();
            return checkedSummaries.stream()
                    .filter(summary -> checked.tenantId().equals(summary.tenantId()))
                    .filter(summary -> matches(checked, summary))
                    .skip(checked.offset())
                    .limit(checked.limit())
                    .map(ProviderHealthReadModelView::from)
                    .toList();
        } catch (final RuntimeException error) {
            throw failClosed();
        }
    }

    private static ModelGatewayObservabilitySummary fromGatewayCall(
            final ModelGatewayCallRecord record) {
        final ModelGatewayCallRecord checked = Objects.requireNonNull(record, "record");
        final ProviderFailureClassification failureClassification =
                failureClassification(checked);
        return new ModelGatewayObservabilitySummary(
                checked.tenantId(),
                checked.traceId(),
                checked.requestId(),
                "model-version:" + checked.modelVersionId(),
                checked.providerIdentityRef(),
                providerSummaryHash(checked),
                latencyBudgetSummary(checked),
                failureClassification,
                trustDecisionSummary(checked),
                readinessSignal(checked, failureClassification),
                checked.createdAt());
    }

    private static boolean matches(
            final ProviderHealthReadModelQuery query,
            final ModelGatewayObservabilitySummary summary) {
        return textMatches(query.providerRef(), summary.providerRef())
                && textMatches(query.modelGatewayVersionRef(), summary.modelGatewayVersionRef())
                && textMatches(query.traceId(), summary.traceId())
                && textMatches(query.sourceRequestId(), summary.sourceRequestId())
                && enumMatches(query.failureClassification(), summary.failureClassification())
                && enumMatches(query.trustDecision(), summary.trustDecisionSummary().decision())
                && enumMatches(query.readinessStatus(), summary.readinessSignal().status())
                && fromMatches(query.createdFrom(), summary.createdAt())
                && toMatches(query.createdTo(), summary.createdAt())
                && fromMatches(query.observedFrom(), summary.createdAt())
                && toMatches(query.observedTo(), summary.createdAt());
    }

    private static ProviderFailureClassification failureClassification(
            final ModelGatewayCallRecord record) {
        if (record.status() == ModelGatewayCallStatus.SUCCEEDED) {
            return ProviderFailureClassification.NONE;
        }
        return ProviderFailureClassification.fromGatewayFailureCode(record.failureCode());
    }

    private static String providerSummaryHash(final ModelGatewayCallRecord record) {
        return record.outputHash() == null ? record.inputHash() : record.outputHash();
    }

    private static ProviderLatencyBudgetSummary latencyBudgetSummary(
            final ModelGatewayCallRecord record) {
        final long totalCharacters = Math.max(
                1L,
                (long) record.inputCharacters()
                        + record.renderedPromptCharacters()
                        + record.outputCharacters());
        final double budgetUsedRatio = Math.min(1.0d, record.estimatedTokens() / (double) totalCharacters);
        return new ProviderLatencyBudgetSummary(
                0L,
                0L,
                0L,
                0L,
                budgetUsedRatio,
                1L);
    }

    private static ProviderTrustDecisionSummary trustDecisionSummary(
            final ModelGatewayCallRecord record) {
        if (record.trustDecision() == ModelGatewayCallTrustDecision.ALLOWED) {
            return ProviderTrustDecisionSummary.allowed(record.providerTrustDecisionRef());
        }
        return ProviderTrustDecisionSummary.denied(record.providerTrustDecisionRef());
    }

    private static ProviderReadinessSignal readinessSignal(
            final ModelGatewayCallRecord record,
            final ProviderFailureClassification failureClassification) {
        final String readinessRef = "readiness:" + record.modelCallRef();
        if (record.status() == ModelGatewayCallStatus.SUCCEEDED
                && record.trustDecision() == ModelGatewayCallTrustDecision.ALLOWED) {
            return ProviderReadinessSignal.ready(readinessRef);
        }
        return ProviderReadinessSignal.notReady(
                readinessRef,
                List.of(new ProviderReadinessFinding(
                        ProviderReadinessSeverity.BLOCKING,
                        failureClassification.name().toLowerCase().replace('_', '-'),
                        "gateway-call:" + record.modelCallRef())));
    }

    private static boolean textMatches(final String expected, final String actual) {
        return expected == null || expected.equals(actual);
    }

    private static <T extends Enum<T>> boolean enumMatches(final T expected, final T actual) {
        return expected == null || expected == actual;
    }

    private static boolean fromMatches(final java.time.Instant from, final java.time.Instant actual) {
        return from == null || !actual.isBefore(from);
    }

    private static boolean toMatches(final java.time.Instant to, final java.time.Instant actual) {
        return to == null || !actual.isAfter(to);
    }

    private static ModelGatewayObservabilityContractException failClosed() {
        return new ModelGatewayObservabilityContractException(READ_MODEL_REJECTED);
    }
}

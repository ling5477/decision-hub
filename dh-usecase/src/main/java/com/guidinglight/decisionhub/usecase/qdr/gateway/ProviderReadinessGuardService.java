package com.guidinglight.decisionhub.usecase.qdr.gateway;

import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;
import com.guidinglight.decisionhub.usecase.qdr.model.QdrPersistenceSafety;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * Stage-QDR-5 B3 Provider Readiness Guard / Policy Evaluation service。
 *
 * <p>Service 只做 internal readiness policy evaluation，复用 B1/B2 safe contracts / read model view；
 * 不读取 raw material、不读取 credential、不调用 real provider、不调用 HTTP、不访问 NQ、不生成交易信号。任何
 * 输入、policy、contract 或内部异常都会 fail-closed，`READY` 也只表示 future readiness criteria。
 */
public final class ProviderReadinessGuardService
        implements ProviderReadinessGuard, ProviderReadinessPolicy {

    private static final String UNAVAILABLE_POLICY = "policy:unavailable";
    private static final String UNAVAILABLE_TRACE = "trace:unavailable";
    private static final String UNAVAILABLE_REQUEST = "request:unavailable";
    private static final String UNAVAILABLE_PROVIDER = "provider:unavailable";

    private final ProviderReadinessPolicy policy;
    private final ModelGatewayObservabilityContractService contractService;

    /** 创建使用内置 fail-closed policy 的 guard service。 */
    public ProviderReadinessGuardService() {
        this(null, new ModelGatewayObservabilityContractService());
    }

    /**
     * 创建可注入 policy 的 guard service。
     *
     * @param policy 外部本地 policy；null 时使用内置 policy。
     */
    public ProviderReadinessGuardService(final ProviderReadinessPolicy policy) {
        this(policy, new ModelGatewayObservabilityContractService());
    }

    /**
     * 创建 provider readiness guard service。
     *
     * @param policy          外部本地 policy；null 时使用内置 policy。
     * @param contractService B1/B2 safe contract validator。
     */
    public ProviderReadinessGuardService(
            final ProviderReadinessPolicy policy,
            final ModelGatewayObservabilityContractService contractService) {
        this.policy = policy;
        this.contractService = Objects.requireNonNull(contractService, "contractService");
    }

    /**
     * 执行 provider readiness guard / policy evaluation。
     *
     * @param command evaluation command。
     * @return READY / NOT_READY / DEGRADED / SKIPPED；异常永远转换为 NOT_READY。
     */
    @Override
    public ProviderReadinessEvaluationResult evaluate(
            final ProviderReadinessEvaluationCommand command) {
        try {
            final ProviderReadinessEvaluationCommand safeCommand = normalize(command);
            final ProviderReadinessEvaluationResult preDecision = preEvaluate(safeCommand);
            if (preDecision != null) {
                return preDecision;
            }
            return evaluatePolicy(safeCommand);
        } catch (final ProviderReadinessGuardRejectException rejected) {
            return failClosed(rejected.decision(), rejected.reason());
        } catch (final RuntimeException error) {
            return failClosed(
                    ProviderReadinessDecision.NOT_READY,
                    ProviderReadinessDecisionReason.INTERNAL_ERROR);
        }
    }

    private ProviderReadinessEvaluationResult evaluatePolicy(
            final ProviderReadinessEvaluationCommand command) {
        final ProviderReadinessPolicy selected = policy == null ? this::defaultPolicy : policy;
        final ProviderReadinessEvaluationResult result = selected.evaluate(command);
        if (result.decision() == ProviderReadinessDecision.READY
                && result.reason() != ProviderReadinessDecisionReason.READY_SAFE_CONTEXT) {
            return failClosed(
                    ProviderReadinessDecision.NOT_READY,
                    ProviderReadinessDecisionReason.POLICY_EVIDENCE_REJECTED,
                    command);
        }
        return result;
    }

    private ProviderReadinessEvaluationResult defaultPolicy(
            final ProviderReadinessEvaluationCommand command) {
        final String normalizedSource = normalizeText(command.sourceRef());
        if (normalizedSource.contains("sourcedenied") || normalizedSource.contains("deniedsource")) {
            return failClosed(
                    ProviderReadinessDecision.NOT_READY,
                    ProviderReadinessDecisionReason.SOURCE_DENIED,
                    command);
        }
        return ProviderReadinessEvaluationResult.ready(command);
    }

    private ProviderReadinessEvaluationCommand normalize(
            final ProviderReadinessEvaluationCommand command) {
        final ProviderReadinessEvaluationCommand checked = Objects.requireNonNull(command, "command");
        final ProviderHealthReadModelView view = checked.providerHealthView();
        final String tenantId = firstNonBlank(checked.tenantId(), view == null ? null : view.tenantId());
        final String providerRef = firstNonBlank(checked.providerRef(), view == null ? null : view.providerRef());
        final String modelGatewayVersionRef = firstNonBlank(
                checked.modelGatewayVersionRef(),
                view == null ? null : view.modelGatewayVersionRef());
        final String providerSummaryHash = firstNonBlank(
                checked.providerSummaryHash(),
                view == null ? null : view.providerSummaryHash());
        final ProviderFailureClassification failureClassification = checked.failureClassification() == null
                && view != null ? view.failureClassification().classification() : checked.failureClassification();
        final ProviderLatencyBudgetSummary latencyBudgetSummary = checked.latencyBudgetSummary() == null
                && view != null ? toSummary(view.latencyBudget()) : checked.latencyBudgetSummary();
        final ProviderTrustDecisionSummary trustDecisionSummary = checked.trustDecisionSummary() == null
                && view != null ? toSummary(view.trustDecision()) : checked.trustDecisionSummary();
        final ProviderReadinessSignal readinessSignal = checked.readinessSignal() == null
                && view != null ? toSignal(view.readinessSignal()) : checked.readinessSignal();
        final String traceId = firstNonBlank(checked.traceId(), view == null ? null : view.traceId());
        final String sourceRequestId = firstNonBlank(
                checked.sourceRequestId(),
                view == null ? null : view.sourceRequestId());
        final Instant createdAt = checked.createdAt() == null && view != null ? view.createdAt() : checked.createdAt();
        final Instant observedAt = checked.observedAt() == null && view != null
                ? view.lastObservedAt()
                : checked.observedAt();

        final String safePolicyVersion = requirePolicyVersion(checked.policyVersion());
        return new ProviderReadinessEvaluationCommand(
                requireSafeText(tenantId, "tenantId", ProviderReadinessDecisionReason.MISSING_TENANT),
                requireSafeText(checked.sourceRef(), "sourceRef", ProviderReadinessDecisionReason.SOURCE_DENIED),
                requireSafeText(providerRef, "providerRef", ProviderReadinessDecisionReason.MISSING_PROVIDER_REF),
                requireSafeText(
                        modelGatewayVersionRef,
                        "modelGatewayVersionRef",
                        ProviderReadinessDecisionReason.POLICY_EVIDENCE_REJECTED),
                requireSafeHash(providerSummaryHash),
                Objects.requireNonNull(failureClassification, "failureClassification"),
                contractService.validate(Objects.requireNonNull(latencyBudgetSummary, "latencyBudgetSummary")),
                contractService.validate(Objects.requireNonNull(trustDecisionSummary, "trustDecisionSummary")),
                contractService.validate(Objects.requireNonNull(readinessSignal, "readinessSignal")),
                view,
                safePolicyVersion,
                requireSafeText(traceId, "traceId", ProviderReadinessDecisionReason.POLICY_EVIDENCE_REJECTED),
                requireSafeText(
                        sourceRequestId,
                        "sourceRequestId",
                        ProviderReadinessDecisionReason.POLICY_EVIDENCE_REJECTED),
                Objects.requireNonNull(createdAt, "createdAt"),
                Objects.requireNonNull(observedAt, "observedAt"));
    }

    private ProviderReadinessEvaluationResult preEvaluate(
            final ProviderReadinessEvaluationCommand command) {
        if (command.failureClassification() == ProviderFailureClassification.UNKNOWN) {
            return failClosed(
                    ProviderReadinessDecision.NOT_READY,
                    ProviderReadinessDecisionReason.UNKNOWN_CLASSIFICATION,
                    command);
        }
        if (command.failureClassification() == ProviderFailureClassification.TIMEOUT
                || latencyTimedOut(command.latencyBudgetSummary())) {
            return failClosed(
                    ProviderReadinessDecision.DEGRADED,
                    ProviderReadinessDecisionReason.TIMEOUT,
                    command);
        }
        if (command.failureClassification() == ProviderFailureClassification.BUDGET_EXCEEDED
                || command.latencyBudgetSummary().budgetUsedRatio() >= 1.0d) {
            return failClosed(
                    ProviderReadinessDecision.DEGRADED,
                    ProviderReadinessDecisionReason.BUDGET_EXCEEDED,
                    command);
        }
        if (command.failureClassification() != ProviderFailureClassification.NONE) {
            return failClosed(
                    ProviderReadinessDecision.NOT_READY,
                    ProviderReadinessDecisionReason.POLICY_EVIDENCE_REJECTED,
                    command);
        }
        if (command.trustDecisionSummary().decision() == ProviderTrustDecisionSummary.Decision.DENIED) {
            return failClosed(
                    ProviderReadinessDecision.NOT_READY,
                    ProviderReadinessDecisionReason.POLICY_DENIED,
                    command);
        }
        if (command.trustDecisionSummary().decision() == ProviderTrustDecisionSummary.Decision.DEGRADED) {
            return failClosed(
                    ProviderReadinessDecision.DEGRADED,
                    ProviderReadinessDecisionReason.POLICY_DENIED,
                    command);
        }
        if (command.trustDecisionSummary().decision() == ProviderTrustDecisionSummary.Decision.SKIPPED) {
            return failClosed(
                    ProviderReadinessDecision.SKIPPED,
                    ProviderReadinessDecisionReason.POLICY_DENIED,
                    command);
        }
        if (command.readinessSignal().status() != ProviderReadinessStatus.READY) {
            return failClosed(
                    toDecision(command.readinessSignal().status()),
                    ProviderReadinessDecisionReason.POLICY_EVIDENCE_REJECTED,
                    command);
        }
        return null;
    }

    private static boolean latencyTimedOut(final ProviderLatencyBudgetSummary summary) {
        return summary.timeoutMs() > 0L && summary.p99Ms() > summary.timeoutMs();
    }

    private static ProviderReadinessDecision toDecision(final ProviderReadinessStatus status) {
        return switch (status) {
            case READY -> ProviderReadinessDecision.READY;
            case NOT_READY -> ProviderReadinessDecision.NOT_READY;
            case DEGRADED -> ProviderReadinessDecision.DEGRADED;
            case SKIPPED -> ProviderReadinessDecision.SKIPPED;
        };
    }

    private static String requirePolicyVersion(final String value) {
        if (value == null || value.isBlank()) {
            throw new ProviderReadinessGuardRejectException(
                    ProviderReadinessDecision.SKIPPED,
                    ProviderReadinessDecisionReason.MISSING_POLICY_VERSION);
        }
        return requireSafeText(value, "policyVersion", ProviderReadinessDecisionReason.MISSING_POLICY_VERSION);
    }

    private static String requireSafeText(
            final String value,
            final String field,
            final ProviderReadinessDecisionReason missingReason) {
        if (value == null || value.isBlank()) {
            throw new ProviderReadinessGuardRejectException(
                    missingReason == ProviderReadinessDecisionReason.MISSING_POLICY_VERSION
                            ? ProviderReadinessDecision.SKIPPED
                            : ProviderReadinessDecision.NOT_READY,
                    missingReason);
        }
        final String checked = value.trim();
        rejectForbiddenValue(checked);
        return QdrPersistenceSafety.requireSafeText(checked, field);
    }

    private static String requireSafeHash(final String value) {
        if (value == null || value.isBlank()) {
            throw new ProviderReadinessGuardRejectException(
                    ProviderReadinessDecision.NOT_READY,
                    ProviderReadinessDecisionReason.POLICY_EVIDENCE_REJECTED);
        }
        rejectForbiddenValue(value);
        return QdrPersistenceSafety.requireSha256Hex(value, "providerSummaryHash");
    }

    private static void rejectForbiddenValue(final String value) {
        final String normalized = normalizeText(value);
        if (normalized.contains("rawprompt") || normalized.contains("prompttext")) {
            throw new ProviderReadinessGuardRejectException(
                    ProviderReadinessDecision.NOT_READY,
                    ProviderReadinessDecisionReason.RAW_PROMPT_REJECTED);
        }
        if (normalized.contains("rawproviderresponse")
                || normalized.contains("rawresponsepayload")
                || normalized.contains("providerraw")) {
            throw new ProviderReadinessGuardRejectException(
                    ProviderReadinessDecision.NOT_READY,
                    ProviderReadinessDecisionReason.RAW_PROVIDER_RESPONSE_REJECTED);
        }
        if (normalized.contains("mutatenqstate")
                || normalized.contains("placeorder")
                || normalized.contains("cancelorder")) {
            throw new ProviderReadinessGuardRejectException(
                    ProviderReadinessDecision.NOT_READY,
                    ProviderReadinessDecisionReason.NQ_MUTATION_REJECTED);
        }
        if (PromptModelSafetyRules.containsExecutableTradingInstruction(value)) {
            throw new ProviderReadinessGuardRejectException(
                    ProviderReadinessDecision.NOT_READY,
                    ProviderReadinessDecisionReason.TRADING_INPUT_REJECTED);
        }
        if (PromptModelSafetyRules.containsSecretLikeMaterial(value)) {
            throw new ProviderReadinessGuardRejectException(
                    ProviderReadinessDecision.NOT_READY,
                    ProviderReadinessDecisionReason.SENSITIVE_INPUT_REJECTED);
        }
    }

    private static ProviderReadinessEvaluationResult failClosed(
            final ProviderReadinessDecision decision,
            final ProviderReadinessDecisionReason reason) {
        return ProviderReadinessEvaluationResult.failClosed(
                decision,
                reason,
                UNAVAILABLE_POLICY,
                UNAVAILABLE_TRACE,
                UNAVAILABLE_REQUEST,
                UNAVAILABLE_PROVIDER);
    }

    private static ProviderReadinessEvaluationResult failClosed(
            final ProviderReadinessDecision decision,
            final ProviderReadinessDecisionReason reason,
            final ProviderReadinessEvaluationCommand command) {
        return ProviderReadinessEvaluationResult.failClosed(
                decision,
                reason,
                command.policyVersion(),
                command.traceId(),
                command.sourceRequestId(),
                command.providerRef());
    }

    private static String firstNonBlank(final String first, final String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private static ProviderLatencyBudgetSummary toSummary(final ProviderLatencyBudgetView view) {
        return new ProviderLatencyBudgetSummary(
                view.p50Ms(),
                view.p95Ms(),
                view.p99Ms(),
                view.timeoutMs(),
                view.budgetUsedRatio(),
                view.sampleCount());
    }

    private static ProviderTrustDecisionSummary toSummary(final ProviderTrustDecisionView view) {
        return new ProviderTrustDecisionSummary(view.decision(), view.decisionRef());
    }

    private static ProviderReadinessSignal toSignal(final ProviderReadinessSignalView view) {
        return new ProviderReadinessSignal(view.status(), view.readinessRef(), view.findings());
    }

    private static String normalizeText(final String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }

    private static final class ProviderReadinessGuardRejectException extends RuntimeException {

        private final ProviderReadinessDecision decision;
        private final ProviderReadinessDecisionReason reason;

        private ProviderReadinessGuardRejectException(
                final ProviderReadinessDecision decision,
                final ProviderReadinessDecisionReason reason) {
            super(reason.name());
            this.decision = decision;
            this.reason = reason;
        }

        private ProviderReadinessDecision decision() {
            return decision;
        }

        private ProviderReadinessDecisionReason reason() {
            return reason;
        }
    }
}

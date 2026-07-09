package com.guidinglight.decisionhub.usecase.qdr.gateway;

import com.guidinglight.decisionhub.usecase.qdr.model.QdrPersistenceSafety;

import java.util.List;
import java.util.Objects;

/**
 * Provider readiness policy evaluation 的安全输出。
 *
 * <p>Result 只输出固定 decision、reason、safe refs 与 readiness signal；不包含 provider enable flag、
 * HTTP enable flag、LIVE flag、交易 permission、raw prompt、provider 原始响应或 credential 字段。
 *
 * @param decision        readiness decision。
 * @param reason          固定 reason code。
 * @param policyVersion   safe policy version ref。
 * @param traceId         safe trace ref。
 * @param sourceRequestId safe source request ref。
 * @param providerRef     safe provider ref。
 * @param readinessSignal readiness safe signal。
 * @param findings        readiness findings。
 */
public record ProviderReadinessEvaluationResult(
        ProviderReadinessDecision decision,
        ProviderReadinessDecisionReason reason,
        String policyVersion,
        String traceId,
        String sourceRequestId,
        String providerRef,
        ProviderReadinessSignal readinessSignal,
        List<ProviderReadinessFinding> findings) {

    /** 复制并校验 result，确保输出只有 safe refs 与固定 reason。 */
    public ProviderReadinessEvaluationResult {
        decision = Objects.requireNonNull(decision, "decision");
        reason = Objects.requireNonNull(reason, "reason");
        policyVersion = QdrPersistenceSafety.requireSafeText(policyVersion, "policyVersion");
        traceId = QdrPersistenceSafety.requireSafeText(traceId, "traceId");
        sourceRequestId = QdrPersistenceSafety.requireSafeText(sourceRequestId, "sourceRequestId");
        providerRef = QdrPersistenceSafety.requireSafeText(providerRef, "providerRef");
        readinessSignal = new ModelGatewayObservabilityContractService()
                .validate(Objects.requireNonNull(readinessSignal, "readinessSignal"));
        findings = List.copyOf(Objects.requireNonNullElse(findings, List.of()));
    }

    /**
     * 创建 READY result；该 READY 仍然不启用真实 provider、HTTP、LIVE 或交易。
     *
     * @param command safe command。
     * @return READY result。
     */
    public static ProviderReadinessEvaluationResult ready(
            final ProviderReadinessEvaluationCommand command) {
        return new ProviderReadinessEvaluationResult(
                ProviderReadinessDecision.READY,
                ProviderReadinessDecisionReason.READY_SAFE_CONTEXT,
                command.policyVersion(),
                command.traceId(),
                command.sourceRequestId(),
                command.providerRef(),
                command.readinessSignal(),
                List.of());
    }

    /**
     * 创建 fail-closed result。
     *
     * @param decision        safe decision。
     * @param reason          fixed reason。
     * @param policyVersion   safe policy ref。
     * @param traceId         safe trace ref。
     * @param sourceRequestId safe source request ref。
     * @param providerRef     safe provider ref。
     * @return fail-closed result。
     */
    public static ProviderReadinessEvaluationResult failClosed(
            final ProviderReadinessDecision decision,
            final ProviderReadinessDecisionReason reason,
            final String policyVersion,
            final String traceId,
            final String sourceRequestId,
            final String providerRef) {
        final String safeFindingCode = safeFindingCode(reason);
        final ProviderReadinessFinding finding = new ProviderReadinessFinding(
                ProviderReadinessSeverity.BLOCKING,
                safeFindingCode,
                "provider-readiness:" + safeFindingCode);
        final ProviderReadinessSignal signal = switch (decision) {
            case DEGRADED -> ProviderReadinessSignal.degraded("readiness:" + finding.code(), List.of(finding));
            case SKIPPED -> ProviderReadinessSignal.skipped("readiness:" + finding.code(), List.of(finding));
            case READY -> ProviderReadinessSignal.ready("readiness:ready-safe-context");
            case NOT_READY -> ProviderReadinessSignal.notReady("readiness:" + finding.code(), List.of(finding));
        };
        return new ProviderReadinessEvaluationResult(
                decision,
                reason,
                policyVersion,
                traceId,
                sourceRequestId,
                providerRef,
                signal,
                List.of(finding));
    }

    private static String safeFindingCode(final ProviderReadinessDecisionReason reason) {
        return switch (reason) {
            case RAW_PROMPT_REJECTED -> "unsafe-input-rejected";
            case RAW_PROVIDER_RESPONSE_REJECTED -> "provider-output-boundary-rejected";
            case NQ_MUTATION_REJECTED -> "nq-mutation-boundary-rejected";
            default -> reason.name().toLowerCase().replace('_', '-');
        };
    }
}

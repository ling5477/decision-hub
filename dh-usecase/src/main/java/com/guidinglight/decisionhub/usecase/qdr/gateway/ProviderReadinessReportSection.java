package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * B4 report 的 provider readiness section。
 *
 * <p>该 section 复用 B3 readiness evaluation result，只表达 future readiness evidence，不授权真实 provider、
 * 真实 HTTP、LIVE、交易或 NQ execution。
 *
 * @param readinessStatus        readiness signal status。
 * @param readinessDecision      readiness decision。
 * @param readinessReason        readiness reason。
 * @param readinessFinding       readiness finding safe summary。
 * @param readinessPolicyVersion readiness policy version safe ref。
 * @param findings               readiness evidence views。
 * @param observedAt             evidence 观察时间。
 */
public record ProviderReadinessReportSection(
        ProviderReadinessStatus readinessStatus,
        ProviderReadinessDecision readinessDecision,
        ProviderReadinessDecisionReason readinessReason,
        String readinessFinding,
        String readinessPolicyVersion,
        List<ProviderReadinessEvidenceView> findings,
        Instant observedAt) {

    /**
     * 校验 readiness report section 的 non-runtime 边界。
     */
    public ProviderReadinessReportSection {
        readinessStatus = Objects.requireNonNull(readinessStatus, "readinessStatus");
        readinessDecision = Objects.requireNonNull(readinessDecision, "readinessDecision");
        readinessReason = Objects.requireNonNull(readinessReason, "readinessReason");
        readinessFinding = ObservabilityReportSafety.requireSafeText(readinessFinding, "readinessFinding");
        readinessPolicyVersion =
                ObservabilityReportSafety.requireSafeText(readinessPolicyVersion, "readinessPolicyVersion");
        findings = Objects.requireNonNullElse(findings, List.<ProviderReadinessEvidenceView>of()).stream()
                .map(finding -> Objects.requireNonNull(finding, "finding"))
                .toList();
        observedAt = ObservabilityReportSafety.requireInstant(observedAt, "observedAt");
    }

    static ProviderReadinessReportSection from(
            final ProviderReadinessEvaluationResult result,
            final Instant observedAt) {
        final ProviderReadinessEvaluationResult checked = Objects.requireNonNull(result, "result");
        final List<ProviderReadinessEvidenceView> evidence = checked.findings().stream()
                .map(ProviderReadinessEvidenceView::from)
                .toList();
        return new ProviderReadinessReportSection(
                checked.readinessSignal().status(),
                checked.decision(),
                checked.reason(),
                firstFinding(evidence),
                checked.policyVersion(),
                evidence,
                observedAt);
    }

    static ProviderReadinessReportSection skipped(final Instant observedAt) {
        final ProviderReadinessEvidenceView evidence = new ProviderReadinessEvidenceView(
                ProviderReadinessSeverity.BLOCKING,
                "missing-readiness-decision",
                "provider-readiness:missing-readiness-decision");
        return new ProviderReadinessReportSection(
                ProviderReadinessStatus.SKIPPED,
                ProviderReadinessDecision.SKIPPED,
                ProviderReadinessDecisionReason.POLICY_EVIDENCE_REJECTED,
                evidence.code(),
                "policy:missing-readiness-decision",
                List.of(evidence),
                observedAt);
    }

    private static String firstFinding(final List<ProviderReadinessEvidenceView> findings) {
        if (findings.isEmpty()) {
            return "none";
        }
        return findings.getFirst().code();
    }
}

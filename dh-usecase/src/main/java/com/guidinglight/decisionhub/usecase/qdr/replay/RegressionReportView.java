package com.guidinglight.decisionhub.usecase.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayOutputRef;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stage-QDR-4 B4 regression report 只读视图。
 *
 * <p>视图只展示 safe refs、hash、version、verdict、finding 和 drift summary；不包含 raw prompt、
 * raw provider response、credential-like 字段，也不把 action label 或 verdict 解释为交易动作。
 *
 * @param tenantId               tenant ID。
 * @param caseId                 replay case ID。
 * @param evaluationId           evaluation ID。
 * @param verdictId              verdict ID。
 * @param traceId                traceId。
 * @param sourceRequestId        来源 request ID。
 * @param sourceDecisionId       来源 decision ID。
 * @param decisionType           decision type。
 * @param actionLabel            只读 action label。
 * @param confidenceBand         confidence band。
 * @param riskLevel              QDR risk level。
 * @param verdict                regression verdict。
 * @param severity               regression severity。
 * @param providerSummaryHash    provider summary safe hash，可为空。
 * @param modelGatewayVersionRef model gateway version ref。
 * @param promptVersionRef       prompt version ref；B2/V9 未持久化时为空。
 * @param policyVersion          policy version。
 * @param createdAt              创建时间。
 * @param updatedAt              更新时间。
 * @param redactedSummary        脱敏报告摘要。
 * @param safeRefs               safe ref 列表。
 * @param driftSummary           drift summary。
 * @param findings               finding view 列表。
 */
public record RegressionReportView(
        String tenantId,
        String caseId,
        String evaluationId,
        String verdictId,
        String traceId,
        String sourceRequestId,
        String sourceDecisionId,
        String decisionType,
        String actionLabel,
        String confidenceBand,
        RiskLevel riskLevel,
        RegressionVerdict.Status verdict,
        RegressionSeverity severity,
        String providerSummaryHash,
        String modelGatewayVersionRef,
        String promptVersionRef,
        String policyVersion,
        Instant createdAt,
        Instant updatedAt,
        String redactedSummary,
        List<String> safeRefs,
        RegressionDriftSummary driftSummary,
        List<RegressionReportFindingView> findings) {

    /**
     * 校验 report view 内容仍满足 redaction、tenant-bound 和 trading-term guard。
     */
    public RegressionReportView {
        tenantId = ReplayPersistenceGuard.requireTenantId(tenantId);
        caseId = ReplayPersistenceGuard.requireSafeText(caseId, "caseId");
        evaluationId = ReplayPersistenceGuard.requireSafeText(evaluationId, "evaluationId");
        verdictId = ReplayPersistenceGuard.requireSafeText(verdictId, "verdictId");
        traceId = ReplayPersistenceGuard.requireSafeText(traceId, "traceId");
        sourceRequestId = ReplayPersistenceGuard.optionalSafeText(sourceRequestId, "sourceRequestId");
        sourceDecisionId = ReplayPersistenceGuard.optionalSafeText(sourceDecisionId, "sourceDecisionId");
        decisionType = ReplayPersistenceGuard.requireSafeText(decisionType, "decisionType");
        actionLabel = ReplayPersistenceGuard.requireActionLabel(actionLabel);
        confidenceBand = ReplayPersistenceGuard.requireSafeText(confidenceBand, "confidenceBand");
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel");
        verdict = Objects.requireNonNull(verdict, "verdict");
        severity = Objects.requireNonNull(severity, "severity");
        providerSummaryHash =
                ReplayPersistenceGuard.optionalSha256Hex(providerSummaryHash, "providerSummaryHash");
        modelGatewayVersionRef =
                ReplayPersistenceGuard.optionalSafeText(modelGatewayVersionRef, "modelGatewayVersionRef");
        promptVersionRef = ReplayPersistenceGuard.optionalSafeText(promptVersionRef, "promptVersionRef");
        policyVersion = ReplayPersistenceGuard.requireSafeText(policyVersion, "policyVersion");
        createdAt = ReplayPersistenceGuard.requireInstant(createdAt, "createdAt");
        updatedAt = ReplayPersistenceGuard.requireInstant(updatedAt, "updatedAt");
        redactedSummary = ReplayPersistenceGuard.requireSafeText(redactedSummary, "redactedSummary");
        safeRefs = safeRefs.stream()
                .map(ref -> ReplayPersistenceGuard.requireSafeText(ref, "safeRefs"))
                .toList();
        driftSummary = Objects.requireNonNull(driftSummary, "driftSummary");
        findings = List.copyOf(Objects.requireNonNullElse(findings, List.of()));
    }

    static RegressionReportView from(
            final ReplayCaseRecord replayCase,
            final EvaluationCaseRecord evaluationCase,
            final RegressionVerdictRecord verdictRecord,
            final List<RegressionFindingRecord> findingRecords) {
        requireSameTenant(replayCase, evaluationCase, verdictRecord);
        final ExpectedDecisionSummary reportSummary =
                evaluationCase.actualSummary() == null
                        ? evaluationCase.expectedSummary()
                        : evaluationCase.actualSummary();
        final List<RegressionReportFindingView> findingViews = findingRecords.stream()
                .map(RegressionReportFindingView::from)
                .toList();
        return new RegressionReportView(
                verdictRecord.tenantId(),
                verdictRecord.caseId(),
                verdictRecord.evaluationId(),
                verdictRecord.verdictId(),
                verdictRecord.traceId(),
                verdictRecord.sourceRequestId(),
                verdictRecord.sourceDecisionId(),
                reportSummary.decisionType(),
                reportSummary.actionLabel(),
                reportSummary.confidenceBand(),
                reportSummary.riskLevel(),
                verdictRecord.verdict(),
                verdictRecord.severity(),
                providerSummaryHash(evaluationCase),
                firstPresent(
                        evaluationCase.modelGatewayVersionRef(),
                        verdictRecord.modelGatewayVersionRef()),
                null,
                verdictRecord.policyVersion(),
                verdictRecord.createdAt(),
                verdictRecord.updatedAt(),
                redactedSummary(verdictRecord),
                safeRefs(replayCase, evaluationCase, verdictRecord),
                RegressionDriftSummary.from(replayCase, evaluationCase, verdictRecord, findingViews),
                findingViews);
    }

    private static void requireSameTenant(
            final ReplayCaseRecord replayCase,
            final EvaluationCaseRecord evaluationCase,
            final RegressionVerdictRecord verdictRecord) {
        if (!replayCase.tenantId().equals(evaluationCase.tenantId())
                || !evaluationCase.tenantId().equals(verdictRecord.tenantId())
                || !replayCase.caseId().equals(verdictRecord.caseId())
                || !evaluationCase.evaluationId().equals(verdictRecord.evaluationId())) {
            throw new ReplayPersistenceException("regression report tenant or identity mismatch");
        }
    }

    private static String providerSummaryHash(final EvaluationCaseRecord evaluationCase) {
        final ReplayOutputRef outputRef = evaluationCase.outputRef();
        return outputRef == null ? null : outputRef.contentHash();
    }

    private static List<String> safeRefs(
            final ReplayCaseRecord replayCase,
            final EvaluationCaseRecord evaluationCase,
            final RegressionVerdictRecord verdictRecord) {
        final List<String> refs = new ArrayList<>();
        refs.add(compactInputRef(replayCase.inputRef()));
        if (evaluationCase.outputRef() != null) {
            refs.add(compactOutputRef(evaluationCase.outputRef()));
        }
        refs.add("expectedSummaryHash:" + verdictRecord.expectedSummaryHash());
        if (verdictRecord.actualSummaryHash() != null) {
            refs.add("actualSummaryHash:" + verdictRecord.actualSummaryHash());
        }
        return refs;
    }

    private static String compactInputRef(final ReplayInputRef inputRef) {
        return inputRef.refType() + ":" + inputRef.refId() + ":" + inputRef.contentHash().substring(0, 16);
    }

    private static String compactOutputRef(final ReplayOutputRef outputRef) {
        return outputRef.refType() + ":" + outputRef.refId() + ":" + outputRef.contentHash().substring(0, 16);
    }

    private static String redactedSummary(final RegressionVerdictRecord verdictRecord) {
        return "caseId="
                + verdictRecord.caseId()
                + ";evaluationId="
                + verdictRecord.evaluationId()
                + ";verdictId="
                + verdictRecord.verdictId()
                + ";verdict="
                + verdictRecord.verdict().name()
                + ";severity="
                + verdictRecord.severity().name();
    }

    private static String firstPresent(final String left, final String right) {
        if (left != null) {
            return left;
        }
        return right;
    }
}

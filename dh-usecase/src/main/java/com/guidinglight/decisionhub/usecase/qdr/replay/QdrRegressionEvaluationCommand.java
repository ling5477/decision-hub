package com.guidinglight.decisionhub.usecase.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionEvidenceRef;
import com.guidinglight.decisionhub.usecase.qdr.gateway.QdrModelGatewayIntegrationResult;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * B3 mock gateway regression evaluation command。
 *
 * <p>命令只接收 existing dry-run / QDR decision artifact 的 safe identity、mock gateway safe
 * summary/ref、expected/actual summary 和 regression policy。它不携带 raw prompt、raw provider
 * response、credential，也不包含 provider/HTTP/NQ runtime client。
 *
 * @param tenantId               tenant ID。
 * @param traceId                traceId。
 * @param requestId              requestId。
 * @param decisionId             decisionId。
 * @param sourceRequestId        来源 request ID，可为空，默认 requestId。
 * @param modelGatewayVersionRef actual model gateway version ref。
 * @param promptVersionRef       actual prompt version ref。
 * @param providerSummaryHash    actual provider summary hash。
 * @param gatewayResult          mock gateway safe summary。
 * @param expectedSummary        expected decision summary。
 * @param actualSummary          actual decision summary。
 * @param evidenceRefs           safe evidence refs。
 * @param policy                 regression baseline policy。
 * @param createdAt              创建时间。
 */
public record QdrRegressionEvaluationCommand(
        String tenantId,
        String traceId,
        String requestId,
        String decisionId,
        String sourceRequestId,
        String modelGatewayVersionRef,
        String promptVersionRef,
        String providerSummaryHash,
        QdrModelGatewayIntegrationResult gatewayResult,
        ExpectedDecisionSummary expectedSummary,
        ExpectedDecisionSummary actualSummary,
        List<RegressionEvidenceRef> evidenceRefs,
        RegressionBaselinePolicy policy,
        Instant createdAt) {

    /**
     * 校验 B3 regression evaluation command 的租户、trace、request、decision 和 safe metadata。
     */
    public QdrRegressionEvaluationCommand {
        tenantId = QdrRegressionSafety.requireTenantId(tenantId);
        traceId = QdrRegressionSafety.requireSafeText(traceId, "traceId");
        requestId = QdrRegressionSafety.requireSafeText(requestId, "requestId");
        decisionId = QdrRegressionSafety.requireSafeText(decisionId, "decisionId");
        sourceRequestId = QdrRegressionSafety.optionalSafeText(sourceRequestId, "sourceRequestId");
        if (sourceRequestId == null) {
            sourceRequestId = requestId;
        }
        modelGatewayVersionRef =
                QdrRegressionSafety.requireSafeText(modelGatewayVersionRef, "modelGatewayVersionRef");
        promptVersionRef = QdrRegressionSafety.requireSafeText(promptVersionRef, "promptVersionRef");
        providerSummaryHash =
                QdrRegressionSafety.requireSha256Hex(providerSummaryHash, "providerSummaryHash");
        gatewayResult = validateGatewayResult(gatewayResult);
        expectedSummary = ReplayPersistenceGuard.requireSummary(expectedSummary);
        actualSummary = ReplayPersistenceGuard.requireSummary(actualSummary);
        evidenceRefs = List.copyOf(Objects.requireNonNullElse(evidenceRefs, List.of()));
        if (evidenceRefs.isEmpty()) {
            throw new IllegalArgumentException("evidenceRefs must not be empty");
        }
        policy = Objects.requireNonNull(policy, "policy");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    private static QdrModelGatewayIntegrationResult validateGatewayResult(
            final QdrModelGatewayIntegrationResult result) {
        final QdrModelGatewayIntegrationResult checked = Objects.requireNonNull(result, "gatewayResult");
        QdrRegressionSafety.requireSafeText(checked.promptVersionId(), "gatewayResult.promptVersionId");
        QdrRegressionSafety.requireSafeText(checked.modelVersionId(), "gatewayResult.modelVersionId");
        QdrRegressionSafety.requireSafeText(checked.providerProfileId(), "gatewayResult.providerProfileId");
        QdrRegressionSafety.requireSafeText(checked.gatewayCallRef(), "gatewayResult.gatewayCallRef");
        QdrRegressionSafety.requireSafeText(checked.trustDecision(), "gatewayResult.trustDecision");
        QdrRegressionSafety.requireSafeText(checked.redactionStatus(), "gatewayResult.redactionStatus");
        QdrRegressionSafety.requireSafeText(checked.budgetSummary(), "gatewayResult.budgetSummary");
        QdrRegressionSafety.requireSafeText(checked.redactedSummary(), "gatewayResult.redactedSummary");
        QdrRegressionSafety.requireSafeText(checked.traceRef(), "gatewayResult.traceRef");
        QdrRegressionSafety.requireSafeText(checked.auditRef(), "gatewayResult.auditRef");
        return checked;
    }
}

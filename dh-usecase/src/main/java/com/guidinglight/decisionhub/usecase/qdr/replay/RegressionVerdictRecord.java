package com.guidinglight.decisionhub.usecase.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * QDR regression verdict 持久化 read model。
 *
 * <p>`PASS / FAIL / WARN / SKIPPED` 只表示 replay/evaluation 结果，不是交易建议，不触发 NQ mutation。
 *
 * @param id                     verdict 主键。
 * @param tenantId               tenant 边界。
 * @param caseId                 replay case ID。
 * @param evaluationId           evaluation ID。
 * @param verdictId              verdict ID。
 * @param sourceDecisionId       来源 decision ref。
 * @param sourceRequestId        来源 request ref。
 * @param traceId                traceId。
 * @param requestId              requestId。
 * @param policyVersion          policy version。
 * @param modelGatewayVersionRef model gateway version ref。
 * @param inputRefId             input ref 主键。
 * @param outputRefId            output ref 主键，可为空。
 * @param expectedSummaryId      expected summary 主键。
 * @param actualSummaryId        actual summary 主键，可为空。
 * @param expectedSummaryHash    expected summary hash。
 * @param actualSummaryHash      actual summary hash，可为空。
 * @param verdict                verdict 状态。
 * @param severity               severity。
 * @param findingCode            聚合 finding code。
 * @param findingMessage         聚合 finding message。
 * @param failureReason          failure reason。
 * @param createdAt              创建时间。
 * @param updatedAt              更新时间。
 */
public record RegressionVerdictRecord(
        UUID id,
        String tenantId,
        String caseId,
        String evaluationId,
        String verdictId,
        String sourceDecisionId,
        String sourceRequestId,
        String traceId,
        String requestId,
        String policyVersion,
        String modelGatewayVersionRef,
        UUID inputRefId,
        UUID outputRefId,
        UUID expectedSummaryId,
        UUID actualSummaryId,
        String expectedSummaryHash,
        String actualSummaryHash,
        RegressionVerdict.Status verdict,
        RegressionSeverity severity,
        String findingCode,
        String findingMessage,
        String failureReason,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * 校验 regression verdict read model。
     */
    public RegressionVerdictRecord {
        id = ReplayPersistenceGuard.requireUuid(id, "id");
        tenantId = ReplayPersistenceGuard.requireTenantId(tenantId);
        caseId = ReplayPersistenceGuard.requireSafeText(caseId, "caseId");
        evaluationId = ReplayPersistenceGuard.requireSafeText(evaluationId, "evaluationId");
        verdictId = ReplayPersistenceGuard.requireSafeText(verdictId, "verdictId");
        sourceDecisionId = ReplayPersistenceGuard.optionalSafeText(sourceDecisionId, "sourceDecisionId");
        sourceRequestId = ReplayPersistenceGuard.optionalSafeText(sourceRequestId, "sourceRequestId");
        ReplayPersistenceGuard.requireSourceRef(sourceDecisionId, sourceRequestId);
        traceId = ReplayPersistenceGuard.requireSafeText(traceId, "traceId");
        requestId = ReplayPersistenceGuard.requireSafeText(requestId, "requestId");
        policyVersion = ReplayPersistenceGuard.requireSafeText(policyVersion, "policyVersion");
        modelGatewayVersionRef =
                ReplayPersistenceGuard.optionalSafeText(modelGatewayVersionRef, "modelGatewayVersionRef");
        inputRefId = ReplayPersistenceGuard.requireUuid(inputRefId, "inputRefId");
        outputRefId = ReplayPersistenceGuard.optionalUuid(outputRefId, "outputRefId");
        expectedSummaryId = ReplayPersistenceGuard.requireUuid(expectedSummaryId, "expectedSummaryId");
        actualSummaryId = ReplayPersistenceGuard.optionalUuid(actualSummaryId, "actualSummaryId");
        expectedSummaryHash =
                ReplayPersistenceGuard.requireSha256Hex(expectedSummaryHash, "expectedSummaryHash");
        actualSummaryHash =
                ReplayPersistenceGuard.optionalSha256Hex(actualSummaryHash, "actualSummaryHash");
        verdict = Objects.requireNonNull(verdict, "verdict");
        severity = Objects.requireNonNull(severity, "severity");
        findingCode = ReplayPersistenceGuard.optionalSafeText(findingCode, "findingCode");
        findingMessage = ReplayPersistenceGuard.optionalSafeText(findingMessage, "findingMessage");
        failureReason = ReplayPersistenceGuard.optionalSafeText(failureReason, "failureReason");
        createdAt = ReplayPersistenceGuard.requireInstant(createdAt, "createdAt");
        updatedAt = ReplayPersistenceGuard.requireInstant(updatedAt, "updatedAt");
    }
}

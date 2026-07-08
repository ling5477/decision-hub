package com.guidinglight.decisionhub.usecase.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * QDR regression finding 持久化 read model。
 *
 * <p>finding 只记录结构化 code、severity、脱敏 message 和 evidence ref；不保存原始 payload 或交易动作。
 *
 * @param id                     finding 主键。
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
 * @param inputRefId             input ref 主键，可为空。
 * @param outputRefId            output ref 主键，可为空。
 * @param expectedSummaryId      expected summary 主键，可为空。
 * @param actualSummaryId        actual summary 主键，可为空。
 * @param expectedSummaryHash    expected summary hash，可为空。
 * @param actualSummaryHash      actual summary hash，可为空。
 * @param verdict                verdict 状态。
 * @param severity               severity。
 * @param findingCode            finding code。
 * @param findingMessage         脱敏 finding message。
 * @param evidenceRef            evidence ref。
 * @param createdAt              创建时间。
 * @param updatedAt              更新时间。
 */
public record RegressionFindingRecord(
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
        String evidenceRef,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * 校验 finding read model。
     */
    public RegressionFindingRecord {
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
        inputRefId = ReplayPersistenceGuard.optionalUuid(inputRefId, "inputRefId");
        outputRefId = ReplayPersistenceGuard.optionalUuid(outputRefId, "outputRefId");
        expectedSummaryId = ReplayPersistenceGuard.optionalUuid(expectedSummaryId, "expectedSummaryId");
        actualSummaryId = ReplayPersistenceGuard.optionalUuid(actualSummaryId, "actualSummaryId");
        expectedSummaryHash =
                ReplayPersistenceGuard.optionalSha256Hex(expectedSummaryHash, "expectedSummaryHash");
        actualSummaryHash =
                ReplayPersistenceGuard.optionalSha256Hex(actualSummaryHash, "actualSummaryHash");
        verdict = Objects.requireNonNull(verdict, "verdict");
        severity = Objects.requireNonNull(severity, "severity");
        findingCode = ReplayPersistenceGuard.requireSafeText(findingCode, "findingCode");
        findingMessage = ReplayPersistenceGuard.requireSafeText(findingMessage, "findingMessage");
        evidenceRef = ReplayPersistenceGuard.optionalSafeText(evidenceRef, "evidenceRef");
        createdAt = ReplayPersistenceGuard.requireInstant(createdAt, "createdAt");
        updatedAt = ReplayPersistenceGuard.requireInstant(updatedAt, "updatedAt");
    }
}

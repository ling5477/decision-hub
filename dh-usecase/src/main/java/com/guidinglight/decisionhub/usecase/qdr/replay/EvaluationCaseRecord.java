package com.guidinglight.decisionhub.usecase.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayOutputRef;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * QDR evaluation case 持久化 read model。
 *
 * <p>该 record 表示 replay case 在某个 policy/model gateway ref 下的一次结构化评估，不执行 replay，
 * 不调用 provider，不产生 trading signal。
 *
 * @param id                     evaluation case 主键。
 * @param tenantId               tenant 边界。
 * @param caseId                 replay case ID。
 * @param evaluationId           evaluation ID。
 * @param verdictId              verdict ID，可为空。
 * @param sourceDecisionId       来源 decision ref。
 * @param sourceRequestId        来源 request ref。
 * @param traceId                traceId。
 * @param requestId              requestId。
 * @param policyVersion          policy version。
 * @param modelVersionRef        model version ref。
 * @param modelGatewayVersionRef model gateway version ref。
 * @param inputRefId             input ref 主键。
 * @param outputRefId            output ref 主键，可为空。
 * @param expectedSummaryId      expected summary 主键。
 * @param actualSummaryId        actual summary 主键，可为空。
 * @param inputRef               结构化 input ref。
 * @param outputRef              结构化 output ref，可为空。
 * @param expectedSummary        expected summary。
 * @param actualSummary          actual summary，可为空。
 * @param expectedSummaryHash    expected summary hash。
 * @param actualSummaryHash      actual summary hash，可为空。
 * @param verdict                当前 verdict 摘要，可为空。
 * @param severity               聚合 severity，可为空。
 * @param findingCode            聚合 finding code，可为空。
 * @param findingMessage         聚合 finding message，可为空。
 * @param evaluationChecksum     evaluation checksum。
 * @param createdAt              创建时间。
 * @param updatedAt              更新时间。
 */
public record EvaluationCaseRecord(
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
        String modelVersionRef,
        String modelGatewayVersionRef,
        UUID inputRefId,
        UUID outputRefId,
        UUID expectedSummaryId,
        UUID actualSummaryId,
        ReplayInputRef inputRef,
        ReplayOutputRef outputRef,
        ExpectedDecisionSummary expectedSummary,
        ExpectedDecisionSummary actualSummary,
        String expectedSummaryHash,
        String actualSummaryHash,
        RegressionVerdict.Status verdict,
        RegressionSeverity severity,
        String findingCode,
        String findingMessage,
        String evaluationChecksum,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * 校验 evaluation case read model。
     */
    public EvaluationCaseRecord {
        id = ReplayPersistenceGuard.requireUuid(id, "id");
        tenantId = ReplayPersistenceGuard.requireTenantId(tenantId);
        caseId = ReplayPersistenceGuard.requireSafeText(caseId, "caseId");
        evaluationId = ReplayPersistenceGuard.requireSafeText(evaluationId, "evaluationId");
        verdictId = ReplayPersistenceGuard.optionalSafeText(verdictId, "verdictId");
        sourceDecisionId = ReplayPersistenceGuard.optionalSafeText(sourceDecisionId, "sourceDecisionId");
        sourceRequestId = ReplayPersistenceGuard.optionalSafeText(sourceRequestId, "sourceRequestId");
        ReplayPersistenceGuard.requireSourceRef(sourceDecisionId, sourceRequestId);
        traceId = ReplayPersistenceGuard.requireSafeText(traceId, "traceId");
        requestId = ReplayPersistenceGuard.requireSafeText(requestId, "requestId");
        policyVersion = ReplayPersistenceGuard.requireSafeText(policyVersion, "policyVersion");
        modelVersionRef = ReplayPersistenceGuard.optionalSafeText(modelVersionRef, "modelVersionRef");
        modelGatewayVersionRef =
                ReplayPersistenceGuard.optionalSafeText(modelGatewayVersionRef, "modelGatewayVersionRef");
        if (modelVersionRef == null && modelGatewayVersionRef == null) {
            throw new IllegalArgumentException("modelVersionRef or modelGatewayVersionRef is required");
        }
        inputRefId = ReplayPersistenceGuard.requireUuid(inputRefId, "inputRefId");
        outputRefId = ReplayPersistenceGuard.optionalUuid(outputRefId, "outputRefId");
        expectedSummaryId = ReplayPersistenceGuard.requireUuid(expectedSummaryId, "expectedSummaryId");
        actualSummaryId = ReplayPersistenceGuard.optionalUuid(actualSummaryId, "actualSummaryId");
        inputRef = ReplayPersistenceGuard.requireInputRef(inputRef);
        outputRef = ReplayPersistenceGuard.optionalOutputRef(outputRef);
        expectedSummary = ReplayPersistenceGuard.requireSummary(expectedSummary);
        actualSummary = ReplayPersistenceGuard.optionalSummary(actualSummary);
        expectedSummaryHash =
                ReplayPersistenceGuard.requireSha256Hex(expectedSummaryHash, "expectedSummaryHash");
        actualSummaryHash =
                ReplayPersistenceGuard.optionalSha256Hex(actualSummaryHash, "actualSummaryHash");
        verdict = verdict == null ? null : Objects.requireNonNull(verdict, "verdict");
        severity = severity == null ? null : Objects.requireNonNull(severity, "severity");
        findingCode = ReplayPersistenceGuard.optionalSafeText(findingCode, "findingCode");
        findingMessage = ReplayPersistenceGuard.optionalSafeText(findingMessage, "findingMessage");
        evaluationChecksum =
                ReplayPersistenceGuard.requireSha256Hex(evaluationChecksum, "evaluationChecksum");
        createdAt = ReplayPersistenceGuard.requireInstant(createdAt, "createdAt");
        updatedAt = ReplayPersistenceGuard.requireInstant(updatedAt, "updatedAt");
    }
}

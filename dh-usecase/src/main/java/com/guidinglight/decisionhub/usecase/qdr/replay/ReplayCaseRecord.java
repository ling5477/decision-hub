package com.guidinglight.decisionhub.usecase.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;

import java.time.Instant;
import java.util.UUID;

/**
 * QDR replay case 持久化 read model。
 *
 * <p>该 record 只暴露 tenant-bound identity、结构化 input ref、expected summary 与 hash/checksum；
 * 不暴露 raw prompt、raw provider response 或 credential，也不是 executable trading signal。
 *
 * @param id                  replay case 主键。
 * @param tenantId            tenant 边界。
 * @param caseId              租户内 case ID。
 * @param sourceDecisionId    来源 decision ref。
 * @param sourceRequestId     来源 request ref。
 * @param traceId             traceId。
 * @param requestId           requestId。
 * @param policyVersion       policy version。
 * @param modelGatewayVersionRef model gateway version ref。
 * @param inputRefId          input ref 主键。
 * @param expectedSummaryId   expected summary 主键。
 * @param inputRef            结构化 input ref。
 * @param expectedSummary     结构化 expected summary。
 * @param expectedSummaryHash expected summary hash。
 * @param caseChecksum        replay case checksum。
 * @param createdAt           创建时间。
 * @param updatedAt           更新时间。
 */
public record ReplayCaseRecord(
        UUID id,
        String tenantId,
        String caseId,
        String sourceDecisionId,
        String sourceRequestId,
        String traceId,
        String requestId,
        String policyVersion,
        String modelGatewayVersionRef,
        UUID inputRefId,
        UUID expectedSummaryId,
        ReplayInputRef inputRef,
        ExpectedDecisionSummary expectedSummary,
        String expectedSummaryHash,
        String caseChecksum,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * 校验 replay case read model 仍满足 B2 persistence boundary。
     */
    public ReplayCaseRecord {
        id = ReplayPersistenceGuard.requireUuid(id, "id");
        tenantId = ReplayPersistenceGuard.requireTenantId(tenantId);
        caseId = ReplayPersistenceGuard.requireSafeText(caseId, "caseId");
        sourceDecisionId = ReplayPersistenceGuard.optionalSafeText(sourceDecisionId, "sourceDecisionId");
        sourceRequestId = ReplayPersistenceGuard.optionalSafeText(sourceRequestId, "sourceRequestId");
        ReplayPersistenceGuard.requireSourceRef(sourceDecisionId, sourceRequestId);
        traceId = ReplayPersistenceGuard.requireSafeText(traceId, "traceId");
        requestId = ReplayPersistenceGuard.requireSafeText(requestId, "requestId");
        policyVersion = ReplayPersistenceGuard.requireSafeText(policyVersion, "policyVersion");
        modelGatewayVersionRef =
                ReplayPersistenceGuard.optionalSafeText(modelGatewayVersionRef, "modelGatewayVersionRef");
        inputRefId = ReplayPersistenceGuard.requireUuid(inputRefId, "inputRefId");
        expectedSummaryId = ReplayPersistenceGuard.requireUuid(expectedSummaryId, "expectedSummaryId");
        inputRef = ReplayPersistenceGuard.requireInputRef(inputRef);
        expectedSummary = ReplayPersistenceGuard.requireSummary(expectedSummary);
        expectedSummaryHash =
                ReplayPersistenceGuard.requireSha256Hex(expectedSummaryHash, "expectedSummaryHash");
        caseChecksum = ReplayPersistenceGuard.requireSha256Hex(caseChecksum, "caseChecksum");
        createdAt = ReplayPersistenceGuard.requireInstant(createdAt, "createdAt");
        updatedAt = ReplayPersistenceGuard.requireInstant(updatedAt, "updatedAt");
    }
}

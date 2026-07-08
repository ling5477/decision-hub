package com.guidinglight.decisionhub.usecase.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.replay.ReplayCase;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 保存 QDR replay case 的命令。
 *
 * <p>命令只携带结构化 B1 replay contract、input/summary ref ID 与 checksum；不携带 prompt 正文、
 * provider 原始响应、credential 或任何交易执行 payload。
 *
 * @param replayCaseId        replay case 主键。
 * @param inputRefId          input ref 主键。
 * @param expectedSummaryId   expected summary 主键。
 * @param replayCase          B1 replay case 合同。
 * @param requestId           requestId。
 * @param modelGatewayVersionRef model gateway version ref。
 * @param expectedSummaryHash expected summary hash。
 * @param caseChecksum        replay case checksum。
 * @param updatedAt           更新时间。
 */
public record SaveReplayCaseCommand(
        UUID replayCaseId,
        UUID inputRefId,
        UUID expectedSummaryId,
        ReplayCase replayCase,
        String requestId,
        String modelGatewayVersionRef,
        String expectedSummaryHash,
        String caseChecksum,
        Instant updatedAt) {

    /**
     * 校验保存命令。
     */
    public SaveReplayCaseCommand {
        replayCaseId = ReplayPersistenceGuard.requireUuid(replayCaseId, "replayCaseId");
        inputRefId = ReplayPersistenceGuard.requireUuid(inputRefId, "inputRefId");
        expectedSummaryId = ReplayPersistenceGuard.requireUuid(expectedSummaryId, "expectedSummaryId");
        replayCase = Objects.requireNonNull(replayCase, "replayCase");
        ReplayPersistenceGuard.requireTenantId(replayCase.tenantId());
        ReplayPersistenceGuard.requireSafeText(replayCase.caseId(), "caseId");
        final String sourceDecisionId =
                ReplayPersistenceGuard.optionalSafeText(replayCase.sourceDecisionId(), "sourceDecisionId");
        final String sourceRequestId =
                ReplayPersistenceGuard.optionalSafeText(replayCase.sourceRequestId(), "sourceRequestId");
        ReplayPersistenceGuard.requireSourceRef(sourceDecisionId, sourceRequestId);
        ReplayPersistenceGuard.requireSafeText(replayCase.traceId(), "traceId");
        ReplayPersistenceGuard.requireInputRef(replayCase.inputRef());
        ReplayPersistenceGuard.requireSummary(replayCase.expectedSummary());
        Objects.requireNonNull(replayCase.policy(), "policy");
        ReplayPersistenceGuard.requireSafeText(replayCase.policy().policyVersion(), "policyVersion");
        ReplayPersistenceGuard.requireInstant(replayCase.createdAt(), "createdAt");
        requestId = ReplayPersistenceGuard.requireSafeText(requestId, "requestId");
        modelGatewayVersionRef =
                ReplayPersistenceGuard.optionalSafeText(modelGatewayVersionRef, "modelGatewayVersionRef");
        expectedSummaryHash =
                ReplayPersistenceGuard.requireSha256Hex(expectedSummaryHash, "expectedSummaryHash");
        caseChecksum = ReplayPersistenceGuard.requireSha256Hex(caseChecksum, "caseChecksum");
        updatedAt = ReplayPersistenceGuard.requireInstant(updatedAt, "updatedAt");
    }

    /**
     * @return tenant ID。
     */
    public String tenantId() {
        return replayCase.tenantId();
    }

    /**
     * @return replay case ID。
     */
    public String caseId() {
        return replayCase.caseId();
    }
}

package com.guidinglight.decisionhub.usecase.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.replay.EvaluationCase;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 保存 QDR evaluation case 的命令。
 *
 * <p>命令只绑定 replay case、结构化 input/output ref、expected/actual summary 与 checksum；不触发
 * replay execution，不调用 provider，不保存 raw material。
 *
 * @param evaluationCaseId     evaluation case 主键。
 * @param inputRefId           input ref 主键。
 * @param outputRefId          output ref 主键，可为空。
 * @param expectedSummaryId    expected summary 主键。
 * @param actualSummaryId      actual summary 主键，可为空。
 * @param evaluationCase       B1 evaluation case 合同。
 * @param sourceDecisionId     来源 decision ref。
 * @param sourceRequestId      来源 request ref。
 * @param traceId              traceId。
 * @param requestId            requestId。
 * @param inputRef             结构化 input ref。
 * @param expectedSummaryHash  expected summary hash。
 * @param actualSummaryHash    actual summary hash，可为空。
 * @param evaluationChecksum   evaluation checksum。
 * @param createdAt            创建时间。
 * @param updatedAt            更新时间。
 */
public record SaveEvaluationCaseCommand(
        UUID evaluationCaseId,
        UUID inputRefId,
        UUID outputRefId,
        UUID expectedSummaryId,
        UUID actualSummaryId,
        EvaluationCase evaluationCase,
        String sourceDecisionId,
        String sourceRequestId,
        String traceId,
        String requestId,
        ReplayInputRef inputRef,
        String expectedSummaryHash,
        String actualSummaryHash,
        String evaluationChecksum,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * 校验保存命令。
     */
    public SaveEvaluationCaseCommand {
        evaluationCaseId = ReplayPersistenceGuard.requireUuid(evaluationCaseId, "evaluationCaseId");
        inputRefId = ReplayPersistenceGuard.requireUuid(inputRefId, "inputRefId");
        outputRefId = ReplayPersistenceGuard.optionalUuid(outputRefId, "outputRefId");
        expectedSummaryId = ReplayPersistenceGuard.requireUuid(expectedSummaryId, "expectedSummaryId");
        actualSummaryId = ReplayPersistenceGuard.optionalUuid(actualSummaryId, "actualSummaryId");
        evaluationCase = Objects.requireNonNull(evaluationCase, "evaluationCase");
        ReplayPersistenceGuard.requireTenantId(evaluationCase.tenantId());
        ReplayPersistenceGuard.requireSafeText(evaluationCase.evaluationId(), "evaluationId");
        ReplayPersistenceGuard.requireSafeText(evaluationCase.caseId(), "caseId");
        ReplayPersistenceGuard.requireSafeText(evaluationCase.policyVersion(), "policyVersion");
        final String modelVersionRef =
                ReplayPersistenceGuard.optionalSafeText(evaluationCase.modelVersionRef(), "modelVersionRef");
        final String modelGatewayVersionRef =
                ReplayPersistenceGuard.optionalSafeText(
                        evaluationCase.modelGatewayVersionRef(), "modelGatewayVersionRef");
        if (modelVersionRef == null && modelGatewayVersionRef == null) {
            throw new IllegalArgumentException("modelVersionRef or modelGatewayVersionRef is required");
        }
        ReplayPersistenceGuard.requireSummary(evaluationCase.expectedSummary());
        ReplayPersistenceGuard.optionalSummary(evaluationCase.actualSummary());
        ReplayPersistenceGuard.optionalOutputRef(evaluationCase.outputRef());
        sourceDecisionId = ReplayPersistenceGuard.optionalSafeText(sourceDecisionId, "sourceDecisionId");
        sourceRequestId = ReplayPersistenceGuard.optionalSafeText(sourceRequestId, "sourceRequestId");
        ReplayPersistenceGuard.requireSourceRef(sourceDecisionId, sourceRequestId);
        traceId = ReplayPersistenceGuard.requireSafeText(traceId, "traceId");
        requestId = ReplayPersistenceGuard.requireSafeText(requestId, "requestId");
        inputRef = ReplayPersistenceGuard.requireInputRef(inputRef);
        expectedSummaryHash =
                ReplayPersistenceGuard.requireSha256Hex(expectedSummaryHash, "expectedSummaryHash");
        actualSummaryHash =
                ReplayPersistenceGuard.optionalSha256Hex(actualSummaryHash, "actualSummaryHash");
        evaluationChecksum =
                ReplayPersistenceGuard.requireSha256Hex(evaluationChecksum, "evaluationChecksum");
        createdAt = ReplayPersistenceGuard.requireInstant(createdAt, "createdAt");
        updatedAt = ReplayPersistenceGuard.requireInstant(updatedAt, "updatedAt");
    }

    /**
     * @return tenant ID。
     */
    public String tenantId() {
        return evaluationCase.tenantId();
    }

    /**
     * @return evaluation ID。
     */
    public String evaluationId() {
        return evaluationCase.evaluationId();
    }
}

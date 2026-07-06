package com.guidinglight.decisionhub.usecase.qdr.readmodel;

import com.guidinglight.decisionhub.domain.qdr.DecisionRunStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * stage-qdr-2 B1 read model projection：Decision run 只读详情。
 *
 * <p>本视图禁止承载 raw credential / raw provider response / raw prompt；仅用于展示审计摘要。
 */
public record DecisionRunDetailView(
        String decisionRequestId,
        String decisionRunId,
        String tenantId,
        String traceId,
        String requestId,
        String requestKey,
        String requestType,
        String sourceSystem,
        String sourceRefId,
        Integer runNo,
        DecisionRunStatus status,
        Instant startedAt,
        Instant finishedAt,
        Long latencyMs,
        String errorCode,
        String errorMessage,
        String quantSignalSummary,
        String quantDecisionSummary,
        String outputSummary,
        Instant createdAt) {

    public DecisionRunDetailView {
        decisionRequestId = ReadModelValidation.requireText(decisionRequestId, "decisionRequestId");
        decisionRunId = ReadModelValidation.requireText(decisionRunId, "decisionRunId");
        tenantId = ReadModelValidation.requireText(tenantId, "tenantId");
        traceId = ReadModelValidation.requireText(traceId, "traceId");
        requestId = ReadModelValidation.requireText(requestId, "requestId");
        requestKey = ReadModelValidation.requireText(requestKey, "requestKey");
        requestType = ReadModelValidation.requireText(requestType, "requestType");
        sourceSystem = ReadModelValidation.requireText(sourceSystem, "sourceSystem");
        sourceRefId = ReadModelValidation.optionalText(sourceRefId, "sourceRefId");
        runNo = ReadModelValidation.requirePositive(runNo, "runNo");
        status = Objects.requireNonNull(status, "status");
        latencyMs = ReadModelValidation.optionalNonNegative(latencyMs, "latencyMs");
        errorCode = ReadModelValidation.optionalText(errorCode, "errorCode");
        errorMessage = ReadModelValidation.optionalSummary(errorMessage, "errorMessage");
        quantSignalSummary =
                ReadModelValidation.optionalSummary(quantSignalSummary, "quantSignalSummary");
        quantDecisionSummary =
                ReadModelValidation.optionalSummary(quantDecisionSummary, "quantDecisionSummary");
        outputSummary = ReadModelValidation.optionalSummary(outputSummary, "outputSummary");
        startedAt = Objects.requireNonNull(startedAt, "startedAt");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }
}

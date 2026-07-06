package com.guidinglight.decisionhub.usecase.qdr.readmodel;

import java.time.Instant;
import java.util.Objects;

/**
 * stage-qdr-2 B1 trace step 只读摘要。
 *
 * <p>该 projection 只承载 summary/ref 字段，不能保存 raw prompt、raw provider response、凭证或可执行交易指令。
 */
public record DecisionTraceStepView(
        String stepId,
        Integer stepNo,
        String stepName,
        String stepType,
        String status,
        Instant startedAt,
        Instant finishedAt,
        Long latencyMs,
        String errorCode,
        String errorMessage,
        String inputSummaryJson,
        String outputSummaryJson,
        String providerCallRef,
        String auditRef) {

    public DecisionTraceStepView {
        stepId = ReadModelValidation.requireText(stepId, "stepId");
        stepNo = ReadModelValidation.requirePositive(stepNo, "stepNo");
        stepName = ReadModelValidation.requireText(stepName, "stepName");
        stepType = ReadModelValidation.requireText(stepType, "stepType");
        status = ReadModelValidation.requireText(status, "status");
        latencyMs = ReadModelValidation.optionalNonNegative(latencyMs, "latencyMs");
        errorCode = ReadModelValidation.optionalText(errorCode, "errorCode");
        errorMessage = ReadModelValidation.optionalSummary(errorMessage, "errorMessage");
        inputSummaryJson = ReadModelValidation.optionalSummary(inputSummaryJson, "inputSummaryJson");
        outputSummaryJson = ReadModelValidation.optionalSummary(outputSummaryJson, "outputSummaryJson");
        providerCallRef = ReadModelValidation.optionalSummary(providerCallRef, "providerCallRef");
        auditRef = ReadModelValidation.optionalSummary(auditRef, "auditRef");
        startedAt = Objects.requireNonNull(startedAt, "startedAt");
    }
}

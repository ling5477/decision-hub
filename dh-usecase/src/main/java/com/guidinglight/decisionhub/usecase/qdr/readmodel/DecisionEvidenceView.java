package com.guidinglight.decisionhub.usecase.qdr.readmodel;

import java.util.List;
import java.util.Objects;

/**
 * stage-qdr-2 B1 decision run evidence 只读引用视图。
 *
 * <p>该 DTO 只暴露脱敏摘要和引用，不能承载 raw prompt、raw provider response、凭证或可执行交易指令。
 */
public record DecisionEvidenceView(
        String decisionRunId,
        String tenantId,
        String traceId,
        String contextSnapshotRef,
        List<String> providerCallLogRefs,
        String decisionOutputRef,
        String quantDecisionRef,
        RedactionStatus redactionStatus,
        String evidenceRefsJson) {

    public DecisionEvidenceView {
        decisionRunId = ReadModelValidation.requireText(decisionRunId, "decisionRunId");
        tenantId = ReadModelValidation.requireText(tenantId, "tenantId");
        traceId = ReadModelValidation.requireText(traceId, "traceId");
        contextSnapshotRef =
                ReadModelValidation.optionalSummary(contextSnapshotRef, "contextSnapshotRef");
        providerCallLogRefs =
                ReadModelValidation.optionalRefs(providerCallLogRefs, "providerCallLogRefs");
        decisionOutputRef = ReadModelValidation.optionalSummary(decisionOutputRef, "decisionOutputRef");
        quantDecisionRef = ReadModelValidation.optionalSummary(quantDecisionRef, "quantDecisionRef");
        redactionStatus = Objects.requireNonNull(redactionStatus, "redactionStatus");
        evidenceRefsJson = ReadModelValidation.optionalSummary(evidenceRefsJson, "evidenceRefsJson");
    }
}

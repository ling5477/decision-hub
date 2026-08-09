package com.guidinglight.decisionhub.usecase.qdr.evidence;

/** Exact decision identity used to resolve origin environment without caller environment input. */
public record DecisionEnvironmentProvenanceQuery(
        String tenantId,
        String traceId,
        String requestId,
        String decisionId,
        String decisionRunId) {

    public DecisionEnvironmentProvenanceQuery {
        tenantId = DecisionEvidencePolicy.requireSafeText(tenantId, "tenantId");
        traceId = DecisionEvidencePolicy.requireSafeText(traceId, "traceId");
        requestId = DecisionEvidencePolicy.requireSafeText(requestId, "requestId");
        decisionId = DecisionEvidencePolicy.requireSafeText(decisionId, "decisionId");
        decisionRunId = DecisionEvidencePolicy.requireSafeText(decisionRunId, "decisionRunId");
    }
}

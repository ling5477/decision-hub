package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * QDR pipeline 可消费的 mock gateway safe refs。
 *
 * <p>该结果只能用于 reasoning / evidence summary / audit trace，不得被映射为 approval status、NQ mutation
 * 或交易执行动作。
 */
public record QdrModelGatewayIntegrationResult(
        String promptVersionId,
        String modelVersionId,
        String providerProfileId,
        String gatewayCallRef,
        String trustDecision,
        String redactionStatus,
        String budgetSummary,
        String redactedSummary,
        String traceRef,
        String auditRef) {

    /** 校验 refs 非空；这些字段均为 safe metadata，不包含 raw prompt 或 raw provider response。 */
    public QdrModelGatewayIntegrationResult {
        promptVersionId = requireText(promptVersionId, "promptVersionId");
        modelVersionId = requireText(modelVersionId, "modelVersionId");
        providerProfileId = requireText(providerProfileId, "providerProfileId");
        gatewayCallRef = requireText(gatewayCallRef, "gatewayCallRef");
        trustDecision = requireText(trustDecision, "trustDecision");
        redactionStatus = requireText(redactionStatus, "redactionStatus");
        budgetSummary = requireText(budgetSummary, "budgetSummary");
        redactedSummary = requireText(redactedSummary, "redactedSummary");
        traceRef = requireText(traceRef, "traceRef");
        auditRef = requireText(auditRef, "auditRef");
    }

    /**
     * 转为 response trace summary 的安全条目。
     *
     * @return 不含 raw material 的 trace summary。
     */
    public List<String> traceSummaryEntries() {
        return List.of(
                "modelGateway:MODEL_GATEWAY_MOCK_CALL",
                "promptVersion:" + promptVersionId,
                "modelVersion:" + modelVersionId,
                "providerProfile:" + providerProfileId,
                "gatewayCall:" + gatewayCallRef,
                "trust:" + trustDecision,
                "redaction:" + redactionStatus,
                "budget:" + budgetSummary);
    }

    /**
     * 转为 quant_decision.constraints_json 的安全 refs。
     *
     * @return gateway refs map。
     */
    public Map<String, Object> toConstraintRefs() {
        return Map.of(
                "modelGatewayStep", "MODEL_GATEWAY_MOCK_CALL",
                "promptVersionId", promptVersionId,
                "modelVersionId", modelVersionId,
                "providerProfileId", providerProfileId,
                "gatewayCallRef", gatewayCallRef,
                "trustDecision", trustDecision,
                "redactionStatus", redactionStatus,
                "budgetSummary", budgetSummary);
    }

    private static String requireText(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return checked;
    }
}

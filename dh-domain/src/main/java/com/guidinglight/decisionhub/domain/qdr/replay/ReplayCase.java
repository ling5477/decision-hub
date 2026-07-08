package com.guidinglight.decisionhub.domain.qdr.replay;

import java.time.Instant;

/**
 * QDR replay case 合同。
 *
 * <p>该对象是 B1 内存合同，不落库、不触发 replay execution、不读取真实 provider，也不保存原始 prompt。
 */
public record ReplayCase(
        String tenantId,
        String caseId,
        String sourceDecisionId,
        String sourceRequestId,
        String traceId,
        ReplayInputRef inputRef,
        ExpectedDecisionSummary expectedSummary,
        EvaluationPolicy policy,
        Instant createdAt) {

    /**
     * 规范化 replay case 标识字段；完整性由 usecase 合同服务 fail-closed 校验。
     */
    public ReplayCase {
        tenantId = trimToNull(tenantId);
        caseId = trimToNull(caseId);
        sourceDecisionId = trimToNull(sourceDecisionId);
        sourceRequestId = trimToNull(sourceRequestId);
        traceId = trimToNull(traceId);
    }

    private static String trimToNull(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

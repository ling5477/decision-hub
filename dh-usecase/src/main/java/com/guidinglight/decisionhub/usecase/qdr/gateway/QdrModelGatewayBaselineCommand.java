package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.Objects;
import java.util.UUID;

/**
 * QDR mock gateway baseline bootstrap command。
 *
 * <p>该命令只携带 tenant / trace / request / decisionRunId 这类审计定位字段，不包含 raw prompt、
 * provider response、credential 或交易执行信息。
 *
 * @param tenantId      tenant 边界。
 * @param traceId       traceId。
 * @param requestId     requestId。
 * @param decisionRunId QDR decision_run id。
 */
public record QdrModelGatewayBaselineCommand(
        String tenantId, String traceId, String requestId, UUID decisionRunId) {

    /** 校验 bootstrap 所需审计定位字段。 */
    public QdrModelGatewayBaselineCommand {
        tenantId = requireText(tenantId, "tenantId");
        traceId = requireText(traceId, "traceId");
        requestId = requireText(requestId, "requestId");
        decisionRunId = Objects.requireNonNull(decisionRunId, "decisionRunId");
    }

    private static String requireText(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return checked;
    }
}

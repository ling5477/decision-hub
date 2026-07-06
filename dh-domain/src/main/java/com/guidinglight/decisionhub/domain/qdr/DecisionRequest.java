package com.guidinglight.decisionhub.domain.qdr;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Quant Decision Review 的主线请求记录。
 *
 * <p>该对象对应 `decision_request` 表，只保存只读审查请求的脱敏输入与追踪字段；不得包含凭证、账户密钥、
 * 可执行订单、数量、价格或 NQ mutation 指令。
 */
public record DecisionRequest(
        UUID id,
        String requestKey,
        String requestType,
        String sourceSystem,
        String sourceRefId,
        String tenantId,
        String traceId,
        String requestId,
        Map<String, Object> inputPayloadJson,
        Map<String, Object> contextPayloadJson,
        DecisionRequestStatus status,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * 校验主线请求记录的审计字段与 JSON payload 边界。
     */
    public DecisionRequest {
        id = Objects.requireNonNull(id, "id");
        requestKey = requireText(requestKey, "requestKey");
        requestType = requireText(requestType, "requestType");
        sourceSystem = requireText(sourceSystem, "sourceSystem");
        tenantId = requireText(tenantId, "tenantId");
        traceId = requireText(traceId, "traceId");
        requestId = requireText(requestId, "requestId");
        inputPayloadJson = Map.copyOf(Objects.requireNonNull(inputPayloadJson, "inputPayloadJson"));
        contextPayloadJson = contextPayloadJson == null ? null : Map.copyOf(contextPayloadJson);
        status = Objects.requireNonNull(status, "status");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    private static String requireText(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return checked;
    }
}

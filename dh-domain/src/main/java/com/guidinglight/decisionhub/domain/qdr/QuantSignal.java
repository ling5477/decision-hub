package com.guidinglight.decisionhub.domain.qdr;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Quant Decision Review 的输入信号摘要。
 *
 * <p>该对象对应 `quant_signal` 表，只保存 DH 侧只读审查所需的脱敏 signal/report/risk/backtest 摘要；
 * 不保存账户、订单、价格、数量、杠杆、凭证或可执行交易 payload。
 */
public record QuantSignal(
        UUID id,
        UUID decisionRequestId,
        String sourceSystem,
        String symbol,
        String exchange,
        String timeframe,
        String signalType,
        Map<String, Object> signalPayloadJson,
        String strategyId,
        String strategyVersion,
        String datasetVersion,
        Instant receivedAt,
        Instant createdAt) {

    /**
     * 校验 signal 归属字段和 JSON payload。
     */
    public QuantSignal {
        id = Objects.requireNonNull(id, "id");
        decisionRequestId = Objects.requireNonNull(decisionRequestId, "decisionRequestId");
        sourceSystem = requireText(sourceSystem, "sourceSystem");
        signalType = requireText(signalType, "signalType");
        signalPayloadJson =
                Map.copyOf(Objects.requireNonNull(signalPayloadJson, "signalPayloadJson"));
        receivedAt = Objects.requireNonNull(receivedAt, "receivedAt");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    private static String requireText(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return checked;
    }
}

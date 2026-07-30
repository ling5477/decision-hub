package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * QDR decision pipeline 调用 mock ModelGateway 的命令。
 *
 * <p>命令只包含已经通过 dry-run 安全门禁的脱敏上下文字段。调用方不得把 raw prompt、raw provider
 * response、credential、订单数量、价格或执行指令放入该命令。
 *
 * @param tenantId      tenant 边界。
 * @param traceId       traceId。
 * @param requestId     requestId / V5 decisionId。
 * @param decisionRunId V6 decision_run id。
 * @param symbol        脱敏 subject symbol。
 * @param market        脱敏 market。
 * @param timeframe     脱敏 timeframe。
 * @param riskLevel     已有 QDR risk summary。
 * @param evidenceRefs  已脱敏 evidence refs。
 */
public record QdrModelGatewayIntegrationCommand(
        String tenantId,
        String traceId,
        String requestId,
        UUID decisionRunId,
        String symbol,
        String market,
        String timeframe,
        String riskLevel,
        List<String> evidenceRefs) {

    /** 校验 gateway integration 所需字段，集合复制避免后续被调用方修改。 */
    public QdrModelGatewayIntegrationCommand {
        tenantId = requireText(tenantId, "tenantId");
        traceId = requireText(traceId, "traceId");
        requestId = requireText(requestId, "requestId");
        decisionRunId = Objects.requireNonNull(decisionRunId, "decisionRunId");
        symbol = requireText(symbol, "symbol");
        market = requireText(market, "market");
        timeframe = requireText(timeframe, "timeframe");
        riskLevel = requireText(riskLevel, "riskLevel");
        evidenceRefs = List.copyOf(evidenceRefs == null ? List.of() : evidenceRefs);
    }

    private static String requireText(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return checked;
    }
}

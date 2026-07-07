package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.List;
import java.util.Map;

/**
 * Model gateway request。
 *
 * <p>request 不承载 provider credential、API key 或 raw provider response。缺失字段、secret-like
 * input、budget 缺失与 redaction failure 由 gateway service 转为结构化 fail-closed result。
 *
 * @param context               gateway identity context。
 * @param promptTemplateId      prompt template id。
 * @param promptVersion         prompt version string。
 * @param promptVersionChecksum expected prompt checksum。
 * @param modelVersionChecksum  expected model checksum。
 * @param renderInputs          已脱敏 render input。
 * @param memoryEntries         已脱敏 memory/context entry。
 * @param policy                model call policy。
 * @param budget                budget guard。
 * @param redactionPolicy       redaction guard policy。
 */
public record ModelGatewayRequest(
        ModelCallContext context,
        String promptTemplateId,
        String promptVersion,
        String promptVersionChecksum,
        String modelVersionChecksum,
        Map<String, String> renderInputs,
        List<String> memoryEntries,
        ModelCallPolicy policy,
        ModelCallBudget budget,
        ModelCallRedactionPolicy redactionPolicy) {

    /**
     * 复制集合输入，防止调用方在 gateway 运行中修改 request。
     */
    public ModelGatewayRequest {
        renderInputs = Map.copyOf(renderInputs == null ? Map.of() : renderInputs);
        memoryEntries = List.copyOf(memoryEntries == null ? List.of() : memoryEntries);
    }
}

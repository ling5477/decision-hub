package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Model gateway 本地 usage summary。
 *
 * <p>usage 是 deterministic 估算，不代表真实 provider 用量或账单。
 *
 * @param inputCharacters          input + memory 字符数。
 * @param renderedPromptCharacters rendered prompt 字符数。
 * @param outputCharacters         structured output 字符数。
 * @param estimatedTokens          本地估算 token。
 * @param memoryEntries            memory entry 数量。
 */
public record ModelGatewayUsage(
        int inputCharacters,
        int renderedPromptCharacters,
        int outputCharacters,
        int estimatedTokens,
        int memoryEntries) {
}

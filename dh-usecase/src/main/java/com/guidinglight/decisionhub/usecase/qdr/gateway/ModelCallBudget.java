package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Model gateway 本地预算上限。
 *
 * <p>预算只用于 mock runtime 的 deterministic guard，不代表真实 provider 账单或真实用量。负数、
 * 缺失或超限由 {@link ModelGatewayService} 统一 fail-closed。
 *
 * @param maxInputCharacters          render input + memory 最大字符数。
 * @param maxRenderedPromptCharacters rendered prompt 最大字符数。
 * @param maxOutputCharacters         model structured summary 最大字符数。
 * @param maxEstimatedTokens          本地估算 token 上限。
 * @param maxMemoryEntries            context memory entry 最大数量。
 */
public record ModelCallBudget(
        int maxInputCharacters,
        int maxRenderedPromptCharacters,
        int maxOutputCharacters,
        int maxEstimatedTokens,
        int maxMemoryEntries) {
}

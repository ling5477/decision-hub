package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.Objects;

/**
 * Mock provider structured result。
 *
 * <p>该结果不含 raw provider response；只返回 safe ref、redacted summary 与 structured decision。
 *
 * @param mode                      provider mode/result class。
 * @param decision                  structured readonly decision。
 * @param redactedSummary           safe summary。
 * @param safeProviderRef           safe provider ref。
 * @param estimatedOutputCharacters output 字符估算。
 * @param estimatedTokens           token 估算。
 */
public record MockModelProviderResult(
        MockModelProviderMode mode,
        ModelGatewayDecision decision,
        String redactedSummary,
        String safeProviderRef,
        int estimatedOutputCharacters,
        int estimatedTokens) {

    /**
     * 校验 result mode。
     */
    public MockModelProviderResult {
        mode = Objects.requireNonNull(mode, "mode");
    }

    /**
     * 创建 success result。
     *
     * @param decision        structured decision。
     * @param redactedSummary safe summary。
     * @param safeProviderRef safe provider ref。
     * @return success result。
     */
    public static MockModelProviderResult success(
            final ModelGatewayDecision decision,
            final String redactedSummary,
            final String safeProviderRef) {
        final int outputCharacters = redactedSummary == null ? 0 : redactedSummary.length();
        final int estimatedTokens = Math.max(1, (outputCharacters + 3) / 4);
        return new MockModelProviderResult(
                MockModelProviderMode.NORMAL,
                decision,
                redactedSummary,
                safeProviderRef,
                outputCharacters,
                estimatedTokens);
    }

    /**
     * 创建非成功 result。
     *
     * @param mode provider mode。
     * @return result。
     */
    public static MockModelProviderResult failure(final MockModelProviderMode mode) {
        return new MockModelProviderResult(mode, null, null, "mock-provider-failure", 0, 0);
    }
}

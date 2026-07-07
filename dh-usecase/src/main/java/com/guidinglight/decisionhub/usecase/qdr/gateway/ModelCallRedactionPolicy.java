package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Model gateway redaction guard 配置。
 *
 * <p>该 policy 只控制本地 deterministic validation。命中 secret-like material 或可执行交易指令时，
 * gateway 必须 fail-closed，不保存 raw prompt，也不返回 raw provider response。
 *
 * @param policyRef                          redaction policy ref。
 * @param rejectSecretLikeMaterial           是否拒绝疑似密钥材料。
 * @param rejectExecutableTradingInstruction 是否拒绝可执行交易指令。
 */
public record ModelCallRedactionPolicy(
        String policyRef,
        boolean rejectSecretLikeMaterial,
        boolean rejectExecutableTradingInstruction) {

    /**
     * 创建 B2 默认 fail-closed redaction policy。
     *
     * @return 默认 redaction policy。
     */
    public static ModelCallRedactionPolicy strictDefault() {
        return new ModelCallRedactionPolicy("qdr-b2-strict-redaction", true, true);
    }
}

package com.guidinglight.decisionhub.domain.qdr.model;

/**
 * Provider profile 标识。
 *
 * @param value tenant 内唯一 provider profile id。
 */
public record ProviderProfileId(String value) {

    /**
     * 校验 provider profile id 非空。
     */
    public ProviderProfileId {
        value = PromptModelSafetyRules.requireText(value, "providerProfileId");
    }
}

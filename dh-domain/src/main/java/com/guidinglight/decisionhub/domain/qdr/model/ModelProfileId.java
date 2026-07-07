package com.guidinglight.decisionhub.domain.qdr.model;

/**
 * Model profile 标识。
 *
 * @param value tenant 内唯一 model profile id。
 */
public record ModelProfileId(String value) {

    /**
     * 校验 model profile id 非空。
     */
    public ModelProfileId {
        value = PromptModelSafetyRules.requireText(value, "modelProfileId");
    }
}

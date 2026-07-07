package com.guidinglight.decisionhub.domain.qdr.model;

/**
 * Prompt version 的不可变标识。
 *
 * @param value tenant 内唯一的 prompt version id。
 */
public record PromptVersionId(String value) {

    /**
     * 校验 prompt version id 非空。
     */
    public PromptVersionId {
        value = PromptModelSafetyRules.requireText(value, "promptVersionId");
    }
}

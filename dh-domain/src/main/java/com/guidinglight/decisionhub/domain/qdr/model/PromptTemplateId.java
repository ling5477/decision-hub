package com.guidinglight.decisionhub.domain.qdr.model;

/**
 * Prompt template 的稳定标识。
 *
 * @param value tenant 内唯一的 prompt template id。
 */
public record PromptTemplateId(String value) {

    /**
     * 校验 prompt template id 非空。
     */
    public PromptTemplateId {
        value = PromptModelSafetyRules.requireText(value, "promptTemplateId");
    }
}

package com.guidinglight.decisionhub.domain.qdr.model;

/**
 * Model version 标识。
 *
 * @param value tenant 内唯一 model version id。
 */
public record ModelVersionId(String value) {

    /**
     * 校验 model version id 非空。
     */
    public ModelVersionId {
        value = PromptModelSafetyRules.requireText(value, "modelVersionId");
    }
}

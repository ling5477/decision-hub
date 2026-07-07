package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.ModelVersion;
import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;

import java.util.Objects;

/**
 * ModelVersion registry registration command。
 *
 * <p>注册命令显式携带 tenantId，用于防止跨 tenant 写入或重复 key 覆盖。
 *
 * @param tenantId     tenant 边界。
 * @param modelVersion immutable model version。
 */
public record ModelVersionRegistrationCommand(String tenantId, ModelVersion modelVersion) {

    /**
     * 校验注册命令必填字段，并要求 command tenant 与 model version tenant 一致。
     */
    public ModelVersionRegistrationCommand {
        tenantId = PromptModelSafetyRules.requireText(tenantId, "tenantId");
        modelVersion = Objects.requireNonNull(modelVersion, "modelVersion");
        if (!tenantId.equals(modelVersion.tenantId())) {
            throw new ModelVersionRegistryException("model version tenant mismatch");
        }
    }
}

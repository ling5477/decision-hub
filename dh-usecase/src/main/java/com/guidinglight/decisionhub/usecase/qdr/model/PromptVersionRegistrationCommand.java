package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;
import com.guidinglight.decisionhub.domain.qdr.model.PromptVersion;

import java.util.Objects;

/**
 * PromptVersion registry registration command。
 *
 * <p>command tenantId 必须与 PromptVersion tenantId 一致；不一致时 registry 必须 fail-closed。
 *
 * @param tenantId      tenant 边界。
 * @param promptVersion 待注册 immutable prompt version。
 */
public record PromptVersionRegistrationCommand(String tenantId, PromptVersion promptVersion) {

    /**
     * 校验 registration command。
     */
    public PromptVersionRegistrationCommand {
        tenantId = PromptModelSafetyRules.requireText(tenantId, "tenantId");
        promptVersion = Objects.requireNonNull(promptVersion, "promptVersion");
        if (!tenantId.equals(promptVersion.tenantId())) {
            throw new PromptVersionRegistryException("prompt version tenant mismatch");
        }
    }
}

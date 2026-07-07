package com.guidinglight.decisionhub.domain.qdr.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Model profile baseline。
 *
 * <p>ModelProfile 只保存 model capability 与 provider 关联，不保存 credential、apiKey、apiSecret、
 * token、cookie 或 passphrase。上下文窗口与输出上限必须为正数，供后续 B2/B3 policy 使用。
 *
 * @param id                  model profile id。
 * @param tenantId            tenant 边界。
 * @param providerProfileId   关联 provider profile。
 * @param modelKey            model identity key。
 * @param displayName         展示名。
 * @param capability          capability 摘要。
 * @param contextWindowTokens context window 上限。
 * @param maxOutputTokens     输出 token 上限。
 * @param createdAt           创建时间。
 */
public record ModelProfile(
        ModelProfileId id,
        String tenantId,
        ProviderProfileId providerProfileId,
        String modelKey,
        String displayName,
        String capability,
        int contextWindowTokens,
        int maxOutputTokens,
        Instant createdAt) {

    /**
     * 校验 model profile 不含凭证并具备保守资源边界。
     */
    public ModelProfile {
        id = Objects.requireNonNull(id, "id");
        tenantId = PromptModelSafetyRules.requireText(tenantId, "tenantId");
        providerProfileId = Objects.requireNonNull(providerProfileId, "providerProfileId");
        modelKey = PromptModelSafetyRules.requireSafeProfileText("modelKey", modelKey);
        displayName = PromptModelSafetyRules.requireSafeProfileText("displayName", displayName);
        capability = PromptModelSafetyRules.requireSafeProfileText("capability", capability);
        if (contextWindowTokens <= 0) {
            throw ModelVersionValidationException.invalidBounds("contextWindowTokens must be positive");
        }
        if (maxOutputTokens <= 0 || maxOutputTokens > contextWindowTokens) {
            throw ModelVersionValidationException.invalidBounds(
                    "maxOutputTokens must be positive and bounded");
        }
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }
}

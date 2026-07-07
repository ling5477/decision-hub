package com.guidinglight.decisionhub.domain.qdr.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Provider identity / capability profile。
 *
 * <p>该对象只允许保存 provider identity、capability 和 status，不保存 apiKey、apiSecret、token、
 * passphrase、cookie 或任何 credential material。B1 中所有 profile 仍为 mock/local-planned 语义。
 *
 * @param id                provider profile id。
 * @param tenantId          tenant 边界。
 * @param providerKind      provider 类型。
 * @param providerKey       provider identity key。
 * @param displayName       展示名。
 * @param capabilitySummary capability 摘要。
 * @param status            profile 状态。
 * @param trustPolicyRef    trust policy ref；B1 仅保存引用。
 * @param createdAt         创建时间。
 */
public record ProviderProfile(
        ProviderProfileId id,
        String tenantId,
        ProviderKind providerKind,
        String providerKey,
        String displayName,
        String capabilitySummary,
        ProviderProfileStatus status,
        String trustPolicyRef,
        Instant createdAt) {

    /**
     * 校验 provider profile 不包含凭证字段或疑似密钥材料。
     */
    public ProviderProfile {
        id = Objects.requireNonNull(id, "id");
        tenantId = PromptModelSafetyRules.requireText(tenantId, "tenantId");
        providerKind = Objects.requireNonNull(providerKind, "providerKind");
        providerKey = PromptModelSafetyRules.requireSafeProfileText("providerKey", providerKey);
        displayName = PromptModelSafetyRules.requireSafeProfileText("displayName", displayName);
        capabilitySummary =
                PromptModelSafetyRules.requireSafeProfileText("capabilitySummary", capabilitySummary);
        status = Objects.requireNonNull(status, "status");
        trustPolicyRef = PromptModelSafetyRules.optionalSafeProfileText("trustPolicyRef", trustPolicyRef);
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }
}

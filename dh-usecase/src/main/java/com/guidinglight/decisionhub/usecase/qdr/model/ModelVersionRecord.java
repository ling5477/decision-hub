package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.ProviderKind;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderProfileStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * ModelVersion persistence read model。
 *
 * <p>该 record 不暴露 provider credential、endpoint、SDK 配置或 provider 原始响应。
 *
 * @param modelProfileId      model profile id。
 * @param modelVersionId      model version id。
 * @param providerProfileId   provider profile identity ref。
 * @param tenantId            tenant 边界。
 * @param providerKind        provider 类型。
 * @param providerKey         provider identity key。
 * @param modelKey            model key。
 * @param displayName         脱敏展示名。
 * @param capabilitySummary   脱敏 capability 摘要。
 * @param contextWindowTokens context window 上限。
 * @param maxOutputTokens     输出 token 上限。
 * @param profileStatus       profile 状态。
 * @param trustPolicyRef      trust policy 安全引用。
 * @param modelName           model name。
 * @param modelVersion        model version 字符串。
 * @param checksum            immutable checksum。
 * @param createdAt           创建时间。
 */
public record ModelVersionRecord(
        UUID modelProfileId,
        UUID modelVersionId,
        UUID providerProfileId,
        String tenantId,
        ProviderKind providerKind,
        String providerKey,
        String modelKey,
        String displayName,
        String capabilitySummary,
        int contextWindowTokens,
        int maxOutputTokens,
        ProviderProfileStatus profileStatus,
        String trustPolicyRef,
        String modelName,
        String modelVersion,
        String checksum,
        Instant createdAt) {

    /**
     * 校验 read model 字段仍满足 redaction storage boundary。
     */
    public ModelVersionRecord {
        modelProfileId = QdrPersistenceSafety.requireUuid(modelProfileId, "modelProfileId");
        modelVersionId = QdrPersistenceSafety.requireUuid(modelVersionId, "modelVersionId");
        providerProfileId = QdrPersistenceSafety.requireUuid(providerProfileId, "providerProfileId");
        tenantId = QdrPersistenceSafety.requireText(tenantId, "tenantId");
        providerKind = Objects.requireNonNull(providerKind, "providerKind");
        providerKey = QdrPersistenceSafety.requireSafeText(providerKey, "providerKey");
        modelKey = QdrPersistenceSafety.requireSafeText(modelKey, "modelKey");
        displayName = QdrPersistenceSafety.requireSafeText(displayName, "displayName");
        capabilitySummary = QdrPersistenceSafety.requireSafeText(capabilitySummary, "capabilitySummary");
        contextWindowTokens = QdrPersistenceSafety.requirePositive(contextWindowTokens, "contextWindowTokens");
        maxOutputTokens = QdrPersistenceSafety.requirePositive(maxOutputTokens, "maxOutputTokens");
        if (maxOutputTokens > contextWindowTokens) {
            throw new IllegalArgumentException("maxOutputTokens must be bounded by contextWindowTokens");
        }
        profileStatus = Objects.requireNonNull(profileStatus, "profileStatus");
        trustPolicyRef = QdrPersistenceSafety.optionalSafeText(trustPolicyRef, "trustPolicyRef");
        modelName = QdrPersistenceSafety.requireSafeText(modelName, "modelName");
        modelVersion = QdrPersistenceSafety.requireSafeText(modelVersion, "modelVersion");
        checksum = QdrPersistenceSafety.requireSha256Hex(checksum, "checksum");
        createdAt = QdrPersistenceSafety.requireInstant(createdAt, "createdAt");
    }
}

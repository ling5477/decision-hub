package com.guidinglight.decisionhub.domain.qdr.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable model version baseline。
 *
 * <p>ModelVersion 必须关联 ModelProfile，并以 deterministic checksum 固定 modelName、version 与
 * capability。该对象不包含 provider credential、HTTP endpoint、SDK client 或 runtime invocation。
 *
 * @param id             model version id。
 * @param tenantId       tenant 边界。
 * @param modelProfileId 关联 model profile。
 * @param modelName      model name。
 * @param modelVersion   provider/model version 字符串。
 * @param capability     capability 摘要。
 * @param checksum       deterministic checksum。
 * @param createdAt      创建时间。
 */
public record ModelVersion(
        ModelVersionId id,
        String tenantId,
        ModelProfileId modelProfileId,
        String modelName,
        String modelVersion,
        String capability,
        ModelVersionChecksum checksum,
        Instant createdAt) {

    /**
     * 校验 immutable model version，并验证 checksum 与安全字段一致。
     */
    public ModelVersion {
        id = Objects.requireNonNull(id, "id");
        tenantId = PromptModelSafetyRules.requireText(tenantId, "tenantId");
        modelProfileId = Objects.requireNonNull(modelProfileId, "modelProfileId");
        modelName = PromptModelSafetyRules.requireSafeProfileText("modelName", modelName);
        modelVersion = PromptModelSafetyRules.requireSafeProfileText("modelVersion", modelVersion);
        capability = PromptModelSafetyRules.requireSafeProfileText("capability", capability);
        checksum = Objects.requireNonNull(checksum, "checksum");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");

        final ModelVersionChecksum expected =
                ModelVersionChecksum.compute(tenantId, modelProfileId, modelName, modelVersion, capability);
        if (!expected.equals(checksum)) {
            throw ModelVersionValidationException.checksumMismatch();
        }
    }

    /**
     * 创建 immutable model version 并自动计算 checksum。
     *
     * @param id             model version id。
     * @param tenantId       tenant 边界。
     * @param modelProfileId 关联 model profile。
     * @param modelName      model name。
     * @param modelVersion   provider/model version 字符串。
     * @param capability     capability 摘要。
     * @param createdAt      创建时间。
     * @return immutable ModelVersion。
     */
    public static ModelVersion create(
            final ModelVersionId id,
            final String tenantId,
            final ModelProfileId modelProfileId,
            final String modelName,
            final String modelVersion,
            final String capability,
            final Instant createdAt) {
        final String checkedTenant = PromptModelSafetyRules.requireText(tenantId, "tenantId");
        final String checkedName = PromptModelSafetyRules.requireSafeProfileText("modelName", modelName);
        final String checkedVersion =
                PromptModelSafetyRules.requireSafeProfileText("modelVersion", modelVersion);
        final String checkedCapability =
                PromptModelSafetyRules.requireSafeProfileText("capability", capability);
        final ModelVersionChecksum checksum =
                ModelVersionChecksum.compute(
                        checkedTenant, modelProfileId, checkedName, checkedVersion, checkedCapability);
        return new ModelVersion(
                id,
                checkedTenant,
                modelProfileId,
                checkedName,
                checkedVersion,
                checkedCapability,
                checksum,
                createdAt);
    }
}

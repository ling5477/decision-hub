package com.guidinglight.decisionhub.domain.qdr.model;

import java.util.List;
import java.util.regex.Pattern;

/**
 * ModelVersion baseline checksum。
 *
 * @param value 64 位 hex SHA-256。
 */
public record ModelVersionChecksum(String value) {

    private static final Pattern SHA_256_HEX = Pattern.compile("[0-9a-f]{64}");

    /**
     * 校验 checksum 格式。
     */
    public ModelVersionChecksum {
        value = PromptModelSafetyRules.requireText(value, "modelVersionChecksum").toLowerCase();
        if (!SHA_256_HEX.matcher(value).matches()) {
            throw new IllegalArgumentException("modelVersionChecksum must be SHA-256 hex");
        }
    }

    /**
     * 根据 model version 安全元数据计算 checksum。
     *
     * @param tenantId       tenant 边界。
     * @param modelProfileId model profile id。
     * @param modelName      model name。
     * @param modelVersion   model version。
     * @param capability     capability 摘要。
     * @return deterministic checksum。
     */
    public static ModelVersionChecksum compute(
            final String tenantId,
            final ModelProfileId modelProfileId,
            final String modelName,
            final String modelVersion,
            final String capability) {
        return new ModelVersionChecksum(
                PromptModelSafetyRules.sha256Hex(
                        List.of(tenantId, modelProfileId.value(), modelName, modelVersion, capability)));
    }
}

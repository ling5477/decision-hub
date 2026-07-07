package com.guidinglight.decisionhub.domain.qdr.model;

import java.util.List;
import java.util.regex.Pattern;

/**
 * PromptVersion 的 deterministic SHA-256 checksum。
 *
 * @param value 64 位 hex SHA-256。
 */
public record PromptVersionChecksum(String value) {

    private static final Pattern SHA_256_HEX = Pattern.compile("[0-9a-f]{64}");

    /**
     * 校验 checksum 格式。
     */
    public PromptVersionChecksum {
        value = PromptModelSafetyRules.requireText(value, "promptVersionChecksum").toLowerCase();
        if (!SHA_256_HEX.matcher(value).matches()) {
            throw new IllegalArgumentException("promptVersionChecksum must be SHA-256 hex");
        }
    }

    /**
     * 根据 prompt version 的安全字段计算 checksum。
     *
     * @param tenantId        tenant 边界。
     * @param templateId      prompt template id。
     * @param version         version 字符串。
     * @param renderPolicyKey render policy 标识。
     * @param templateBody    template body；调用前必须已通过安全校验。
     * @return deterministic checksum。
     */
    public static PromptVersionChecksum compute(
            final String tenantId,
            final PromptTemplateId templateId,
            final String version,
            final String renderPolicyKey,
            final String templateBody) {
        return new PromptVersionChecksum(
                PromptModelSafetyRules.sha256Hex(
                        List.of(tenantId, templateId.value(), version, renderPolicyKey, templateBody)));
    }
}

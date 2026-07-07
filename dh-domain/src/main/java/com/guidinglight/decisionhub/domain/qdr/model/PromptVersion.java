package com.guidinglight.decisionhub.domain.qdr.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable prompt version。
 *
 * <p>任何 prompt body 或 metadata 变更都必须创建新的 PromptVersion；构造时会用 tenant、template id、
 * version、render policy 与 template body 重新计算 checksum，mismatch 直接 fail-closed。B1 不做持久化，
 * 不保存 raw provider response，也不允许疑似密钥或可执行交易指令进入 prompt body。
 *
 * @param id              prompt version id。
 * @param templateId      关联 template id。
 * @param tenantId        tenant 边界。
 * @param version         业务版本号。
 * @param templateBody    in-memory template body。
 * @param renderPolicyKey render policy 标识。
 * @param status          version 状态。
 * @param checksum        deterministic checksum。
 * @param createdAt       创建时间。
 * @param createdBy       创建人或系统标识。
 */
public record PromptVersion(
        PromptVersionId id,
        PromptTemplateId templateId,
        String tenantId,
        String version,
        String templateBody,
        String renderPolicyKey,
        PromptVersionStatus status,
        PromptVersionChecksum checksum,
        Instant createdAt,
        String createdBy) {

    /**
     * 校验 immutable prompt version，并验证 checksum 与当前内容完全一致。
     */
    public PromptVersion {
        id = Objects.requireNonNull(id, "id");
        templateId = Objects.requireNonNull(templateId, "templateId");
        tenantId = PromptModelSafetyRules.requireText(tenantId, "tenantId");
        version = PromptModelSafetyRules.requireText(version, "version");
        templateBody = PromptModelSafetyRules.requireSafePromptBody(templateBody);
        renderPolicyKey = PromptModelSafetyRules.requireText(renderPolicyKey, "renderPolicyKey");
        status = Objects.requireNonNull(status, "status");
        checksum = Objects.requireNonNull(checksum, "checksum");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        createdBy = PromptModelSafetyRules.requireText(createdBy, "createdBy");

        final PromptVersionChecksum expected =
                PromptVersionChecksum.compute(tenantId, templateId, version, renderPolicyKey, templateBody);
        if (!expected.equals(checksum)) {
            throw PromptValidationException.checksumMismatch();
        }
    }

    /**
     * 使用安全字段创建 PromptVersion，并自动计算 checksum。
     *
     * @param id              prompt version id。
     * @param templateId      template id。
     * @param tenantId        tenant 边界。
     * @param version         业务版本号。
     * @param templateBody    template body。
     * @param renderPolicyKey render policy key。
     * @param status          version 状态。
     * @param createdAt       创建时间。
     * @param createdBy       创建人或系统标识。
     * @return immutable PromptVersion。
     */
    public static PromptVersion create(
            final PromptVersionId id,
            final PromptTemplateId templateId,
            final String tenantId,
            final String version,
            final String templateBody,
            final String renderPolicyKey,
            final PromptVersionStatus status,
            final Instant createdAt,
            final String createdBy) {
        final String checkedTenant = PromptModelSafetyRules.requireText(tenantId, "tenantId");
        final String checkedVersion = PromptModelSafetyRules.requireText(version, "version");
        final String checkedBody = PromptModelSafetyRules.requireSafePromptBody(templateBody);
        final String checkedPolicy = PromptModelSafetyRules.requireText(renderPolicyKey, "renderPolicyKey");
        final PromptVersionChecksum checksum =
                PromptVersionChecksum.compute(
                        checkedTenant, templateId, checkedVersion, checkedPolicy, checkedBody);
        return new PromptVersion(
                id,
                templateId,
                checkedTenant,
                checkedVersion,
                checkedBody,
                checkedPolicy,
                status,
                checksum,
                createdAt,
                createdBy);
    }
}

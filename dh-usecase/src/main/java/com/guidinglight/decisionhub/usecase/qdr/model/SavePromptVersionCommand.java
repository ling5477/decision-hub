package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.PromptVersionStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 保存 PromptVersion 的持久化命令。
 *
 * <p>命令只携带 template ref、hash、checksum 与脱敏摘要；不携带 prompt 正文或 provider 原始响应。
 *
 * @param promptTemplateId   prompt template id。
 * @param promptVersionId    prompt version id。
 * @param tenantId           tenant 边界。
 * @param templateKey        租户内 template key。
 * @param templateDisplayName 脱敏展示名。
 * @param version            version 字符串。
 * @param renderPolicyKey    render policy key。
 * @param templateRef        template 安全引用。
 * @param templateHash       template SHA-256 hash。
 * @param redactedSummary    脱敏摘要。
 * @param status             prompt version 状态。
 * @param checksum           immutable checksum。
 * @param createdAt          创建时间。
 * @param createdBy          创建人或系统标识。
 */
public record SavePromptVersionCommand(
        UUID promptTemplateId,
        UUID promptVersionId,
        String tenantId,
        String templateKey,
        String templateDisplayName,
        String version,
        String renderPolicyKey,
        String templateRef,
        String templateHash,
        String redactedSummary,
        PromptVersionStatus status,
        String checksum,
        Instant createdAt,
        String createdBy) {

    /**
     * 校验保存命令，不允许 raw material 或疑似凭证材料进入持久化 contract。
     */
    public SavePromptVersionCommand {
        promptTemplateId = QdrPersistenceSafety.requireUuid(promptTemplateId, "promptTemplateId");
        promptVersionId = QdrPersistenceSafety.requireUuid(promptVersionId, "promptVersionId");
        tenantId = QdrPersistenceSafety.requireText(tenantId, "tenantId");
        templateKey = QdrPersistenceSafety.requireSafeText(templateKey, "templateKey");
        templateDisplayName = QdrPersistenceSafety.optionalSafeText(templateDisplayName, "templateDisplayName");
        if (templateDisplayName == null) {
            templateDisplayName = templateKey;
        }
        version = QdrPersistenceSafety.requireSafeText(version, "version");
        renderPolicyKey = QdrPersistenceSafety.requireSafeText(renderPolicyKey, "renderPolicyKey");
        templateRef = QdrPersistenceSafety.requireSafeText(templateRef, "templateRef");
        templateHash = QdrPersistenceSafety.requireSha256Hex(templateHash, "templateHash");
        redactedSummary = QdrPersistenceSafety.requireSafeText(redactedSummary, "redactedSummary");
        status = Objects.requireNonNull(status, "status");
        checksum = QdrPersistenceSafety.requireSha256Hex(checksum, "checksum");
        createdAt = QdrPersistenceSafety.requireInstant(createdAt, "createdAt");
        createdBy = QdrPersistenceSafety.requireSafeText(createdBy, "createdBy");
    }
}

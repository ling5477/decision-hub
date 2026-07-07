package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.PromptVersionStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * PromptVersion persistence read model。
 *
 * <p>该 record 不暴露 prompt 正文或 provider 原始响应，只返回 tenant-bound ref、hash、checksum 与脱敏摘要。
 *
 * @param promptTemplateId prompt template id。
 * @param promptVersionId  prompt version id。
 * @param tenantId         tenant 边界。
 * @param templateKey      template key。
 * @param version          version 字符串。
 * @param renderPolicyKey  render policy key。
 * @param templateRef      template 安全引用。
 * @param templateHash     template SHA-256 hash。
 * @param redactedSummary  脱敏摘要。
 * @param status           prompt version 状态。
 * @param checksum         immutable checksum。
 * @param createdAt        创建时间。
 * @param createdBy        创建人或系统标识。
 */
public record PromptVersionRecord(
        UUID promptTemplateId,
        UUID promptVersionId,
        String tenantId,
        String templateKey,
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
     * 校验 read model 字段仍满足 redaction storage boundary。
     */
    public PromptVersionRecord {
        promptTemplateId = QdrPersistenceSafety.requireUuid(promptTemplateId, "promptTemplateId");
        promptVersionId = QdrPersistenceSafety.requireUuid(promptVersionId, "promptVersionId");
        tenantId = QdrPersistenceSafety.requireText(tenantId, "tenantId");
        templateKey = QdrPersistenceSafety.requireSafeText(templateKey, "templateKey");
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

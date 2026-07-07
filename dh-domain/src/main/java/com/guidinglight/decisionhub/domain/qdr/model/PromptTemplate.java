package com.guidinglight.decisionhub.domain.qdr.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Prompt template 元数据。
 *
 * <p>该对象只保存 tenant、templateKey 与当前 version 指针，不保存 raw prompt 文本；具体文本只存在于
 * immutable {@link PromptVersion} 中，并在 B1 mock/in-memory 范围内使用。
 *
 * @param id               template id。
 * @param tenantId         tenant 边界。
 * @param templateKey      可搜索的业务 key。
 * @param currentVersionId 当前 version 指针。
 * @param status           当前指针状态。
 * @param createdAt        创建时间。
 */
public record PromptTemplate(
        PromptTemplateId id,
        String tenantId,
        String templateKey,
        PromptVersionId currentVersionId,
        PromptVersionStatus status,
        Instant createdAt) {

    /**
     * 校验 template 元数据，不允许空白 tenant/templateKey。
     */
    public PromptTemplate {
        id = Objects.requireNonNull(id, "id");
        tenantId = PromptModelSafetyRules.requireText(tenantId, "tenantId");
        templateKey = PromptModelSafetyRules.requireText(templateKey, "templateKey");
        currentVersionId = Objects.requireNonNull(currentVersionId, "currentVersionId");
        status = Objects.requireNonNull(status, "status");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }
}

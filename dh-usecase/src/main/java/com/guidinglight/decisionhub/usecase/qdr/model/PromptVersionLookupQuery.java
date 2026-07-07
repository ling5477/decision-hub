package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;
import com.guidinglight.decisionhub.domain.qdr.model.PromptTemplateId;

import java.util.Objects;

/**
 * PromptVersion registry lookup query。
 *
 * <p>查询必须 tenant-bound；registry 不允许用 template/version 跨 tenant 查询。
 *
 * @param tenantId   tenant 边界。
 * @param templateId prompt template id。
 * @param version    version 字符串。
 */
public record PromptVersionLookupQuery(String tenantId, PromptTemplateId templateId, String version) {

    /**
     * 校验 lookup query 必填字段。
     */
    public PromptVersionLookupQuery {
        tenantId = PromptModelSafetyRules.requireText(tenantId, "tenantId");
        templateId = Objects.requireNonNull(templateId, "templateId");
        version = PromptModelSafetyRules.requireText(version, "version");
    }
}

package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.ModelVersionId;
import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;

import java.util.Objects;

/**
 * ModelVersion registry lookup query。
 *
 * <p>查询必须 tenant-bound；B2 gateway 只能通过 registry 命中 model version，不允许直接信任
 * request 中的 model metadata。
 *
 * @param tenantId       tenant 边界。
 * @param modelVersionId model version id。
 */
public record ModelVersionLookupQuery(String tenantId, ModelVersionId modelVersionId) {

    /**
     * 校验 lookup query 必填字段。
     */
    public ModelVersionLookupQuery {
        tenantId = PromptModelSafetyRules.requireText(tenantId, "tenantId");
        modelVersionId = Objects.requireNonNull(modelVersionId, "modelVersionId");
    }
}

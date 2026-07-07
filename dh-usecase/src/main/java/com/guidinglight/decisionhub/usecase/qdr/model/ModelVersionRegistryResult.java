package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.ModelVersion;

import java.util.Objects;

/**
 * ModelVersion registry register result。
 *
 * @param modelVersion 注册后可读取的 model version。
 * @param created      true 表示本次新建。
 * @param idempotent   true 表示相同 checksum 的重复注册被幂等接受。
 */
public record ModelVersionRegistryResult(ModelVersion modelVersion, boolean created, boolean idempotent) {

    /**
     * 校验 result 必填字段。
     */
    public ModelVersionRegistryResult {
        modelVersion = Objects.requireNonNull(modelVersion, "modelVersion");
    }
}

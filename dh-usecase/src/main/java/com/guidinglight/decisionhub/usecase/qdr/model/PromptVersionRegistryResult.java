package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.PromptVersion;

import java.util.Objects;

/**
 * PromptVersion registry register result。
 *
 * @param promptVersion 注册后可读取的 prompt version。
 * @param created       true 表示本次新建。
 * @param idempotent    true 表示相同 checksum 的重复注册被幂等接受。
 */
public record PromptVersionRegistryResult(PromptVersion promptVersion, boolean created, boolean idempotent) {

    /**
     * 校验 result 必填字段。
     */
    public PromptVersionRegistryResult {
        promptVersion = Objects.requireNonNull(promptVersion, "promptVersion");
    }
}

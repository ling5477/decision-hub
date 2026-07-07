package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.PromptVersion;

/**
 * Prompt render policy contract。
 *
 * <p>B1 只允许 contract 或 deterministic local validation/render，不调用 provider、不调用 HTTP、不写 raw prompt。
 */
public interface PromptRenderPolicy {

    /**
     * 对 PromptVersion 做本地 render。
     *
     * @param promptVersion immutable prompt version。
     * @param context       tenant-bound render context。
     * @return render 结果。
     */
    PromptRenderResult render(PromptVersion promptVersion, PromptRenderContext context);
}

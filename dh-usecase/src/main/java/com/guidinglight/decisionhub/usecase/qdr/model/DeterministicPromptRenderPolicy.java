package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.PromptVersion;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * B1 deterministic local PromptRenderPolicy。
 *
 * <p>实现只做本地 guard + 简单占位符替换；guard deny 或异常时 fail-closed。它不调用 provider、
 * 不访问 HTTP，也不持久化 rendered prompt。
 */
public final class DeterministicPromptRenderPolicy implements PromptRenderPolicy {

    private final PromptInjectionGuard injectionGuard;

    /**
     * 创建 render policy。
     *
     * @param injectionGuard deterministic prompt injection guard。
     */
    public DeterministicPromptRenderPolicy(final PromptInjectionGuard injectionGuard) {
        this.injectionGuard = Objects.requireNonNull(injectionGuard, "injectionGuard");
    }

    @Override
    public PromptRenderResult render(final PromptVersion promptVersion, final PromptRenderContext context) {
        final PromptVersion checkedVersion = Objects.requireNonNull(promptVersion, "promptVersion");
        final PromptRenderContext checkedContext = Objects.requireNonNull(context, "context");
        if (!checkedVersion.tenantId().equals(checkedContext.tenantId())) {
            return PromptRenderResult.denied(
                    "PROMPT_TENANT_MISMATCH",
                    List.of(PromptInjectionViolation.of(PromptInjectionViolationCode.GUARD_FAILURE)));
        }
        try {
            final PromptInjectionDecision bodyDecision =
                    injectionGuard.evaluate(checkedContext.tenantId(), checkedVersion.templateBody());
            if (!bodyDecision.allowed()) {
                return PromptRenderResult.denied("PROMPT_DENIED", bodyDecision.violations());
            }
            for (Map.Entry<String, String> entry : checkedContext.inputs().entrySet()) {
                final PromptInjectionDecision inputDecision =
                        injectionGuard.evaluate(checkedContext.tenantId(), entry.getValue());
                if (!inputDecision.allowed()) {
                    return PromptRenderResult.denied("PROMPT_INPUT_DENIED", inputDecision.violations());
                }
            }
            return PromptRenderResult.allowed(renderTemplate(checkedVersion.templateBody(), checkedContext.inputs()));
        } catch (final RuntimeException error) {
            return PromptRenderResult.denied(
                    "PROMPT_GUARD_FAILURE",
                    List.of(PromptInjectionViolation.of(PromptInjectionViolationCode.GUARD_FAILURE)));
        }
    }

    private static String renderTemplate(final String templateBody, final Map<String, String> inputs) {
        String rendered = templateBody;
        for (Map.Entry<String, String> entry : inputs.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }
}

package com.guidinglight.decisionhub.usecase.qdr.gateway;

import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;

import java.util.List;
import java.util.Objects;

/**
 * Deterministic mock model provider。
 *
 * <p>实现不调用 HTTP、不调用 SDK、不读取环境变量、不读取外部文件、不访问 NQ、不触发交易。相同
 * request + renderedPrompt + mode 返回相同 structured result。
 */
public final class MockModelProvider implements ModelProviderPort {

    private final MockModelProviderMode mode;

    /**
     * 创建正常 mock provider。
     */
    public MockModelProvider() {
        this(MockModelProviderMode.NORMAL);
    }

    /**
     * 创建指定 mode 的 mock provider。
     *
     * @param mode deterministic mode。
     */
    public MockModelProvider(final MockModelProviderMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    @Override
    public MockModelProviderResult invoke(
            final ModelGatewayRequest request, final String renderedPrompt) {
        return switch (mode) {
            case NORMAL -> normalResult(request, renderedPrompt);
            case UNAVAILABLE -> throwUnavailable();
            case TIMEOUT -> MockModelProviderResult.failure(MockModelProviderMode.TIMEOUT);
            case BUDGET_EXCEEDED -> MockModelProviderResult.failure(MockModelProviderMode.BUDGET_EXCEEDED);
            case POLICY_DENIED -> MockModelProviderResult.failure(MockModelProviderMode.POLICY_DENIED);
            case MALFORMED -> MockModelProviderResult.failure(MockModelProviderMode.MALFORMED);
            case REDACTION_FAILURE -> new MockModelProviderResult(
                    MockModelProviderMode.REDACTION_FAILURE,
                    ModelGatewayDecision.observe("mock provider redaction rejected"),
                    "credential marker rejected",
                    "mock-provider-redaction",
                    "credential marker rejected".length(),
                    6);
        };
    }

    private static MockModelProviderResult normalResult(
            final ModelGatewayRequest request, final String renderedPrompt) {
        final ModelCallContext context = request.context();
        final String hash =
                PromptModelSafetyRules.sha256Hex(
                                List.of(
                                        context.tenantId(),
                                        context.traceId(),
                                        context.requestId(),
                                        context.decisionRunId(),
                                        context.promptVersionId(),
                                        context.modelVersionId(),
                                        renderedPrompt))
                        .substring(0, 16);
        final String summary = "mock-qdr-review:" + hash;
        return MockModelProviderResult.success(
                ModelGatewayDecision.observe(summary), summary, "mock-provider:" + hash);
    }

    private static MockModelProviderResult throwUnavailable() {
        throw new ModelProviderUnavailableException();
    }
}

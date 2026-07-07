package com.guidinglight.decisionhub.usecase.qdr.gateway;

import com.guidinglight.decisionhub.domain.qdr.model.ModelVersion;
import com.guidinglight.decisionhub.domain.qdr.model.ModelVersionChecksum;
import com.guidinglight.decisionhub.domain.qdr.model.ModelVersionId;
import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;
import com.guidinglight.decisionhub.domain.qdr.model.PromptTemplateId;
import com.guidinglight.decisionhub.domain.qdr.model.PromptVersion;
import com.guidinglight.decisionhub.domain.qdr.model.PromptVersionChecksum;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionLookupQuery;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionRegistryPort;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptInjectionDecision;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptInjectionGuard;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptRenderContext;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptRenderPolicy;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptRenderResult;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionLookupQuery;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionRegistryPort;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * B2 ModelGatewayService。
 *
 * <p>该 service 是 mock model provider 的唯一 usecase 入口：先做 identity / registry /
 * prompt-injection / redaction / budget / ProviderTrustPolicy，再调用 ModelProviderPort。任何拒绝、
 * provider 异常或未知异常都转换为结构化 fail-closed result，不保存 raw prompt，不返回 raw provider
 * response。
 */
public final class ModelGatewayService implements ModelGatewayPort {

    private final PromptVersionRegistryPort promptVersionRegistry;
    private final ModelVersionRegistryPort modelVersionRegistry;
    private final PromptRenderPolicy promptRenderPolicy;
    private final PromptInjectionGuard promptInjectionGuard;
    private final ProviderTrustPolicy providerTrustPolicy;
    private final ModelProviderPort modelProvider;

    /**
     * 创建 gateway service。
     *
     * @param promptVersionRegistry prompt registry。
     * @param modelVersionRegistry  model registry。
     * @param promptRenderPolicy    prompt render policy。
     * @param promptInjectionGuard  prompt injection guard。
     * @param providerTrustPolicy   provider trust policy。
     * @param modelProvider         mock model provider。
     */
    public ModelGatewayService(
            final PromptVersionRegistryPort promptVersionRegistry,
            final ModelVersionRegistryPort modelVersionRegistry,
            final PromptRenderPolicy promptRenderPolicy,
            final PromptInjectionGuard promptInjectionGuard,
            final ProviderTrustPolicy providerTrustPolicy,
            final ModelProviderPort modelProvider) {
        this.promptVersionRegistry =
                Objects.requireNonNull(promptVersionRegistry, "promptVersionRegistry");
        this.modelVersionRegistry =
                Objects.requireNonNull(modelVersionRegistry, "modelVersionRegistry");
        this.promptRenderPolicy = Objects.requireNonNull(promptRenderPolicy, "promptRenderPolicy");
        this.promptInjectionGuard =
                Objects.requireNonNull(promptInjectionGuard, "promptInjectionGuard");
        this.providerTrustPolicy =
                Objects.requireNonNull(providerTrustPolicy, "providerTrustPolicy");
        this.modelProvider = Objects.requireNonNull(modelProvider, "modelProvider");
    }

    @Override
    public ModelGatewayResult call(final ModelGatewayRequest request) {
        try {
            final Optional<ModelGatewayFailureCode> requiredFailure = validateRequired(request);
            if (requiredFailure.isPresent()) {
                return ModelGatewayResult.failure(requiredFailure.orElseThrow(), request);
            }
            final Optional<ModelGatewayFailureCode> inputBudgetFailure = validateInputBudget(request);
            if (inputBudgetFailure.isPresent()) {
                return ModelGatewayResult.failure(inputBudgetFailure.orElseThrow(), request);
            }
            final Optional<ModelGatewayFailureCode> inputRedactionFailure = validateInputRedaction(request);
            if (inputRedactionFailure.isPresent()) {
                return ModelGatewayResult.failure(inputRedactionFailure.orElseThrow(), request);
            }
            final Optional<ModelGatewayFailureCode> injectionFailure = validatePromptInputs(request);
            if (injectionFailure.isPresent()) {
                return ModelGatewayResult.failure(injectionFailure.orElseThrow(), request);
            }
            final Optional<PromptVersion> promptVersion = lookupPromptVersion(request);
            if (promptVersion.isEmpty()) {
                return ModelGatewayResult.failure(ModelGatewayFailureCode.PROMPT_VERSION_NOT_FOUND, request);
            }
            if (!verifyPromptVersion(request, promptVersion.orElseThrow())) {
                return ModelGatewayResult.failure(ModelGatewayFailureCode.REGISTRY_MISMATCH, request);
            }
            final Optional<ModelVersion> modelVersion = lookupModelVersion(request);
            if (modelVersion.isEmpty()) {
                return ModelGatewayResult.failure(ModelGatewayFailureCode.MODEL_VERSION_NOT_FOUND, request);
            }
            if (!verifyModelVersion(request, modelVersion.orElseThrow())) {
                return ModelGatewayResult.failure(ModelGatewayFailureCode.REGISTRY_MISMATCH, request);
            }
            final PromptRenderResult renderResult = renderPrompt(request, promptVersion.orElseThrow());
            if (!renderResult.allowed()) {
                return ModelGatewayResult.failure(toPromptFailure(renderResult), request);
            }
            final String renderedPrompt = renderResult.renderedText();
            final Optional<ModelGatewayFailureCode> renderedBudgetFailure =
                    validateRenderedBudget(request, renderedPrompt);
            if (renderedBudgetFailure.isPresent()) {
                return ModelGatewayResult.failure(renderedBudgetFailure.orElseThrow(), request);
            }
            final Optional<ModelGatewayFailureCode> renderedRedactionFailure =
                    validateTextRedaction(request.redactionPolicy(), renderedPrompt);
            if (renderedRedactionFailure.isPresent()) {
                return ModelGatewayResult.failure(renderedRedactionFailure.orElseThrow(), request);
            }
            final ModelProviderTrustDecision trustDecision = evaluateTrustPolicy(request);
            if (!trustDecision.allowed()) {
                return ModelGatewayResult.failure(trustDecision.failureCode(), request);
            }
            final MockModelProviderResult providerResult = invokeProvider(request, renderedPrompt);
            final Optional<ModelGatewayFailureCode> providerFailure = validateProviderResult(providerResult);
            if (providerFailure.isPresent()) {
                return ModelGatewayResult.failure(providerFailure.orElseThrow(), request);
            }
            final Optional<ModelGatewayFailureCode> outputBudgetFailure =
                    validateOutputBudget(request, renderedPrompt, providerResult);
            if (outputBudgetFailure.isPresent()) {
                return ModelGatewayResult.failure(outputBudgetFailure.orElseThrow(), request);
            }
            final Optional<ModelGatewayFailureCode> outputRedactionFailure =
                    validateOutputRedaction(request.redactionPolicy(), providerResult);
            if (outputRedactionFailure.isPresent()) {
                return ModelGatewayResult.failure(outputRedactionFailure.orElseThrow(), request);
            }
            return successResult(request, trustDecision, providerResult, renderedPrompt);
        } catch (final ModelGatewayException error) {
            return ModelGatewayResult.failure(error.failureCode(), request);
        } catch (final RuntimeException error) {
            return ModelGatewayResult.failure(ModelGatewayFailureCode.UNKNOWN_ERROR, request);
        }
    }

    private Optional<ModelGatewayFailureCode> validateRequired(final ModelGatewayRequest request) {
        if (request == null || request.context() == null) {
            return Optional.of(ModelGatewayFailureCode.MISSING_REQUIRED_FIELD);
        }
        final ModelCallContext context = request.context();
        if (isBlank(context.tenantId())
                || isBlank(context.traceId())
                || isBlank(context.requestId())
                || isBlank(context.decisionRunId())
                || isBlank(context.promptVersionId())
                || isBlank(context.modelVersionId())
                || isBlank(context.providerProfileId())
                || isBlank(request.promptTemplateId())
                || isBlank(request.promptVersion())
                || isBlank(request.promptVersionChecksum())
                || isBlank(request.modelVersionChecksum())) {
            return Optional.of(ModelGatewayFailureCode.MISSING_REQUIRED_FIELD);
        }
        if (request.policy() == null) {
            return Optional.of(ModelGatewayFailureCode.POLICY_DENIED);
        }
        if (request.budget() == null) {
            return Optional.of(ModelGatewayFailureCode.BUDGET_EXCEEDED);
        }
        if (request.redactionPolicy() == null || isBlank(request.redactionPolicy().policyRef())) {
            return Optional.of(ModelGatewayFailureCode.REDACTION_FAILED);
        }
        return Optional.empty();
    }

    private Optional<ModelGatewayFailureCode> validateInputBudget(final ModelGatewayRequest request) {
        final ModelCallBudget budget = request.budget();
        if (budget.maxInputCharacters() < 0
                || budget.maxRenderedPromptCharacters() < 0
                || budget.maxOutputCharacters() < 0
                || budget.maxEstimatedTokens() < 0
                || budget.maxMemoryEntries() < 0) {
            return Optional.of(ModelGatewayFailureCode.BUDGET_EXCEEDED);
        }
        if (request.memoryEntries().size() > budget.maxMemoryEntries()) {
            return Optional.of(ModelGatewayFailureCode.BUDGET_EXCEEDED);
        }
        final int inputCharacters = inputCharacters(request);
        if (inputCharacters > budget.maxInputCharacters()
                || estimateTokens(inputCharacters) > budget.maxEstimatedTokens()) {
            return Optional.of(ModelGatewayFailureCode.BUDGET_EXCEEDED);
        }
        return Optional.empty();
    }

    private Optional<ModelGatewayFailureCode> validateRenderedBudget(
            final ModelGatewayRequest request, final String renderedPrompt) {
        final ModelCallBudget budget = request.budget();
        final int renderedCharacters = length(renderedPrompt);
        if (renderedCharacters > budget.maxRenderedPromptCharacters()
                || estimateTokens(inputCharacters(request) + renderedCharacters)
                        > budget.maxEstimatedTokens()) {
            return Optional.of(ModelGatewayFailureCode.BUDGET_EXCEEDED);
        }
        return Optional.empty();
    }

    private Optional<ModelGatewayFailureCode> validateOutputBudget(
            final ModelGatewayRequest request,
            final String renderedPrompt,
            final MockModelProviderResult providerResult) {
        final ModelCallBudget budget = request.budget();
        final int outputCharacters = providerResult.estimatedOutputCharacters();
        final int estimatedTokens =
                estimateTokens(inputCharacters(request) + length(renderedPrompt)) + providerResult.estimatedTokens();
        if (outputCharacters > budget.maxOutputCharacters()
                || estimatedTokens > budget.maxEstimatedTokens()) {
            return Optional.of(ModelGatewayFailureCode.BUDGET_EXCEEDED);
        }
        return Optional.empty();
    }

    private Optional<ModelGatewayFailureCode> validateInputRedaction(final ModelGatewayRequest request) {
        for (String value : request.renderInputs().values()) {
            final Optional<ModelGatewayFailureCode> failure =
                    validateTextRedaction(request.redactionPolicy(), value);
            if (failure.isPresent()) {
                return failure;
            }
        }
        for (String value : request.memoryEntries()) {
            final Optional<ModelGatewayFailureCode> failure =
                    validateTextRedaction(request.redactionPolicy(), value);
            if (failure.isPresent()) {
                return failure;
            }
        }
        return Optional.empty();
    }

    private Optional<ModelGatewayFailureCode> validatePromptInputs(final ModelGatewayRequest request) {
        try {
            for (String value : request.renderInputs().values()) {
                final PromptInjectionDecision decision =
                        promptInjectionGuard.evaluate(request.context().tenantId(), value);
                if (!decision.allowed()) {
                    return Optional.of(ModelGatewayFailureCode.PROMPT_DENIED);
                }
            }
            for (String value : request.memoryEntries()) {
                final PromptInjectionDecision decision =
                        promptInjectionGuard.evaluate(request.context().tenantId(), value);
                if (!decision.allowed()) {
                    return Optional.of(ModelGatewayFailureCode.PROMPT_DENIED);
                }
            }
            return Optional.empty();
        } catch (final RuntimeException error) {
            return Optional.of(ModelGatewayFailureCode.PROMPT_DENIED);
        }
    }

    private Optional<PromptVersion> lookupPromptVersion(final ModelGatewayRequest request) {
        return promptVersionRegistry.lookup(
                new PromptVersionLookupQuery(
                        request.context().tenantId(),
                        new PromptTemplateId(request.promptTemplateId()),
                        request.promptVersion()));
    }

    private boolean verifyPromptVersion(
            final ModelGatewayRequest request, final PromptVersion promptVersion) {
        return request.context().tenantId().equals(promptVersion.tenantId())
                && request.context().promptVersionId().equals(promptVersion.id().value())
                && new PromptVersionChecksum(request.promptVersionChecksum()).equals(promptVersion.checksum());
    }

    private Optional<ModelVersion> lookupModelVersion(final ModelGatewayRequest request) {
        return modelVersionRegistry.lookup(
                new ModelVersionLookupQuery(
                        request.context().tenantId(),
                        new ModelVersionId(request.context().modelVersionId())));
    }

    private boolean verifyModelVersion(final ModelGatewayRequest request, final ModelVersion modelVersion) {
        return request.context().tenantId().equals(modelVersion.tenantId())
                && new ModelVersionChecksum(request.modelVersionChecksum()).equals(modelVersion.checksum());
    }

    private PromptRenderResult renderPrompt(
            final ModelGatewayRequest request, final PromptVersion promptVersion) {
        try {
            return promptRenderPolicy.render(
                    promptVersion,
                    new PromptRenderContext(
                            request.context().tenantId(),
                            request.context().traceId(),
                            request.context().requestId(),
                            request.renderInputs()));
        } catch (final RuntimeException error) {
            return PromptRenderResult.denied("PROMPT_RENDER_FAILED", List.of());
        }
    }

    private ModelProviderTrustDecision evaluateTrustPolicy(final ModelGatewayRequest request) {
        try {
            final ModelProviderTrustDecision decision = providerTrustPolicy.evaluate(request);
            if (decision == null) {
                return ModelProviderTrustDecision.denied(
                        ModelGatewayFailureCode.POLICY_DENIED, "provider-trust-null");
            }
            return decision;
        } catch (final RuntimeException error) {
            return ModelProviderTrustDecision.denied(
                    ModelGatewayFailureCode.POLICY_DENIED, "provider-trust-exception");
        }
    }

    private MockModelProviderResult invokeProvider(
            final ModelGatewayRequest request, final String renderedPrompt) {
        return modelProvider.invoke(request, renderedPrompt);
    }

    private Optional<ModelGatewayFailureCode> validateProviderResult(
            final MockModelProviderResult providerResult) {
        if (providerResult == null) {
            return Optional.of(ModelGatewayFailureCode.PROVIDER_OUTPUT_INVALID);
        }
        return switch (providerResult.mode()) {
            case NORMAL -> {
                if (providerResult.decision() == null
                        || isBlank(providerResult.redactedSummary())
                        || isBlank(providerResult.safeProviderRef())) {
                    yield Optional.of(ModelGatewayFailureCode.PROVIDER_OUTPUT_INVALID);
                }
                yield Optional.empty();
            }
            case UNAVAILABLE -> Optional.of(ModelGatewayFailureCode.PROVIDER_UNAVAILABLE);
            case TIMEOUT -> Optional.of(ModelGatewayFailureCode.PROVIDER_TIMEOUT);
            case BUDGET_EXCEEDED -> Optional.of(ModelGatewayFailureCode.BUDGET_EXCEEDED);
            case POLICY_DENIED -> Optional.of(ModelGatewayFailureCode.POLICY_DENIED);
            case MALFORMED -> Optional.of(ModelGatewayFailureCode.PROVIDER_OUTPUT_INVALID);
            case REDACTION_FAILURE -> Optional.of(ModelGatewayFailureCode.REDACTION_FAILED);
        };
    }

    private Optional<ModelGatewayFailureCode> validateOutputRedaction(
            final ModelCallRedactionPolicy policy, final MockModelProviderResult providerResult) {
        Optional<ModelGatewayFailureCode> failure =
                validateTextRedaction(policy, providerResult.redactedSummary());
        if (failure.isPresent()) {
            return failure;
        }
        failure = validateTextRedaction(policy, providerResult.decision().rationale());
        if (failure.isPresent()) {
            return failure;
        }
        for (Map.Entry<String, String> entry : providerResult.decision().constraints().entrySet()) {
            failure = validateTextRedaction(policy, entry.getKey());
            if (failure.isPresent()) {
                return failure;
            }
            failure = validateTextRedaction(policy, entry.getValue());
            if (failure.isPresent()) {
                return failure;
            }
        }
        return Optional.empty();
    }

    private Optional<ModelGatewayFailureCode> validateTextRedaction(
            final ModelCallRedactionPolicy policy, final String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        if (policy.rejectSecretLikeMaterial()
                && PromptModelSafetyRules.containsSecretLikeMaterial(value)) {
            return Optional.of(ModelGatewayFailureCode.REDACTION_FAILED);
        }
        if (policy.rejectExecutableTradingInstruction()
                && PromptModelSafetyRules.containsExecutableTradingInstruction(value)) {
            return Optional.of(ModelGatewayFailureCode.REDACTION_FAILED);
        }
        return Optional.empty();
    }

    private ModelGatewayFailureCode toPromptFailure(final PromptRenderResult renderResult) {
        if ("PROMPT_DENIED".equals(renderResult.failureCode())
                || "PROMPT_INPUT_DENIED".equals(renderResult.failureCode())
                || "PROMPT_TENANT_MISMATCH".equals(renderResult.failureCode())
                || "PROMPT_GUARD_FAILURE".equals(renderResult.failureCode())) {
            return ModelGatewayFailureCode.PROMPT_DENIED;
        }
        return ModelGatewayFailureCode.PROMPT_RENDER_FAILED;
    }

    private ModelGatewayResult successResult(
            final ModelGatewayRequest request,
            final ModelProviderTrustDecision trustDecision,
            final MockModelProviderResult providerResult,
            final String renderedPrompt) {
        final String callRef =
                "model-call:"
                        + PromptModelSafetyRules.sha256Hex(
                                        List.of(
                                                request.context().tenantId(),
                                                request.context().traceId(),
                                                request.context().requestId(),
                                                request.context().decisionRunId(),
                                                providerResult.safeProviderRef()))
                                .substring(0, 16);
        final String auditRef = "planned-audit:" + callRef.substring("model-call:".length());
        return ModelGatewayResult.success(
                request,
                providerResult.decision(),
                trustDecision.decisionRef(),
                callRef,
                auditRef,
                providerResult.redactedSummary(),
                new ModelGatewayUsage(
                        inputCharacters(request),
                        length(renderedPrompt),
                        providerResult.estimatedOutputCharacters(),
                        estimateTokens(inputCharacters(request) + length(renderedPrompt))
                                + providerResult.estimatedTokens(),
                        request.memoryEntries().size()));
    }

    private static int inputCharacters(final ModelGatewayRequest request) {
        int total = 0;
        for (String value : request.renderInputs().values()) {
            total += length(value);
        }
        for (String value : request.memoryEntries()) {
            total += length(value);
        }
        return total;
    }

    private static int length(final String value) {
        return value == null ? 0 : value.length();
    }

    private static int estimateTokens(final int characters) {
        if (characters <= 0) {
            return 0;
        }
        return Math.max(1, (characters + 3) / 4);
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}

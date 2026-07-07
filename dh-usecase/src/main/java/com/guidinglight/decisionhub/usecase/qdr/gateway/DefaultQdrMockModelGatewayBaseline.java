package com.guidinglight.decisionhub.usecase.qdr.gateway;

import com.guidinglight.decisionhub.domain.qdr.model.ModelProfileId;
import com.guidinglight.decisionhub.domain.qdr.model.ModelVersion;
import com.guidinglight.decisionhub.domain.qdr.model.ModelVersionId;
import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;
import com.guidinglight.decisionhub.domain.qdr.model.PromptTemplateId;
import com.guidinglight.decisionhub.domain.qdr.model.PromptVersion;
import com.guidinglight.decisionhub.domain.qdr.model.PromptVersionId;
import com.guidinglight.decisionhub.domain.qdr.model.PromptVersionStatus;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderKind;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderProfile;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderProfileId;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderProfileStatus;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionRegistrationCommand;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionRegistryPort;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionRegistrationCommand;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionRegistryPort;
import com.guidinglight.decisionhub.usecase.qdr.model.SaveModelVersionCommand;
import com.guidinglight.decisionhub.usecase.qdr.model.SavePromptVersionCommand;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * B4 deterministic mock gateway baseline bootstrap。
 *
 * <p>每个 tenant 的 prompt/model/provider refs 都由固定 seed 计算，并同步注册到 B1/B2 in-memory
 * registry 与 B3 persistence ports。该实现不读取环境变量、不读取 secret、不访问外部文件、不调用 HTTP。
 */
public final class DefaultQdrMockModelGatewayBaseline implements QdrModelGatewayBaselinePort {

    private static final String TEMPLATE_KEY = "qdr-dryrun-gateway-review";
    private static final String PROMPT_VERSION = "v1";
    private static final String RENDER_POLICY_KEY = "qdr-b4-render";
    private static final String MODEL_NAME = "qdr-mock-review-model";
    private static final String MODEL_VERSION = "v1";
    private static final String MODEL_KEY = "qdr-mock-review";
    private static final String PROVIDER_KEY = "qdr-mock-provider";
    private static final String TRUST_POLICY_REF = "qdr-b4-mock-provider-trust";
    private static final String TEMPLATE_BODY =
            "Summarize readonly QDR evidence for {{symbol}} on {{market}} {{timeframe}}. "
                    + "Return risk explanation and evidence summary only.";

    private final PromptVersionRegistryPort promptVersionRegistry;
    private final ModelVersionRegistryPort modelVersionRegistry;
    private final ProviderProfileRegistryPort providerProfileRegistry;
    private final PromptVersionPersistencePort promptVersionPersistence;
    private final ModelVersionPersistencePort modelVersionPersistence;
    private final Clock clock;

    /**
     * 创建 mock baseline bootstrap。
     *
     * @param promptVersionRegistry    B1 prompt registry。
     * @param modelVersionRegistry     B2 model registry。
     * @param providerProfileRegistry  B4 provider profile registry。
     * @param promptVersionPersistence B3 prompt persistence port。
     * @param modelVersionPersistence  B3 model persistence port。
     * @param clock                    时间源。
     */
    public DefaultQdrMockModelGatewayBaseline(
            final PromptVersionRegistryPort promptVersionRegistry,
            final ModelVersionRegistryPort modelVersionRegistry,
            final ProviderProfileRegistryPort providerProfileRegistry,
            final PromptVersionPersistencePort promptVersionPersistence,
            final ModelVersionPersistencePort modelVersionPersistence,
            final Clock clock) {
        this.promptVersionRegistry =
                Objects.requireNonNull(promptVersionRegistry, "promptVersionRegistry");
        this.modelVersionRegistry =
                Objects.requireNonNull(modelVersionRegistry, "modelVersionRegistry");
        this.providerProfileRegistry =
                Objects.requireNonNull(providerProfileRegistry, "providerProfileRegistry");
        this.promptVersionPersistence =
                Objects.requireNonNull(promptVersionPersistence, "promptVersionPersistence");
        this.modelVersionPersistence =
                Objects.requireNonNull(modelVersionPersistence, "modelVersionPersistence");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public QdrModelGatewayBaseline prepare(final QdrModelGatewayBaselineCommand command) {
        final QdrModelGatewayBaselineCommand checked = Objects.requireNonNull(command, "command");
        final Instant now = clock.instant();
        final UUID promptTemplateId = stableUuid(checked.tenantId(), "prompt-template");
        final UUID promptVersionId = stableUuid(checked.tenantId(), "prompt-version", PROMPT_VERSION);
        final UUID modelProfileId = stableUuid(checked.tenantId(), "model-profile");
        final UUID modelVersionId = stableUuid(checked.tenantId(), "model-version", MODEL_VERSION);
        final UUID providerProfileId = stableUuid(checked.tenantId(), "provider-profile");

        final PromptVersion promptVersion =
                PromptVersion.create(
                        new PromptVersionId(promptVersionId.toString()),
                        new PromptTemplateId(promptTemplateId.toString()),
                        checked.tenantId(),
                        PROMPT_VERSION,
                        TEMPLATE_BODY,
                        RENDER_POLICY_KEY,
                        PromptVersionStatus.ACTIVE,
                        now,
                        "system");
        final ModelVersion modelVersion =
                ModelVersion.create(
                        new ModelVersionId(modelVersionId.toString()),
                        checked.tenantId(),
                        new ModelProfileId(modelProfileId.toString()),
                        MODEL_NAME,
                        MODEL_VERSION,
                        "readonly qdr evidence summary",
                        now);
        final ProviderProfile providerProfile =
                new ProviderProfile(
                        new ProviderProfileId(providerProfileId.toString()),
                        checked.tenantId(),
                        ProviderKind.MOCK,
                        PROVIDER_KEY,
                        "QDR mock provider",
                        "readonly qdr evidence summary",
                        ProviderProfileStatus.ENABLED,
                        TRUST_POLICY_REF,
                        now);

        promptVersionRegistry.register(
                new PromptVersionRegistrationCommand(checked.tenantId(), promptVersion));
        modelVersionRegistry.register(
                new ModelVersionRegistrationCommand(checked.tenantId(), modelVersion));
        providerProfileRegistry.register(providerProfile);
        promptVersionPersistence.save(
                new SavePromptVersionCommand(
                        promptTemplateId,
                        promptVersionId,
                        checked.tenantId(),
                        TEMPLATE_KEY,
                        "QDR dry-run gateway review",
                        PROMPT_VERSION,
                        RENDER_POLICY_KEY,
                        "prompt-template:" + shortHash(TEMPLATE_BODY),
                        PromptModelSafetyRules.sha256Hex(List.of(TEMPLATE_BODY)),
                        "QDR readonly evidence summary prompt",
                        PromptVersionStatus.ACTIVE,
                        promptVersion.checksum().value(),
                        now,
                        "system"));
        modelVersionPersistence.save(
                new SaveModelVersionCommand(
                        modelProfileId,
                        modelVersionId,
                        providerProfileId,
                        checked.tenantId(),
                        ProviderKind.MOCK,
                        PROVIDER_KEY,
                        MODEL_KEY,
                        "QDR mock review model",
                        "readonly qdr evidence summary",
                        4096,
                        512,
                        ProviderProfileStatus.ENABLED,
                        TRUST_POLICY_REF,
                        MODEL_NAME,
                        MODEL_VERSION,
                        modelVersion.checksum().value(),
                        now));
        return new QdrModelGatewayBaseline(
                promptTemplateId,
                promptVersionId,
                PROMPT_VERSION,
                promptVersion.checksum().value(),
                modelProfileId,
                modelVersionId,
                modelVersion.checksum().value(),
                providerProfileId,
                ProviderKind.MOCK,
                "mock-provider:" + providerProfileId,
                new ModelCallPolicy(TRUST_POLICY_REF, true, true, true),
                new ModelCallBudget(1024, 2048, 512, 900, 8),
                ModelCallRedactionPolicy.strictDefault());
    }

    private static UUID stableUuid(final String tenantId, final String... parts) {
        final String joined = "qdr-b4|" + tenantId + "|" + String.join("|", parts);
        return UUID.nameUUIDFromBytes(joined.getBytes(StandardCharsets.UTF_8));
    }

    private static String shortHash(final String value) {
        return PromptModelSafetyRules.sha256Hex(List.of(value)).substring(0, 16);
    }
}

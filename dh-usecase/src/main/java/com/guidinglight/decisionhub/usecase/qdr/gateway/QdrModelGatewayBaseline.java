package com.guidinglight.decisionhub.usecase.qdr.gateway;

import com.guidinglight.decisionhub.domain.qdr.model.ProviderKind;

import java.util.Objects;
import java.util.UUID;

/**
 * QDR mock gateway baseline refs。
 *
 * <p>baseline 来自 B1/B2/B3 mock registry / persistence bootstrap，只包含 version refs、checksum、
 * provider identity ref、policy 和 budget；不包含 raw prompt 或 raw provider response。
 */
public record QdrModelGatewayBaseline(
        UUID promptTemplateId,
        UUID promptVersionId,
        String promptVersion,
        String promptVersionChecksum,
        UUID modelProfileId,
        UUID modelVersionId,
        String modelVersionChecksum,
        UUID providerProfileId,
        ProviderKind providerKind,
        String providerIdentityRef,
        ModelCallPolicy policy,
        ModelCallBudget budget,
        ModelCallRedactionPolicy redactionPolicy) {

    /** 校验 baseline 中的 registry / persistence refs 完整。 */
    public QdrModelGatewayBaseline {
        promptTemplateId = Objects.requireNonNull(promptTemplateId, "promptTemplateId");
        promptVersionId = Objects.requireNonNull(promptVersionId, "promptVersionId");
        promptVersion = requireText(promptVersion, "promptVersion");
        promptVersionChecksum = requireText(promptVersionChecksum, "promptVersionChecksum");
        modelProfileId = Objects.requireNonNull(modelProfileId, "modelProfileId");
        modelVersionId = Objects.requireNonNull(modelVersionId, "modelVersionId");
        modelVersionChecksum = requireText(modelVersionChecksum, "modelVersionChecksum");
        providerProfileId = Objects.requireNonNull(providerProfileId, "providerProfileId");
        providerKind = Objects.requireNonNull(providerKind, "providerKind");
        providerIdentityRef = requireText(providerIdentityRef, "providerIdentityRef");
        policy = Objects.requireNonNull(policy, "policy");
        budget = Objects.requireNonNull(budget, "budget");
        redactionPolicy = Objects.requireNonNull(redactionPolicy, "redactionPolicy");
    }

    private static String requireText(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return checked;
    }
}

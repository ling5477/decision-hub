package com.guidinglight.decisionhub.usecase.qdr.gateway;

import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderKind;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderProfile;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderProfileStatus;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * B2 deterministic ProviderTrustPolicy。
 *
 * <p>该实现只接受 in-memory provider profile，不读取环境变量、不读取 credential、不访问 HTTP。
 * unknown、disabled、planned、tenant mismatch、real provider 或 policy 缺失均 fail-closed。
 */
public final class DeterministicProviderTrustPolicy implements ProviderTrustPolicy {

    private final Map<String, ProviderProfile> providerProfiles;

    /**
     * 创建 deterministic trust policy。
     *
     * @param providerProfiles in-memory provider profiles。
     */
    public DeterministicProviderTrustPolicy(final Collection<ProviderProfile> providerProfiles) {
        this.providerProfiles =
                Objects.requireNonNull(providerProfiles, "providerProfiles").stream()
                        .collect(Collectors.toUnmodifiableMap(p -> p.id().value(), Function.identity()));
    }

    @Override
    public ModelProviderTrustDecision evaluate(final ModelGatewayRequest request) {
        if (request == null || request.context() == null || request.policy() == null) {
            return ModelProviderTrustDecision.denied(
                    ModelGatewayFailureCode.POLICY_DENIED, "provider-trust-missing-context");
        }
        final ModelCallContext context = request.context();
        if (isBlank(context.providerProfileId())) {
            return ModelProviderTrustDecision.denied(
                    ModelGatewayFailureCode.UNKNOWN_PROVIDER, "provider-trust-unknown");
        }
        final ProviderProfile profile = providerProfiles.get(context.providerProfileId().trim());
        if (profile == null) {
            return ModelProviderTrustDecision.denied(
                    ModelGatewayFailureCode.UNKNOWN_PROVIDER, "provider-trust-unknown");
        }
        if (!profile.tenantId().equals(context.tenantId())) {
            return ModelProviderTrustDecision.denied(
                    ModelGatewayFailureCode.POLICY_DENIED, "provider-trust-tenant-mismatch");
        }
        if (profile.status() == ProviderProfileStatus.DISABLED
                || profile.status() == ProviderProfileStatus.PLANNED) {
            return ModelProviderTrustDecision.denied(
                    ModelGatewayFailureCode.PROVIDER_DISABLED, "provider-trust-disabled");
        }
        if (isBlank(request.policy().policyRef())) {
            return ModelProviderTrustDecision.denied(
                    ModelGatewayFailureCode.POLICY_DENIED, "provider-trust-policy-missing");
        }
        if (!request.policy().mockOnly()) {
            return ModelProviderTrustDecision.denied(
                    ModelGatewayFailureCode.REAL_PROVIDER_FORBIDDEN, "provider-trust-mock-required");
        }
        if (request.policy().requireEnabledProvider() && profile.status() != ProviderProfileStatus.ENABLED) {
            return ModelProviderTrustDecision.denied(
                    ModelGatewayFailureCode.PROVIDER_DISABLED, "provider-trust-not-enabled");
        }
        if (request.policy().mockOnly() && profile.providerKind() != ProviderKind.MOCK) {
            return ModelProviderTrustDecision.denied(
                    ModelGatewayFailureCode.REAL_PROVIDER_FORBIDDEN, "provider-trust-real-forbidden");
        }
        return ModelProviderTrustDecision.allowed("provider-trust:" + safeRef(context.providerProfileId()));
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private static String safeRef(final String value) {
        return PromptModelSafetyRules.sha256Hex(java.util.List.of(value)).substring(0, 16);
    }
}

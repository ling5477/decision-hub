package com.guidinglight.decisionhub.usecase.qdr.gateway;

import com.guidinglight.decisionhub.domain.qdr.model.ProviderProfile;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory mock provider profile registry。
 *
 * <p>该 registry 只保存 deterministic mock profile identity，不保存 credential、endpoint、API key 或
 * provider SDK client。相同 id 不同 profile 视为 bootstrap mismatch 并 fail-closed。
 */
public final class InMemoryProviderProfileRegistry implements ProviderProfileRegistryPort {

    private final Map<String, ProviderProfile> profiles = new ConcurrentHashMap<>();

    @Override
    public ProviderProfile register(final ProviderProfile providerProfile) {
        final ProviderProfile checked = Objects.requireNonNull(providerProfile, "providerProfile");
        final ProviderProfile existing = profiles.putIfAbsent(checked.id().value(), checked);
        if (existing == null || existing.equals(checked)) {
            return existing == null ? checked : existing;
        }
        throw new IllegalStateException("provider profile bootstrap mismatch");
    }

    @Override
    public Optional<ProviderProfile> findById(final String providerProfileId) {
        if (providerProfileId == null || providerProfileId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(profiles.get(providerProfileId.trim()));
    }
}

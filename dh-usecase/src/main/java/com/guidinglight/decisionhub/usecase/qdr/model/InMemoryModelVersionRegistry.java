package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.ModelVersion;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * stage-qdr-3 B2 in-memory/mock ModelVersion registry。
 *
 * <p>该实现只用于 usecase/mock 范围，数据仅驻留进程内；不写 DB、不调用 HTTP、不依赖 provider
 * SDK。registry key 强制包含 tenantId，避免跨 tenant lookup。
 */
public final class InMemoryModelVersionRegistry implements ModelVersionRegistryPort {

    private final Map<RegistryKey, ModelVersion> versions = new ConcurrentHashMap<>();

    @Override
    public ModelVersionRegistryResult register(final ModelVersionRegistrationCommand command) {
        final ModelVersionRegistrationCommand checked = Objects.requireNonNull(command, "command");
        final ModelVersion modelVersion = checked.modelVersion();
        final RegistryKey key = new RegistryKey(checked.tenantId(), modelVersion.id().value());
        final ModelVersion existing = versions.putIfAbsent(key, modelVersion);
        if (existing == null) {
            return new ModelVersionRegistryResult(modelVersion, true, false);
        }
        if (!existing.checksum().equals(modelVersion.checksum())) {
            throw new ModelVersionRegistryException("model version checksum conflict");
        }
        return new ModelVersionRegistryResult(existing, false, true);
    }

    @Override
    public Optional<ModelVersion> lookup(final ModelVersionLookupQuery query) {
        final ModelVersionLookupQuery checked = Objects.requireNonNull(query, "query");
        return Optional.ofNullable(
                versions.get(new RegistryKey(checked.tenantId(), checked.modelVersionId().value())));
    }

    private record RegistryKey(String tenantId, String modelVersionId) {
    }
}

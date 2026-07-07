package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.PromptVersion;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * stage-qdr-3 B1 in-memory/mock PromptVersion registry。
 *
 * <p>该实现只用于 usecase/mock 范围，数据仅驻留进程内；不写 DB、不调用 HTTP、不依赖 provider SDK。
 * registry key 强制包含 tenantId，避免跨 tenant lookup。
 */
public final class InMemoryPromptVersionRegistry implements PromptVersionRegistryPort {

    private final Map<RegistryKey, PromptVersion> versions = new ConcurrentHashMap<>();

    @Override
    public PromptVersionRegistryResult register(final PromptVersionRegistrationCommand command) {
        final PromptVersionRegistrationCommand checked = Objects.requireNonNull(command, "command");
        final PromptVersion promptVersion = checked.promptVersion();
        final RegistryKey key =
                new RegistryKey(
                        checked.tenantId(), promptVersion.templateId().value(), promptVersion.version());
        final PromptVersion existing = versions.putIfAbsent(key, promptVersion);
        if (existing == null) {
            return new PromptVersionRegistryResult(promptVersion, true, false);
        }
        if (!existing.checksum().equals(promptVersion.checksum())) {
            throw new PromptVersionRegistryException("prompt version checksum conflict");
        }
        return new PromptVersionRegistryResult(existing, false, true);
    }

    @Override
    public Optional<PromptVersion> lookup(final PromptVersionLookupQuery query) {
        final PromptVersionLookupQuery checked = Objects.requireNonNull(query, "query");
        return Optional.ofNullable(
                versions.get(
                        new RegistryKey(
                                checked.tenantId(), checked.templateId().value(), checked.version())));
    }

    private record RegistryKey(String tenantId, String templateId, String version) {
    }
}

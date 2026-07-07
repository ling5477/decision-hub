package com.guidinglight.decisionhub.usecase.qdr.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.qdr.model.PromptTemplateId;
import com.guidinglight.decisionhub.domain.qdr.model.PromptVersion;
import com.guidinglight.decisionhub.domain.qdr.model.PromptVersionId;
import com.guidinglight.decisionhub.domain.qdr.model.PromptVersionStatus;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * stage-qdr-3 B1 in-memory PromptVersionRegistry 回归。
 *
 * <p>覆盖 tenant-bound lookup、重复注册、checksum 冲突和跨 tenant fail-closed/not found。
 */
final class InMemoryPromptVersionRegistryTest {

    private static final Instant NOW = Instant.parse("2026-07-07T00:00:00Z");
    private static final PromptTemplateId TEMPLATE_ID = new PromptTemplateId("qdr-review-template");

    private InMemoryPromptVersionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InMemoryPromptVersionRegistry();
    }

    @Test
    void registryRegisterSuccess() {
        final PromptVersionRegistryResult result =
                registry.register(new PromptVersionRegistrationCommand("tenant-a", prompt("v1")));

        assertTrue(result.created());
        assertFalse(result.idempotent());
        assertEquals("tenant-a", result.promptVersion().tenantId());
    }

    @Test
    void registryLookupSuccessSameTenant() {
        registry.register(new PromptVersionRegistrationCommand("tenant-a", prompt("v1")));

        final Optional<PromptVersion> found =
                registry.lookup(new PromptVersionLookupQuery("tenant-a", TEMPLATE_ID, "v1"));

        assertTrue(found.isPresent());
        assertEquals("v1", found.orElseThrow().version());
    }

    @Test
    void registryLookupCrossTenantReturnsEmpty() {
        registry.register(new PromptVersionRegistrationCommand("tenant-a", prompt("v1")));

        final Optional<PromptVersion> found =
                registry.lookup(new PromptVersionLookupQuery("tenant-b", TEMPLATE_ID, "v1"));

        assertTrue(found.isEmpty());
    }

    @Test
    void duplicateVersionSameChecksumIsIdempotent() {
        registry.register(new PromptVersionRegistrationCommand("tenant-a", prompt("v1")));

        final PromptVersionRegistryResult duplicate =
                registry.register(new PromptVersionRegistrationCommand("tenant-a", prompt("v1")));

        assertFalse(duplicate.created());
        assertTrue(duplicate.idempotent());
    }

    @Test
    void duplicateVersionDifferentChecksumFailsClosed() {
        registry.register(new PromptVersionRegistrationCommand("tenant-a", prompt("v1")));

        assertThrows(
                PromptVersionRegistryException.class,
                () ->
                        registry.register(
                                new PromptVersionRegistrationCommand(
                                        "tenant-a",
                                        prompt("prompt-v2", "v1", "Review QDR risk evidence."))));
    }

    @Test
    void registrationTenantMismatchFailsClosed() {
        assertThrows(
                PromptVersionRegistryException.class,
                () -> registry.register(new PromptVersionRegistrationCommand("tenant-b", prompt("v1"))));
    }

    private static PromptVersion prompt(final String version) {
        return prompt("prompt-" + version, version, "Review QDR evidence.");
    }

    private static PromptVersion prompt(final String id, final String version, final String body) {
        return PromptVersion.create(
                new PromptVersionId(id),
                TEMPLATE_ID,
                "tenant-a",
                version,
                body,
                "default-render",
                PromptVersionStatus.ACTIVE,
                NOW,
                "system");
    }
}

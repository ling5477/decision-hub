package com.guidinglight.decisionhub.usecase.qdr.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.qdr.model.ModelProfileId;
import com.guidinglight.decisionhub.domain.qdr.model.ModelVersion;
import com.guidinglight.decisionhub.domain.qdr.model.ModelVersionId;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * stage-qdr-3 B2 in-memory ModelVersionRegistry 回归。
 *
 * <p>覆盖 tenant-bound lookup、重复注册、checksum 冲突和跨 tenant fail-closed/not found。
 */
final class InMemoryModelVersionRegistryTest {

    private static final Instant NOW = Instant.parse("2026-07-07T00:00:00Z");
    private static final ModelProfileId MODEL_PROFILE_ID = new ModelProfileId("mock-model-profile");

    private InMemoryModelVersionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InMemoryModelVersionRegistry();
    }

    @Test
    void registryRegisterSuccess() {
        final ModelVersionRegistryResult result =
                registry.register(new ModelVersionRegistrationCommand("tenant-a", model("model-v1", "v1")));

        assertTrue(result.created());
        assertFalse(result.idempotent());
        assertEquals("tenant-a", result.modelVersion().tenantId());
    }

    @Test
    void registryLookupSuccessSameTenant() {
        registry.register(new ModelVersionRegistrationCommand("tenant-a", model("model-v1", "v1")));

        final Optional<ModelVersion> found =
                registry.lookup(
                        new ModelVersionLookupQuery("tenant-a", new ModelVersionId("model-v1")));

        assertTrue(found.isPresent());
        assertEquals("v1", found.orElseThrow().modelVersion());
    }

    @Test
    void registryLookupCrossTenantReturnsEmpty() {
        registry.register(new ModelVersionRegistrationCommand("tenant-a", model("model-v1", "v1")));

        final Optional<ModelVersion> found =
                registry.lookup(
                        new ModelVersionLookupQuery("tenant-b", new ModelVersionId("model-v1")));

        assertTrue(found.isEmpty());
    }

    @Test
    void duplicateVersionSameChecksumIsIdempotent() {
        registry.register(new ModelVersionRegistrationCommand("tenant-a", model("model-v1", "v1")));

        final ModelVersionRegistryResult duplicate =
                registry.register(new ModelVersionRegistrationCommand("tenant-a", model("model-v1", "v1")));

        assertFalse(duplicate.created());
        assertTrue(duplicate.idempotent());
    }

    @Test
    void duplicateVersionDifferentChecksumFailsClosed() {
        registry.register(new ModelVersionRegistrationCommand("tenant-a", model("model-v1", "v1")));

        assertThrows(
                ModelVersionRegistryException.class,
                () ->
                        registry.register(
                                new ModelVersionRegistrationCommand(
                                        "tenant-a", model("model-v1", "v2"))));
    }

    @Test
    void registrationTenantMismatchFailsClosed() {
        assertThrows(
                ModelVersionRegistryException.class,
                () -> registry.register(new ModelVersionRegistrationCommand("tenant-b", model("model-v1", "v1"))));
    }

    private static ModelVersion model(final String id, final String version) {
        return ModelVersion.create(
                new ModelVersionId(id),
                "tenant-a",
                MODEL_PROFILE_ID,
                "mock-qdr-model",
                version,
                "readonly-qdr",
                NOW);
    }
}

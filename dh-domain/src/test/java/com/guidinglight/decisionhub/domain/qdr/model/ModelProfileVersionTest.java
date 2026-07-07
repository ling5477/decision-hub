package com.guidinglight.decisionhub.domain.qdr.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * stage-qdr-3 B1 ModelProfile / ProviderProfile / ModelVersion domain 校验。
 */
final class ModelProfileVersionTest {

    private static final Instant NOW = Instant.parse("2026-07-07T00:00:00Z");

    @Test
    void modelProfileRejectsCredentialLikeFields() {
        final ModelVersionValidationException error =
                assertThrows(
                        ModelVersionValidationException.class,
                        () ->
                                new ModelProfile(
                                        new ModelProfileId("model-profile-a"),
                                        "tenant-a",
                                        new ProviderProfileId("provider-profile-a"),
                                        "mock-qdr",
                                        "apiSecret=ABC123",
                                        "structured qdr reasoning",
                                        8192,
                                        1024,
                                        NOW));

        assertFalse(error.getMessage().contains("ABC123"));
        assertFalse(error.getMessage().contains("apiSecret=ABC123"));
    }

    @Test
    void providerProfileRejectsSecretLikeFields() {
        final ModelVersionValidationException error =
                assertThrows(
                        ModelVersionValidationException.class,
                        () ->
                                new ProviderProfile(
                                        new ProviderProfileId("provider-profile-a"),
                                        "tenant-a",
                                        ProviderKind.MOCK,
                                        "mock-provider",
                                        "token=ABC123",
                                        "deterministic local mock",
                                        ProviderProfileStatus.ENABLED,
                                        "mock-trust-policy",
                                        NOW));

        assertFalse(error.getMessage().contains("ABC123"));
        assertFalse(error.getMessage().contains("token=ABC123"));
    }

    @Test
    void providerProfileStoresIdentityCapabilityAndStatusOnly() {
        final ProviderProfile profile =
                new ProviderProfile(
                        new ProviderProfileId("provider-profile-a"),
                        "tenant-a",
                        ProviderKind.MOCK,
                        "mock-provider",
                        "Mock Provider",
                        "deterministic local mock",
                        ProviderProfileStatus.ENABLED,
                        "mock-trust-policy",
                        NOW);

        assertEquals(ProviderKind.MOCK, profile.providerKind());
        assertEquals(ProviderProfileStatus.ENABLED, profile.status());
        assertEquals("mock-provider", profile.providerKey());
    }

    @Test
    void modelVersionMustAssociateModelProfile() {
        assertThrows(
                NullPointerException.class,
                () ->
                        ModelVersion.create(
                                new ModelVersionId("model-version-a"),
                                "tenant-a",
                                null,
                                "mock-qdr",
                                "2026-07-07",
                                "structured qdr reasoning",
                                NOW));
    }

    @Test
    void modelVersionChecksumIsDeterministicAndChangesWithVersion() {
        final ModelVersion first = modelVersion("model-version-a", "2026-07-07");
        final ModelVersion second = modelVersion("model-version-b", "2026-07-07");
        final ModelVersion third = modelVersion("model-version-c", "2026-07-08");

        assertEquals(first.checksum(), second.checksum());
        assertNotEquals(first.checksum(), third.checksum());
    }

    @Test
    void modelVersionChecksumMismatchFailsClosed() {
        final ModelVersion valid = modelVersion("model-version-a", "2026-07-07");

        assertThrows(
                ModelVersionValidationException.class,
                () ->
                        new ModelVersion(
                                valid.id(),
                                valid.tenantId(),
                                valid.modelProfileId(),
                                valid.modelName(),
                                valid.modelVersion(),
                                valid.capability(),
                                new ModelVersionChecksum(
                                        "0000000000000000000000000000000000000000000000000000000000000000"),
                                valid.createdAt()));
    }

    @Test
    void modelVersionIsImmutableRecord() {
        assertTrue(ModelVersion.class.isRecord());
        assertTrue(Modifier.isFinal(ModelVersion.class.getModifiers()));
        assertTrue(
                Arrays.stream(ModelVersion.class.getDeclaredFields())
                        .filter(field -> !field.isSynthetic())
                        .allMatch(field -> Modifier.isFinal(field.getModifiers())));
    }

    private static ModelVersion modelVersion(final String id, final String version) {
        return ModelVersion.create(
                new ModelVersionId(id),
                "tenant-a",
                new ModelProfileId("model-profile-a"),
                "mock-qdr",
                version,
                "structured qdr reasoning",
                NOW);
    }
}

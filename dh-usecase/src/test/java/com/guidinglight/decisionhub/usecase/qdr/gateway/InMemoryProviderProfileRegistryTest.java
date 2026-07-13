package com.guidinglight.decisionhub.usecase.qdr.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.domain.qdr.model.ProviderKind;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderProfile;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderProfileId;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderProfileStatus;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link InMemoryProviderProfileRegistry} 完整 profile 一致性与 fail-closed 回归。
 *
 * <p>Registry 以稳定 ID lookup，并对已存在 ID 的完整 immutable snapshot 做严格比较；任何材料字段或创建元数据
 * 漂移都不得覆盖原 profile。
 */
final class InMemoryProviderProfileRegistryTest {

    private static final ProviderProfileId PROFILE_ID = new ProviderProfileId("provider-profile-a");
    private static final Instant CREATED_AT = Instant.parse("2026-07-13T12:00:00Z");

    private InMemoryProviderProfileRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InMemoryProviderProfileRegistry();
    }

    @Test
    void firstRegistrationAndEquivalentDuplicateAreIdempotent() {
        final ProviderProfile original = profile();

        assertSame(original, registry.register(original));
        assertSame(original, registry.register(profile()));
        assertSame(original, registry.findById(PROFILE_ID.value()).orElseThrow());
    }

    @Test
    void sameIdWithDifferentProviderKindFailsClosedWithoutOverwrite() {
        final ProviderProfile original = registry.register(profile());
        final ProviderProfile conflicting = copy(original, ProviderKind.LOCAL_PLANNED, original.providerKey(),
                original.capabilitySummary(), original.status(), original.trustPolicyRef(), original.createdAt());

        assertMismatchAndOriginalPreserved(conflicting, original);
    }

    @Test
    void sameIdWithDifferentProviderKeyFailsClosedWithoutOverwrite() {
        final ProviderProfile original = registry.register(profile());
        final ProviderProfile conflicting = copy(original, original.providerKind(), "different-provider-key",
                original.capabilitySummary(), original.status(), original.trustPolicyRef(), original.createdAt());

        assertMismatchAndOriginalPreserved(conflicting, original);
    }

    @Test
    void sameIdWithDifferentCapabilityConfigurationFailsClosedWithoutOverwrite() {
        final ProviderProfile original = registry.register(profile());
        final ProviderProfile conflicting = copy(original, original.providerKind(), original.providerKey(),
                "different structured capability", original.status(), original.trustPolicyRef(),
                original.createdAt());

        assertMismatchAndOriginalPreserved(conflicting, original);
    }

    @Test
    void sameIdWithDifferentStatusFailsClosedWithoutOverwrite() {
        final ProviderProfile original = registry.register(profile());
        final ProviderProfile conflicting = copy(original, original.providerKind(), original.providerKey(),
                original.capabilitySummary(), ProviderProfileStatus.DISABLED, original.trustPolicyRef(),
                original.createdAt());

        assertMismatchAndOriginalPreserved(conflicting, original);
    }

    @Test
    void sameIdWithDifferentTrustPolicyFailsClosedWithoutOverwrite() {
        final ProviderProfile original = registry.register(profile());
        final ProviderProfile conflicting = copy(original, original.providerKind(), original.providerKey(),
                original.capabilitySummary(), original.status(), "different-trust-policy", original.createdAt());

        assertMismatchAndOriginalPreserved(conflicting, original);
    }

    @Test
    void sameIdWithDifferentCreatedAtFailsClosedWithoutOverwrite() {
        final ProviderProfile original = registry.register(profile());
        final ProviderProfile conflicting = copy(original, original.providerKind(), original.providerKey(),
                original.capabilitySummary(), original.status(), original.trustPolicyRef(),
                original.createdAt().plusSeconds(1));

        assertMismatchAndOriginalPreserved(conflicting, original);
    }

    private void assertMismatchAndOriginalPreserved(
            final ProviderProfile conflicting, final ProviderProfile original) {
        final IllegalStateException error =
                assertThrows(IllegalStateException.class, () -> registry.register(conflicting));

        assertEquals("provider profile bootstrap mismatch", error.getMessage());
        assertSame(original, registry.findById(PROFILE_ID.value()).orElseThrow());
    }

    private static ProviderProfile profile() {
        return new ProviderProfile(
                PROFILE_ID,
                "tenant-a",
                ProviderKind.MOCK,
                "qdr-mock-provider",
                "QDR mock provider",
                "readonly qdr evidence summary",
                ProviderProfileStatus.ENABLED,
                "qdr-b4-mock-provider-trust",
                CREATED_AT);
    }

    private static ProviderProfile copy(
            final ProviderProfile source,
            final ProviderKind providerKind,
            final String providerKey,
            final String capabilitySummary,
            final ProviderProfileStatus status,
            final String trustPolicyRef,
            final Instant createdAt) {
        return new ProviderProfile(
                source.id(),
                source.tenantId(),
                providerKind,
                providerKey,
                source.displayName(),
                capabilitySummary,
                status,
                trustPolicyRef,
                createdAt);
    }
}

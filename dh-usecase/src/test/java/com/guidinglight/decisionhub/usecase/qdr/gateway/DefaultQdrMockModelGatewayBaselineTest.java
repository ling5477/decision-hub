package com.guidinglight.decisionhub.usecase.qdr.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.qdr.model.ProviderKind;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderProfile;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderProfileStatus;
import com.guidinglight.decisionhub.usecase.qdr.model.InMemoryModelVersionRegistry;
import com.guidinglight.decisionhub.usecase.qdr.model.InMemoryPromptVersionRegistry;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionRecord;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionRecord;
import com.guidinglight.decisionhub.usecase.qdr.model.SaveModelVersionCommand;
import com.guidinglight.decisionhub.usecase.qdr.model.SavePromptVersionCommand;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * {@link DefaultQdrMockModelGatewayBaseline} 的确定性 profile bootstrap 回归。
 *
 * <p>测试只使用 in-memory registry 与无行为 persistence mock；baseline 没有真实 provider 或 HTTP
 * collaborator，不会访问 credential、NQ、Agent/LangGraph 或 LIVE 路径。
 */
final class DefaultQdrMockModelGatewayBaselineTest {

    private static final Instant BASELINE_CREATED_AT = Instant.parse("2026-07-13T12:00:00Z");

    @Test
    void repeatedPrepareUsesStableProfileAndReadsClockOnlyDuringConstruction() {
        final AdvancingClock clock = new AdvancingClock();
        final Fixture fixture = new Fixture(clock);

        final QdrModelGatewayBaseline first = fixture.baseline.prepare(command("first"));
        final ProviderProfile firstProfile = fixture.registeredProfile(first);
        final QdrModelGatewayBaseline second = fixture.baseline.prepare(command("second"));
        final ProviderProfile secondProfile = fixture.registeredProfile(second);

        assertEquals(first.providerProfileId(), second.providerProfileId());
        assertEquals(first.modelProfileId(), second.modelProfileId());
        assertEquals(first.modelVersionId(), second.modelVersionId());
        assertEquals(first.modelVersionChecksum(), second.modelVersionChecksum());
        assertEquals(ProviderKind.MOCK, second.providerKind());
        assertEquals(first.providerIdentityRef(), second.providerIdentityRef());
        assertEquals(first.policy(), second.policy());
        assertEquals(first.budget(), second.budget());
        assertSame(firstProfile, secondProfile);
        assertEquals(BASELINE_CREATED_AT, secondProfile.createdAt());
        assertEquals(ProviderProfileStatus.ENABLED, secondProfile.status());
        assertEquals("qdr-mock-provider", secondProfile.providerKey());
        assertEquals("qdr-b4-mock-provider-trust", secondProfile.trustPolicyRef());
        assertEquals(1, clock.invocationCount());
    }

    @Test
    void concurrentPrepareKeepsOneStrictlyEquivalentProviderProfile() throws Exception {
        final AdvancingClock clock = new AdvancingClock();
        final Fixture fixture = new Fixture(clock);
        final ExecutorService executor = Executors.newFixedThreadPool(8);
        final CountDownLatch ready = new CountDownLatch(8);
        final CountDownLatch start = new CountDownLatch(1);
        try {
            final List<Future<QdrModelGatewayBaseline>> futures = new ArrayList<>();
            for (int index = 0; index < 8; index++) {
                final int ordinal = index;
                futures.add(
                        executor.submit(
                                () -> {
                                    ready.countDown();
                                    if (!start.await(10, TimeUnit.SECONDS)) {
                                        throw new IllegalStateException("concurrent baseline start gate timed out");
                                    }
                                    return fixture.baseline.prepare(command("concurrent-" + ordinal));
                                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            final List<QdrModelGatewayBaseline> results = new ArrayList<>();
            for (final Future<QdrModelGatewayBaseline> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }

            final UUID providerProfileId = results.getFirst().providerProfileId();
            assertTrue(results.stream().allMatch(result -> result.providerProfileId().equals(providerProfileId)));
            assertTrue(
                    results.stream()
                            .allMatch(result -> result.modelVersionId().equals(results.getFirst().modelVersionId())));
            assertEquals(BASELINE_CREATED_AT, fixture.registeredProfile(results.getFirst()).createdAt());
            assertEquals(1, clock.invocationCount());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private static QdrModelGatewayBaselineCommand command(final String suffix) {
        return new QdrModelGatewayBaselineCommand(
                "tenant-a",
                "trace-" + suffix,
                "request-" + suffix,
                UUID.nameUUIDFromBytes(suffix.getBytes(StandardCharsets.UTF_8)));
    }

    private static final class Fixture {

        private final InMemoryProviderProfileRegistry providerProfiles =
                new InMemoryProviderProfileRegistry();
        private final DefaultQdrMockModelGatewayBaseline baseline;

        private Fixture(final Clock clock) {
            baseline =
                    new DefaultQdrMockModelGatewayBaseline(
                            new InMemoryPromptVersionRegistry(),
                            new InMemoryModelVersionRegistry(),
                            providerProfiles,
                            new NoStatePromptVersionPersistence(),
                            new NoStateModelVersionPersistence(),
                            clock);
        }

        private ProviderProfile registeredProfile(final QdrModelGatewayBaseline result) {
            return providerProfiles.findById(result.providerProfileId().toString()).orElseThrow();
        }
    }

    /** 每次读取都推进一秒，用于证明生产对象只在构造时读取一次。 */
    private static final class AdvancingClock extends Clock {

        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            if (!ZoneOffset.UTC.equals(zone)) {
                throw new IllegalArgumentException("test clock only supports UTC");
            }
            return this;
        }

        @Override
        public Instant instant() {
            return BASELINE_CREATED_AT.plusSeconds(invocations.getAndIncrement());
        }

        private int invocationCount() {
            return invocations.get();
        }
    }

    /** 无共享状态的 prompt persistence fake；只返回输入对应的安全 record。 */
    private static final class NoStatePromptVersionPersistence
            implements PromptVersionPersistencePort {

        @Override
        public PromptVersionRecord save(final SavePromptVersionCommand command) {
            return new PromptVersionRecord(
                    command.promptTemplateId(),
                    command.promptVersionId(),
                    command.tenantId(),
                    command.templateKey(),
                    command.version(),
                    command.renderPolicyKey(),
                    command.templateRef(),
                    command.templateHash(),
                    command.redactedSummary(),
                    command.status(),
                    command.checksum(),
                    command.createdAt(),
                    command.createdBy());
        }

        @Override
        public Optional<PromptVersionRecord> findByTenantAndTemplateVersion(
                final String tenantId, final UUID promptTemplateId, final String version) {
            return Optional.empty();
        }

        @Override
        public Optional<PromptVersionRecord> findByTenantAndPromptVersionId(
                final String tenantId, final UUID promptVersionId) {
            return Optional.empty();
        }
    }

    /** 无共享状态的 model persistence fake；只返回输入对应的安全 record。 */
    private static final class NoStateModelVersionPersistence implements ModelVersionPersistencePort {

        @Override
        public ModelVersionRecord save(final SaveModelVersionCommand command) {
            return new ModelVersionRecord(
                    command.modelProfileId(),
                    command.modelVersionId(),
                    command.providerProfileId(),
                    command.tenantId(),
                    command.providerKind(),
                    command.providerKey(),
                    command.modelKey(),
                    command.displayName(),
                    command.capabilitySummary(),
                    command.contextWindowTokens(),
                    command.maxOutputTokens(),
                    command.profileStatus(),
                    command.trustPolicyRef(),
                    command.modelName(),
                    command.modelVersion(),
                    command.checksum(),
                    command.createdAt());
        }

        @Override
        public Optional<ModelVersionRecord> findByTenantAndModelVersionId(
                final String tenantId, final UUID modelVersionId) {
            return Optional.empty();
        }
    }
}

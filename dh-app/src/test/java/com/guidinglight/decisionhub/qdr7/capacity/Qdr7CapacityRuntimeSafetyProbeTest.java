package com.guidinglight.decisionhub.qdr7.capacity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.guidinglight.decisionhub.security.nq.HmacNqDryRunAuthenticator;
import com.guidinglight.decisionhub.security.nq.InMemoryNonceReplayGuard;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunCommand;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunErrorCode;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunResult;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunRuntimeProperties;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunService;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunSnapshot;
import com.guidinglight.decisionhub.usecase.decision.dryrun.LimitedDryRunRuntimePolicy;
import com.guidinglight.decisionhub.usecase.decision.dryrun.NoSideEffectDecisionContract;
import com.guidinglight.decisionhub.usecase.decision.dryrun.PersistentGuardedDecisionDryRunService;
import com.guidinglight.decisionhub.usecase.decision.dryrun.RuntimeConcurrencyPolicy;
import com.guidinglight.decisionhub.usecase.decision.dryrun.RuntimeDeadlinePolicy;
import com.guidinglight.decisionhub.usecase.decision.dryrun.RuntimeEnvironmentPolicy;
import com.guidinglight.decisionhub.usecase.decision.dryrun.RuntimeFeatureFlag;
import com.guidinglight.decisionhub.usecase.decision.dryrun.RuntimeKillSwitch;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Qdr7CapacityRuntimeSafetyProbeTest {

  @Test
  void noArgProbeCannotBypassSpringBeanBinding() {
    assertThatExceptionOfType(NoSuchMethodException.class)
        .isThrownBy(() -> Qdr7CapacityRuntimeSafetyProbe.class.getDeclaredMethod("run"));
  }

  @Test
  void drivesActualBoundedQueueDeadlineAndInputCapWithoutSideEffects() throws Exception {
    final DecisionDryRunRuntimeProperties properties =
        new DecisionDryRunRuntimeProperties(
            true, false, false, true, Set.of("NQ_DRYRUN"), Set.of("tenant-a:NQ_DRYRUN"), 64);
    final HmacNqDryRunAuthenticator authenticator =
        new HmacNqDryRunAuthenticator(
            properties.allowedSources(),
            properties.allowedTenantSourcePairs(),
            "capacity-test-only-secret",
            Duration.ofMinutes(5),
            64L,
            new InMemoryNonceReplayGuard(
                16,
                Duration.ofMinutes(10),
                Clock.fixed(Instant.parse("2026-08-09T00:00:00Z"), ZoneOffset.UTC)));
    final Map<String, Object> result =
        Qdr7CapacityRuntimeSafetyProbe.runComponentOnly(
            policy(),
            authenticator,
            new MemoryBoundService(properties.memoryCapBytes()),
            properties,
            64L,
            "tenant-a",
            "NQ_DRYRUN",
            "unit-test");

    assertThat(result)
        .containsEntry("status", "PASS")
        .containsEntry("realHttpCalls", 0)
        .containsEntry("providerCalls", 0)
        .containsEntry("nqCalls", 0)
        .containsEntry("orders", 0)
        .containsEntry("feedbackLearningWrites", 0);
    @SuppressWarnings("unchecked")
    final Map<String, Object> queue = (Map<String, Object>) result.get("queueBackpressure");
    @SuppressWarnings("unchecked")
    final Map<String, Object> deadline = (Map<String, Object>) result.get("deadlineTimeout");
    @SuppressWarnings("unchecked")
    final Map<String, Object> input = (Map<String, Object>) result.get("inputMemoryCap");
    assertThat(queue)
        .containsEntry("aboveCapacity", "REJECTED_FAIL_CLOSED")
        .containsEntry("recovery", "PASS");
    assertThat(deadline)
        .containsEntry("deadlineExceeded", "DEADLINE_EXCEEDED")
        .containsEntry("applicationTimeout", "APPLICATION_TIMEOUT_PRESERVED")
        .containsEntry("harnessInterruption", "SEPARATE_CLASSIFICATION");
    assertThat(input)
        .containsEntry("belowCap", "PASS")
        .containsEntry("atBoundary", "PASS")
        .containsEntry("aboveCapBody", "REJECTED_FAIL_CLOSED")
        .containsEntry("fallbackBypass", 0);
  }

  @Test
  void springBindingRequiresPersistentGuardBoundedWrapperAndExactPolicyInstance() {
    final LimitedDryRunRuntimePolicy expected = policy();
    final PersistentGuardedDecisionDryRunService valid = persistent(expected);
    Qdr7CapacityRuntimeSafetyProbe.validateSpringBinding(expected, valid);

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(
            () ->
                Qdr7CapacityRuntimeSafetyProbe.validateSpringBinding(
                    expected, new MemoryBoundService(64)))
        .withMessageContaining("PersistentGuardedDecisionDryRunService");
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(
            () ->
                Qdr7CapacityRuntimeSafetyProbe.validateSpringBinding(
                    policy(), persistent(expected)))
        .withMessageContaining("policy binding drifted");
  }

  private static PersistentGuardedDecisionDryRunService persistent(
      final LimitedDryRunRuntimePolicy runtimePolicy) {
    return new PersistentGuardedDecisionDryRunService(
        new MemoryBoundService(64),
        org.mockito.Mockito.mock(
            com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyGuardPort.class),
        org.mockito.Mockito.mock(
            com.guidinglight.decisionhub.usecase.qdr.guard.GuardTransactionBoundary.class),
        org.mockito.Mockito.mock(
            com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository.class),
        org.mockito.Mockito.mock(
            com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunRequestFingerprint
                .class),
        org.mockito.Mockito.mock(
            com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunSafeResultProjector
                .class),
        new com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunGuardProperties(
            false, "", 0, 0, Duration.ZERO, Duration.ZERO, Duration.ZERO),
        Clock.systemUTC(),
        runtimePolicy);
  }

  private static LimitedDryRunRuntimePolicy policy() {
    return new LimitedDryRunRuntimePolicy(
        new RuntimeFeatureFlag(true),
        new RuntimeEnvironmentPolicy(Set.of("test"), false),
        new RuntimeKillSwitch(RuntimeKillSwitch.State.ALLOW),
        new RuntimeDeadlinePolicy(Duration.ofMillis(300), Duration.ofMillis(100)),
        new RuntimeConcurrencyPolicy(1, 1),
        LimitedDryRunRuntimePolicy.MOCK_PROVIDER_KIND,
        0,
        new NoSideEffectDecisionContract());
  }

  private static final class MemoryBoundService implements DecisionDryRunService {

    private final int memoryCapBytes;

    private MemoryBoundService(final int memoryCapBytes) {
      this.memoryCapBytes = memoryCapBytes;
    }

    @Override
    public DecisionDryRunResult execute(final DecisionDryRunCommand command) {
      if (command.context().approxBytes() > memoryCapBytes) {
        return reject(
            command, 500, DecisionDryRunErrorCode.MEMORY_LIMIT_EXCEEDED, "memory limit exceeded");
      }
      return DecisionDryRunResult.success(
          new DecisionDryRunSnapshot(
              "decision-1",
              true,
              "NO_TRADE",
              BigDecimal.ONE,
              "LOW",
              List.of("READ_ONLY"),
              List.of("runtime-policy=allow"),
              "replay:1",
              "audit:1",
              "1.0"));
    }

    @Override
    public DecisionDryRunResult reject(
        final DecisionDryRunCommand command,
        final int status,
        final DecisionDryRunErrorCode errorCode,
        final String message) {
      return DecisionDryRunResult.rejected(
          status, errorCode, message, command.requestId(), command.traceId(), "audit:reject");
    }
  }
}

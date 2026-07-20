package com.guidinglight.decisionhub.usecase.decision.dryrun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Stage-QDR-7 B3 runtime policy 的 fail-closed 单元测试。 */
class LimitedDryRunRuntimePolicyTest {

  @Test
  void featureFlagDefaultsAndExplicitDisableRejectBeforeOtherGates() {
    final LimitedDryRunRuntimePolicy policy =
        policy(
            false,
            Set.of("test"),
            false,
            RuntimeKillSwitch.State.ALLOW,
            5000,
            4,
            8,
            100,
            "MOCK",
            0);

    assertDenied(policy, RuntimeFailureClassification.RUNTIME_DISABLED);
  }

  @Test
  void onlyOneDevOrTestProfileIsAllowed() {
    assertTrue(policyForProfiles(Set.of("dev")).evaluate().allowed());
    assertTrue(policyForProfiles(Set.of("test")).evaluate().allowed());
    assertDenied(
        policyForProfiles(Set.of()), RuntimeFailureClassification.ENVIRONMENT_DENIED);
    assertDenied(
        policyForProfiles(Set.of("prod")), RuntimeFailureClassification.ENVIRONMENT_DENIED);
    assertDenied(
        policyForProfiles(Set.of("unknown")), RuntimeFailureClassification.ENVIRONMENT_DENIED);
    assertDenied(
        policyForProfiles(Set.of("dev", "test")),
        RuntimeFailureClassification.ENVIRONMENT_DENIED);
    assertDenied(
        policy(
            true,
            Set.of("test"),
            true,
            RuntimeKillSwitch.State.ALLOW,
            5000,
            4,
            8,
            100,
            "MOCK",
            0),
        RuntimeFailureClassification.ENVIRONMENT_DENIED);
  }

  @Test
  void everyNonAllowKillStateFailsClosed() {
    for (final RuntimeKillSwitch.State state : RuntimeKillSwitch.State.values()) {
      final LimitedDryRunRuntimePolicy policy =
          policy(true, Set.of("test"), false, state, 5000, 4, 8, 100, "MOCK", 0);
      if (state == RuntimeKillSwitch.State.ALLOW) {
        assertTrue(policy.evaluate().allowed());
      } else {
        assertDenied(policy, RuntimeFailureClassification.KILL_SWITCH_DENIED);
      }
    }
  }

  @Test
  void invalidDeadlineConcurrencyQueueAndQueueWaitFailClosed() {
    assertConfigurationDenied(policy(true, Set.of("test"), false, RuntimeKillSwitch.State.ALLOW, 0, 4, 8, 100, "MOCK", 0));
    assertConfigurationDenied(policy(true, Set.of("test"), false, RuntimeKillSwitch.State.ALLOW, 30001, 4, 8, 100, "MOCK", 0));
    assertConfigurationDenied(policy(true, Set.of("test"), false, RuntimeKillSwitch.State.ALLOW, 5000, 0, 8, 100, "MOCK", 0));
    assertConfigurationDenied(policy(true, Set.of("test"), false, RuntimeKillSwitch.State.ALLOW, 5000, 33, 8, 100, "MOCK", 0));
    assertConfigurationDenied(policy(true, Set.of("test"), false, RuntimeKillSwitch.State.ALLOW, 5000, 4, 65, 100, "MOCK", 0));
    assertConfigurationDenied(policy(true, Set.of("test"), false, RuntimeKillSwitch.State.ALLOW, 5000, 4, 8, 1001, "MOCK", 0));
    assertConfigurationDenied(policy(true, Set.of("test"), false, RuntimeKillSwitch.State.ALLOW, 100, 4, 8, 100, "MOCK", 0));
    assertConfigurationDenied(policy(true, Set.of("test"), false, RuntimeKillSwitch.State.ALLOW, 5000, 4, 0, 1, "MOCK", 0));
  }

  @Test
  void providerMustRemainMockAndRetryMustRemainZero() {
    assertDenied(
        policy(
            true,
            Set.of("test"),
            false,
            RuntimeKillSwitch.State.ALLOW,
            5000,
            4,
            8,
            100,
            "REAL",
            0),
        RuntimeFailureClassification.MOCK_PROVIDER_REQUIRED);
    assertDenied(
        policy(
            true,
            Set.of("test"),
            false,
            RuntimeKillSwitch.State.ALLOW,
            5000,
            4,
            8,
            100,
            "MOCK",
            1),
        RuntimeFailureClassification.MOCK_PROVIDER_REQUIRED);
  }

  @Test
  void noSideEffectContractAllowsOnlyReadonlySnapshotActions() {
    final NoSideEffectDecisionContract contract = new NoSideEffectDecisionContract();
    for (final String action : contract.allowedActions()) {
      assertTrue(contract.validate(success(action)).isEmpty());
    }
    for (final String action : contract.forbiddenActions()) {
      assertEquals(
          RuntimeFailureClassification.NO_SIDE_EFFECT_VIOLATION,
          contract.validate(success(action)).orElseThrow());
    }
    assertFalse(contract.forbiddenMutations().isEmpty());
    assertFalse(contract.forbiddenTransports().isEmpty());
  }

  private static LimitedDryRunRuntimePolicy policyForProfiles(final Set<String> profiles) {
    return policy(
        true,
        profiles,
        false,
        RuntimeKillSwitch.State.ALLOW,
        5000,
        4,
        8,
        100,
        "MOCK",
        0);
  }

  private static LimitedDryRunRuntimePolicy policy(
      final boolean enabled,
      final Set<String> profiles,
      final boolean productionEnabled,
      final RuntimeKillSwitch.State killState,
      final long deadlineMillis,
      final int maxConcurrency,
      final int queueCapacity,
      final long queueWaitMillis,
      final String providerKind,
      final int retryCount) {
    return new LimitedDryRunRuntimePolicy(
        new RuntimeFeatureFlag(enabled),
        new RuntimeEnvironmentPolicy(profiles, productionEnabled),
        new RuntimeKillSwitch(killState),
        new RuntimeDeadlinePolicy(
            Duration.ofMillis(deadlineMillis), Duration.ofMillis(queueWaitMillis)),
        new RuntimeConcurrencyPolicy(maxConcurrency, queueCapacity),
        providerKind,
        retryCount,
        new NoSideEffectDecisionContract());
  }

  private static void assertConfigurationDenied(final LimitedDryRunRuntimePolicy policy) {
    assertDenied(policy, RuntimeFailureClassification.RUNTIME_CONFIGURATION_INVALID);
  }

  private static void assertDenied(
      final LimitedDryRunRuntimePolicy policy,
      final RuntimeFailureClassification classification) {
    final LimitedDryRunRuntimePolicy.Evaluation evaluation = policy.evaluate();
    assertFalse(evaluation.allowed());
    assertEquals(classification, evaluation.failureClassification());
  }

  private static DecisionDryRunResult success(final String action) {
    return DecisionDryRunResult.success(
        new DecisionDryRunSnapshot(
            "decision-1",
            true,
            action,
            BigDecimal.ONE,
            "LOW",
            List.of("READ_ONLY"),
            List.of("runtime-policy=allow"),
            "replay:1",
            "audit:1",
            "1.0"));
  }
}

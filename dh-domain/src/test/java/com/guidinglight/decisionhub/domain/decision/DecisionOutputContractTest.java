package com.guidinglight.decisionhub.domain.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** GateK K1 domain contract tests for fail-closed decision output behavior. */
class DecisionOutputContractTest {

  private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

  @Test
  void noEvidenceDefaultsToAbstainWithMandatoryForbiddenActions() {
    final DecisionOutput output =
        DecisionOutput.abstainForNoEvidence("req-1", "trace-1", "tenant-a", NOW);

    assertEquals(DecisionType.READ_ONLY_RECOMMENDATION, output.getDecisionType());
    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertEquals(DecisionStatus.ABSTAINED, output.getStatus());
    assertEquals(DecisionRiskLevel.UNKNOWN, output.getRiskLevel());
    assertTrue(output.getForbiddenActions().containsAll(ForbiddenAction.mandatorySet()));
  }

  @Test
  void providerFailureDefaultsToAbstain() {
    final DecisionOutput output =
        DecisionOutput.abstainForProviderFailure(
            "req-1", "trace-1", "tenant-a", ProviderSignalStatus.FAILED, NOW);

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertEquals(ProviderSignalStatus.FAILED, output.getProviderStatus());
    assertTrue(output.getReasonCodes().contains("PROVIDER_UNAVAILABLE"));
  }

  @Test
  void policyDeniedIsBlockedAndAbstains() {
    final DecisionOutput output =
        DecisionOutput.blockedByPolicy(
            "req-1", "trace-1", "tenant-a", DecisionPolicyStatus.DENIED, NOW);

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertEquals(DecisionStatus.BLOCKED, output.getStatus());
    assertEquals(DecisionRiskLevel.BLOCKED, output.getRiskLevel());
    assertEquals(DecisionPolicyStatus.DENIED, output.getPolicyStatus());
  }

  @Test
  void highRiskForbidsDirectionalBias() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DecisionOutput.directionalBias(
                "req-1",
                "trace-1",
                "tenant-a",
                DecisionAction.LONG_BIAS,
                DecisionRiskLevel.HIGH,
                List.of("HIGH_RISK"),
                List.of("evidence-1"),
                NOW));
  }
}

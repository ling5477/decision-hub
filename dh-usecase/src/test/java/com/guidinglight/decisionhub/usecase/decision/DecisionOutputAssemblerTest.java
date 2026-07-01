package com.guidinglight.decisionhub.usecase.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyResult;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskReview;
import com.guidinglight.decisionhub.domain.decision.DecisionStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.decision.ForbiddenAction;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class DecisionOutputAssemblerTest {

  private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

  @Test
  void policyDeniedReturnsBlockedOutput() {
    final DecisionOutput output =
        new DecisionOutputAssembler()
            .policyDenied(
                validRequest(),
                new DecisionPolicyResult(DecisionPolicyStatus.DENIED, List.of("DENIED")),
                NOW);

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertEquals(DecisionStatus.BLOCKED, output.getStatus());
    assertEquals(DecisionPolicyStatus.DENIED, output.getPolicyStatus());
    assertEquals(ForbiddenAction.mandatorySet(), output.getForbiddenActions());
  }

  @Test
  void providerTimeoutReturnsAbstain() {
    final DecisionOutput output =
        new DecisionOutputAssembler()
            .assemble(
                context(),
                new DecisionPolicyResult(DecisionPolicyStatus.ALLOWED, List.of("OK")),
                DecisionSignalResult.failure(ProviderSignalStatus.TIMEOUT, "MOCK_PROVIDER_TIMEOUT"),
                new DecisionRiskReview(DecisionRiskLevel.UNKNOWN, List.of("PROVIDER_UNAVAILABLE")),
                NOW);

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertEquals(DecisionStatus.ABSTAINED, output.getStatus());
    assertEquals(ProviderSignalStatus.TIMEOUT, output.getProviderStatus());
  }

  @Test
  void highRiskPreventsDirectionalBias() {
    final DecisionOutput output =
        new DecisionOutputAssembler()
            .assemble(
                context(),
                new DecisionPolicyResult(DecisionPolicyStatus.ALLOWED, List.of("OK")),
                DecisionSignalResult.mock(DecisionAction.SHORT_BIAS, List.of("MOCK_SHORT_BIAS")),
                new DecisionRiskReview(DecisionRiskLevel.HIGH, List.of("HIGH_RISK")),
                NOW);

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertEquals(DecisionStatus.ABSTAINED, output.getStatus());
    assertEquals(DecisionRiskLevel.HIGH, output.getRiskLevel());
    assertTrue(output.getReasonCodes().contains("HIGH_RISK"));
  }

  @Test
  void nonDirectionalSignalReturnsObservationOnly() {
    final DecisionOutput output =
        new DecisionOutputAssembler()
            .assemble(
                context(),
                new DecisionPolicyResult(DecisionPolicyStatus.ALLOWED, List.of("OK")),
                DecisionSignalResult.mock(DecisionAction.NO_TRADE, List.of("MOCK_NO_TRADE")),
                new DecisionRiskReview(DecisionRiskLevel.LOW, List.of("RISK_OK")),
                NOW);

    assertEquals(DecisionAction.NO_TRADE, output.getAction());
    assertEquals(DecisionStatus.OBSERVATION_ONLY, output.getStatus());
    assertEquals(DecisionRiskLevel.LOW, output.getRiskLevel());
    assertEquals(ProviderSignalStatus.MOCKED, output.getProviderStatus());
    assertEquals(ForbiddenAction.mandatorySet(), output.getForbiddenActions());
  }

  private static DecisionContext context() {
    return new DefaultDecisionContextBuilder().build(validRequest());
  }

  private static DecisionRequest validRequest() {
    return DecisionRequest.readOnlyRecommendation(
        "req-1",
        "trace-1",
        "tenant-1",
        "codex-test",
        new DecisionSubject("BTC-USDT", "CRYPTO", "1h", "strategy-1", "research-1"),
        "context://safe",
        new DecisionContextSnapshot("snapshot-1", NOW, List.of("evidence://case-1")),
        NOW);
  }
}

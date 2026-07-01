package com.guidinglight.decisionhub.usecase.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import com.guidinglight.decisionhub.domain.decision.ForbiddenAction;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

final class DecisionOrchestratorTest {

  private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void defaultOrchestratorReturnsReadOnlyMockOutput() {
    final DecisionOrchestrator orchestrator = new DefaultDecisionOrchestrator();

    final DecisionOutput output = orchestrator.decide(validRequest(List.of("evidence://case-1")));

    assertEquals(DecisionType.READ_ONLY_RECOMMENDATION, output.getDecisionType());
    assertEquals(DecisionAction.NO_TRADE, output.getAction());
    assertEquals(DecisionStatus.OBSERVATION_ONLY, output.getStatus());
    assertEquals(DecisionPolicyStatus.ALLOWED, output.getPolicyStatus());
    assertEquals(ProviderSignalStatus.MOCKED, output.getProviderStatus());
    assertEquals(ForbiddenAction.mandatorySet(), output.getForbiddenActions());
    assertTrue(output.getReasonCodes().contains("MOCK_NO_TRADE"));
    assertFalse(output.getReasonCodes().contains("PLACE_ORDER"));
  }

  @Test
  void noEvidenceFailsClosedToAbstain() {
    final DecisionOrchestrator orchestrator = orchestratorWith(new MockDecisionSignalProvider());

    final DecisionOutput output = orchestrator.decide(validRequest(List.of()));

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertEquals(DecisionStatus.ABSTAINED, output.getStatus());
    assertEquals(DecisionRiskLevel.UNKNOWN, output.getRiskLevel());
    assertEquals(ProviderSignalStatus.NOT_CALLED, output.getProviderStatus());
    assertTrue(output.getReasonCodes().contains("NO_EVIDENCE"));
  }

  @Test
  void forbiddenExecutionIntentIsBlockedBeforeProvider() {
    final DecisionOrchestrator orchestrator = orchestratorWith(new MockDecisionSignalProvider());
    final DecisionRequest request =
        DecisionRequest.readOnlyRecommendation(
            "req-1",
            "trace-1",
            "tenant-1",
            "codex-test",
            new DecisionSubject("BTC-USDT", "CRYPTO", "1h", "placeOrder-plan", "research-1"),
            "context://safe",
            new DecisionContextSnapshot("snapshot-1", NOW, List.of("evidence://case-1")),
            NOW);

    final DecisionOutput output = orchestrator.decide(request);

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertEquals(DecisionStatus.BLOCKED, output.getStatus());
    assertEquals(DecisionPolicyStatus.DENIED, output.getPolicyStatus());
    assertEquals(ProviderSignalStatus.NOT_CALLED, output.getProviderStatus());
  }

  @Test
  void providerFailureFailsClosedToAbstain() {
    final DecisionOrchestrator orchestrator =
        orchestratorWith(
            new MockDecisionSignalProvider(
                ProviderSignalStatus.TIMEOUT,
                DecisionAction.ABSTAIN,
                List.of("MOCK_PROVIDER_TIMEOUT")));

    final DecisionOutput output = orchestrator.decide(validRequest(List.of("evidence://case-1")));

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertEquals(DecisionStatus.ABSTAINED, output.getStatus());
    assertEquals(DecisionRiskLevel.UNKNOWN, output.getRiskLevel());
    assertEquals(ProviderSignalStatus.TIMEOUT, output.getProviderStatus());
  }

  @Test
  void highRiskBlocksDirectionalBias() {
    final DecisionOrchestrator orchestrator =
        orchestratorWith(
            new MockDecisionSignalProvider(
                ProviderSignalStatus.MOCKED,
                DecisionAction.LONG_BIAS,
                List.of("MOCK_LONG_BIAS", "HIGH_RISK_SIGNAL")));

    final DecisionOutput output = orchestrator.decide(validRequest(List.of("evidence://case-1")));

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertEquals(DecisionStatus.ABSTAINED, output.getStatus());
    assertEquals(DecisionRiskLevel.HIGH, output.getRiskLevel());
    assertEquals(ProviderSignalStatus.NOT_CALLED, output.getProviderStatus());
    assertTrue(output.getReasonCodes().contains("HIGH_RISK"));
  }

  @Test
  void internalFailureReturnsStructuredAbstain() {
    final DecisionContextBuilder failingBuilder =
        request -> {
          throw new IllegalStateException("synthetic failure");
        };
    final DecisionOrchestrator orchestrator =
        new DefaultDecisionOrchestrator(
            failingBuilder,
            new DefaultDecisionPolicyChecker(),
            new MockDecisionSignalProvider(),
            new DefaultDecisionRiskReviewer(),
            new DecisionOutputAssembler(),
            CLOCK);

    final DecisionOutput output = orchestrator.decide(validRequest(List.of("evidence://case-1")));

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertEquals(DecisionStatus.ABSTAINED, output.getStatus());
    assertEquals(DecisionRiskLevel.UNKNOWN, output.getRiskLevel());
    assertTrue(output.getReasonCodes().contains("UNEXPECTED_FAILURE"));
  }

  @Test
  void nullRequestReturnsBlockedOutput() {
    final DecisionOrchestrator orchestrator = orchestratorWith(new MockDecisionSignalProvider());

    final DecisionOutput output = orchestrator.decide(null);

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertEquals(DecisionStatus.BLOCKED, output.getStatus());
    assertEquals(DecisionPolicyStatus.INVALID, output.getPolicyStatus());
    assertEquals("unknown-request", output.getRequestId());
  }

  private static DecisionOrchestrator orchestratorWith(final DecisionSignalProvider signalProvider) {
    return new DefaultDecisionOrchestrator(
        new DefaultDecisionContextBuilder(),
        new DefaultDecisionPolicyChecker(),
        signalProvider,
        new DefaultDecisionRiskReviewer(),
        new DecisionOutputAssembler(),
        CLOCK);
  }

  private static DecisionRequest validRequest(final List<String> evidenceRefs) {
    return DecisionRequest.readOnlyRecommendation(
        "req-1",
        "trace-1",
        "tenant-1",
        "codex-test",
        new DecisionSubject("BTC-USDT", "CRYPTO", "1h", "strategy-1", "research-1"),
        "context://safe",
        new DecisionContextSnapshot("snapshot-1", NOW, evidenceRefs),
        NOW);
  }
}

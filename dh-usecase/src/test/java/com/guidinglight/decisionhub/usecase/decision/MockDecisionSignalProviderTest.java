package com.guidinglight.decisionhub.usecase.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class MockDecisionSignalProviderTest {

  private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

  @Test
  void defaultProviderReturnsMockNoTrade() {
    final DecisionSignalResult signal = new MockDecisionSignalProvider().signal(context());

    assertEquals(ProviderSignalStatus.MOCKED, signal.status());
    assertEquals(DecisionAction.NO_TRADE, signal.action());
    assertEquals(List.of("MOCK_NO_TRADE"), signal.reasonCodes());
  }

  @Test
  void failureStatusRequiresAbstain() {
    final DecisionSignalResult signal =
        new MockDecisionSignalProvider(
                ProviderSignalStatus.UNTRUSTED,
                DecisionAction.ABSTAIN,
                List.of("MOCK_PROVIDER_UNTRUSTED"))
            .signal(context());

    assertEquals(ProviderSignalStatus.UNTRUSTED, signal.status());
    assertEquals(DecisionAction.ABSTAIN, signal.action());
    assertTrue(signal.requiresAbstain());
  }

  @Test
  void successStatusIsRejectedForK2MockProvider() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MockDecisionSignalProvider(
                ProviderSignalStatus.SUCCESS,
                DecisionAction.NO_TRADE,
                List.of("REAL_PROVIDER_SUCCESS_NOT_ALLOWED")));
  }

  private static DecisionContext context() {
    final DecisionRequest request =
        DecisionRequest.readOnlyRecommendation(
            "req-1",
            "trace-1",
            "tenant-1",
            "codex-test",
            new DecisionSubject("BTC-USDT", "CRYPTO", "1h", "strategy-1", "research-1"),
            "context://safe",
            new DecisionContextSnapshot("snapshot-1", NOW, List.of("evidence://case-1")),
            NOW);
    return new DefaultDecisionContextBuilder().build(request);
  }
}

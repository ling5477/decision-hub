package com.guidinglight.decisionhub.usecase.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyResult;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class DefaultDecisionPolicyCheckerTest {

  private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

  @Test
  void validRequestIsAllowed() {
    final DecisionPolicyResult result = new DefaultDecisionPolicyChecker().check(validRequest());

    assertEquals(DecisionPolicyStatus.ALLOWED, result.status());
    assertEquals(List.of("READ_ONLY_POLICY_OK"), result.reasonCodes());
  }

  @Test
  void nullRequestIsInvalid() {
    final DecisionPolicyResult result = new DefaultDecisionPolicyChecker().check(null);

    assertEquals(DecisionPolicyStatus.INVALID, result.status());
    assertEquals(List.of("MISSING_REQUEST"), result.reasonCodes());
  }

  @Test
  void forbiddenExecutionIntentIsDenied() {
    final DecisionRequest request =
        DecisionRequest.readOnlyRecommendation(
            "req-1",
            "trace-1",
            "tenant-1",
            "codex-test",
            new DecisionSubject("BTC-USDT", "CRYPTO", "1h", "strategy-1", "research-1"),
            "context://cancelOrder-attempt",
            new DecisionContextSnapshot("snapshot-1", NOW, List.of("evidence://case-1")),
            NOW);

    final DecisionPolicyResult result = new DefaultDecisionPolicyChecker().check(request);

    assertEquals(DecisionPolicyStatus.DENIED, result.status());
    assertEquals(List.of("FORBIDDEN_EXECUTION_INTENT"), result.reasonCodes());
  }

  @Test
  void k1FactoryRejectsMissingMandatoryFields() {
    assertThrows(
        NullPointerException.class,
        () ->
            DecisionRequest.readOnlyRecommendation(
                null,
                "trace-1",
                "tenant-1",
                "codex-test",
                new DecisionSubject("BTC-USDT", "CRYPTO", "1h", "strategy-1", "research-1"),
                "context://safe",
                new DecisionContextSnapshot("snapshot-1", NOW, List.of("evidence://case-1")),
                NOW));
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

package com.guidinglight.decisionhub.usecase.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskReview;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class DefaultDecisionRiskReviewerTest {

  private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

  @Test
  void noEvidenceIsUnknown() {
    final DecisionRiskReview review =
        new DefaultDecisionRiskReviewer()
            .review(context(List.of()), DecisionSignalResult.mock(DecisionAction.NO_TRADE, List.of()));

    assertEquals(DecisionRiskLevel.UNKNOWN, review.level());
    assertEquals(List.of("NO_EVIDENCE"), review.reasonCodes());
  }

  @Test
  void providerFailureIsUnknown() {
    final DecisionRiskReview review =
        new DefaultDecisionRiskReviewer()
            .review(
                context(List.of("evidence://case-1")),
                DecisionSignalResult.failure(ProviderSignalStatus.FAILED, "MOCK_PROVIDER_FAILED"));

    assertEquals(DecisionRiskLevel.UNKNOWN, review.level());
    assertEquals(List.of("PROVIDER_UNAVAILABLE"), review.reasonCodes());
  }

  @Test
  void highRiskMarkerIsHigh() {
    final DecisionRiskReview review =
        new DefaultDecisionRiskReviewer()
            .review(
                context(List.of("evidence://HIGH_RISK-case")),
                DecisionSignalResult.mock(DecisionAction.LONG_BIAS, List.of("MOCK_LONG_BIAS")));

    assertEquals(DecisionRiskLevel.HIGH, review.level());
    assertEquals(List.of("HIGH_RISK"), review.reasonCodes());
  }

  @Test
  void validMockSignalIsLowRisk() {
    final DecisionRiskReview review =
        new DefaultDecisionRiskReviewer()
            .review(
                context(List.of("evidence://case-1")),
                DecisionSignalResult.mock(DecisionAction.NO_TRADE, List.of("MOCK_NO_TRADE")));

    assertEquals(DecisionRiskLevel.LOW, review.level());
    assertEquals(List.of("RISK_OK"), review.reasonCodes());
  }

  private static DecisionContext context(final List<String> evidenceRefs) {
    final DecisionRequest request =
        DecisionRequest.readOnlyRecommendation(
            "req-1",
            "trace-1",
            "tenant-1",
            "codex-test",
            new DecisionSubject("BTC-USDT", "CRYPTO", "1h", "strategy-1", "research-1"),
            "context://safe",
            new DecisionContextSnapshot("snapshot-1", NOW, evidenceRefs),
            NOW);
    return new DefaultDecisionContextBuilder().build(request);
  }
}

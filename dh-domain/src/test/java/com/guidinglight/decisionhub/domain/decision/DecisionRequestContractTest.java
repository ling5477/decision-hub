package com.guidinglight.decisionhub.domain.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** GateK K1 domain contract tests for read-only decision requests. */
class DecisionRequestContractTest {

  @Test
  void readOnlyRecommendationFactoryLocksDecisionTypeAndSchemaVersion() {
    final DecisionRequest request =
        DecisionRequest.readOnlyRecommendation(
            "req-1",
            "trace-1",
            "tenant-a",
            "gatek-test",
            new DecisionSubject("BTC-USDT", "CRYPTO", "1h", "strategy-a", "research-a"),
            "ctx-1",
            new DecisionContextSnapshot("snap-1", Instant.parse("2026-07-01T00:00:00Z"), List.of()),
            Instant.parse("2026-07-01T00:00:01Z"));

    assertEquals(DecisionType.READ_ONLY_RECOMMENDATION, request.getDecisionType());
    assertEquals(DecisionRequest.DEFAULT_SCHEMA_VERSION, request.getSchemaVersion());
    assertNotNull(request.getSubject());
    assertNotNull(request.getContextSnapshot());
  }
}

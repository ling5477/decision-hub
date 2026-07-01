package com.guidinglight.decisionhub.usecase.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayContextView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayOutputView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayRequestView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayTimelineView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** K4 replay query service 回归测试，覆盖只读查询状态与 fail-closed 归一化。 */
final class DecisionReplayQueryServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

  @Test
  void replay_existingDecisionReturnsFoundView() {
    final RecordingReplayRepository repository = new RecordingReplayRepository(foundView("tenant-1"));
    final DecisionReplayQueryService service = new DefaultDecisionReplayQueryService(repository);

    final DecisionReplayView view = service.replay("tenant-1", "decision-1", "trace-1", "request-1");

    assertEquals(DecisionReplayStatus.FOUND, view.replayStatus());
    assertTrue(view.isComplete());
    assertEquals("tenant-1", repository.lastQuery.tenantId());
    assertEquals("decision-1", repository.lastQuery.decisionId());
  }

  @Test
  void replay_missingDecisionReturnsNotFound() {
    final DecisionReplayQueryService service =
        new DefaultDecisionReplayQueryService(
            new RecordingReplayRepository(DecisionReplayView.notFound("tenant-1", "decision-404")));

    final DecisionReplayView view = service.replay("tenant-1", "decision-404", null, null);

    assertEquals(DecisionReplayStatus.NOT_FOUND, view.replayStatus());
    assertFalse(view.isComplete());
  }

  @Test
  void replay_tenantMismatchReturnsNoData() {
    final DecisionReplayQueryService service =
        new DefaultDecisionReplayQueryService(new RecordingReplayRepository(foundView("tenant-other")));

    final DecisionReplayView view = service.replay("tenant-1", "decision-1", null, null);

    assertEquals(DecisionReplayStatus.TENANT_MISMATCH, view.replayStatus());
    assertEquals("tenant-1", view.tenantId());
    assertEquals("decision-1", view.decisionId());
    assertEquals(null, view.request());
    assertEquals(null, view.output());
  }

  @Test
  void replay_incompleteDataDoesNotPretendComplete() {
    final DecisionReplayQueryService service =
        new DefaultDecisionReplayQueryService(
            new RecordingReplayRepository(
                DecisionReplayView.incomplete(
                    "tenant-1", "decision-1", "trace-1", "request-1", List.of("OUTPUT_MISSING"))));

    final DecisionReplayView view = service.replay("tenant-1", "decision-1", null, null);

    assertEquals(DecisionReplayStatus.INCOMPLETE, view.replayStatus());
    assertFalse(view.isComplete());
    assertTrue(view.reasonCodes().contains("OUTPUT_MISSING"));
  }

  @Test
  void replay_corruptedDataIsPropagatedAsCorrupted() {
    final DecisionReplayQueryService service =
        new DefaultDecisionReplayQueryService(
            new RecordingReplayRepository(
                DecisionReplayView.corrupted("tenant-1", "decision-1", "REPLAY_DATA_CORRUPTED")));

    final DecisionReplayView view = service.replay("tenant-1", "decision-1", null, null);

    assertEquals(DecisionReplayStatus.CORRUPTED, view.replayStatus());
    assertFalse(view.isComplete());
  }

  @Test
  void replay_repositoryFailureReturnsBlocked() {
    final DecisionReplayQueryService service =
        new DefaultDecisionReplayQueryService(new ThrowingReplayRepository());

    final DecisionReplayView view = service.replay("tenant-1", "decision-1", null, null);

    assertEquals(DecisionReplayStatus.BLOCKED, view.replayStatus());
    assertTrue(view.reasonCodes().contains("REPLAY_QUERY_FAILED"));
  }

  @Test
  void replay_invalidInputReturnsBlockedInsteadOfThrowingRuntimeException() {
    final DecisionReplayQueryService service =
        new DefaultDecisionReplayQueryService(new RecordingReplayRepository(foundView("tenant-1")));

    final DecisionReplayView view = service.replay(" ", "decision-1", null, null);

    assertEquals(DecisionReplayStatus.BLOCKED, view.replayStatus());
    assertTrue(view.reasonCodes().contains("INVALID_REPLAY_QUERY"));
  }

  @Test
  void replay_traceOrRequestMismatchReturnsBlockedNoData() {
    final DecisionReplayQueryService service =
        new DefaultDecisionReplayQueryService(new RecordingReplayRepository(foundView("tenant-1")));

    final DecisionReplayView view = service.replay("tenant-1", "decision-1", "trace-other", null);

    assertEquals(DecisionReplayStatus.BLOCKED, view.replayStatus());
    assertTrue(view.reasonCodes().contains("TRACE_ID_MISMATCH"));
    assertEquals(null, view.request());
  }

  private static DecisionReplayView foundView(final String tenantId) {
    return DecisionReplayView.found(
        new DecisionReplayRequestView(
            "decision-1",
            "request-1",
            "trace-1",
            tenantId,
            "codex-test",
            DecisionType.READ_ONLY_RECOMMENDATION,
            Map.of("symbol", "BTC-USDT"),
            "context://safe",
            NOW,
            "1.0.0",
            NOW),
        new DecisionReplayContextView(
            "decision-1",
            tenantId,
            "trace-1",
            Map.of("snapshotPresent", true),
            List.of("evidence://case-1"),
            NOW),
        new DecisionReplayOutputView(
            "decision-1",
            tenantId,
            "trace-1",
            "request-1",
            DecisionType.READ_ONLY_RECOMMENDATION,
            DecisionAction.NO_TRADE,
            DecisionRiskLevel.LOW,
            DecisionPolicyStatus.ALLOWED,
            new BigDecimal("0.5000"),
            Map.of("action", "NO_TRADE"),
            NOW),
        DecisionReplayTimelineView.empty());
  }

  private static final class RecordingReplayRepository implements DecisionReplayQueryRepository {
    private final DecisionReplayView result;
    private DecisionReplayQuery lastQuery;

    private RecordingReplayRepository(final DecisionReplayView result) {
      this.result = result;
    }

    @Override
    public DecisionReplayView findReplay(final DecisionReplayQuery query) {
      this.lastQuery = query;
      return result;
    }
  }

  private static final class ThrowingReplayRepository implements DecisionReplayQueryRepository {
    @Override
    public DecisionReplayView findReplay(final DecisionReplayQuery query) {
      throw new IllegalStateException("synthetic read failure");
    }
  }
}

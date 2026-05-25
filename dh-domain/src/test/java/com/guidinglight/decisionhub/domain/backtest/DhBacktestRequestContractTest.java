package com.guidinglight.decisionhub.domain.backtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Stage2-PoC-B1：DH -> NQ 回测请求字段、状态机迁移、约束。 */
class DhBacktestRequestContractTest {

  private DhBacktestRequest aDraft() {
    return DhBacktestRequest.draft(
        "req-1",
        "trace-1",
        "cand-1",
        "MomentumLite",
        "v1.0",
        "{\"k\":1}",
        "entry-ref",
        "exit-ref",
        LocalDate.parse("2024-01-01"),
        LocalDate.parse("2025-12-31"),
        1_000_000.0,
        List.of("AAPL", "MSFT"),
        BacktestFrequency.DAILY,
        "alice",
        Instant.parse("2026-05-25T08:00:00Z"));
  }

  @Test
  void draft_initialStatusIsDraft() {
    final DhBacktestRequest r = aDraft();
    assertEquals(DhBacktestRequestStatus.DRAFT, r.getStatus());
    assertEquals("req-1", r.getRequestId());
    assertEquals("trace-1", r.getTraceId());
    assertEquals(BacktestFrequency.DAILY, r.getFrequency());
    assertEquals(List.of("AAPL", "MSFT"), r.getSymbols());
  }

  @Test
  void withStatus_returnsNewImmutableObject() {
    final DhBacktestRequest r = aDraft();
    final DhBacktestRequest queued = r.withStatus(DhBacktestRequestStatus.QUEUED);
    assertNotEquals(r.getStatus(), queued.getStatus());
    assertEquals(DhBacktestRequestStatus.DRAFT, r.getStatus());
    assertEquals(DhBacktestRequestStatus.QUEUED, queued.getStatus());
    assertEquals(r.getRequestId(), queued.getRequestId());
  }

  @Test
  void draft_rejectsInvalidArgs() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DhBacktestRequest.draft(
                "req",
                "t",
                "c",
                "n",
                "v",
                "{}",
                null,
                null,
                LocalDate.parse("2025-12-31"),
                LocalDate.parse("2024-01-01"),
                1.0,
                List.of("X"),
                BacktestFrequency.DAILY,
                "u",
                Instant.now()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DhBacktestRequest.draft(
                "req",
                "t",
                "c",
                "n",
                "v",
                "{}",
                null,
                null,
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2025-12-31"),
                0.0,
                List.of("X"),
                BacktestFrequency.DAILY,
                "u",
                Instant.now()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DhBacktestRequest.draft(
                "req",
                "t",
                "c",
                "n",
                "v",
                "{}",
                null,
                null,
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2025-12-31"),
                1.0,
                List.of(),
                BacktestFrequency.DAILY,
                "u",
                Instant.now()));
  }

  @Test
  void resultSnapshot_carriesRequestIdAndRaw() {
    final DhBacktestResultSnapshot snap =
        DhBacktestResultSnapshot.of(
            "res-1",
            "req-1",
            "trace-1",
            "cand-1",
            1.5,
            -0.1,
            0.25,
            0.6,
            1.8,
            LocalDate.parse("2024-01-01"),
            LocalDate.parse("2025-12-31"),
            BacktestVerdict.MARGINAL,
            Instant.parse("2026-05-25T08:00:00Z"),
            "{\"src\":\"nq\"}");
    assertEquals("req-1", snap.getRequestId());
    assertEquals(BacktestVerdict.MARGINAL, snap.getVerdict());
    assertEquals("{\"src\":\"nq\"}", snap.getRawPayloadJson());
  }

  @Test
  void status_enumCompleteness() {
    assertEquals(6, DhBacktestRequestStatus.values().length);
  }
}

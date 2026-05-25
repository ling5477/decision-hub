package com.guidinglight.decisionhub.domain.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.domain.backtest.BacktestVerdict;
import com.guidinglight.decisionhub.domain.feedback.payload.AlertLevel;
import com.guidinglight.decisionhub.domain.feedback.payload.BacktestResultReadyPayload;
import com.guidinglight.decisionhub.domain.feedback.payload.PaperRunAlertRaisedPayload;
import com.guidinglight.decisionhub.domain.feedback.payload.PaperRunCreatedPayload;
import com.guidinglight.decisionhub.domain.feedback.payload.PaperRunDailyReportGeneratedPayload;
import com.guidinglight.decisionhub.domain.feedback.payload.PaperRunRecoveryEventRecordedPayload;
import com.guidinglight.decisionhub.domain.feedback.payload.PaperRunStabilityCheckCompletedPayload;
import com.guidinglight.decisionhub.domain.feedback.payload.PaperRunStartedPayload;
import com.guidinglight.decisionhub.domain.feedback.payload.PaperRunStoppedPayload;
import com.guidinglight.decisionhub.domain.feedback.payload.StabilityCheckResult;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Stage2-PoC-B1：8 个 payload value object 字段与必填校验。 */
class NqFeedbackPayloadContractTest {

  @Test
  void paperRunCreated_keepsRequiredAndRaw() {
    final PaperRunCreatedPayload p =
        PaperRunCreatedPayload.of(
            "pr-1", "c-1", "S1", "alice", Instant.parse("2026-05-25T08:00:00Z"), "h-1", "{\"k\":1}");
    assertEquals("pr-1", p.getPaperRunId());
    assertEquals("c-1", p.getCandidateId());
    assertEquals("S1", p.getStrategyName());
    assertEquals("{\"k\":1}", p.getRawPayloadJson());
    assertThrows(
        NullPointerException.class,
        () ->
            PaperRunCreatedPayload.of(
                null, "c", "n", "u", Instant.now(), null, "{}"));
  }

  @Test
  void paperRunStarted_keepsRequiredAndRaw() {
    final PaperRunStartedPayload p =
        PaperRunStartedPayload.of("pr-1", Instant.now(), "CONTINUOUS", "{}");
    assertEquals("CONTINUOUS", p.getMode());
    assertNotNull(p.getRawPayloadJson());
  }

  @Test
  void paperRunStopped_keepsRequiredAndRaw() {
    final PaperRunStoppedPayload p =
        PaperRunStoppedPayload.of("pr-1", Instant.now(), "MANUAL", "{}");
    assertEquals("MANUAL", p.getReason());
  }

  @Test
  void paperRunDailyReport_allowsNullMetrics() {
    final PaperRunDailyReportGeneratedPayload p =
        PaperRunDailyReportGeneratedPayload.of(
            "pr-1", "r-1", LocalDate.parse("2026-05-25"), null, null, null, "{}");
    assertEquals(LocalDate.parse("2026-05-25"), p.getReportDate());
  }

  @Test
  void paperRunAlert_alertLevelEnum() {
    final PaperRunAlertRaisedPayload p =
        PaperRunAlertRaisedPayload.of(
            "pr-1", "a-1", AlertLevel.WARN, "RISK_THRESHOLD", "msg", Instant.now(), "{}");
    assertEquals(AlertLevel.WARN, p.getAlertLevel());
  }

  @Test
  void paperRunRecovery_required() {
    final PaperRunRecoveryEventRecordedPayload p =
        PaperRunRecoveryEventRecordedPayload.of(
            "pr-1", "rec-1", "RESTART_OK", Instant.now(), "{}");
    assertEquals("rec-1", p.getRecoveryEventId());
  }

  @Test
  void paperRunStability_resultEnum() {
    final PaperRunStabilityCheckCompletedPayload p =
        PaperRunStabilityCheckCompletedPayload.of(
            "pr-1", "ck-1", StabilityCheckResult.STABLE, "ok", Instant.now(), "{}");
    assertEquals(StabilityCheckResult.STABLE, p.getResult());
  }

  @Test
  void backtestResultReady_linksRequestIdAndCarriesVerdict() {
    final BacktestResultReadyPayload p =
        BacktestResultReadyPayload.of(
            "bt-1",
            "req-1",
            "c-1",
            1.5,
            -0.12,
            0.28,
            0.62,
            1.8,
            LocalDate.parse("2024-01-01"),
            LocalDate.parse("2025-12-31"),
            BacktestVerdict.PASS,
            Instant.now(),
            "{}");
    assertEquals("req-1", p.getRequestId());
    assertEquals(BacktestVerdict.PASS, p.getVerdict());
  }
}

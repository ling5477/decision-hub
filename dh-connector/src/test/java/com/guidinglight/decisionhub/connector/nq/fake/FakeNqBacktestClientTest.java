package com.guidinglight.decisionhub.connector.nq.fake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.connector.nq.NqBacktestSubmitResult;
import com.guidinglight.decisionhub.connector.nq.NqBacktestSubmitStatus;
import com.guidinglight.decisionhub.domain.backtest.BacktestFrequency;
import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Stage3-B3：FakeNqBacktestClient typed submit 行为契约测试。
 *
 * <p>覆盖 STAGE3_DH_BACKTEST_ADAPTER_SPEC §7.1 / §11.1。
 */
final class FakeNqBacktestClientTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-05-26T10:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

  @Test
  void happyPath_returnsAcceptedWithDeterministicJobId() {
    final FakeNqBacktestClient client = new FakeNqBacktestClient(FIXED_CLOCK);
    final DhBacktestRequest request = sampleRequest("req-001");

    final NqBacktestSubmitResult r1 = client.submit(request);
    final NqBacktestSubmitResult r2 = client.submit(request);

    assertEquals(NqBacktestSubmitStatus.ACCEPTED, r1.getStatus());
    assertEquals("req-001", r1.getRequestId());
    assertNotNull(r1.getJobId());
    assertTrue(r1.getJobId().startsWith("fake-job-"));
    assertEquals(FIXED_NOW, r1.getAcceptedAt());
    assertNull(r1.getErrorCode());
    assertNull(r1.getErrorMessage());

    // Deterministic: same requestId -> same jobId
    assertEquals(r1.getJobId(), r2.getJobId());
  }

  @Test
  void differentRequestId_yieldsDifferentJobId() {
    final FakeNqBacktestClient client = new FakeNqBacktestClient(FIXED_CLOCK);

    final NqBacktestSubmitResult ra = client.submit(sampleRequest("req-aaa"));
    final NqBacktestSubmitResult rb = client.submit(sampleRequest("req-bbb"));

    assertNotNull(ra.getJobId());
    assertNotNull(rb.getJobId());
    assertTrue(!ra.getJobId().equals(rb.getJobId()), "jobIds must differ for different requestIds");
  }

  @Test
  void nullRequest_throwsIllegalArgument() {
    final FakeNqBacktestClient client = new FakeNqBacktestClient(FIXED_CLOCK);
    assertThrows(IllegalArgumentException.class, () -> client.submit((DhBacktestRequest) null));
  }

  @Test
  void doesNotProduceResultReady_onlyAcceptedQueued() {
    // Stage3-B3 硬边界：Fake 路径不允许返回 RESULT_READY；仅 ACCEPTED。
    final FakeNqBacktestClient client = new FakeNqBacktestClient(FIXED_CLOCK);
    final NqBacktestSubmitResult r = client.submit(sampleRequest("req-no-result"));
    assertEquals(NqBacktestSubmitStatus.ACCEPTED, r.getStatus());
    assertSame(NqBacktestSubmitStatus.ACCEPTED, r.getStatus()); // never DUPLICATE/DISABLED/FAILED
  }

  @Test
  void defaultClockConstructor_yieldsNonNullAcceptedAt() {
    final FakeNqBacktestClient client = new FakeNqBacktestClient();
    final NqBacktestSubmitResult r = client.submit(sampleRequest("req-default-clock"));
    assertNotNull(r.getAcceptedAt());
  }

  private static DhBacktestRequest sampleRequest(final String requestId) {
    return DhBacktestRequest.draft(
        requestId,
        "stage3-trace-" + requestId,
        "stage3-cand-001",
        "test-strategy-momentum",
        "v1.0",
        "{\"window\":20}",
        null,
        null,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 4, 30),
        100000.00,
        List.of("TEST-SYM-A"),
        BacktestFrequency.DAILY,
        "stage3-test-runner",
        FIXED_NOW);
  }
}

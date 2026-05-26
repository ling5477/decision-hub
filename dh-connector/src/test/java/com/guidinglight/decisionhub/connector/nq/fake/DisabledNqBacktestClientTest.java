package com.guidinglight.decisionhub.connector.nq.fake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.connector.nq.NqBacktestSubmitResult;
import com.guidinglight.decisionhub.connector.nq.NqBacktestSubmitStatus;
import com.guidinglight.decisionhub.domain.backtest.BacktestFrequency;
import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Stage3-B3：DisabledNqBacktestClient 行为契约测试。
 *
 * <p>覆盖 STAGE3_DH_BACKTEST_ADAPTER_SPEC §7.2 / §11.1。
 */
final class DisabledNqBacktestClientTest {

  @Test
  void typedSubmit_returnsDisabledOutcome() {
    final DisabledNqBacktestClient client = new DisabledNqBacktestClient();

    final NqBacktestSubmitResult r = client.submit(sampleRequest("req-disabled-001"));

    assertEquals(NqBacktestSubmitStatus.DISABLED, r.getStatus());
    assertEquals("req-disabled-001", r.getRequestId());
    assertNull(r.getJobId());
    assertNull(r.getAcceptedAt());
    assertEquals(DisabledNqBacktestClient.ERROR_CODE_DH_DISABLED, r.getErrorCode());
  }

  @Test
  void typedSubmit_doesNotThrowRuntimeException() {
    final DisabledNqBacktestClient client = new DisabledNqBacktestClient();
    // The whole point: Disabled mode must not break ResearchRun main flow.
    final NqBacktestSubmitResult r = client.submit(sampleRequest("req-no-throw"));
    assertNotNull(r);
    assertEquals(NqBacktestSubmitStatus.DISABLED, r.getStatus());
  }

  @Test
  void nullRequest_throwsIllegalArgument() {
    final DisabledNqBacktestClient client = new DisabledNqBacktestClient();
    assertThrows(IllegalArgumentException.class, () -> client.submit((DhBacktestRequest) null));
  }

  @Test
  void mapStyleSubmit_returnsDisabledPlaceholder() {
    final DisabledNqBacktestClient client = new DisabledNqBacktestClient();
    final Map<String, Object> r = client.submit(Map.of("foo", "bar"));
    assertEquals("DISABLED", r.get("status"));
    assertEquals(DisabledNqBacktestClient.ERROR_CODE_DH_DISABLED, r.get("errorCode"));
  }

  @Test
  void getJob_returnsDisabledPlaceholder() {
    final DisabledNqBacktestClient client = new DisabledNqBacktestClient();
    final Map<String, Object> r = client.getJob("job-xyz");
    assertEquals("DISABLED", r.get("status"));
    assertEquals("job-xyz", r.get("jobId"));
  }

  private static DhBacktestRequest sampleRequest(final String requestId) {
    return DhBacktestRequest.draft(
        requestId,
        "stage3-trace",
        "stage3-cand-001",
        "test-strategy",
        "v1.0",
        "{}",
        null,
        null,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 4, 30),
        100000.00,
        List.of("TEST-SYM-A"),
        BacktestFrequency.DAILY,
        "stage3-test-runner",
        Instant.parse("2026-05-26T10:00:00Z"));
  }
}

package com.guidinglight.decisionhub.usecase.agent.backtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.connector.nq.NqBacktestClient;
import com.guidinglight.decisionhub.connector.nq.NqBacktestSubmitResult;
import com.guidinglight.decisionhub.connector.nq.fake.DisabledNqBacktestClient;
import com.guidinglight.decisionhub.connector.nq.fake.FakeNqBacktestClient;
import com.guidinglight.decisionhub.domain.backtest.BacktestFrequency;
import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequest;
import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequestStatus;
import com.guidinglight.decisionhub.usecase.agent.backtest.impl.DefaultDhBacktestRequestService;
import com.guidinglight.decisionhub.usecase.agent.backtest.inmemory.InMemoryDhBacktestRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Stage3-B3：{@link DhBacktestRequestService} 主路径测试。
 *
 * <p>覆盖 STAGE3_DH_BACKTEST_ADAPTER_SPEC §4 状态机 + §11.1 测试目标矩阵。
 */
final class DhBacktestRequestServiceTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-05-26T10:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

  @Test
  void happyPath_fakeAccepted_persistsAndReturnsResult() {
    final var repo = new InMemoryDhBacktestRequestRepository();
    final var service =
        new DefaultDhBacktestRequestService(repo, new FakeNqBacktestClient(FIXED_CLOCK), FIXED_CLOCK);

    final DhBacktestRequestResult result = service.submit(sampleCommand("cand-001"));

    assertEquals(DhBacktestRequestOutcome.FAKE_ACCEPTED, result.getOutcome());
    assertEquals(DhBacktestRequestStatus.ACCEPTED, result.getStatus());
    assertNotNull(result.getRequestId());
    assertNotNull(result.getJobId());
    assertTrue(result.getJobId().startsWith("fake-job-"));
    assertEquals(FIXED_NOW, result.getAcceptedAt());
    assertFalse(result.isRetryable());

    // 仓储一致性
    final var snapshot = repo.findByRequestId(result.getRequestId()).orElseThrow();
    assertEquals(DhBacktestRequestStatus.ACCEPTED, snapshot.getStatus());
    assertEquals(result.getJobId(), snapshot.getJobId());
  }

  @Test
  void validation_emptySymbols_returnsValidationFailed() {
    final var repo = new InMemoryDhBacktestRequestRepository();
    final var service =
        new DefaultDhBacktestRequestService(repo, new FakeNqBacktestClient(FIXED_CLOCK), FIXED_CLOCK);

    final DhBacktestRequestResult result =
        service.submit(
            DhBacktestRequestCommand.builder()
                .traceId("t")
                .correlationId("c")
                .candidateId("cand-empty")
                .strategyName("s")
                .strategyVersion("v")
                .strategyParametersJson("{}")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 4, 30))
                .initialCapital(100.0)
                .symbols(List.of())
                .frequency(BacktestFrequency.DAILY)
                .requestedBy("test")
                .build());

    assertEquals(DhBacktestRequestOutcome.FAILED, result.getOutcome());
    assertEquals(DhBacktestRequestErrorCode.DH_VALIDATION_FAILED, result.getErrorCode());
  }

  @Test
  void disabledClient_returnsDisabledOutcome_doesNotFailResearchRun() {
    final var repo = new InMemoryDhBacktestRequestRepository();
    final var service =
        new DefaultDhBacktestRequestService(repo, new DisabledNqBacktestClient(), FIXED_CLOCK);

    final DhBacktestRequestResult result = service.submit(sampleCommand("cand-disabled"));

    assertEquals(DhBacktestRequestOutcome.DISABLED, result.getOutcome());
    assertEquals(DhBacktestRequestErrorCode.DH_DISABLED, result.getErrorCode());
    assertFalse(result.isRetryable());
    assertNull(result.getJobId());
    // 重要：state 不是 FAILED；ResearchRun 主流程不应感知为失败
    assertEquals(DhBacktestRequestStatus.QUEUED, result.getStatus());
  }

  @Test
  void clientThrowsRuntime_mappedToFailedNotPropagated() {
    final var repo = new InMemoryDhBacktestRequestRepository();
    final NqBacktestClient throwing =
        new NqBacktestClient() {
          @Override
          public Map<String, Object> submit(final Map<String, Object> request) {
            throw new RuntimeException("boom");
          }

          @Override
          public Map<String, Object> getJob(final String jobId) {
            throw new UnsupportedOperationException();
          }

          @Override
          public NqBacktestSubmitResult submit(final DhBacktestRequest request) {
            throw new RuntimeException("boom typed");
          }
        };
    final var service = new DefaultDhBacktestRequestService(repo, throwing, FIXED_CLOCK);

    final DhBacktestRequestResult result = service.submit(sampleCommand("cand-throw"));

    assertEquals(DhBacktestRequestOutcome.FAILED, result.getOutcome());
    assertEquals(DhBacktestRequestErrorCode.NETWORK, result.getErrorCode());
    // RuntimeException 不传播，不阻塞 caller
    assertNotNull(result.getRequestId());
  }

  @Test
  void clientReturnsNull_mappedToProtocolViolation() {
    final var repo = new InMemoryDhBacktestRequestRepository();
    final NqBacktestClient nullClient =
        new NqBacktestClient() {
          @Override
          public Map<String, Object> submit(final Map<String, Object> request) {
            return null;
          }

          @Override
          public Map<String, Object> getJob(final String jobId) {
            return null;
          }

          @Override
          public NqBacktestSubmitResult submit(final DhBacktestRequest request) {
            return null;
          }
        };
    final var service = new DefaultDhBacktestRequestService(repo, nullClient, FIXED_CLOCK);
    final DhBacktestRequestResult result = service.submit(sampleCommand("cand-null"));
    assertEquals(DhBacktestRequestErrorCode.PROTOCOL_VIOLATION, result.getErrorCode());
  }

  static DhBacktestRequestCommand sampleCommand(final String candidateId) {
    return DhBacktestRequestCommand.builder()
        .traceId("stage3-trace-" + candidateId)
        .correlationId("stage3-corr-" + candidateId)
        .candidateId(candidateId)
        .strategyName("test-strategy-momentum")
        .strategyVersion("v1.0")
        .strategyParametersJson("{\"window\":20}")
        .startDate(LocalDate.of(2026, 1, 1))
        .endDate(LocalDate.of(2026, 4, 30))
        .initialCapital(100000.0)
        .symbols(List.of("TEST-SYM-A"))
        .frequency(BacktestFrequency.DAILY)
        .requestedBy("stage3-test-runner")
        .build();
  }
}

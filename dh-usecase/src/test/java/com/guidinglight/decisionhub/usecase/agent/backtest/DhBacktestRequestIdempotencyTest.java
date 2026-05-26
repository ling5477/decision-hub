package com.guidinglight.decisionhub.usecase.agent.backtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.guidinglight.decisionhub.connector.nq.fake.FakeNqBacktestClient;
import com.guidinglight.decisionhub.domain.backtest.BacktestFrequency;
import com.guidinglight.decisionhub.usecase.agent.backtest.impl.DefaultDhBacktestRequestService;
import com.guidinglight.decisionhub.usecase.agent.backtest.inmemory.InMemoryDhBacktestRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Stage3-B3：DH 端 24h paramsHash 幂等短路测试。
 *
 * <p>覆盖 STAGE3_DH_BACKTEST_ADAPTER_SPEC §8.1 / §11.1。
 */
final class DhBacktestRequestIdempotencyTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-05-26T10:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

  @Test
  void sameParamsHashWithin24h_returnsIdempotentShortCircuit() {
    final var repo = new InMemoryDhBacktestRequestRepository();
    final var service =
        new DefaultDhBacktestRequestService(repo, new FakeNqBacktestClient(FIXED_CLOCK), FIXED_CLOCK);
    final var cmd = sampleCommand();

    final DhBacktestRequestResult first = service.submit(cmd);
    final DhBacktestRequestResult second = service.submit(cmd);

    assertEquals(DhBacktestRequestOutcome.FAKE_ACCEPTED, first.getOutcome());
    assertEquals(DhBacktestRequestOutcome.IDEMPOTENT_SHORT_CIRCUIT, second.getOutcome());
    // 第二次返回原 requestId
    assertEquals(first.getRequestId(), second.getRequestId());
    assertEquals(first.getJobId(), second.getJobId());
  }

  @Test
  void differentParamsHash_yieldsNewRequest() {
    final var repo = new InMemoryDhBacktestRequestRepository();
    final var service =
        new DefaultDhBacktestRequestService(repo, new FakeNqBacktestClient(FIXED_CLOCK), FIXED_CLOCK);

    final DhBacktestRequestResult a =
        service.submit(
            DhBacktestRequestCommand.builder()
                .traceId("t")
                .correlationId("c")
                .candidateId("cand-001")
                .strategyName("s")
                .strategyVersion("v1.0")
                .strategyParametersJson("{\"window\":20}")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 4, 30))
                .initialCapital(100000.0)
                .symbols(List.of("TEST-SYM-A"))
                .frequency(BacktestFrequency.DAILY)
                .requestedBy("test")
                .build());
    final DhBacktestRequestResult b =
        service.submit(
            DhBacktestRequestCommand.builder()
                .traceId("t")
                .correlationId("c")
                .candidateId("cand-001")
                .strategyName("s")
                .strategyVersion("v1.0")
                .strategyParametersJson("{\"window\":40}") // 不同参数
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 4, 30))
                .initialCapital(100000.0)
                .symbols(List.of("TEST-SYM-A"))
                .frequency(BacktestFrequency.DAILY)
                .requestedBy("test")
                .build());

    assertNotNull(a.getRequestId());
    assertNotNull(b.getRequestId());
    assertNotEquals(a.getRequestId(), b.getRequestId());
    assertEquals(DhBacktestRequestOutcome.FAKE_ACCEPTED, a.getOutcome());
    assertEquals(DhBacktestRequestOutcome.FAKE_ACCEPTED, b.getOutcome());
  }

  @Test
  void afterWindow_newRequestGenerated() {
    // 第一次提交在 NOW；第二次提交在 NOW + 25h（超 24h 窗口）
    final var repo = new InMemoryDhBacktestRequestRepository();
    final Clock c1 = FIXED_CLOCK;
    final Clock c2 = Clock.fixed(FIXED_NOW.plusSeconds(25 * 3600L), ZoneOffset.UTC);

    final var svc1 = new DefaultDhBacktestRequestService(repo, new FakeNqBacktestClient(c1), c1);
    final var svc2 = new DefaultDhBacktestRequestService(repo, new FakeNqBacktestClient(c2), c2);
    final var cmd = sampleCommand();

    final DhBacktestRequestResult first = svc1.submit(cmd);
    final DhBacktestRequestResult second = svc2.submit(cmd);

    assertEquals(DhBacktestRequestOutcome.FAKE_ACCEPTED, first.getOutcome());
    assertEquals(DhBacktestRequestOutcome.FAKE_ACCEPTED, second.getOutcome());
    assertNotEquals(first.getRequestId(), second.getRequestId());
  }

  private static DhBacktestRequestCommand sampleCommand() {
    return DhBacktestRequestCommand.builder()
        .traceId("stage3-trace-id")
        .correlationId("stage3-corr-id")
        .candidateId("cand-idem-001")
        .strategyName("test-strategy")
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

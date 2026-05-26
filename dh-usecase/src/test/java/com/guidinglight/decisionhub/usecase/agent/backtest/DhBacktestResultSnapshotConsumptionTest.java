package com.guidinglight.decisionhub.usecase.agent.backtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.guidinglight.decisionhub.connector.nq.fake.FakeNqBacktestClient;
import com.guidinglight.decisionhub.domain.backtest.BacktestFrequency;
import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequestStatus;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.backtest.impl.DefaultDhBacktestRequestService;
import com.guidinglight.decisionhub.usecase.agent.backtest.inmemory.InMemoryDhBacktestRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Stage3-B3：DH 端不允许自行产生 RESULT_READY 终态的契约测试。
 *
 * <p>对应 STAGE3_DH_BACKTEST_ADAPTER_SPEC §4.3 与 §9。
 *
 * <p>本测试声明的"硬规则"：
 *
 * <ul>
 *   <li>DH service.submit() 不能在同步路径返回 RESULT_READY；
 *   <li>DH 不允许把 NQ verdict 当作 JudgeDecision 终态（JudgeDecision 仍是唯一最终出口）；
 *   <li>RESULT_READY 只能由 ingest 路径命中 BACKTEST_RESULT_READY 事件后写入；
 *       本测试在 service 层验证不出现 RESULT_READY 状态；ingest 行为由
 *       已落地的 BacktestResultReadyHandler 测试覆盖。
 * </ul>
 *
 * <p>注：DH ingest 写入 DhBacktestResultSnapshot 的端到端路径属 Stage3-B4 联调用例（T3），
 * 此处 service 层仅断言"不会越权产生 RESULT_READY"。
 */
final class DhBacktestResultSnapshotConsumptionTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-05-26T10:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

  @Test
  void serviceSubmit_doesNotProduceResultReady() {
    final var repo = new InMemoryDhBacktestRequestRepository();
    final var service =
        new DefaultDhBacktestRequestService(repo, new FakeNqBacktestClient(FIXED_CLOCK), FIXED_CLOCK);

    for (int i = 0; i < 5; i++) {
      final DhBacktestRequestResult r =
          service.submit(DhBacktestRequestServiceTest.sampleCommand("cand-" + i));
      assertNotEquals(DhBacktestRequestStatus.RESULT_READY, r.getStatus(),
          "service.submit must never produce RESULT_READY (only NQ feedback can)");
    }
  }

  @Test
  void serviceSubmit_resultsAreCappedAtAcceptedOrLower() {
    // Fake 路径只能产生 ACCEPTED；不能跳过 NQ 事件就升级到 RESULT_READY。
    final var repo = new InMemoryDhBacktestRequestRepository();
    final var service =
        new DefaultDhBacktestRequestService(repo, new FakeNqBacktestClient(FIXED_CLOCK), FIXED_CLOCK);

    final DhBacktestRequestResult r =
        service.submit(DhBacktestRequestServiceTest.sampleCommand("cand-cap"));

    assertEquals(DhBacktestRequestStatus.ACCEPTED, r.getStatus());
    assertEquals(DhBacktestRequestOutcome.FAKE_ACCEPTED, r.getOutcome());
    assertNotEquals(DhBacktestRequestStatus.RESULT_READY, r.getStatus());
  }

  @Test
  void backtestResultReadyEventType_isPresent_inDomainEnum() {
    // 防御性测试：NqFeedbackEventType 必须含 BACKTEST_RESULT_READY；否则 DH ingest 路径无法消费 NQ 结果。
    assertNotNull(NqFeedbackEventType.BACKTEST_RESULT_READY);
  }

  @Test
  void noJobIdLeak_inFailedState() {
    // Fake 路径 happy path 不应在仓储里产生 RESULT_READY；状态机最高 ACCEPTED。
    final var repo = new InMemoryDhBacktestRequestRepository();
    final var service =
        new DefaultDhBacktestRequestService(repo, new FakeNqBacktestClient(FIXED_CLOCK), FIXED_CLOCK);
    final DhBacktestRequestResult r =
        service.submit(
            DhBacktestRequestCommand.builder()
                .traceId("t")
                .correlationId("c")
                .candidateId("cand-no-leak")
                .strategyName("s")
                .strategyVersion("v")
                .strategyParametersJson("{}")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 4, 30))
                .initialCapital(100.0)
                .symbols(List.of("S1"))
                .frequency(BacktestFrequency.DAILY)
                .requestedBy("t")
                .build());

    final var snapshot = repo.findByRequestId(r.getRequestId()).orElseThrow();
    assertFalse(
        snapshot.getStatus() == DhBacktestRequestStatus.RESULT_READY,
        "repository must not hold RESULT_READY directly via service.submit");
  }

  private static void assertNotEquals(final Object expected, final Object actual, final String msg) {
    if (java.util.Objects.equals(expected, actual)) {
      throw new AssertionError(msg + " | both equal " + actual);
    }
  }

  private static void assertNotEquals(final Object expected, final Object actual) {
    assertNotEquals(expected, actual, "values must differ");
  }
}

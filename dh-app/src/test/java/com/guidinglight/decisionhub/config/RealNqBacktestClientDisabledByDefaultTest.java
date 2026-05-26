package com.guidinglight.decisionhub.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.guidinglight.decisionhub.connector.nq.NqBacktestClient;
import com.guidinglight.decisionhub.connector.nq.fake.DisabledNqBacktestClient;
import com.guidinglight.decisionhub.connector.nq.fake.FakeNqBacktestClient;
import com.guidinglight.decisionhub.usecase.agent.backtest.DhBacktestRequestRepository;
import com.guidinglight.decisionhub.usecase.agent.backtest.DhBacktestRequestService;
import com.guidinglight.decisionhub.usecase.agent.backtest.impl.DefaultDhBacktestRequestService;
import com.guidinglight.decisionhub.usecase.agent.backtest.inmemory.InMemoryDhBacktestRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Stage3-B3：默认配置下绝对不能装配 RealNqBacktestClient 的契约测试。
 *
 * <p>对应 STAGE3_DH_BACKTEST_ADAPTER_SPEC §11.1 / §2.2 装配真值表。
 *
 * <p>本测试覆盖三层 gate 装配真值表的四种 case：
 *
 * <ol>
 *   <li>默认（所有 properties 未设置）→ FakeNqBacktestClient
 *   <li>stage3.nq.enabled=true && backtest-request.enabled=false → DisabledNqBacktestClient
 *   <li>stage3.nq.enabled=true && backtest-request.enabled=true && fake-mode=true → Fake
 *   <li>stage3.nq.enabled=true && backtest-request.enabled=true && fake-mode=false →
 *       Stage3-B3 本轮不实现 RealNqBacktestClient，仍走 Fake 兜底；断言不会出现"未知 NqBacktestClient 实现"
 * </ol>
 */
final class RealNqBacktestClientDisabledByDefaultTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(Stage3NqBacktestWiringConfig.class);

  @Test
  void defaultProfile_picksFakeClient() {
    runner.run(
        ctx -> {
          assertThat(ctx).hasSingleBean(NqBacktestClient.class);
          assertThat(ctx.getBean(NqBacktestClient.class)).isInstanceOf(FakeNqBacktestClient.class);
          assertThat(ctx).hasSingleBean(DhBacktestRequestService.class);
          assertThat(ctx.getBean(DhBacktestRequestService.class))
              .isInstanceOf(DefaultDhBacktestRequestService.class);
          assertThat(ctx).hasSingleBean(DhBacktestRequestRepository.class);
          assertThat(ctx.getBean(DhBacktestRequestRepository.class))
              .isInstanceOf(InMemoryDhBacktestRequestRepository.class);
        });
  }

  @Test
  void stage3EnabledButBacktestDisabled_picksDisabledClient() {
    runner
        .withPropertyValues(
            "decisionhub.stage3.nq.enabled=true",
            "decisionhub.stage3.nq.backtest-request.enabled=false")
        .run(
            ctx -> {
              assertThat(ctx).hasSingleBean(NqBacktestClient.class);
              assertThat(ctx.getBean(NqBacktestClient.class))
                  .isInstanceOf(DisabledNqBacktestClient.class);
            });
  }

  @Test
  void stage3EnabledAndBacktestEnabledFakeMode_picksFakeClient() {
    runner
        .withPropertyValues(
            "decisionhub.stage3.nq.enabled=true",
            "decisionhub.stage3.nq.backtest-request.enabled=true",
            "decisionhub.stage3.nq.backtest-request.fake-mode=true")
        .run(
            ctx ->
                assertThat(ctx.getBean(NqBacktestClient.class)).isInstanceOf(FakeNqBacktestClient.class));
  }

  @Test
  void stage3EnabledFakeModeFalse_stillFallsBackToFake_inB3() {
    // Stage3-B3 本轮未实现 RealNqBacktestClient；fake-mode=false 仍走 Fake 兜底
    runner
        .withPropertyValues(
            "decisionhub.stage3.nq.enabled=true",
            "decisionhub.stage3.nq.backtest-request.enabled=true",
            "decisionhub.stage3.nq.backtest-request.fake-mode=false")
        .run(
            ctx ->
                assertThat(ctx.getBean(NqBacktestClient.class)).isInstanceOf(FakeNqBacktestClient.class));
  }
}

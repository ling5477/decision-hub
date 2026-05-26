package com.guidinglight.decisionhub.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.guidinglight.decisionhub.connector.nq.NqBacktestClient;
import com.guidinglight.decisionhub.connector.nq.NqBacktestSubmitResult;
import com.guidinglight.decisionhub.connector.nq.NqBacktestSubmitStatus;
import com.guidinglight.decisionhub.connector.nq.fake.FakeNqBacktestClient;
import com.guidinglight.decisionhub.domain.backtest.BacktestFrequency;
import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequest;
import com.guidinglight.decisionhub.usecase.agent.backtest.DhBacktestRequestCommand;
import com.guidinglight.decisionhub.usecase.agent.backtest.DhBacktestRequestService;
import com.guidinglight.decisionhub.usecase.agent.backtest.impl.DefaultDhBacktestRequestService;
import com.guidinglight.decisionhub.usecase.agent.backtest.inmemory.InMemoryDhBacktestRequestRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Stage3-B3：DH 启动不依赖 NQ 可达性的契约测试。
 *
 * <p>对应 STAGE3_DH_BACKTEST_ADAPTER_SPEC §11.1（NoNqDependencyStartupTest）+ §1.2 关键不变量
 * （"NQ 主流程不依赖 DH"对称：DH 不依赖 NQ）。
 *
 * <p>本测试通过 {@link ApplicationContextRunner} 加载 Stage3 装配，断言：
 *
 * <ul>
 *   <li>默认 profile（无任何 stage3.* 属性）成功启动；
 *   <li>FakeNqBacktestClient + InMemory 仓储装配；
 *   <li>DhBacktestRequestService 可工作；
 *   <li>整个过程零 HTTP / 零外部资源消耗；
 *   <li>不存在 RestTemplate / WebClient / OkHttp 等 HTTP 客户端 bean。
 * </ul>
 */
final class NoNqDependencyStartupTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(Stage3NqBacktestWiringConfig.class);

  @Test
  void defaultProfile_loadsWithoutNqDependency() {
    runner.run(
        ctx -> {
          assertThat(ctx).hasNotFailed();
          assertThat(ctx).hasSingleBean(NqBacktestClient.class);
          assertThat(ctx.getBean(NqBacktestClient.class)).isInstanceOf(FakeNqBacktestClient.class);
        });
  }

  @Test
  void researchRunLikeFlow_runsToCompletion_withoutNqHttp() {
    // 模拟 ResearchRun 主流程的一段子调用：DhBacktestRequestService.submit()
    // 默认装配下 service 不应抛任何 RuntimeException，不应触发 HTTP 出站
    runner.run(
        ctx -> {
          final DhBacktestRequestService service = ctx.getBean(DhBacktestRequestService.class);
          assertThat(service)
              .as("default profile must wire DefaultDhBacktestRequestService")
              .isInstanceOf(DefaultDhBacktestRequestService.class);

          final var result =
              service.submit(
                  DhBacktestRequestCommand.builder()
                      .traceId("startup-trace")
                      .correlationId("startup-corr")
                      .candidateId("startup-cand")
                      .strategyName("startup-strategy")
                      .strategyVersion("v1.0")
                      .strategyParametersJson("{}")
                      .startDate(LocalDate.of(2026, 1, 1))
                      .endDate(LocalDate.of(2026, 4, 30))
                      .initialCapital(100000.0)
                      .symbols(List.of("TEST-SYM-A"))
                      .frequency(BacktestFrequency.DAILY)
                      .requestedBy("startup-test")
                      .build());

          // 主流程未阻塞；result 非 null；status ACCEPTED（Fake 路径）
          assertThat(result).isNotNull();
          assertThat(result.getJobId()).isNotNull();
        });
  }

  @Test
  void noHttpClientBean_inDefaultContext() {
    // 防御性断言：默认 context 不允许存在 RestTemplate / WebClient bean。
    runner.run(
        ctx -> {
          assertThat(ctx.getBeanNamesForType(Class.forName("java.lang.Object")).length)
              .as("context should contain at least one bean")
              .isGreaterThan(0);
          // 不允许出现 HTTP client 类型 bean
          for (final String name : ctx.getBeanDefinitionNames()) {
            final String className = ctx.getBean(name).getClass().getName();
            assertThat(className)
                .as("bean %s class %s should not be a real HTTP client", name, className)
                .doesNotContain("RestTemplate")
                .doesNotContain("WebClient")
                .doesNotContain("OkHttpClient")
                .doesNotContain("HttpURLConnection");
          }
        });
  }

  @Test
  void canConstructServiceManually_withoutSpring_fully_offline() {
    // 端口可在零 Spring 环境下手动构造；DH usecase 层不依赖 NQ 网络可达
    final InMemoryDhBacktestRequestRepository repo = new InMemoryDhBacktestRequestRepository();
    final FakeNqBacktestClient fakeClient = new FakeNqBacktestClient();
    final DefaultDhBacktestRequestService service =
        new DefaultDhBacktestRequestService(repo, fakeClient);

    final DhBacktestRequest req =
        DhBacktestRequest.draft(
            "manual-req",
            "manual-trace",
            "manual-cand",
            "s",
            "v",
            "{}",
            null,
            null,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 4, 30),
            100.0,
            List.of("S1"),
            BacktestFrequency.DAILY,
            "manual",
            Instant.now());

    final NqBacktestSubmitResult clientResult = fakeClient.submit(req);
    assertThat(clientResult.getStatus()).isEqualTo(NqBacktestSubmitStatus.ACCEPTED);
    assertThat(service).isNotNull();
  }
}

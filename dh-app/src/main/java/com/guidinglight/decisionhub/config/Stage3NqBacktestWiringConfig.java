package com.guidinglight.decisionhub.config;

import com.guidinglight.decisionhub.connector.nq.NqBacktestClient;
import com.guidinglight.decisionhub.connector.nq.fake.DisabledNqBacktestClient;
import com.guidinglight.decisionhub.connector.nq.fake.FakeNqBacktestClient;
import com.guidinglight.decisionhub.usecase.agent.backtest.DhBacktestRequestRepository;
import com.guidinglight.decisionhub.usecase.agent.backtest.DhBacktestRequestService;
import com.guidinglight.decisionhub.usecase.agent.backtest.impl.DefaultDhBacktestRequestService;
import com.guidinglight.decisionhub.usecase.agent.backtest.inmemory.InMemoryDhBacktestRequestRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Stage3-B3：DH backtest request adapter Spring 装配。
 *
 * <p>对应 STAGE3_DH_BACKTEST_ADAPTER_SPEC §3.4 / §2.2 装配真值表。
 *
 * <p>装配规则（Fake 与 Disabled 互斥的 SpEL 条件，确保任一 profile 下都恰好装配一个 NqBacktestClient bean）：
 *
 * <ul>
 *   <li>{@link #disabledNqBacktestClient()}：stage3.nq.enabled=true && backtest-request.enabled=false
 *   <li>{@link #fakeNqBacktestClient()}：!(上面条件) — 即默认 profile 与 enabled+enabled 模式
 * </ul>
 *
 * <p>真值表：
 *
 * <pre>
 *   stage3.nq.enabled | backtest-request.enabled | fake-mode | 装配结果
 *   ----------------- + ------------------------ + --------- + --------------------
 *   false             | (any)                    | (any)     | FakeNqBacktestClient
 *   true              | false                    | (any)     | DisabledNqBacktestClient
 *   true              | true                     | true      | FakeNqBacktestClient
 *   true              | true                     | false     | FakeNqBacktestClient（B3 本轮兜底；RealClient 推迟）
 * </pre>
 *
 * <p>硬约束：
 *
 * <ul>
 *   <li>默认 profile 必须装配 FakeNqBacktestClient（NQ 不可达时 DH 仍可启动）；
 *   <li>本轮不实现 RealNqBacktestClient；fake-mode=false 时也走 Fake 兜底；
 *   <li>所有路径不引入 RestTemplate / WebClient / OkHttp / HttpURLConnection（ArchUnit R11 守门）。
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(NqBacktestClientProperties.class)
public class Stage3NqBacktestWiringConfig {

  /**
   * DisabledNqBacktestClient 装配：仅当 stage3.nq.enabled=true AND backtest-request.enabled=false。
   *
   * <p>Spring 不原生支持多个 {@code @ConditionalOnProperty} 的 AND；用 SpEL 表达式实现。
   */
  @Bean
  @ConditionalOnExpression(
      "'${decisionhub.stage3.nq.enabled:false}' == 'true' "
          + "&& '${decisionhub.stage3.nq.backtest-request.enabled:false}' == 'false'")
  public NqBacktestClient disabledNqBacktestClient() {
    return new DisabledNqBacktestClient();
  }

  /**
   * Fake 默认装配（兜底）：当上面 Disabled 条件不满足时使用。
   *
   * <p>SpEL 条件与 Disabled 互斥：不(stage3=true && br=false) 即 (stage3=false || br=true)。
   *
   * <p>同时显式 {@code @ConditionalOnMissingBean(NqBacktestClient.class)} 兜底，
   * 允许测试场景注入额外 mock NqBacktestClient bean 时不冲突。
   */
  @Bean
  @ConditionalOnExpression(
      "'${decisionhub.stage3.nq.enabled:false}' == 'false' "
          + "|| '${decisionhub.stage3.nq.backtest-request.enabled:false}' == 'true'")
  @ConditionalOnMissingBean(NqBacktestClient.class)
  public NqBacktestClient fakeNqBacktestClient() {
    return new FakeNqBacktestClient();
  }

  /** DhBacktestRequestRepository 默认 InMemory；可被 JDBC 实现覆盖。 */
  @Bean
  @ConditionalOnMissingBean(DhBacktestRequestRepository.class)
  public DhBacktestRequestRepository dhBacktestRequestRepository() {
    return new InMemoryDhBacktestRequestRepository();
  }

  /** DhBacktestRequestService 默认实现。 */
  @Bean
  @ConditionalOnMissingBean(DhBacktestRequestService.class)
  public DhBacktestRequestService dhBacktestRequestService(
      final DhBacktestRequestRepository repository, final NqBacktestClient nqBacktestClient) {
    return new DefaultDhBacktestRequestService(repository, nqBacktestClient);
  }
}

package com.guidinglight.decisionhub.config;

import com.guidinglight.decisionhub.ledger.EventStore;
import com.guidinglight.decisionhub.providers.MockProvider;
import com.guidinglight.decisionhub.providers.ModelProvider;
import com.guidinglight.decisionhub.providers.ProviderRegistry;
import com.guidinglight.decisionhub.usecase.run.RunRepository;
import com.guidinglight.decisionhub.usecase.run.RunService;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Stage1-CLOSE：旧多模型平台装配。已被 {@link AgentRuntimeWiringConfig} 取代。
 *
 * <p>当前仍保留是为了让 {@code /legacy/runs} 路径在切换期间继续可用；Stage2 接通 NQ 真实事件后整体删除。
 *
 * @deprecated Stage1-CLOSE：旧多模型平台 Bean 装配。
 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
@SuppressWarnings("deprecation")
@Configuration
public class AppWiringConfig {

  /** @deprecated Stage1-CLOSE：旧 RunService bean。 */
  @Deprecated(since = "Stage1-CLOSE", forRemoval = true)
  @Bean
  public RunService runService(
      final RunRepository runRepository,
      final EventStore eventStore,
      final List<ModelProvider> providers) {
    return new RunService(runRepository, eventStore, providers);
  }

  /** @deprecated Stage1-CLOSE：旧 ProviderRegistry bean。 */
  @Deprecated(since = "Stage1-CLOSE", forRemoval = true)
  @Bean
  public ProviderRegistry providerRegistry() {
    final ProviderRegistry r = new ProviderRegistry();
    r.register(new MockProvider());
    return r;
  }
}

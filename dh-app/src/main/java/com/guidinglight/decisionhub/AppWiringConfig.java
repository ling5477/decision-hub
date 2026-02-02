package com.guidinglight.decisionhub;

import com.guidinglight.decisionhub.providers.MockProvider;
import com.guidinglight.decisionhub.providers.ProviderRegistry;
import com.guidinglight.decisionhub.ledger.EventStore;
import com.guidinglight.decisionhub.usecase.run.RunRepository;
import com.guidinglight.decisionhub.usecase.run.RunService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppWiringConfig {

  @Bean
  public RunService runService(RunRepository runRepository, EventStore eventStore) {
    return new RunService(runRepository, eventStore);
  }

  @Bean
  public ProviderRegistry providerRegistry() {
    ProviderRegistry r = new ProviderRegistry();
    r.register(new MockProvider());
    return r;
  }
}

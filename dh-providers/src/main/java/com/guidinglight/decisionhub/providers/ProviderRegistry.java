package com.guidinglight.decisionhub.providers;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** @deprecated Stage1-CLOSE：旧 ProviderRegistry；新链路使用 Spring Bean 装配。 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public final class ProviderRegistry {

  private final Map<String, ModelProvider> providers = new ConcurrentHashMap<>();

  public void register(ModelProvider provider) {
    providers.put(provider.key(), provider);
  }

  public Optional<ModelProvider> find(String key) {
    return Optional.ofNullable(providers.get(key));
  }
}

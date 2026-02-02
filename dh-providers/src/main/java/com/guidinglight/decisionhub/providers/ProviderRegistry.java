package com.guidinglight.decisionhub.providers;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ProviderRegistry {

  private final Map<String, ModelProvider> providers = new ConcurrentHashMap<>();

  public void register(ModelProvider provider) {
    providers.put(provider.key(), provider);
  }

  public Optional<ModelProvider> find(String key) {
    return Optional.ofNullable(providers.get(key));
  }
}

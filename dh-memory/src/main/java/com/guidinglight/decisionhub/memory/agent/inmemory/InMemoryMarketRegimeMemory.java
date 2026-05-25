package com.guidinglight.decisionhub.memory.agent.inmemory;

import com.guidinglight.decisionhub.memory.agent.MarketRegimeMemory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Stage1：MarketRegimeMemory 的内存实现。 */
public final class InMemoryMarketRegimeMemory implements MarketRegimeMemory {

  private final Map<String, Map<String, Object>> index = new ConcurrentHashMap<>();

  @Override
  public Optional<Map<String, Object>> findRegime(final String tenantId, final String regimeKey) {
    return Optional.ofNullable(index.get(compositeKey(tenantId, regimeKey)));
  }

  @Override
  public void saveRegime(
      final String tenantId, final String regimeKey, final Map<String, Object> payloadJson) {
    index.put(
        compositeKey(tenantId, regimeKey),
        payloadJson == null ? Map.of() : Map.copyOf(payloadJson));
  }

  @Override
  public List<String> listRegimes(final String tenantId) {
    final String prefix = tenantId + "::";
    final List<String> out = new ArrayList<>();
    for (String key : index.keySet()) {
      if (key.startsWith(prefix)) {
        out.add(key.substring(prefix.length()));
      }
    }
    return out;
  }

  private static String compositeKey(final String tenantId, final String regimeKey) {
    return tenantId + "::" + regimeKey;
  }
}

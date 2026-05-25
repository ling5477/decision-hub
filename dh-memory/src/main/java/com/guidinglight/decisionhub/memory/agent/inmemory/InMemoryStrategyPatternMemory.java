package com.guidinglight.decisionhub.memory.agent.inmemory;

import com.guidinglight.decisionhub.memory.agent.StrategyPatternMemory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Stage1：StrategyPatternMemory 的内存实现。 */
public final class InMemoryStrategyPatternMemory implements StrategyPatternMemory {

  private final Map<String, Map<String, Object>> index = new ConcurrentHashMap<>();

  @Override
  public Optional<Map<String, Object>> findPattern(
      final String tenantId, final String patternKey) {
    return Optional.ofNullable(index.get(compositeKey(tenantId, patternKey)));
  }

  @Override
  public void savePattern(
      final String tenantId, final String patternKey, final Map<String, Object> payloadJson) {
    index.put(
        compositeKey(tenantId, patternKey),
        payloadJson == null ? Map.of() : Map.copyOf(payloadJson));
  }

  @Override
  public List<String> listPatterns(final String tenantId) {
    final String prefix = tenantId + "::";
    final List<String> out = new ArrayList<>();
    for (String key : index.keySet()) {
      if (key.startsWith(prefix)) {
        out.add(key.substring(prefix.length()));
      }
    }
    return out;
  }

  private static String compositeKey(final String tenantId, final String patternKey) {
    return tenantId + "::" + patternKey;
  }
}

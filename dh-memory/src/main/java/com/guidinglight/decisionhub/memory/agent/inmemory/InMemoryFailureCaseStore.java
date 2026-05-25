package com.guidinglight.decisionhub.memory.agent.inmemory;

import com.guidinglight.decisionhub.memory.agent.FailureCaseStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Stage1：FailureCaseStore 的内存实现。 */
public final class InMemoryFailureCaseStore implements FailureCaseStore {

  private final Map<String, List<Map<String, Object>>> indexByRun = new ConcurrentHashMap<>();

  @Override
  public void record(
      final String tenantId,
      final String traceId,
      final String runId,
      final String category,
      final Map<String, Object> payloadJson,
      final Instant now) {
    final Map<String, Object> snapshot = new HashMap<>();
    snapshot.put("tenantId", tenantId);
    snapshot.put("traceId", traceId);
    snapshot.put("runId", runId);
    snapshot.put("category", category);
    snapshot.put("payload", payloadJson == null ? Map.of() : Map.copyOf(payloadJson));
    snapshot.put("recordedAt", now);
    indexByRun
        .computeIfAbsent(compositeKey(tenantId, runId), k -> new ArrayList<>())
        .add(Map.copyOf(snapshot));
  }

  @Override
  public List<Map<String, Object>> listByRun(final String tenantId, final String runId) {
    return List.copyOf(indexByRun.getOrDefault(compositeKey(tenantId, runId), List.of()));
  }

  private static String compositeKey(final String tenantId, final String runId) {
    return tenantId + "::" + runId;
  }
}

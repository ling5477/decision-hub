package com.guidinglight.decisionhub.usecase.agent.inmemory;

import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Stage1：NqFeedbackEventRepository 的内存实现。 */
public final class InMemoryNqFeedbackEventRepository implements NqFeedbackEventRepository {

  private final Map<String, List<NqFeedbackEvent>> indexByRun = new ConcurrentHashMap<>();

  @Override
  public void append(final NqFeedbackEvent event) {
    indexByRun
        .computeIfAbsent(compositeKey(event.getTenantId(), event.getRunId()), k -> new ArrayList<>())
        .add(event);
  }

  @Override
  public List<NqFeedbackEvent> listByRun(final String tenantId, final String runId) {
    return indexByRun.getOrDefault(compositeKey(tenantId, runId), List.of()).stream()
        .sorted(Comparator.comparing(NqFeedbackEvent::getReceivedAt))
        .collect(Collectors.toList());
  }

  private static String compositeKey(final String tenantId, final String runId) {
    return tenantId + "::" + runId;
  }
}

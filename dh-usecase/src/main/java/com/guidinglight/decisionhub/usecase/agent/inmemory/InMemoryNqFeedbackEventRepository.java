package com.guidinglight.decisionhub.usecase.agent.inmemory;

import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Stage1 + Stage2-PoC-B2：NqFeedbackEventRepository 的内存实现。
 *
 * <p>Stage1 部分（append/listByRun）保持原行为；Stage2 envelope 部分用 ConcurrentHashMap 提供 eventId 唯一键幂等，JDBC
 * 替换留待 Batch 5。
 */
public final class InMemoryNqFeedbackEventRepository implements NqFeedbackEventRepository {

  private final Map<String, List<NqFeedbackEvent>> indexByRun = new ConcurrentHashMap<>();
  private final Map<String, NqFeedbackEnvelope> envelopesByEventId = new ConcurrentHashMap<>();

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

  @Override
  public boolean saveEnvelope(final NqFeedbackEnvelope envelope) {
    // putIfAbsent: 首次 null -> true；已有 -> false（幂等命中）。
    return envelopesByEventId.putIfAbsent(envelope.getEventId(), envelope) == null;
  }

  @Override
  public Optional<NqFeedbackEnvelope> findEnvelopeByEventId(final String eventId) {
    return Optional.ofNullable(envelopesByEventId.get(eventId));
  }

  private static String compositeKey(final String tenantId, final String runId) {
    return tenantId + "::" + runId;
  }
}

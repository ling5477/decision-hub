package com.guidinglight.decisionhub.memory.agent.inmemory;

import com.guidinglight.decisionhub.domain.experience.PheromoneEdge;
import com.guidinglight.decisionhub.memory.agent.PheromoneStore;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Stage1：PheromoneStore 的内存实现。
 */
public final class InMemoryPheromoneStore implements PheromoneStore {

  private final Map<String, PheromoneEdge> index = new ConcurrentHashMap<>();

  @Override
  public Optional<PheromoneEdge> find(
      final String tenantId, final String fromNode, final String toNode) {
    return Optional.ofNullable(index.get(compositeKey(tenantId, fromNode, toNode)));
  }

  @Override
  public void save(final PheromoneEdge edge) {
    index.put(compositeKey(edge.getTenantId(), edge.getFromNode(), edge.getToNode()), edge);
  }

  @Override
  public List<PheromoneEdge> listByFrom(final String tenantId, final String fromNode) {
    return index.values().stream()
        .filter(e -> tenantId.equals(e.getTenantId()))
        .filter(e -> fromNode.equals(e.getFromNode()))
        .sorted(Comparator.comparingDouble(PheromoneEdge::getPheromoneScore).reversed())
        .collect(Collectors.toList());
  }

  private static String compositeKey(
      final String tenantId, final String fromNode, final String toNode) {
    return tenantId + "::" + fromNode + "->" + toNode;
  }
}

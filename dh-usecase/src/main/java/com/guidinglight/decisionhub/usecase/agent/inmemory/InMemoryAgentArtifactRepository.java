package com.guidinglight.decisionhub.usecase.agent.inmemory;

import com.guidinglight.decisionhub.domain.agent.AgentArtifact;
import com.guidinglight.decisionhub.usecase.agent.AgentArtifactRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Stage1：AgentArtifactRepository 的内存实现。 */
public final class InMemoryAgentArtifactRepository implements AgentArtifactRepository {

  private final Map<String, List<AgentArtifact>> indexByRun = new ConcurrentHashMap<>();

  @Override
  public void save(final AgentArtifact artifact) {
    indexByRun.computeIfAbsent(artifact.getRunId(), k -> new ArrayList<>()).add(artifact);
  }

  @Override
  public List<AgentArtifact> listByRun(final String runId) {
    return List.copyOf(indexByRun.getOrDefault(runId, List.of()));
  }
}

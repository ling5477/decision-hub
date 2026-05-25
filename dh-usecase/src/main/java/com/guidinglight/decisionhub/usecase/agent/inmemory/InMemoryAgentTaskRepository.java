package com.guidinglight.decisionhub.usecase.agent.inmemory;

import com.guidinglight.decisionhub.domain.agent.AgentTask;
import com.guidinglight.decisionhub.usecase.agent.AgentTaskRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Stage1：AgentTaskRepository 的内存实现。每个 run 最多一个 task。 */
public final class InMemoryAgentTaskRepository implements AgentTaskRepository {

  private final Map<String, AgentTask> indexByRun = new ConcurrentHashMap<>();

  @Override
  public void save(final AgentTask task) {
    indexByRun.put(task.getRunId(), task);
  }

  @Override
  public Optional<AgentTask> findByRun(final String runId) {
    return Optional.ofNullable(indexByRun.get(runId));
  }
}

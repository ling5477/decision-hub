package com.guidinglight.decisionhub.usecase.agent.inmemory;

import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Stage1：ResearchRunRepository 的内存实现。 */
public final class InMemoryResearchRunRepository implements ResearchRunRepository {

  private final Map<String, ResearchRun> index = new ConcurrentHashMap<>();

  @Override
  public void save(final ResearchRun run) {
    index.put(run.getRunId(), run);
  }

  @Override
  public Optional<ResearchRun> find(final String runId) {
    return Optional.ofNullable(index.get(runId));
  }

  @Override
  public List<ResearchRun> listByTenant(final String tenantId) {
    return index.values().stream()
        .filter(r -> tenantId.equals(r.getTenantId()))
        .sorted(Comparator.comparing(ResearchRun::getCreatedAt).reversed())
        .collect(Collectors.toList());
  }
}

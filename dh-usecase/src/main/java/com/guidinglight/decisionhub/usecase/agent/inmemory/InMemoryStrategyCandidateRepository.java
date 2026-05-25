package com.guidinglight.decisionhub.usecase.agent.inmemory;

import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import com.guidinglight.decisionhub.usecase.agent.StrategyCandidateRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Stage1：StrategyCandidateRepository 的内存实现。 */
public final class InMemoryStrategyCandidateRepository implements StrategyCandidateRepository {

  private final Map<String, StrategyCandidate> index = new ConcurrentHashMap<>();

  @Override
  public void save(final StrategyCandidate candidate) {
    index.put(candidate.getCandidateId(), candidate);
  }

  @Override
  public Optional<StrategyCandidate> find(final String candidateId) {
    return Optional.ofNullable(index.get(candidateId));
  }

  @Override
  public List<StrategyCandidate> listByRun(final String runId) {
    return index.values().stream()
        .filter(c -> runId.equals(c.getRunId()))
        .sorted(Comparator.comparing(StrategyCandidate::getCreatedAt))
        .collect(Collectors.toList());
  }
}

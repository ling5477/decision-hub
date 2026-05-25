package com.guidinglight.decisionhub.usecase.agent.inmemory;

import com.guidinglight.decisionhub.domain.judge.JudgeDecision;
import com.guidinglight.decisionhub.usecase.agent.JudgeDecisionRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Stage1：JudgeDecisionRepository 的内存实现。 */
public final class InMemoryJudgeDecisionRepository implements JudgeDecisionRepository {

  private final Map<String, JudgeDecision> indexByRun = new ConcurrentHashMap<>();

  @Override
  public void save(final JudgeDecision decision) {
    indexByRun.put(decision.getRunId(), decision);
  }

  @Override
  public Optional<JudgeDecision> findByRun(final String runId) {
    return Optional.ofNullable(indexByRun.get(runId));
  }
}

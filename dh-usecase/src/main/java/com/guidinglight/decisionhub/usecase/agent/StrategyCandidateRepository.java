package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import java.util.List;
import java.util.Optional;

/** Stage1：StrategyCandidate 持久化端口。 */
public interface StrategyCandidateRepository {

  /** Upsert。 */
  void save(StrategyCandidate candidate);

  /** 按 id 查询。 */
  Optional<StrategyCandidate> find(String candidateId);

  /** 列出某 run 下全部候选（按 createdAt 升序）。 */
  List<StrategyCandidate> listByRun(String runId);
}

package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.judge.JudgeDecision;
import java.util.Optional;

/** Stage1：JudgeDecision 持久化端口。 */
public interface JudgeDecisionRepository {

  /** Upsert。 */
  void save(JudgeDecision decision);

  /** 按 runId 查询（Stage1：每个 run 至多一个最终 JudgeDecision）。 */
  Optional<JudgeDecision> findByRun(String runId);
}

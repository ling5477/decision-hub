package com.guidinglight.decisionhub.usecase.run;

import com.guidinglight.decisionhub.domain.run.Run;

import java.util.Optional;

/** @deprecated Stage1-CLOSE：旧 Run 仓储，新链路使用 {@link com.guidinglight.decisionhub.usecase.agent.ResearchRunRepository}。 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public interface RunRepository {
  void save(Run run);
  Optional<Run> findById(String runId);
}

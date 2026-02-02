package com.guidinglight.decisionhub.usecase.run;

import com.guidinglight.decisionhub.domain.run.Run;

import java.util.Optional;

public interface RunRepository {
  void save(Run run);
  Optional<Run> findById(String runId);
}

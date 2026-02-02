package com.guidinglight.decisionhub.usecase.run;

import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.run.Run;
import com.guidinglight.decisionhub.ledger.EventStore;
import com.guidinglight.decisionhub.ledger.LedgerEvent;
import com.guidinglight.decisionhub.ledger.LedgerEventType;
import com.guidinglight.decisionhub.common.util.IdGenerator;

import java.util.Map;

public class RunService {

  private final RunRepository runRepository;
  private final EventStore eventStore;

  public RunService(RunRepository runRepository, EventStore eventStore) {
    this.runRepository = runRepository;
    this.eventStore = eventStore;
  }

  public Run create(String tenantId, String question, Map<String, Object> configSnapshot) {
    Run run = new Run(tenantId, question, configSnapshot, TimeProvider.now());
    runRepository.save(run);

    eventStore.append(new LedgerEvent(
        IdGenerator.newId(),
        run.getRunId(),
        run.getTenantId(),
        LedgerEventType.RUN_CREATED,
        TimeProvider.now(),
        Map.of("question", question)
    ));
    return run;
  }

  public Run get(String runId) {
    return runRepository.findById(runId).orElseThrow();
  }
}

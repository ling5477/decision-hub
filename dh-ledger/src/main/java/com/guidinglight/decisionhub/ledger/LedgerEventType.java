package com.guidinglight.decisionhub.ledger;

public enum LedgerEventType {
  RUN_CREATED,
  RUN_ENQUEUED,
  RUN_STARTED,
  STEP_STARTED,
  STEP_COMPLETED,
  GATE_EVALUATED,
  DECISION_FINALIZED
}

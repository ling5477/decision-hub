package com.guidinglight.decisionhub.ledger;

import java.util.List;

public interface EventStore {
  void append(LedgerEvent event);
  List<LedgerEvent> listByRunId(String runId);
}

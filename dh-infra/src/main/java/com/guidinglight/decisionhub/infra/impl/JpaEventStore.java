package com.guidinglight.decisionhub.infra.impl;

import com.guidinglight.decisionhub.common.util.JsonUtil;
import com.guidinglight.decisionhub.infra.jpa.LedgerEventEntity;
import com.guidinglight.decisionhub.infra.jpa.LedgerEventJpaRepository;
import com.guidinglight.decisionhub.ledger.EventStore;
import com.guidinglight.decisionhub.ledger.LedgerEvent;
import com.guidinglight.decisionhub.ledger.LedgerEventType;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaEventStore implements EventStore {

  private final LedgerEventJpaRepository repo;

  public JpaEventStore(LedgerEventJpaRepository repo) {
    this.repo = repo;
  }

  @Override
  public void append(LedgerEvent event) {
    LedgerEventEntity e = new LedgerEventEntity();
    e.setEventId(event.eventId());
    e.setRunId(event.runId());
    e.setTenantId(event.tenantId());
    e.setType(event.type().name());
    e.setAt(event.at());
    e.setPayloadJson(JsonUtil.toJson(event.payload()));
    repo.save(e);
  }

  @Override
  public List<LedgerEvent> listByRunId(String runId) {
    return repo.findByRunIdOrderByAtAsc(runId).stream()
        .map(e -> new LedgerEvent(
            e.getEventId(),
            e.getRunId(),
            e.getTenantId(),
            LedgerEventType.valueOf(e.getType()),
            e.getAt(),
            java.util.Map.of("payloadJson", e.getPayloadJson())
        ))
        .toList();
  }
}

package com.guidinglight.decisionhub.ledger;

import java.time.Instant;
import java.util.Map;

public record LedgerEvent(
    String eventId,
    String runId,
    String tenantId,
    LedgerEventType type,
    Instant at,
    Map<String, Object> payload
) {}

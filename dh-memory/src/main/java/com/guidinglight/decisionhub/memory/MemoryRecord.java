package com.guidinglight.decisionhub.memory;

import java.time.Instant;
import java.util.Map;

public record MemoryRecord(
    String id,
    String tenantId,
    String namespace,
    String text,
    Map<String, Object> meta,
    Instant createdAt
) {}

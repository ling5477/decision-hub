package com.guidinglight.decisionhub.api.run;

import com.guidinglight.decisionhub.domain.run.RunStatus;

import java.time.Instant;
import java.util.Map;

public record RunView(
    String runId,
    String tenantId,
    RunStatus status,
    String question,
    Map<String, Object> configSnapshot,
    Instant createdAt,
    Instant updatedAt
) {}

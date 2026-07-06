package com.guidinglight.decisionhub.domain.qdr;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Quant Decision Review 的单次编排运行。
 *
 * <p>该对象对应 `decision_run` 表。stage-qdr-1 只记录 deterministic mock / existing orchestrator
 * 的只读运行，不接真实 provider、不接 LangGraph、不接外部 HTTP。
 */
public record DecisionRun(
        UUID id,
        UUID decisionRequestId,
        int runNo,
        DecisionRunStatus status,
        String orchestratorKey,
        String modelProvider,
        String modelName,
        Instant startedAt,
        Instant finishedAt,
        Long latencyMs,
        String errorCode,
        String errorMessage,
        Instant createdAt) {

    /**
     * 校验运行记录的归属、序号和耗时边界。
     */
    public DecisionRun {
        id = Objects.requireNonNull(id, "id");
        decisionRequestId = Objects.requireNonNull(decisionRequestId, "decisionRequestId");
        if (runNo < 1) {
            throw new IllegalArgumentException("runNo must be positive");
        }
        status = Objects.requireNonNull(status, "status");
        startedAt = Objects.requireNonNull(startedAt, "startedAt");
        if (latencyMs != null && latencyMs < 0) {
            throw new IllegalArgumentException("latencyMs must not be negative");
        }
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }
}

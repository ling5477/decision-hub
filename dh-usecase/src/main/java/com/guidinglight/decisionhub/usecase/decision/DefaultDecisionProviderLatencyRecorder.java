package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionProviderLatency;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * K5 默认 provider latency recorder。
 *
 * <p>默认 timeout 为 1000ms；超过阈值只影响本地 guard 结果，不触发 retry、不访问外部系统、不启动任何真实
 * provider runtime。
 */
public final class DefaultDecisionProviderLatencyRecorder implements DecisionProviderLatencyRecorder {

    private static final long DEFAULT_TIMEOUT_MS = 1000L;

    private final long timeoutMs;

    /**
     * 创建默认 1000ms timeout 的 recorder。
     */
    public DefaultDecisionProviderLatencyRecorder() {
        this(DEFAULT_TIMEOUT_MS);
    }

    /**
     * 创建可测试的 latency recorder。
     *
     * @param timeoutMs 本地 timeout 阈值，必须非负
     */
    public DefaultDecisionProviderLatencyRecorder(final long timeoutMs) {
        if (timeoutMs < 0) {
            throw new IllegalArgumentException("timeoutMs must not be negative");
        }
        this.timeoutMs = timeoutMs;
    }

    @Override
    public DecisionProviderLatency record(
            final String providerName, final Instant startedAt, final Instant endedAt) {
        final Instant checkedStarted = Objects.requireNonNull(startedAt, "startedAt");
        final Instant checkedEnded = Objects.requireNonNull(endedAt, "endedAt");
        final long latencyMs = Math.max(0L, Duration.between(checkedStarted, checkedEnded).toMillis());
        return new DecisionProviderLatency(
                providerName, latencyMs, timeoutMs, latencyMs > timeoutMs, checkedEnded);
    }
}

package com.guidinglight.decisionhub.domain.decision;

import java.time.Instant;
import java.util.Objects;

/**
 * K5 provider latency 本地观测值。
 *
 * <p>latencyMs 来自 DH 进程内的开始/结束时间差或测试注入时间，不依赖真实 HTTP、外部模型、NQ runtime 或交易所。
 */
public record DecisionProviderLatency(
        String providerName, long latencyMs, long timeoutMs, boolean timedOut, Instant recordedAt) {

    /**
     * 校验 latency 观测，保证 provider call log 中不会出现负值。
     */
    public DecisionProviderLatency {
        providerName = requireText(providerName, "providerName");
        if (latencyMs < 0) {
            throw new IllegalArgumentException("latencyMs must not be negative");
        }
        if (timeoutMs < 0) {
            throw new IllegalArgumentException("timeoutMs must not be negative");
        }
        recordedAt = Objects.requireNonNull(recordedAt, "recordedAt");
    }

    /**
     * 返回 latency policy 是否要求 fail-closed。
     */
    public boolean requiresFailClosed() {
        return timedOut;
    }

    private static String requireText(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return checked;
    }
}

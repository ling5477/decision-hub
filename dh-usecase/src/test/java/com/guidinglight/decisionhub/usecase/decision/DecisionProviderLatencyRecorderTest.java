package com.guidinglight.decisionhub.usecase.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionProviderLatency;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/**
 * K5 provider latency recorder 单元测试，验证本地耗时记录与 timeout policy。
 */
final class DecisionProviderLatencyRecorderTest {

    private static final Instant START = Instant.parse("2026-07-01T00:00:00Z");

    @Test
    void recordsNonNegativeLatency() {
        final DecisionProviderLatency latency =
                new DefaultDecisionProviderLatencyRecorder(1000L)
                        .record("MOCK_DECISION_PROVIDER", START, START.plusMillis(25L));

        assertEquals(25L, latency.latencyMs());
        assertEquals(1000L, latency.timeoutMs());
        assertFalse(latency.timedOut());
    }

    @Test
    void marksLatencyPolicyBreachAsTimeout() {
        final DecisionProviderLatency latency =
                new DefaultDecisionProviderLatencyRecorder(10L)
                        .record("MOCK_DECISION_PROVIDER", START, START.plusMillis(11L));

        assertTrue(latency.timedOut());
        assertTrue(latency.requiresFailClosed());
    }

    @Test
    void negativeTimeoutIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DefaultDecisionProviderLatencyRecorder(-1L));
    }
}

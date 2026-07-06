package com.guidinglight.decisionhub.usecase.qdr.readmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * stage-qdr-2 B1 decision run read query 的 tenant-bound 校验回归。
 */
class DecisionRunReadQueryTest {

    @Test
    void tenantIdNullFailsClosed() {
        assertThrows(
                NullPointerException.class, () -> new DecisionRunReadQuery(null, "run-1", null, null));
    }

    @Test
    void tenantIdBlankFailsClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DecisionRunReadQuery("   ", "run-1", null, null));
    }

    @Test
    void decisionRunIdNullFailsClosed() {
        assertThrows(
                NullPointerException.class, () -> new DecisionRunReadQuery("tenant-1", null, null, null));
    }

    @Test
    void optionalBlankTraceFailsClosedWhenProvided() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DecisionRunReadQuery("tenant-1", "run-1", null, "   "));
    }

    @Test
    void validQueryTrimsRequiredAndOptionalFields() {
        final DecisionRunReadQuery query =
                new DecisionRunReadQuery(" tenant-1 ", " run-1 ", " requester-1 ", " trace-1 ");

        assertEquals("tenant-1", query.tenantId());
        assertEquals("run-1", query.decisionRunId());
        assertEquals("requester-1", query.requesterId());
        assertEquals("trace-1", query.traceId());
    }
}

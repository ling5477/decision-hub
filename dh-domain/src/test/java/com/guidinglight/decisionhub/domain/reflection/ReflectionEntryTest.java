package com.guidinglight.decisionhub.domain.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.domain.agent.AgentRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Stage2-PoC-B1：ReflectionEntry 字段、stepIndex 校验、type 枚举。 */
class ReflectionEntryTest {

  @Test
  void of_keepsFields() {
    final ReflectionEntry r =
        ReflectionEntry.of(
            "rfl-1",
            "run-1",
            "trace-1",
            0,
            AgentRole.JUDGE,
            ReflectionType.STEP_REFLECTION,
            "candidate looks consistent",
            Instant.parse("2026-05-25T08:00:00Z"),
            null);
    assertEquals("rfl-1", r.getReflectionId());
    assertEquals("run-1", r.getRunId());
    assertEquals("trace-1", r.getTraceId());
    assertEquals(0, r.getStepIndex());
    assertEquals(AgentRole.JUDGE, r.getAgentRole());
    assertEquals(ReflectionType.STEP_REFLECTION, r.getType());
    assertEquals("candidate looks consistent", r.getContent());
    assertNull(r.getPayloadJson());
  }

  @Test
  void of_rejectsNegativeStepIndex() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ReflectionEntry.of(
                "rfl",
                "run",
                "trace",
                -1,
                AgentRole.JUDGE,
                ReflectionType.STEP_REFLECTION,
                "x",
                Instant.now(),
                null));
  }

  @Test
  void type_enumCompleteness() {
    assertEquals(3, ReflectionType.values().length);
  }
}

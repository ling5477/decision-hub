package com.guidinglight.decisionhub.domain.checkpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Stage2-PoC-B1：CheckpointEntry 字段、checkpointIndex 校验、type/status 枚举。 */
class CheckpointEntryTest {

  @Test
  void of_keepsFields() {
    final CheckpointEntry c =
        CheckpointEntry.of(
            "ck-1",
            "run-1",
            "trace-1",
            0,
            CheckpointType.JUDGE_DECISION,
            CheckpointStatus.RECORDED,
            "{\"snap\":1}",
            Instant.parse("2026-05-25T08:00:00Z"));
    assertEquals("ck-1", c.getCheckpointId());
    assertEquals("run-1", c.getRunId());
    assertEquals("trace-1", c.getTraceId());
    assertEquals(CheckpointType.JUDGE_DECISION, c.getType());
    assertEquals(CheckpointStatus.RECORDED, c.getStatus());
    assertEquals("{\"snap\":1}", c.getSnapshotJson());
  }

  @Test
  void of_rejectsNegativeIndex() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            CheckpointEntry.of(
                "ck",
                "run",
                "trace",
                -1,
                CheckpointType.JUDGE_DECISION,
                CheckpointStatus.DRAFT,
                "{}",
                Instant.now()));
  }

  @Test
  void enums_cover() {
    assertEquals(5, CheckpointType.values().length);
    assertEquals(3, CheckpointStatus.values().length);
  }
}

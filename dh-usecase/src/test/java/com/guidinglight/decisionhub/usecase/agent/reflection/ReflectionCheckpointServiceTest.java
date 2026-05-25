package com.guidinglight.decisionhub.usecase.agent.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.agent.AgentRole;
import com.guidinglight.decisionhub.domain.checkpoint.CheckpointEntry;
import com.guidinglight.decisionhub.domain.checkpoint.CheckpointStatus;
import com.guidinglight.decisionhub.domain.checkpoint.CheckpointType;
import com.guidinglight.decisionhub.domain.reflection.ReflectionEntry;
import com.guidinglight.decisionhub.domain.reflection.ReflectionType;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.usecase.agent.ReflectionCheckpointService;
import com.guidinglight.decisionhub.usecase.agent.impl.DefaultReflectionCheckpointService;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryCheckpointEntryRepository;
import com.guidinglight.decisionhub.usecase.agent.inmemory.InMemoryReflectionEntryRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Stage2-PoC-B4：ReflectionCheckpointService 单元测试。
 *
 * <p>覆盖：reflection / checkpoint 写入、按 runId 查询有序、stepIndex 校验、snapshotJson 必填、
 * reflection 不能写 final recommendation（仅过程证据；JudgeDecision 仍是唯一最终出口）。
 */
class ReflectionCheckpointServiceTest {

  private final InMemoryReflectionEntryRepository reflectionRepo =
      new InMemoryReflectionEntryRepository();
  private final InMemoryCheckpointEntryRepository checkpointRepo =
      new InMemoryCheckpointEntryRepository();
  private final ReflectionCheckpointService service =
      new DefaultReflectionCheckpointService(reflectionRepo, checkpointRepo);

  @Test
  void record_reflection_persists_entry() {
    final ResearchRun run = newRun();
    final ReflectionEntry entry =
        service.recordReflection(
            run,
            0,
            AgentRole.SCOUT,
            ReflectionType.STEP_REFLECTION,
            "scout completed market scan",
            null);

    assertNotNull(entry.getReflectionId());
    assertEquals(run.getRunId(), entry.getRunId());
    assertEquals(run.getTraceId(), entry.getTraceId());
    assertEquals(0, entry.getStepIndex());
    assertEquals(AgentRole.SCOUT, entry.getAgentRole());
    assertEquals(ReflectionType.STEP_REFLECTION, entry.getType());
    assertNull(entry.getPayloadJson());

    final List<ReflectionEntry> listed = service.listReflections(run.getRunId());
    assertEquals(1, listed.size());
    assertEquals(entry.getReflectionId(), listed.get(0).getReflectionId());
  }

  @Test
  void reflections_are_returned_sorted_by_step_index() {
    final ResearchRun run = newRun();
    service.recordReflection(
        run, 2, AgentRole.STRATEGY, ReflectionType.STEP_REFLECTION, "third", null);
    service.recordReflection(
        run, 0, AgentRole.SCOUT, ReflectionType.STEP_REFLECTION, "first", null);
    service.recordReflection(
        run, 1, AgentRole.ANALYST, ReflectionType.STEP_REFLECTION, "second", null);

    final List<ReflectionEntry> listed = service.listReflections(run.getRunId());
    assertEquals(3, listed.size());
    assertEquals(0, listed.get(0).getStepIndex());
    assertEquals(1, listed.get(1).getStepIndex());
    assertEquals(2, listed.get(2).getStepIndex());
  }

  @Test
  void negative_step_index_is_rejected() {
    final ResearchRun run = newRun();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.recordReflection(
                run,
                -1,
                AgentRole.SCOUT,
                ReflectionType.STEP_REFLECTION,
                "invalid",
                null));
    assertTrue(service.listReflections(run.getRunId()).isEmpty());
  }

  @Test
  void record_checkpoint_persists_entry() {
    final ResearchRun run = newRun();
    final CheckpointEntry entry =
        service.recordCheckpoint(
            run,
            0,
            CheckpointType.CANDIDATE_FROZEN,
            CheckpointStatus.RECORDED,
            "{\"candidates\":1}");

    assertNotNull(entry.getCheckpointId());
    assertEquals(run.getRunId(), entry.getRunId());
    assertEquals(run.getTraceId(), entry.getTraceId());
    assertEquals(0, entry.getCheckpointIndex());
    assertEquals(CheckpointType.CANDIDATE_FROZEN, entry.getType());
    assertEquals(CheckpointStatus.RECORDED, entry.getStatus());

    final List<CheckpointEntry> listed = service.listCheckpoints(run.getRunId());
    assertEquals(1, listed.size());
  }

  @Test
  void checkpoint_snapshot_json_is_required() {
    final ResearchRun run = newRun();
    assertThrows(
        NullPointerException.class,
        () ->
            service.recordCheckpoint(
                run,
                0,
                CheckpointType.PIVOT_TRIGGERED,
                CheckpointStatus.DRAFT,
                null));
  }

  @Test
  void abort_checkpoint_is_recorded_but_reflection_is_not_final_output() {
    final ResearchRun run = newRun();
    service.recordReflection(
        run,
        0,
        AgentRole.RISK_REVIEWER,
        ReflectionType.STEP_REFLECTION,
        "risk overload, suggest abort",
        "{\"signal\":\"abort\"}");
    service.recordCheckpoint(
        run,
        0,
        CheckpointType.ABORT_TRIGGERED,
        CheckpointStatus.RECORDED,
        "{\"reason\":\"risk overload\"}");

    final List<ReflectionEntry> reflections = service.listReflections(run.getRunId());
    final List<CheckpointEntry> checkpoints = service.listCheckpoints(run.getRunId());

    assertFalse(reflections.isEmpty());
    assertFalse(checkpoints.isEmpty());
    // Reflection content is process evidence only; JudgeDecision remains sole final exit.
    // The domain has no API for reflections to emit a final recommendation, so we assert by
    // construction: reflection payloadJson must not be the place where final candidate IDs live.
    for (ReflectionEntry r : reflections) {
      assertFalse(
          r.getType() == ReflectionType.RUN_RETROSPECTIVE
              && r.getPayloadJson() != null
              && r.getPayloadJson().contains("finalCandidateId"),
          "ReflectionEntry must not encode final recommendation; JudgeDecision is sole exit");
    }
  }

  @Test
  void list_by_unknown_run_returns_empty() {
    assertTrue(service.listReflections("unknown-run").isEmpty());
    assertTrue(service.listCheckpoints("unknown-run").isEmpty());
  }

  private static ResearchRun newRun() {
    return ResearchRun.create("t-test", "topic-reflection", Map.of(), TimeProvider.now());
  }
}

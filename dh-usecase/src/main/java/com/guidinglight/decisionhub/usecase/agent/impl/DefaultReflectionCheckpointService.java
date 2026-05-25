package com.guidinglight.decisionhub.usecase.agent.impl;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.agent.AgentRole;
import com.guidinglight.decisionhub.domain.checkpoint.CheckpointEntry;
import com.guidinglight.decisionhub.domain.checkpoint.CheckpointStatus;
import com.guidinglight.decisionhub.domain.checkpoint.CheckpointType;
import com.guidinglight.decisionhub.domain.reflection.ReflectionEntry;
import com.guidinglight.decisionhub.domain.reflection.ReflectionType;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.usecase.agent.CheckpointEntryRepository;
import com.guidinglight.decisionhub.usecase.agent.ReflectionCheckpointService;
import com.guidinglight.decisionhub.usecase.agent.ReflectionEntryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Stage2-PoC-B4：默认 ReflectionCheckpointService 实现。
 *
 * <p>仅做编排：组装 {@link ReflectionEntry} / {@link CheckpointEntry} 并落到对应仓储。 不调用 LLM；不实现真实下单；不绕过 NQ 风控。
 * snapshotJson 强制非空（由 {@link CheckpointEntry#of} 校验）。
 */
public final class DefaultReflectionCheckpointService implements ReflectionCheckpointService {

  private final ReflectionEntryRepository reflectionRepository;
  private final CheckpointEntryRepository checkpointRepository;

  /** 构造。 */
  public DefaultReflectionCheckpointService(
      final ReflectionEntryRepository reflectionRepository,
      final CheckpointEntryRepository checkpointRepository) {
    this.reflectionRepository = Objects.requireNonNull(reflectionRepository, "reflectionRepository");
    this.checkpointRepository = Objects.requireNonNull(checkpointRepository, "checkpointRepository");
  }

  @Override
  public ReflectionEntry recordReflection(
      final ResearchRun run,
      final int stepIndex,
      final AgentRole agentRole,
      final ReflectionType type,
      final String content,
      final String payloadJson) {
    Objects.requireNonNull(run, "run");
    final Instant now = TimeProvider.now();
    final ReflectionEntry entry =
        ReflectionEntry.of(
            IdGenerator.newId(),
            run.getRunId(),
            run.getTraceId(),
            stepIndex,
            agentRole,
            type,
            content,
            now,
            payloadJson);
    reflectionRepository.save(entry);
    return entry;
  }

  @Override
  public CheckpointEntry recordCheckpoint(
      final ResearchRun run,
      final int checkpointIndex,
      final CheckpointType type,
      final CheckpointStatus status,
      final String snapshotJson) {
    Objects.requireNonNull(run, "run");
    final Instant now = TimeProvider.now();
    final CheckpointEntry entry =
        CheckpointEntry.of(
            IdGenerator.newId(),
            run.getRunId(),
            run.getTraceId(),
            checkpointIndex,
            type,
            status,
            snapshotJson,
            now);
    checkpointRepository.save(entry);
    return entry;
  }

  @Override
  public List<ReflectionEntry> listReflections(final String runId) {
    return reflectionRepository.listByRun(runId);
  }

  @Override
  public List<CheckpointEntry> listCheckpoints(final String runId) {
    return checkpointRepository.listByRun(runId);
  }
}

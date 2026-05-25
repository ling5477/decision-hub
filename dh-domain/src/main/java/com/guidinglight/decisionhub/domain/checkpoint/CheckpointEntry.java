package com.guidinglight.decisionhub.domain.checkpoint;

import java.time.Instant;
import java.util.Objects;

/**
 * Stage2-PoC-B1：checkpoint 记录。
 *
 * <p>每条 CheckpointEntry 代表一次 run 级冗余快照；用于复盘和 reflection。
 *
 * <p>{@code checkpointIndex} >= 0；同一 runId 内单调递增。 {@code snapshotJson} 不要求结构稳定，但禁止丢失。
 */
public final class CheckpointEntry {

  private final String checkpointId;
  private final String runId;
  private final String traceId;
  private final int checkpointIndex;
  private final CheckpointType type;
  private final CheckpointStatus status;
  private final String snapshotJson;
  private final Instant createdAt;

  private CheckpointEntry(
      final String checkpointId,
      final String runId,
      final String traceId,
      final int checkpointIndex,
      final CheckpointType type,
      final CheckpointStatus status,
      final String snapshotJson,
      final Instant createdAt) {
    this.checkpointId = Objects.requireNonNull(checkpointId, "checkpointId");
    this.runId = Objects.requireNonNull(runId, "runId");
    this.traceId = Objects.requireNonNull(traceId, "traceId");
    if (checkpointIndex < 0) {
      throw new IllegalArgumentException("checkpointIndex must be >= 0");
    }
    this.checkpointIndex = checkpointIndex;
    this.type = Objects.requireNonNull(type, "type");
    this.status = Objects.requireNonNull(status, "status");
    this.snapshotJson = Objects.requireNonNull(snapshotJson, "snapshotJson");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }

  /** 工厂方法。 */
  public static CheckpointEntry of(
      final String checkpointId,
      final String runId,
      final String traceId,
      final int checkpointIndex,
      final CheckpointType type,
      final CheckpointStatus status,
      final String snapshotJson,
      final Instant createdAt) {
    return new CheckpointEntry(
        checkpointId,
        runId,
        traceId,
        checkpointIndex,
        type,
        status,
        snapshotJson,
        createdAt);
  }

  public String getCheckpointId() {
    return checkpointId;
  }

  public String getRunId() {
    return runId;
  }

  public String getTraceId() {
    return traceId;
  }

  public int getCheckpointIndex() {
    return checkpointIndex;
  }

  public CheckpointType getType() {
    return type;
  }

  public CheckpointStatus getStatus() {
    return status;
  }

  /** 冗余 ResearchRun 快照 JSON，禁止丢失。 */
  public String getSnapshotJson() {
    return snapshotJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

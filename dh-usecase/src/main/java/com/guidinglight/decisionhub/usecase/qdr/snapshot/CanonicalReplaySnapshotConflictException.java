package com.guidinglight.decisionhub.usecase.qdr.snapshot;

/** 相同 canonical snapshot identity 对应不同 immutable content 时的冲突异常。 */
public final class CanonicalReplaySnapshotConflictException
    extends CanonicalReplaySnapshotPersistenceException {

  /** 创建不暴露 payload 的固定冲突异常。 */
  public CanonicalReplaySnapshotConflictException() {
    super("canonical replay snapshot identity conflicts with immutable content");
  }
}

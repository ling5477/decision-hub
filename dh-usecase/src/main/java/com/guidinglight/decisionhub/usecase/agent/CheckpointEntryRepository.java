package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.checkpoint.CheckpointEntry;
import java.util.List;

/**
 * Stage2-PoC-B4：CheckpointEntry 仓储端口。
 *
 * <p>Batch 4 仅落 InMemory 实现；JDBC 实现留待 Batch 5。
 */
public interface CheckpointEntryRepository {

  /** 持久化一条 checkpoint。 */
  void save(CheckpointEntry entry);

  /** 按 runId 查询，按 checkpointIndex 升序返回。 */
  List<CheckpointEntry> listByRun(String runId);
}

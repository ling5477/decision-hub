package com.guidinglight.decisionhub.usecase.agent.inmemory;

import com.guidinglight.decisionhub.domain.checkpoint.CheckpointEntry;
import com.guidinglight.decisionhub.usecase.agent.CheckpointEntryRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stage2-PoC-B4：CheckpointEntry 的内存实现。
 *
 * <p>进程重启即丢；Batch 5 切到 JDBC（dh_checkpoint_entries）。
 */
public final class InMemoryCheckpointEntryRepository implements CheckpointEntryRepository {

  private final Map<String, List<CheckpointEntry>> indexByRun = new ConcurrentHashMap<>();

  /** 默认构造。 */
  public InMemoryCheckpointEntryRepository() {
    // no-op
  }

  @Override
  public void save(final CheckpointEntry entry) {
    indexByRun
        .computeIfAbsent(entry.getRunId(), k -> new ArrayList<>())
        .add(entry);
  }

  @Override
  public List<CheckpointEntry> listByRun(final String runId) {
    final List<CheckpointEntry> copy =
        new ArrayList<>(indexByRun.getOrDefault(runId, List.of()));
    copy.sort(Comparator.comparingInt(CheckpointEntry::getCheckpointIndex));
    return copy;
  }
}

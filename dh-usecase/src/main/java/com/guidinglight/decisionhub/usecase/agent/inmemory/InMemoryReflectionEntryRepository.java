package com.guidinglight.decisionhub.usecase.agent.inmemory;

import com.guidinglight.decisionhub.domain.reflection.ReflectionEntry;
import com.guidinglight.decisionhub.usecase.agent.ReflectionEntryRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stage2-PoC-B4：ReflectionEntry 的内存实现。
 *
 * <p>进程重启即丢；Batch 5 切到 JDBC（dh_reflection_entries）。
 */
public final class InMemoryReflectionEntryRepository implements ReflectionEntryRepository {

  private final Map<String, List<ReflectionEntry>> indexByRun = new ConcurrentHashMap<>();

  /** 默认构造。 */
  public InMemoryReflectionEntryRepository() {
    // no-op
  }

  @Override
  public void save(final ReflectionEntry entry) {
    indexByRun
        .computeIfAbsent(entry.getRunId(), k -> new ArrayList<>())
        .add(entry);
  }

  @Override
  public List<ReflectionEntry> listByRun(final String runId) {
    final List<ReflectionEntry> copy =
        new ArrayList<>(indexByRun.getOrDefault(runId, List.of()));
    copy.sort(Comparator.comparingInt(ReflectionEntry::getStepIndex));
    return copy;
  }
}

package com.guidinglight.decisionhub.connector.research.fake;

import com.guidinglight.decisionhub.connector.research.ResearchSnapshotStore;
import com.guidinglight.decisionhub.domain.marketdata.ExternalMarketSnapshot;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stage2-PoC-B3：ResearchSnapshotStore 的内存实现。
 *
 * <p>主索引：{@code snapshotId -> ExternalMarketSnapshot}；二级索引：{@code traceId -> snapshotId set}，
 * 用于 {@link #findByTraceId(String)}。本实现进程重启即丢，Batch 5 才落地 JDBC。
 */
public final class InMemoryResearchSnapshotStore implements ResearchSnapshotStore {

  private final Map<String, ExternalMarketSnapshot> bySnapshotId = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> snapshotIdsByTraceId = new ConcurrentHashMap<>();

  @Override
  public void save(final ExternalMarketSnapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot");
    bySnapshotId.put(snapshot.getSnapshotId(), snapshot);
    snapshotIdsByTraceId
        .computeIfAbsent(snapshot.getTraceId(), k -> ConcurrentHashMap.newKeySet())
        .add(snapshot.getSnapshotId());
  }

  @Override
  public Optional<ExternalMarketSnapshot> findById(final String snapshotId) {
    if (snapshotId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(bySnapshotId.get(snapshotId));
  }

  @Override
  public Optional<ExternalMarketSnapshot> findByTraceId(final String traceId) {
    if (traceId == null) {
      return Optional.empty();
    }
    final Set<String> ids = snapshotIdsByTraceId.get(traceId);
    if (ids == null || ids.isEmpty()) {
      return Optional.empty();
    }
    // 取插入顺序中第一个仍存在的 snapshot；Optional 语义已在接口 javadoc 声明可返回任一份。
    for (String id : new LinkedHashSet<>(ids)) {
      final ExternalMarketSnapshot snapshot = bySnapshotId.get(id);
      if (snapshot != null) {
        return Optional.of(snapshot);
      }
    }
    return Optional.empty();
  }

  @Override
  public List<ExternalMarketSnapshot> findBySymbolAndDateRange(
      final String symbol, final LocalDate start, final LocalDate end) {
    Objects.requireNonNull(symbol, "symbol");
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    if (end.isBefore(start)) {
      throw new IllegalArgumentException("end must not be before start");
    }
    final List<ExternalMarketSnapshot> hits = new ArrayList<>();
    for (ExternalMarketSnapshot snapshot : bySnapshotId.values()) {
      if (!snapshot.getSymbols().contains(symbol)) {
        continue;
      }
      // overlap 条件：snapshot.range 与 [start,end] 存在交集
      if (snapshot.getRangeEnd().isBefore(start) || snapshot.getRangeStart().isAfter(end)) {
        continue;
      }
      hits.add(snapshot);
    }
    return List.copyOf(hits);
  }
}

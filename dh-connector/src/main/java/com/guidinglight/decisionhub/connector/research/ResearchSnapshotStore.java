package com.guidinglight.decisionhub.connector.research;

import com.guidinglight.decisionhub.domain.marketdata.ExternalMarketSnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Stage2-PoC-B3：外部市场数据快照存储端口。
 *
 * <p>Stage2 阶段以 InMemory 实现；Batch 5 才落地 JDBC 实现。接口签名不暴露事务 / 缓存策略，
 * 由真实实现内部决定。
 */
public interface ResearchSnapshotStore {

  /** 保存一份外部市场数据快照；按 {@code snapshotId} 唯一覆盖写入。 */
  void save(ExternalMarketSnapshot snapshot);

  /** 按 snapshotId 精确查询。 */
  Optional<ExternalMarketSnapshot> findById(String snapshotId);

  /** 按 traceId 查询；同一 traceId 可能对应多份快照，实现可返回最近一份或任一份。 */
  Optional<ExternalMarketSnapshot> findByTraceId(String traceId);

  /** 按单 symbol + 日期区间查询；返回 rangeStart/rangeEnd 与查询区间存在重叠的所有快照。 */
  List<ExternalMarketSnapshot> findBySymbolAndDateRange(String symbol, LocalDate start, LocalDate end);
}

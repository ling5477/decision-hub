package com.guidinglight.decisionhub.connector.research;

import com.guidinglight.decisionhub.domain.marketdata.MarketDataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Stage2-PoC-B3：ResearchDataAdapter 的请求入参值对象。
 *
 * <p>{@code symbols} 不允许为空集合；{@code rangeStart} / {@code rangeEnd} 必须非空且 {@code rangeEnd} 不早于
 * {@code rangeStart}；{@code dataTypes} 允许为空（表示请求"无数据类型"，Fake 会返回空 data + COMPLETED）。
 */
public final class MarketSnapshotRequest {

  private final String traceId;
  private final List<String> symbols;
  private final MarketDataSource source;
  private final LocalDate rangeStart;
  private final LocalDate rangeEnd;
  private final List<String> dataTypes;

  private MarketSnapshotRequest(
      final String traceId,
      final List<String> symbols,
      final MarketDataSource source,
      final LocalDate rangeStart,
      final LocalDate rangeEnd,
      final List<String> dataTypes) {
    if (symbols == null || symbols.isEmpty()) {
      throw new IllegalArgumentException("symbols must not be empty");
    }
    this.rangeStart = Objects.requireNonNull(rangeStart, "rangeStart");
    this.rangeEnd = Objects.requireNonNull(rangeEnd, "rangeEnd");
    if (rangeEnd.isBefore(rangeStart)) {
      throw new IllegalArgumentException("rangeEnd must not be before rangeStart");
    }
    this.traceId = traceId;
    this.symbols = List.copyOf(symbols);
    this.source = Objects.requireNonNull(source, "source");
    this.dataTypes = dataTypes == null ? List.of() : List.copyOf(dataTypes);
  }

  /** 工厂方法。 */
  public static MarketSnapshotRequest of(
      final String traceId,
      final List<String> symbols,
      final MarketDataSource source,
      final LocalDate rangeStart,
      final LocalDate rangeEnd,
      final List<String> dataTypes) {
    return new MarketSnapshotRequest(traceId, symbols, source, rangeStart, rangeEnd, dataTypes);
  }

  public String getTraceId() {
    return traceId;
  }

  public List<String> getSymbols() {
    return symbols;
  }

  public MarketDataSource getSource() {
    return source;
  }

  public LocalDate getRangeStart() {
    return rangeStart;
  }

  public LocalDate getRangeEnd() {
    return rangeEnd;
  }

  public List<String> getDataTypes() {
    return dataTypes;
  }
}

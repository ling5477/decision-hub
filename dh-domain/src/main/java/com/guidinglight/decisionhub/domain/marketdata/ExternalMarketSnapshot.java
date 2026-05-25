package com.guidinglight.decisionhub.domain.marketdata;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Stage2-PoC-B1：外部市场数据快照。
 *
 * <p>本对象由 {@code dh-connector/research/ResearchDataAdapter} 在 Batch 3 真正调用 Fake 后产出； Batch 1 只固化领域形状与字段约束。
 *
 * <p>原始数据用 {@code dataJson} 承载多 symbol 的 OHLCV / FUNDAMENTALS / NEWS_SENTIMENT 等；
 * 原始 adapter 返回则用 {@code rawPayloadJson} 保留，禁止丢失。
 */
public final class ExternalMarketSnapshot {

  private final String snapshotId;
  private final String traceId;
  private final List<String> symbols;
  private final MarketDataSource source;
  private final LocalDate rangeStart;
  private final LocalDate rangeEnd;
  private final Instant fetchedAt;
  private final String dataJson;
  private final String sourceVersion;
  private final MarketSnapshotStatus status;
  private final String rawPayloadJson;

  private ExternalMarketSnapshot(
      final String snapshotId,
      final String traceId,
      final List<String> symbols,
      final MarketDataSource source,
      final LocalDate rangeStart,
      final LocalDate rangeEnd,
      final Instant fetchedAt,
      final String dataJson,
      final String sourceVersion,
      final MarketSnapshotStatus status,
      final String rawPayloadJson) {
    this.snapshotId = Objects.requireNonNull(snapshotId, "snapshotId");
    this.traceId = Objects.requireNonNull(traceId, "traceId");
    this.symbols = symbols == null ? List.of() : List.copyOf(symbols);
    if (this.symbols.isEmpty()) {
      throw new IllegalArgumentException("symbols must not be empty");
    }
    this.source = Objects.requireNonNull(source, "source");
    this.rangeStart = Objects.requireNonNull(rangeStart, "rangeStart");
    this.rangeEnd = Objects.requireNonNull(rangeEnd, "rangeEnd");
    if (rangeEnd.isBefore(rangeStart)) {
      throw new IllegalArgumentException("rangeEnd must not be before rangeStart");
    }
    this.fetchedAt = Objects.requireNonNull(fetchedAt, "fetchedAt");
    this.dataJson = Objects.requireNonNull(dataJson, "dataJson");
    this.sourceVersion = sourceVersion;
    this.status = Objects.requireNonNull(status, "status");
    this.rawPayloadJson = Objects.requireNonNull(rawPayloadJson, "rawPayloadJson");
  }

  /** 工厂方法。 */
  public static ExternalMarketSnapshot of(
      final String snapshotId,
      final String traceId,
      final List<String> symbols,
      final MarketDataSource source,
      final LocalDate rangeStart,
      final LocalDate rangeEnd,
      final Instant fetchedAt,
      final String dataJson,
      final String sourceVersion,
      final MarketSnapshotStatus status,
      final String rawPayloadJson) {
    return new ExternalMarketSnapshot(
        snapshotId,
        traceId,
        symbols,
        source,
        rangeStart,
        rangeEnd,
        fetchedAt,
        dataJson,
        sourceVersion,
        status,
        rawPayloadJson);
  }

  public String getSnapshotId() {
    return snapshotId;
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

  public Instant getFetchedAt() {
    return fetchedAt;
  }

  /** 整段 JSON 字符串，承载多 symbol 多 dataType 的结构化数据。 */
  public String getDataJson() {
    return dataJson;
  }

  /** 数据源版本号，可空。 */
  public String getSourceVersion() {
    return sourceVersion;
  }

  public MarketSnapshotStatus getStatus() {
    return status;
  }

  /** 原始 adapter 返回 JSON，禁止丢失。 */
  public String getRawPayloadJson() {
    return rawPayloadJson;
  }
}

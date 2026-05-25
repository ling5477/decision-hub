package com.guidinglight.decisionhub.connector.research.fake;

import com.guidinglight.decisionhub.connector.research.MarketSnapshotRequest;
import com.guidinglight.decisionhub.connector.research.ResearchDataAdapter;
import com.guidinglight.decisionhub.domain.marketdata.ExternalMarketSnapshot;
import com.guidinglight.decisionhub.domain.marketdata.MarketDataSource;
import com.guidinglight.decisionhub.domain.marketdata.MarketSnapshotStatus;
import java.time.Instant;
import java.util.Objects;

/**
 * Stage2-PoC-B3：ResearchDataAdapter 的 Fake 实现。
 *
 * <p>不访问真实 global-stock-data。固定返回 deterministic mock，便于 Stage2 闭环测试。
 *
 * <p>非空 {@code rawPayloadJson} / {@code dataJson} 是强约束；空 dataTypes 返回 {@code "{}"}。
 */
public final class FakeResearchDataAdapter implements ResearchDataAdapter {

  /** 固定的 mock 抓取时间，保证测试 deterministic。 */
  static final Instant FIXED_FETCHED_AT = Instant.parse("2026-05-25T00:00:00Z");

  /** Fake 数据源版本号。 */
  static final String FAKE_SOURCE_VERSION = "global-stock-data-fake-v0";

  @Override
  public ExternalMarketSnapshot fetchSnapshot(final MarketSnapshotRequest request) {
    Objects.requireNonNull(request, "request");
    // request 自身已经在工厂校验过 symbols / dateRange，这里再次显式校验以满足 Port 契约。
    if (request.getSymbols() == null || request.getSymbols().isEmpty()) {
      throw new IllegalArgumentException("symbols must not be empty");
    }
    if (request.getRangeEnd().isBefore(request.getRangeStart())) {
      throw new IllegalArgumentException("rangeEnd must not be before rangeStart");
    }

    final String traceId = request.getTraceId() == null ? "fake-trace" : request.getTraceId();
    final String snapshotId =
        "fake-snapshot-"
            + traceId
            + "-"
            + String.join("_", request.getSymbols())
            + "-"
            + request.getRangeStart()
            + "_"
            + request.getRangeEnd();

    final String dataJson = request.getDataTypes().isEmpty() ? "{}" : buildDataJson(request);
    final String rawPayloadJson =
        "{\"source\":\"fake-research\",\"symbols\":"
            + toJsonArray(request.getSymbols())
            + ",\"dataTypes\":"
            + toJsonArray(request.getDataTypes())
            + ",\"rangeStart\":\""
            + request.getRangeStart()
            + "\",\"rangeEnd\":\""
            + request.getRangeEnd()
            + "\"}";

    return ExternalMarketSnapshot.of(
        snapshotId,
        traceId,
        request.getSymbols(),
        MarketDataSource.FAKE,
        request.getRangeStart(),
        request.getRangeEnd(),
        FIXED_FETCHED_AT,
        dataJson,
        FAKE_SOURCE_VERSION,
        MarketSnapshotStatus.COMPLETED,
        rawPayloadJson);
  }

  private static String buildDataJson(final MarketSnapshotRequest request) {
    final StringBuilder sb = new StringBuilder("{");
    boolean firstSymbol = true;
    for (String symbol : request.getSymbols()) {
      if (!firstSymbol) {
        sb.append(',');
      }
      firstSymbol = false;
      sb.append('"').append(symbol).append("\":{");
      boolean firstType = true;
      for (String type : request.getDataTypes()) {
        if (!firstType) {
          sb.append(',');
        }
        firstType = false;
        sb.append('"').append(type).append("\":\"fake\"");
      }
      sb.append('}');
    }
    sb.append('}');
    return sb.toString();
  }

  private static String toJsonArray(final java.util.List<String> values) {
    final StringBuilder sb = new StringBuilder("[");
    boolean first = true;
    for (String v : values) {
      if (!first) {
        sb.append(',');
      }
      first = false;
      sb.append('"').append(v).append('"');
    }
    sb.append(']');
    return sb.toString();
  }
}

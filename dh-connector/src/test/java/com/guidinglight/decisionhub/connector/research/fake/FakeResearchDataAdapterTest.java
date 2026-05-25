package com.guidinglight.decisionhub.connector.research.fake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.connector.research.MarketSnapshotRequest;
import com.guidinglight.decisionhub.domain.marketdata.ExternalMarketSnapshot;
import com.guidinglight.decisionhub.domain.marketdata.MarketDataSource;
import com.guidinglight.decisionhub.domain.marketdata.MarketSnapshotStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Stage2-PoC-B3：FakeResearchDataAdapter happy path + 拒绝场景 + 空 dataTypes 行为。 */
class FakeResearchDataAdapterTest {

  private final FakeResearchDataAdapter adapter = new FakeResearchDataAdapter();

  @Test
  void fetchSnapshot_happyPath_returnsCompletedWithRawPayload() {
    final MarketSnapshotRequest request =
        MarketSnapshotRequest.of(
            "trace-1",
            List.of("AAPL", "MSFT"),
            MarketDataSource.FAKE,
            LocalDate.parse("2026-05-01"),
            LocalDate.parse("2026-05-25"),
            List.of("OHLCV", "FUNDAMENTALS"));

    final ExternalMarketSnapshot snapshot = adapter.fetchSnapshot(request);

    assertNotNull(snapshot);
    assertEquals("trace-1", snapshot.getTraceId());
    assertEquals(List.of("AAPL", "MSFT"), snapshot.getSymbols());
    assertEquals(MarketDataSource.FAKE, snapshot.getSource());
    assertEquals(LocalDate.parse("2026-05-01"), snapshot.getRangeStart());
    assertEquals(LocalDate.parse("2026-05-25"), snapshot.getRangeEnd());
    assertEquals(MarketSnapshotStatus.COMPLETED, snapshot.getStatus());
    assertSame(FakeResearchDataAdapter.FIXED_FETCHED_AT, snapshot.getFetchedAt());
    assertEquals(FakeResearchDataAdapter.FAKE_SOURCE_VERSION, snapshot.getSourceVersion());
    assertNotNull(snapshot.getRawPayloadJson());
    assertFalse(snapshot.getRawPayloadJson().isBlank());
    assertTrue(snapshot.getRawPayloadJson().contains("\"source\":\"fake-research\""));
    assertNotNull(snapshot.getDataJson());
    assertTrue(snapshot.getDataJson().contains("AAPL"));
    assertTrue(snapshot.getDataJson().contains("OHLCV"));

    final ExternalMarketSnapshot second = adapter.fetchSnapshot(request);
    assertEquals(snapshot.getSnapshotId(), second.getSnapshotId());
    assertEquals(snapshot.getRawPayloadJson(), second.getRawPayloadJson());
  }

  @Test
  void fetchSnapshot_emptySymbols_rejected() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            MarketSnapshotRequest.of(
                "trace-1",
                List.of(),
                MarketDataSource.FAKE,
                LocalDate.parse("2026-05-01"),
                LocalDate.parse("2026-05-25"),
                List.of("OHLCV")));
  }

  @Test
  void fetchSnapshot_startAfterEnd_rejected() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            MarketSnapshotRequest.of(
                "trace-1",
                List.of("AAPL"),
                MarketDataSource.FAKE,
                LocalDate.parse("2026-05-25"),
                LocalDate.parse("2026-05-01"),
                List.of("OHLCV")));
  }

  @Test
  void fetchSnapshot_emptyDataTypes_returnsEmptyDataAndCompleted() {
    final MarketSnapshotRequest request =
        MarketSnapshotRequest.of(
            "trace-2",
            List.of("AAPL"),
            MarketDataSource.FAKE,
            LocalDate.parse("2026-05-01"),
            LocalDate.parse("2026-05-25"),
            List.of());

    final ExternalMarketSnapshot snapshot = adapter.fetchSnapshot(request);

    assertEquals("{}", snapshot.getDataJson());
    assertEquals(MarketSnapshotStatus.COMPLETED, snapshot.getStatus());
    assertNotNull(snapshot.getRawPayloadJson());
    assertFalse(snapshot.getRawPayloadJson().isBlank());
  }
}

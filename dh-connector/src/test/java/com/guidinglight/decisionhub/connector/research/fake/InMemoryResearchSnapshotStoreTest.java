package com.guidinglight.decisionhub.connector.research.fake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.marketdata.ExternalMarketSnapshot;
import com.guidinglight.decisionhub.domain.marketdata.MarketDataSource;
import com.guidinglight.decisionhub.domain.marketdata.MarketSnapshotStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Stage2-PoC-B3：InMemoryResearchSnapshotStore save / findById / findByTraceId / findBySymbolAndDateRange。 */
class InMemoryResearchSnapshotStoreTest {

  private final InMemoryResearchSnapshotStore store = new InMemoryResearchSnapshotStore();

  private static ExternalMarketSnapshot snapshot(
      final String snapshotId,
      final String traceId,
      final List<String> symbols,
      final LocalDate rangeStart,
      final LocalDate rangeEnd) {
    return ExternalMarketSnapshot.of(
        snapshotId,
        traceId,
        symbols,
        MarketDataSource.FAKE,
        rangeStart,
        rangeEnd,
        Instant.parse("2026-05-25T00:00:00Z"),
        "{}",
        "fake-v0",
        MarketSnapshotStatus.COMPLETED,
        "{\"source\":\"fake\"}");
  }

  @Test
  void saveThenFindByIdAndTraceId() {
    final ExternalMarketSnapshot s1 =
        snapshot(
            "snap-1",
            "trace-1",
            List.of("AAPL"),
            LocalDate.parse("2026-05-01"),
            LocalDate.parse("2026-05-25"));
    store.save(s1);

    final Optional<ExternalMarketSnapshot> byId = store.findById("snap-1");
    assertTrue(byId.isPresent());
    assertSame(s1, byId.get());

    final Optional<ExternalMarketSnapshot> byTrace = store.findByTraceId("trace-1");
    assertTrue(byTrace.isPresent());
    assertEquals("snap-1", byTrace.get().getSnapshotId());

    assertFalse(store.findById("missing").isPresent());
    assertFalse(store.findByTraceId("missing").isPresent());
    assertFalse(store.findById(null).isPresent());
    assertFalse(store.findByTraceId(null).isPresent());
  }

  @Test
  void findBySymbolAndDateRange_hitAndMiss() {
    store.save(
        snapshot(
            "snap-1",
            "trace-1",
            List.of("AAPL", "MSFT"),
            LocalDate.parse("2026-05-01"),
            LocalDate.parse("2026-05-15")));
    store.save(
        snapshot(
            "snap-2",
            "trace-2",
            List.of("AAPL"),
            LocalDate.parse("2026-05-20"),
            LocalDate.parse("2026-05-25")));
    store.save(
        snapshot(
            "snap-3",
            "trace-3",
            List.of("GOOG"),
            LocalDate.parse("2026-05-01"),
            LocalDate.parse("2026-05-25")));

    final List<ExternalMarketSnapshot> aaplMay =
        store.findBySymbolAndDateRange(
            "AAPL", LocalDate.parse("2026-05-01"), LocalDate.parse("2026-05-31"));
    assertEquals(2, aaplMay.size());

    final List<ExternalMarketSnapshot> aaplGap =
        store.findBySymbolAndDateRange(
            "AAPL", LocalDate.parse("2026-05-16"), LocalDate.parse("2026-05-19"));
    assertTrue(aaplGap.isEmpty());

    final List<ExternalMarketSnapshot> tsla =
        store.findBySymbolAndDateRange(
            "TSLA", LocalDate.parse("2026-05-01"), LocalDate.parse("2026-05-25"));
    assertTrue(tsla.isEmpty());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            store.findBySymbolAndDateRange(
                "AAPL", LocalDate.parse("2026-05-25"), LocalDate.parse("2026-05-01")));
  }
}

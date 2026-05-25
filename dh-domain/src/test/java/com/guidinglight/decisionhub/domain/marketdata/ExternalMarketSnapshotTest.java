package com.guidinglight.decisionhub.domain.marketdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Stage2-PoC-B1：ExternalMarketSnapshot 字段、source enum、dataJson 与 rawPayloadJson 不丢失。 */
class ExternalMarketSnapshotTest {

  @Test
  void of_keepsRequiredAndRaw() {
    final ExternalMarketSnapshot s =
        ExternalMarketSnapshot.of(
            "snap-1",
            "trace-1",
            List.of("AAPL", "MSFT"),
            MarketDataSource.FAKE,
            LocalDate.parse("2025-01-01"),
            LocalDate.parse("2026-05-25"),
            Instant.parse("2026-05-25T08:00:00Z"),
            "{\"AAPL\":{\"ohlcv\":[]}}",
            "fake-v0",
            MarketSnapshotStatus.COMPLETED,
            "{\"src\":\"fake\"}");
    assertEquals("snap-1", s.getSnapshotId());
    assertEquals(MarketDataSource.FAKE, s.getSource());
    assertEquals(MarketSnapshotStatus.COMPLETED, s.getStatus());
    assertEquals("{\"AAPL\":{\"ohlcv\":[]}}", s.getDataJson());
    assertEquals("{\"src\":\"fake\"}", s.getRawPayloadJson());
  }

  @Test
  void of_rejectsEmptySymbolsOrInvertedRange() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ExternalMarketSnapshot.of(
                "s",
                "t",
                List.of(),
                MarketDataSource.FAKE,
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2025-01-01"),
                Instant.now(),
                "{}",
                null,
                MarketSnapshotStatus.COMPLETED,
                "{}"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ExternalMarketSnapshot.of(
                "s",
                "t",
                List.of("AAPL"),
                MarketDataSource.FAKE,
                LocalDate.parse("2025-12-31"),
                LocalDate.parse("2024-01-01"),
                Instant.now(),
                "{}",
                null,
                MarketSnapshotStatus.COMPLETED,
                "{}"));
  }

  @Test
  void enums_cover() {
    assertEquals(3, MarketDataSource.values().length);
    assertEquals(3, MarketSnapshotStatus.values().length);
  }
}

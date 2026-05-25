package com.guidinglight.decisionhub.domain.forecast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Stage2-PoC-B1：ForecastArtifact 字段、enum、rawPayloadJson 不丢失。 */
class ForecastArtifactTest {

  @Test
  void of_keepsRequiredFields() {
    final ForecastArtifact a =
        ForecastArtifact.of(
            "art-1",
            "trace-1",
            "AAPL",
            ForecastHorizon.D5,
            ForecastTarget.PRICE,
            List.of(
                ForecastPoint.of(LocalDate.parse("2026-06-01"), 150.5, 0.72),
                ForecastPoint.of(LocalDate.parse("2026-06-02"), 151.2, 0.68)),
            "kronos-fake-v0",
            Instant.parse("2026-05-25T08:00:00Z"),
            ForecastArtifactStatus.COMPLETED,
            "{\"src\":\"fake\"}");
    assertEquals("art-1", a.getArtifactId());
    assertEquals("trace-1", a.getTraceId());
    assertEquals(ForecastHorizon.D5, a.getHorizon());
    assertEquals(ForecastTarget.PRICE, a.getTarget());
    assertEquals(ForecastArtifactStatus.COMPLETED, a.getStatus());
    assertEquals(2, a.getPredictions().size());
    assertEquals("{\"src\":\"fake\"}", a.getRawPayloadJson());
    assertNotNull(a.getModelVersion());
  }

  @Test
  void confidence_outOfRangeRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ForecastPoint.of(LocalDate.parse("2026-06-01"), 1.0, -0.1));
    assertThrows(
        IllegalArgumentException.class,
        () -> ForecastPoint.of(LocalDate.parse("2026-06-01"), 1.0, 1.5));
  }

  @Test
  void enums_cover4Each() {
    assertEquals(4, ForecastHorizon.values().length);
    assertEquals(4, ForecastTarget.values().length);
    assertEquals(4, ForecastArtifactStatus.values().length);
  }

  @Test
  void predictions_areImmutable() {
    final ForecastArtifact a =
        ForecastArtifact.of(
            "art-1",
            "trace-1",
            "AAPL",
            ForecastHorizon.D1,
            ForecastTarget.PRICE,
            List.of(),
            null,
            Instant.now(),
            ForecastArtifactStatus.COMPLETED,
            "{}");
    assertTrue(a.getPredictions().isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> a.getPredictions().add(null));
  }
}

package com.guidinglight.decisionhub.connector.tools.fake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.connector.tools.ForecastRequest;
import com.guidinglight.decisionhub.domain.forecast.ForecastArtifact;
import com.guidinglight.decisionhub.domain.forecast.ForecastArtifactStatus;
import com.guidinglight.decisionhub.domain.forecast.ForecastHorizon;
import com.guidinglight.decisionhub.domain.forecast.ForecastTarget;
import org.junit.jupiter.api.Test;

/** Stage2-PoC-B3：FakeForecastToolAdapter happy path + 拒绝场景。 */
class FakeForecastToolAdapterTest {

  private final FakeForecastToolAdapter adapter = new FakeForecastToolAdapter();

  @Test
  void requestForecast_happyPath_returnsCompletedWithRawPayload() {
    final ForecastRequest request =
        ForecastRequest.of("trace-1", "AAPL", ForecastHorizon.D5, ForecastTarget.PRICE);

    final ForecastArtifact artifact = adapter.requestForecast(request);

    assertNotNull(artifact);
    assertEquals("trace-1", artifact.getTraceId());
    assertEquals("AAPL", artifact.getSymbol());
    assertEquals(ForecastHorizon.D5, artifact.getHorizon());
    assertEquals(ForecastTarget.PRICE, artifact.getTarget());
    assertEquals(ForecastArtifactStatus.COMPLETED, artifact.getStatus());
    assertEquals(FakeForecastToolAdapter.FAKE_MODEL_VERSION, artifact.getModelVersion());
    assertSame(FakeForecastToolAdapter.FIXED_GENERATED_AT, artifact.getGeneratedAt());
    assertEquals(2, artifact.getPredictions().size());
    assertNotNull(artifact.getRawPayloadJson());
    assertFalse(artifact.getRawPayloadJson().isBlank());
    assertTrue(artifact.getRawPayloadJson().contains("\"symbol\":\"AAPL\""));

    final ForecastArtifact second = adapter.requestForecast(request);
    assertEquals(artifact.getArtifactId(), second.getArtifactId());
    assertEquals(artifact.getRawPayloadJson(), second.getRawPayloadJson());
  }

  @Test
  void requestForecast_blankSymbol_rejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ForecastRequest.of("trace-1", "", ForecastHorizon.D1, ForecastTarget.PRICE));
    assertThrows(
        IllegalArgumentException.class,
        () -> ForecastRequest.of("trace-1", "   ", ForecastHorizon.D1, ForecastTarget.PRICE));
  }

  @Test
  void requestForecast_nullHorizon_rejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ForecastRequest.of("trace-1", "AAPL", null, ForecastTarget.PRICE));
  }
}

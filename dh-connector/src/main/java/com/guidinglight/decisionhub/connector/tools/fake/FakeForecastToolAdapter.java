package com.guidinglight.decisionhub.connector.tools.fake;

import com.guidinglight.decisionhub.connector.tools.ForecastRequest;
import com.guidinglight.decisionhub.connector.tools.ForecastToolPort;
import com.guidinglight.decisionhub.domain.forecast.ForecastArtifact;
import com.guidinglight.decisionhub.domain.forecast.ForecastArtifactStatus;
import com.guidinglight.decisionhub.domain.forecast.ForecastHorizon;
import com.guidinglight.decisionhub.domain.forecast.ForecastPoint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Stage2-PoC-B3：ForecastToolPort 的 Fake 实现。
 *
 * <p>不访问真实 Kronos / Python 推理服务。固定返回 deterministic mock，便于 Stage2 闭环测试。
 *
 * <p>非空 {@code rawPayloadJson} 是强约束，最低 "{}"。
 */
public final class FakeForecastToolAdapter implements ForecastToolPort {

  /** 固定的 mock 生成时间，保证测试 deterministic。 */
  static final Instant FIXED_GENERATED_AT = Instant.parse("2026-05-25T00:00:00Z");

  /** 固定的 mock 基准日期，predictions 的起点。 */
  static final LocalDate FIXED_BASE_DATE = LocalDate.parse("2026-05-25");

  /** Fake 模型版本号。 */
  static final String FAKE_MODEL_VERSION = "kronos-fake-v0";

  @Override
  public ForecastArtifact requestForecast(final ForecastRequest request) {
    Objects.requireNonNull(request, "request");
    final String symbol = request.getSymbol();
    if (symbol == null || symbol.isBlank()) {
      throw new IllegalArgumentException("symbol must not be blank");
    }
    final ForecastHorizon horizon = request.getHorizon();
    if (horizon == null) {
      throw new IllegalArgumentException("horizon must not be null");
    }
    final String traceId = request.getTraceId() == null ? "fake-trace" : request.getTraceId();
    final String artifactId = "fake-forecast-" + traceId + "-" + symbol + "-" + horizon.name();

    final List<ForecastPoint> predictions =
        List.of(
            ForecastPoint.of(FIXED_BASE_DATE.plusDays(1), 100.0, 0.80),
            ForecastPoint.of(FIXED_BASE_DATE.plusDays(2), 101.5, 0.75));

    final String rawPayloadJson =
        "{\"source\":\"fake-forecast\",\"symbol\":\""
            + symbol
            + "\",\"horizon\":\""
            + horizon.name()
            + "\",\"target\":\""
            + request.getTarget().name()
            + "\"}";

    return ForecastArtifact.of(
        artifactId,
        traceId,
        symbol,
        horizon,
        request.getTarget(),
        predictions,
        FAKE_MODEL_VERSION,
        FIXED_GENERATED_AT,
        ForecastArtifactStatus.COMPLETED,
        rawPayloadJson);
  }
}

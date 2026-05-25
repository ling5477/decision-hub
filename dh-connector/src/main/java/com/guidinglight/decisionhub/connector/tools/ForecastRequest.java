package com.guidinglight.decisionhub.connector.tools;

import com.guidinglight.decisionhub.domain.forecast.ForecastHorizon;
import com.guidinglight.decisionhub.domain.forecast.ForecastTarget;

/**
 * Stage2-PoC-B3：ForecastToolPort 的请求入参值对象。
 *
 * <p>Stage2 PoC 阶段同步占位；不暴露超时 / 重试 / 缓存等运行时参数（这些由真实接入时的适配器内部处理）。
 *
 * <p>{@code symbol} 必须非空，{@code horizon} / {@code target} 必须非空；{@code traceId} 可空，用于把
 * 预测结果挂回 DH 链路。
 */
public final class ForecastRequest {

  private final String traceId;
  private final String symbol;
  private final ForecastHorizon horizon;
  private final ForecastTarget target;

  private ForecastRequest(
      final String traceId,
      final String symbol,
      final ForecastHorizon horizon,
      final ForecastTarget target) {
    if (symbol == null || symbol.isBlank()) {
      throw new IllegalArgumentException("symbol must not be blank");
    }
    if (horizon == null) {
      throw new IllegalArgumentException("horizon must not be null");
    }
    if (target == null) {
      throw new IllegalArgumentException("target must not be null");
    }
    this.traceId = traceId;
    this.symbol = symbol;
    this.horizon = horizon;
    this.target = target;
  }

  /** 工厂方法。 */
  public static ForecastRequest of(
      final String traceId,
      final String symbol,
      final ForecastHorizon horizon,
      final ForecastTarget target) {
    return new ForecastRequest(traceId, symbol, horizon, target);
  }

  public String getTraceId() {
    return traceId;
  }

  public String getSymbol() {
    return symbol;
  }

  public ForecastHorizon getHorizon() {
    return horizon;
  }

  public ForecastTarget getTarget() {
    return target;
  }
}

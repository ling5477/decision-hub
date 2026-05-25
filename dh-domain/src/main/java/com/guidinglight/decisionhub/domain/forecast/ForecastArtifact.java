package com.guidinglight.decisionhub.domain.forecast;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Stage2-PoC-B1：Kronos / 其他时序预测工具的产物。
 *
 * <p>本对象由 {@code dh-connector/tools/ForecastToolPort} 在 Batch 3 真正调用 Fake 后产出； Batch 1 只固化领域形状与字段约束。
 *
 * <p>{@code rawPayloadJson} 必须保留原始 adapter 返回（含 Fake 的 mock JSON），不允许丢失原始数据。
 */
public final class ForecastArtifact {

  private final String artifactId;
  private final String traceId;
  private final String symbol;
  private final ForecastHorizon horizon;
  private final ForecastTarget target;
  private final List<ForecastPoint> predictions;
  private final String modelVersion;
  private final Instant generatedAt;
  private final ForecastArtifactStatus status;
  private final String rawPayloadJson;

  private ForecastArtifact(
      final String artifactId,
      final String traceId,
      final String symbol,
      final ForecastHorizon horizon,
      final ForecastTarget target,
      final List<ForecastPoint> predictions,
      final String modelVersion,
      final Instant generatedAt,
      final ForecastArtifactStatus status,
      final String rawPayloadJson) {
    this.artifactId = Objects.requireNonNull(artifactId, "artifactId");
    this.traceId = Objects.requireNonNull(traceId, "traceId");
    this.symbol = Objects.requireNonNull(symbol, "symbol");
    this.horizon = Objects.requireNonNull(horizon, "horizon");
    this.target = Objects.requireNonNull(target, "target");
    this.predictions = predictions == null ? List.of() : List.copyOf(predictions);
    this.modelVersion = modelVersion;
    this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
    this.status = Objects.requireNonNull(status, "status");
    this.rawPayloadJson = Objects.requireNonNull(rawPayloadJson, "rawPayloadJson");
  }

  /** 工厂方法。 */
  public static ForecastArtifact of(
      final String artifactId,
      final String traceId,
      final String symbol,
      final ForecastHorizon horizon,
      final ForecastTarget target,
      final List<ForecastPoint> predictions,
      final String modelVersion,
      final Instant generatedAt,
      final ForecastArtifactStatus status,
      final String rawPayloadJson) {
    return new ForecastArtifact(
        artifactId,
        traceId,
        symbol,
        horizon,
        target,
        predictions,
        modelVersion,
        generatedAt,
        status,
        rawPayloadJson);
  }

  public String getArtifactId() {
    return artifactId;
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

  public List<ForecastPoint> getPredictions() {
    return predictions;
  }

  /** 模型版本，可空（Fake 给固定字符串）。 */
  public String getModelVersion() {
    return modelVersion;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public ForecastArtifactStatus getStatus() {
    return status;
  }

  /** 原始 adapter 返回 JSON，禁止丢失。 */
  public String getRawPayloadJson() {
    return rawPayloadJson;
  }
}

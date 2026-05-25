package com.guidinglight.decisionhub.domain.forecast;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Stage2-PoC-B1：单点预测值。
 *
 * <p>{@code confidence} 范围 [0.0, 1.0]；由模型给出，DH 不强行重打分。
 */
public final class ForecastPoint {

  private final LocalDate date;
  private final double value;
  private final double confidence;

  private ForecastPoint(final LocalDate date, final double value, final double confidence) {
    this.date = Objects.requireNonNull(date, "date");
    this.value = value;
    if (confidence < 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException("confidence must be in [0.0, 1.0]");
    }
    this.confidence = confidence;
  }

  /** 工厂方法。 */
  public static ForecastPoint of(final LocalDate date, final double value, final double confidence) {
    return new ForecastPoint(date, value, confidence);
  }

  public LocalDate getDate() {
    return date;
  }

  public double getValue() {
    return value;
  }

  public double getConfidence() {
    return confidence;
  }
}

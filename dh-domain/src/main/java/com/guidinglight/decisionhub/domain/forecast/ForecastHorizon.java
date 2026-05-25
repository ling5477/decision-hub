package com.guidinglight.decisionhub.domain.forecast;

/**
 * Stage2-PoC-B1：预测时间跨度。
 *
 * <p>JSON 表达对照：D1 -> "1D"，D5 -> "5D"，D20 -> "20D"，D60 -> "60D"。
 */
public enum ForecastHorizon {
  D1,
  D5,
  D20,
  D60
}

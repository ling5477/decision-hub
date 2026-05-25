package com.guidinglight.decisionhub.domain.forecast;

/**
 * Stage2-PoC-B1：Kronos / 其他时序预测工具的预测目标。
 *
 * <p>本枚举不绑定具体模型；用于在请求中标明 DH 希望得到什么类型的预测值。
 */
public enum ForecastTarget {
  /** 价格。 */
  PRICE,
  /** 波动率。 */
  VOLATILITY,
  /** 方向（涨跌）。 */
  DIRECTION,
  /** 成交量。 */
  VOLUME
}

package com.guidinglight.decisionhub.domain.backtest;

/**
 * Stage2-PoC-B1：回测频率。
 *
 * <p>由 NQ 侧定义合法值；本枚举仅承载 DH 在请求中允许选用的频率。
 */
public enum BacktestFrequency {
  /** 日级。 */
  DAILY,
  /** 小时级。 */
  HOURLY,
  /** 分钟级。 */
  MINUTE
}

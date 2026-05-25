package com.guidinglight.decisionhub.domain.backtest;

/**
 * Stage2-PoC-B1：回测结论分级。
 *
 * <p>由 NQ 侧 backtest 或 paper run 报告中给出；DH 用于经验权重和后续 reflection。 多场景复用：{@link
 * com.guidinglight.decisionhub.domain.feedback.payload.BacktestResultReadyPayload} 与 {@link
 * DhBacktestResultSnapshot}。
 */
public enum BacktestVerdict {
  /** 通过。 */
  PASS,
  /** 未通过。 */
  FAIL,
  /** 边缘，建议进一步评估。 */
  MARGINAL
}

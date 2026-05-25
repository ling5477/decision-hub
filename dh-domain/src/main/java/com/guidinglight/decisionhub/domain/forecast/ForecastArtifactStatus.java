package com.guidinglight.decisionhub.domain.forecast;

/**
 * Stage2-PoC-B1：预测产物状态。
 *
 * <p>{@link #PENDING} / {@link #TIMEOUT} 是为未来真实接入预留的；Stage2 Fake 适配器只产出 {@link #COMPLETED} 或 {@link #FAILED}。
 */
public enum ForecastArtifactStatus {
  /** 已完成。 */
  COMPLETED,
  /** 进行中。 */
  PENDING,
  /** 失败。 */
  FAILED,
  /** 超时。 */
  TIMEOUT
}

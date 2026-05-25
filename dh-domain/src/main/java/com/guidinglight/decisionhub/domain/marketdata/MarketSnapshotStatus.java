package com.guidinglight.decisionhub.domain.marketdata;

/**
 * Stage2-PoC-B1：外部市场数据快照状态。
 *
 * <p>{@link #PENDING} 为未来异步接入预留；Stage2 Fake 适配器只产出 {@link #COMPLETED} 或 {@link #FAILED}。
 */
public enum MarketSnapshotStatus {
  COMPLETED,
  PENDING,
  FAILED
}

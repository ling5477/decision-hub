package com.guidinglight.decisionhub.domain.marketdata;

/**
 * Stage2-PoC-B1：外部市场数据快照的来源标识。
 *
 * <p>Stage2 PoC 不接真实 global-stock-data；本枚举区分 Fake / 真实接入 / 缓存命中，便于回溯。
 */
public enum MarketDataSource {
  /** 真实 global-stock-data 拉取（Stage2 暂不实现，留作未来接入）。 */
  GLOBAL_STOCK_DATA,
  /** DH 内部缓存复用。 */
  INTERNAL_CACHE,
  /** Stage2 Fake 适配器。 */
  FAKE
}

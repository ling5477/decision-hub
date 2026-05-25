package com.guidinglight.decisionhub.connector.research;

import com.guidinglight.decisionhub.domain.marketdata.ExternalMarketSnapshot;

/**
 * Stage2-PoC-B3：global-stock-data 等外部市场数据源的端口接口。
 *
 * <p>Stage2 阶段为端口预留，仅提供同步签名。真实接入由未来的适配器实现（HTTP 调用 + 磁盘缓存 + Resilience4j），
 * 不在本接口暴露超时 / 重试参数。
 *
 * <p>实现方必须保证返回的 {@link ExternalMarketSnapshot#getRawPayloadJson()} 非空（最低 "{}"），
 * 失败 fallback 也需写入异常摘要；不允许把敏感凭据写入 rawPayloadJson。
 */
public interface ResearchDataAdapter {

  /**
   * 同步拉取一次外部市场数据快照。
   *
   * @param request 包含 symbols / dateRange / dataTypes 等的请求；空 symbols 或非法 dateRange 必须拒绝。
   * @return 外部市场数据快照；Stage2 Fake 只返回 {@link
   *     com.guidinglight.decisionhub.domain.marketdata.MarketSnapshotStatus#COMPLETED} 或
   *     {@link com.guidinglight.decisionhub.domain.marketdata.MarketSnapshotStatus#FAILED}。
   * @throws IllegalArgumentException 当 request 不合法（symbols 空 / rangeEnd 早于 rangeStart）时抛出。
   */
  ExternalMarketSnapshot fetchSnapshot(MarketSnapshotRequest request);
}

package com.guidinglight.decisionhub.connector.tools;

import com.guidinglight.decisionhub.domain.forecast.ForecastArtifact;

/**
 * Stage2-PoC-B3：Kronos / 其他时序预测工具的端口接口。
 *
 * <p>Stage2 阶段为端口预留，仅提供同步签名；真实接入（HTTP / Python 推理服务）由未来的
 * {@code dh-connector/tools/http/Kronos*HttpAdapter} 通过 Resilience4j 在适配器内部处理
 * timeout / circuit-breaker / retry，不污染本接口。
 *
 * <p>实现方必须保证返回的 {@link ForecastArtifact#getRawPayloadJson()} 非空（最低 "{}"），
 * 失败 fallback 也需写入异常摘要。
 */
public interface ForecastToolPort {

  /**
   * 同步请求一次预测。
   *
   * @param request 预测入参，必须包含非空 symbol / horizon / target。
   * @return 预测产物；status 至少为 {@link
   *     com.guidinglight.decisionhub.domain.forecast.ForecastArtifactStatus#COMPLETED} 或
   *     {@link com.guidinglight.decisionhub.domain.forecast.ForecastArtifactStatus#FAILED}。
   * @throws IllegalArgumentException 当 request 不合法（symbol/horizon/target 为空）时抛出。
   */
  ForecastArtifact requestForecast(ForecastRequest request);
}

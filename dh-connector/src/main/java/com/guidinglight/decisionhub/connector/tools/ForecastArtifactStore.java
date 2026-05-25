package com.guidinglight.decisionhub.connector.tools;

import com.guidinglight.decisionhub.domain.forecast.ForecastArtifact;
import java.util.List;
import java.util.Optional;

/**
 * Stage2-PoC-B5：ForecastArtifact 持久化端口。
 *
 * <p>Stage2 阶段以 InMemory 实现承载 Fake 工具产物；Batch 5 起新增 JDBC 实现（dh_forecast_artifacts）。
 * 接口签名不暴露事务 / 缓存策略，由真实实现内部决定。
 */
public interface ForecastArtifactStore {

  /** 持久化一条预测产物；按 {@code artifactId} 唯一覆盖写入。 */
  void save(ForecastArtifact artifact);

  /** 按 artifactId 精确查询。 */
  Optional<ForecastArtifact> findById(String artifactId);

  /** 按 traceId 查询；同一 traceId 可能对应多份产物，按 generatedAt 升序返回。 */
  List<ForecastArtifact> findByTraceId(String traceId);

  /** 按 symbol 查询；按 generatedAt 升序返回。 */
  List<ForecastArtifact> findBySymbol(String symbol);
}
